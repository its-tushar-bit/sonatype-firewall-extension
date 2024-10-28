/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.thirdparty.SpdxLicenseExpressionUtil;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.OrganizationalEntity;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.license.Expression;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.cyclonedx.model.vulnerability.Vulnerability.Affect;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.LicenseInfoFactory;
import org.spdx.library.model.license.ListedLicenses;

import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.IDENTIFICATION_SOURCES_PROPERTY;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxIdentificationSourceProperty;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxLicenseFromDbData;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxVulnerabilityFromDbData;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.updateCycloneDxVulnerabilityFromDbData;
import static org.spdx.library.SpdxConstants.NON_STD_LICENSE_ID_PRENUM;

public abstract class AbstractCycloneDxExporter
    extends AbstractSbomExporter
{
  protected final MultiLicenseDAO multiLicenseDAO;

  protected final SpdxLicenseExpressionUtil spdxLicenseExpressionUtil;

  protected AbstractCycloneDxExporter(
      final InsightWork insightWork,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService)
  {
    super(insightWork, thirdPartyFileCoordinateDAO, thirdPartyCoordinateSecurityDAO, thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO, baseUrl, idUtils, versionService);
    this.multiLicenseDAO = multiLicenseDAO;
    this.spdxLicenseExpressionUtil = new SpdxLicenseExpressionUtil(multiLicenseDAO);
  }

  protected Bom mergeCurrentDatabaseState(Bom bom) {
    String oldBomComponentRef = "";
    if (bom.getMetadata() != null && bom.getMetadata().getComponent() != null &&
        bom.getMetadata().getComponent().getBomRef() != null) {
      oldBomComponentRef = bom.getMetadata().getComponent().getBomRef();
    }
    generateNewBomMetadata(bom);
    updateDependenciesWithNewBomComponentRef(bom, oldBomComponentRef);

    List<ThirdPartyFileCoordinate> sonatypeComponents = thirdPartyFileCoordinateDAO.getByThirdPartyFileId(
        exportParams.sbomMetadata.getThirdPartyFileId());

    List<Vulnerability> bomVulnerabilitiesList;

    if (CollectionUtils.isEmpty(bom.getVulnerabilities())) {
      bomVulnerabilitiesList = new ArrayList<>();
      bom.setVulnerabilities(bomVulnerabilitiesList);
    }
    else {
      bomVulnerabilitiesList = bom.getVulnerabilities();
    }

    MultiValuedMap<String, Vulnerability> newBomVulnerabilities = new ArrayListValuedHashMap<>();
    if (sonatypeComponents != null) {
      for (ThirdPartyFileCoordinate sonatypeComponent : sonatypeComponents) {
        List<ThirdPartyCoordinateSecurity> sonatypeComponentVulnerabilities = thirdPartyCoordinateSecurityDAO
            .getByFileCoordinateId(sonatypeComponent.getId());
        List<ThirdPartyCoordinateLicense> sonatypeComponentLicenses = thirdPartyCoordinateLicenseDAO
            .getByFileCoordinateId(sonatypeComponent.getId());

        Optional<Component> bomComponentFound = SbomCycloneDxUtils.findComponentByPackageUrl(
            sonatypeComponent.getPackageUrl(), bom);
        if (bomComponentFound.isPresent()) {
          Component bomComponent = bomComponentFound.get();

          // Merge sonatype vulnerabilities into bom
          mergeSonatypeDataVulnerabilities(bomComponent, sonatypeComponentVulnerabilities, bomVulnerabilitiesList,
              newBomVulnerabilities);
          // If no new licenses were recovered from db, skip merge process (left current licenses unaltered)
          if (sonatypeComponentLicenses == null) {
            continue;
          }
          LicenseChoice bomLicenseChoice = getBomComponentLicenses(bomComponent);

          //Merge new licenses here
          updateOrGenerateNewLicenseChoices(sonatypeComponentLicenses, bomLicenseChoice);

          // Final verification on resulting licenses
          if (bomLicenseChoice != null && CollectionUtils.isEmpty(bomLicenseChoice.getLicenses())) {
            // 1.6+ new library won't validate/generate  an empty array of licenses and a null expression on a component
            bomComponent.setLicenses(null);
          }

          if (StringUtils.isNotEmpty(sonatypeComponent.getMatchStateId())) {
            addOrUpdatePropertyInBom(bomComponent, "sonatype:match_state", sonatypeComponent.getMatchStateId());
          }

          if (CollectionUtils.isNotEmpty(sonatypeComponent.getFilenamesList())) {
            addOrUpdatePropertyInBom(bomComponent, "sonatype:match_filenames",
                String.join(",", sonatypeComponent.getFilenamesList()));
          }
        }
      }
    }
    bomVulnerabilitiesList.addAll(newBomVulnerabilities.values());

    //1.6 requires properties tag to be a non-empty array for xml exports
    if (CollectionUtils.isEmpty(bom.getProperties())) {
      bom.setProperties(null);
    }

    return bom;
  }

  private void mergeSonatypeDataVulnerabilities(
      Component bomComponent,
      List<ThirdPartyCoordinateSecurity> sonatypeVulnerabilities,
      List<Vulnerability> bomVulnerabilities,
      MultiValuedMap<String, Vulnerability> newBomVulnerabilities)
  {
    for (ThirdPartyCoordinateSecurity sonatypeVulnerability : sonatypeVulnerabilities) {
      Optional<Vulnerability> vulnerabilityFromBom;
      vulnerabilityFromBom = findMatchingBomVulnerability(bomComponent, bomVulnerabilities, sonatypeVulnerability);

      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation =
          thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(
              sonatypeVulnerability.getId(), sonatypeVulnerability.getRefId());
      if (vulnerabilityFromBom.isPresent()) {
        updateOrSplitExistingVulnerability(bomComponent, sonatypeVulnerability, vulnerabilityFromBom.get(),
            newBomVulnerabilities, sonatypeVexInformation);
      }
      else {
        createNewBomVulnerability(bomComponent, sonatypeVulnerability, newBomVulnerabilities, sonatypeVexInformation);
      }
    }
  }

  private void updateOrSplitExistingVulnerability(
      final Component bomComponent,
      final ThirdPartyCoordinateSecurity sonatypeVulnerability,
      final Vulnerability bomVulnerability,
      final MultiValuedMap<String, Vulnerability> newBomVulnerabilities,
      final ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    List<Affect> affects = bomVulnerability.getAffects();
    if (CollectionUtils.size(affects) == 1 ||
        (sonatypeVexInformation == null && bomVulnerability.getAnalysis() == null)) {
      // there is only 1 affect (which is this component with possibly VEX) or
      // there are multiple affecting components but no existing bom VEX or sonatype VEX
      // just update the vulnerability data
      updateCycloneDxVulnerabilityFromDbData(bomVulnerability, sonatypeVulnerability, sonatypeVexInformation);
      return;
    }
    // there are multiple affects and there is either VEX info in original bom or in sonatype data
    // in all such cases we need to split this to a new vulnerability because of the vex
    affects.removeIf(affect -> affect.getRef().equals(bomComponent.getBomRef()));
    createNewBomVulnerability(bomComponent, sonatypeVulnerability, newBomVulnerabilities, sonatypeVexInformation);
  }

  private void createNewBomVulnerability(
      final Component bomComponent,
      final ThirdPartyCoordinateSecurity sonatypeVulnerability,
      final MultiValuedMap<String, Vulnerability> newBomVulnerabilities,
      final ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation)
  {
    if (sonatypeVexInformation == null) {
      for (Vulnerability vulnerability : newBomVulnerabilities.get(sonatypeVulnerability.getRefId())) {
        if (vulnerability.getAnalysis() == null) {
          //if there's no vex in either (bom, db) we can combine this
          Affect affect = new Affect();
          affect.setRef(bomComponent.getBomRef());
          vulnerability.getAffects().add(affect);
          return;
        }
      }
    }
    Vulnerability newVulnerability =
        createCycloneDxVulnerabilityFromDbData(bomComponent, sonatypeVulnerability, sonatypeVexInformation);
    newBomVulnerabilities.put(newVulnerability.getId(), newVulnerability);
  }

  private Optional<Vulnerability> findMatchingBomVulnerability(
      final Component bomComponent,
      final List<Vulnerability> bomVulnerabilities,
      final ThirdPartyCoordinateSecurity sonatypeVulnerability)
  {
    for (Vulnerability bomVulnerability : bomVulnerabilities) {
      Set<String> affectRefs = bomVulnerability.getAffects().stream().map(Affect::getRef).collect(Collectors.toSet());
      if (affectRefs.contains(bomComponent.getBomRef()) &&
          bomVulnerability.getId().equals(sonatypeVulnerability.getRefId())) {
        return Optional.of(bomVulnerability);
      }
    }
    return Optional.empty();
  }

  private void updateOrGenerateNewLicenseChoices(
      List<ThirdPartyCoordinateLicense> sonatypeComponentLicenses,
      LicenseChoice bomLicenseChoice)
  {
    for (ThirdPartyCoordinateLicense sonatypeComponentLicense : sonatypeComponentLicenses) {
      if (CollectionUtils.isNotEmpty(bomLicenseChoice.getLicenses())) {
        Optional<License> licenseFromBom = bomLicenseChoice.getLicenses().stream()
            .filter(it -> doLicensesMatch(sonatypeComponentLicense, it)).findFirst();
        if (licenseFromBom.isPresent()) {
          updateBomComponentLicenseWithSonatypeData(licenseFromBom.get(), sonatypeComponentLicense);
          continue;
        }
      }
      else {
        bomLicenseChoice.setLicenses(new ArrayList<>());
      }
      bomLicenseChoice.addLicense(createCycloneDxLicenseFromDbData(sonatypeComponentLicense));
    }
  }

  private boolean doLicensesMatch(ThirdPartyCoordinateLicense sonatypeComponentLicense, License bomComponentLicense) {
    return bomComponentLicense.getId() != null && bomComponentLicense.getId().equals(sonatypeComponentLicense
        .getLicenseId()) || (bomComponentLicense.getName() != null && bomComponentLicense.getName()
        .equals(sonatypeComponentLicense.getName()));
  }

  private LicenseChoice getBomComponentLicenses(Component bomComponent) {
    if (bomComponent.getLicenses() == null) {
      // Initialize proper empty data structures for holding licenses to avoid null exceptions
      LicenseChoice licenseChoice = new LicenseChoice();
      licenseChoice.setLicenses(Collections.emptyList());
      bomComponent.setLicenses(licenseChoice);
    }
    else if (bomComponent.getLicenses().getExpression() != null &&
        StringUtils.isNotEmpty(bomComponent.getLicenses().getExpression().getValue())) {
      Expression bomComponentLicenseExpression = bomComponent.getLicenses().getExpression();
      bomComponent.getLicenses().setLicenses(new ArrayList<>());
      String purl = bomComponent.getPurl() != null ? bomComponent.getPurl() : "";
      bomComponent.getLicenses().getLicenses().addAll(
          parseLicenseChoiceExpression(bomComponentLicenseExpression.getValue(), purl));
    }
    return bomComponent.getLicenses();
  }

  protected List<License> parseLicenseChoiceExpression(String expression, String purl) {
    List<License> licenses = new ArrayList<>();
    try {
      AnyLicenseInfo anyLicenseInfo = LicenseInfoFactory.parseSPDXLicenseString(expression);
      Map<String, String> processedLicenses = new HashMap<>();
      spdxLicenseExpressionUtil.parseLicenses(anyLicenseInfo, processedLicenses, purl);
      for (String licenseId : processedLicenses.keySet()) {
        License processedLicense = new License();
        if (ListedLicenses.getListedLicenses().isSpdxListedLicenseId(licenseId)) {
          processedLicense.setId(licenseId);
        }
        else {
          processedLicense.setName(licenseId.replaceAll(NON_STD_LICENSE_ID_PRENUM, ""));
        }

        if (StringUtils.isEmpty(processedLicense.getBomRef())) {
          processedLicense.setBomRef(UUID.randomUUID().toString());
        }

        licenses.add(processedLicense);
      }
    }
    catch (InvalidSPDXAnalysisException e) {
      log.debug("Failed to process spdx license string: {}", expression);
    }
    return licenses;
  }

  private void updateBomComponentLicenseWithSonatypeData(
      License bomLicense,
      ThirdPartyCoordinateLicense sonatypeComponentLicense)
  {
    if (sonatypeComponentLicense.getUrl() != null) {
      bomLicense.setUrl(sonatypeComponentLicense.getUrl());
    }

    if (CollectionUtils.isEmpty(bomLicense.getProperties())) {
      if (StringUtils.isNotEmpty(sonatypeComponentLicense.getIdentificationSources())) {
        bomLicense.setProperties(Collections.singletonList(createCycloneDxIdentificationSourceProperty(
            sonatypeComponentLicense.getIdentificationSources())));
      }
    }
    else {
      Optional<Property> identificationSources = bomLicense.getProperties().stream().filter(
          property -> property.getName().equals(IDENTIFICATION_SOURCES_PROPERTY)).findFirst();
      if (identificationSources.isPresent()) {
        identificationSources.get().setValue(sonatypeComponentLicense.getIdentificationSources());
      }
      else {
        bomLicense.getProperties().add(createCycloneDxIdentificationSourceProperty(
            sonatypeComponentLicense.getIdentificationSources()));
      }
    }
  }

  private void generateNewBomMetadata(Bom bom) {
    Metadata newBomMetadata = new Metadata();
    newBomMetadata.setTimestamp(new Date());

    ToolInformation toolInformation = new ToolInformation();
    Component generatorToolComponent = new Component();
    generatorToolComponent.setType(Type.APPLICATION);
    generatorToolComponent.setName("Sonatype SBOM Manager");
    generatorToolComponent.setVersion(versionService.getFullVersion());
    toolInformation.setComponents(Collections.singletonList(generatorToolComponent));
    newBomMetadata.setToolChoice(toolInformation);

    OrganizationalEntity organizationalEntity = new OrganizationalEntity();
    organizationalEntity.setName("Sonatype Inc.");
    organizationalEntity.setUrls(Collections.singletonList("https://www.sonatype.com/"));

    Component bomComponentInfo = new Component();
    bomComponentInfo.setType(Type.APPLICATION);
    bomComponentInfo.setName(idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata
        .getApplicationId()));
    bomComponentInfo.setVersion(exportParams.sbomMetadata.getSbomVersion());
    bomComponentInfo.setBomRef(UUID.randomUUID().toString());
    newBomMetadata.setComponent(bomComponentInfo);
    bom.setMetadata(newBomMetadata);
  }

  // Since we overwrote the original metadata and set a new parent component
  // we need to update the dependency tree with the new parent component ref
  private void updateDependenciesWithNewBomComponentRef(Bom bom, String oldBomComponentRef) {
    if (StringUtils.isNotEmpty(oldBomComponentRef)) {
      String newBomComponentRef = bom.getMetadata().getComponent().getBomRef();
      if (CollectionUtils.isNotEmpty(bom.getDependencies())) {
        Optional<Dependency> rootDependencyOptional = bom.getDependencies().stream()
            .filter(it -> it.getRef().equals(oldBomComponentRef)).findFirst();
        if (rootDependencyOptional.isPresent()) {
          Dependency rootDependency = rootDependencyOptional.get();
          int rootDependencyIndex = bom.getDependencies().indexOf(rootDependency);
          Dependency newRootDependency = new Dependency(newBomComponentRef);
          newRootDependency.setDependencies(rootDependency.getDependencies());
          bom.getDependencies().set(rootDependencyIndex, newRootDependency);
        }
      }
    }
  }

  private void addOrUpdatePropertyInBom(Component bomComponent, String propName, String propValue) {
    Property bomProperty = new Property();
    bomProperty.setName(propName);
    bomProperty.setValue(propValue);
    if (CollectionUtils.isNotEmpty(bomComponent.getProperties())) {
      bomComponent.getProperties().stream()
          .filter(p -> p.getName().equals(bomProperty.getName()))
          .findFirst()
          .ifPresentOrElse(p -> p.setValue(bomProperty.getValue()), () -> bomComponent.addProperty(bomProperty));
    }
    else {
      bomComponent.addProperty(bomProperty);
    }
  }
}
