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
import java.util.UUID;

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
          mergeSonatypeDataVulnerabilities(bomComponent, sonatypeComponentVulnerabilities, bomVulnerabilitiesList);
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

          if ( StringUtils.isNotEmpty(sonatypeComponent.getMatchStateId())) {
            Property pSimilar = new Property();
            pSimilar.setName("sonatype:match_state");
            pSimilar.setValue(sonatypeComponent.getMatchStateId());
            if (CollectionUtils.isNotEmpty(bomComponent.getProperties())) {
              bomComponent.getProperties().stream()
                  .filter(p -> p.getName().equals(pSimilar.getName()))
                  .findFirst()
                  .ifPresentOrElse(p -> p.setValue(pSimilar.getValue()), () -> bomComponent.addProperty(pSimilar));
            }
            else {
              bomComponent.addProperty(pSimilar);
            }
          }
        }
      }
    }

    //1.6 requires properties tag to be a non-empty array for xml exports
    if (CollectionUtils.isEmpty(bom.getProperties())) {
      bom.setProperties(null);
    }

    return bom;
  }

  private void mergeSonatypeDataVulnerabilities(
      Component bomComponent,
      List<ThirdPartyCoordinateSecurity> sonatypeVulnerabilities,
      List<Vulnerability> bomVulnerabilities)
  {
    List<Vulnerability> newBomVulnerabilities = new ArrayList<>();
    for (ThirdPartyCoordinateSecurity sonatypeVulnerability : sonatypeVulnerabilities) {

      Optional<Vulnerability> vulnerabilityFromBom = Optional.empty();

      if (bomVulnerabilities != null) {
        vulnerabilityFromBom = bomVulnerabilities.stream()
            .filter((Vulnerability bomVulnerability) -> bomVulnerability.getId()
                .equals(sonatypeVulnerability.getRefId())).findAny();
      }

      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation =
          thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(
              sonatypeVulnerability.getId(), sonatypeVulnerability.getRefId());
      if (vulnerabilityFromBom.isPresent()) {
        updateCycloneDxVulnerabilityFromDbData(vulnerabilityFromBom.get(), sonatypeVulnerability,
            sonatypeVexInformation);
      }
      else {
        newBomVulnerabilities.add(createCycloneDxVulnerabilityFromDbData(bomComponent, sonatypeVulnerability,
                sonatypeVexInformation));
      }
    }

    if (!newBomVulnerabilities.isEmpty()) {
      // If there are new vulnerabilities not present in the bom document, add them
      bomVulnerabilities.addAll(newBomVulnerabilities);
    }
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
          parseLicenseChoiceExpression(bomComponentLicenseExpression.getValue(), purl, bomComponent.getBomRef()));
    }
    return bomComponent.getLicenses();
  }

  protected List<License> parseLicenseChoiceExpression(String expression, String purl, String bomRef) {
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

        if (bomRef != null) {
          processedLicense.setBomRef(bomRef);
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
}
