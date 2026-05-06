/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.SbomIdentityUtils;
import com.sonatype.insight.SbomTaxonomy;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.report.pdf.PdfData;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentLicense;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentLicenseThreat;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentPolicyViolation;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentSecurityIssue;
import com.sonatype.insight.brain.sbom.components.BomPageMetadataDTO;
import com.sonatype.insight.brain.sbom.license.ThirdPartyComponentLicenseResolutionService;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.thirdparty.SpdxLicenseExpressionUtil;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionService;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang.WordUtils;
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

import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.addOrUpdateBomElementProperty;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxLicenseForResolvedLicense;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxVulnerabilityFromDbData;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.updateCycloneDxLegacyPropertyIfPresent;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.updateCycloneDxVulnerabilityFromDbData;
import static org.spdx.library.SpdxConstants.NON_STD_LICENSE_ID_PRENUM;

public abstract class AbstractCycloneDxExporter
    extends AbstractSbomExporter
{
  protected final MultiLicenseDAO multiLicenseDAO;

  protected final SpdxLicenseExpressionUtil spdxLicenseExpressionUtil;

  protected final ApiReportDataServiceV2 apiReportDataServiceV2;

  protected final ThirdPartyScanDAO thirdPartyScanDAO;

  protected final ApplicationDAO applicationDAO;

  protected final MigrationTrackerDAO migrationTrackerDAO;

  protected static final String REPORT_NAME = "Compliance Report";

  private static final String IDENTIFICATION_SOURCE_SONATYPE_CONTAINER =
      IdentificationSource.SONATYPE_CONTAINER.getName();

  protected AbstractCycloneDxExporter(
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ApplicationDAO applicationDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final MigrationTrackerDAO migrationTrackerDAO,
      final BaseUrl baseUrl,
      final IdUtils idUtils,
      final VersionService versionService,
      final ApiReportDataServiceV2 apiReportDataServiceV2,
      final ThirdPartyComponentLicenseResolutionService thirdPartyLicenseResolver,
      final ThirdPartyPersistenceService thirdPartyPersistenceService)
  {
    super(
        thirdPartyFileDAO,
        thirdPartyFileCoordinateDAO,
        thirdPartyCoordinateSecurityDAO,
        thirdPartyCoordinateLicenseDAO,
        thirdPartyVulnerabilityExploitabilityExchangeDAO,
        baseUrl,
        idUtils,
        versionService,
        thirdPartyLicenseResolver,
        thirdPartyPersistenceService);
    this.multiLicenseDAO = multiLicenseDAO;
    this.spdxLicenseExpressionUtil = new SpdxLicenseExpressionUtil(multiLicenseDAO);
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.applicationDAO = applicationDAO;
    this.migrationTrackerDAO = migrationTrackerDAO;
  }

  protected Bom mergeCurrentDatabaseState(Bom bom, Map<String, Component> componentRefToComponents) {
    String oldBomComponentRef = "";
    if (bom.getMetadata() != null && bom.getMetadata().getComponent() != null &&
        bom.getMetadata().getComponent().getBomRef() != null)
    {
      oldBomComponentRef = bom.getMetadata().getComponent().getBomRef();
    }
    generateNewBomMetadata(bom);
    updateDependenciesWithNewBomComponentRef(bom, oldBomComponentRef);
    Map<String, Component> componentRefToBomComponentsMap =
        componentRefToComponents == null ? generateComponentRefMap(bom) : componentRefToComponents;
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
    if (CollectionUtils.isNotEmpty(sonatypeComponents)) {
      Map<String, List<ThirdPartyCoordinateSecurity>> vulnerabilities = thirdPartyCoordinateSecurityDAO
          .getByFileCoordinateIds(sonatypeComponents.stream().map(ThirdPartyFileCoordinate::getId).toList())
          .stream()
          .collect(Collectors.groupingBy(ThirdPartyCoordinateSecurity::getFileCoordinateId));
      Application app = applicationDAO.getByIdNotNull(exportParams.sbomMetadata.getApplicationId());

      Map<String, List<ThirdPartyVulnerabilityExploitabilityExchange>> vexByCoordinateSecurityId =
          prefetchVexData(vulnerabilities);

      for (ThirdPartyFileCoordinate sonatypeComponent : sonatypeComponents) {
        List<ThirdPartyCoordinateSecurity> sonatypeComponentVulnerabilities = vulnerabilities
            .getOrDefault(sonatypeComponent.getId(), Collections.emptyList());
        Set<ResolvedLicenseDTO> resolvedLicenseDTOS =
            thirdPartyLicenseResolver.resolveLicenseOverridesOrThirdPartyLicenses(
                app, sonatypeComponent);

        Optional<Component> bomComponentFound = Optional.empty();
        Component componentByComponentRef = componentRefToBomComponentsMap.get(sonatypeComponent.getComponentRef());
        if (componentByComponentRef != null) {
          bomComponentFound = Optional.of(componentByComponentRef);
        }
        else if (sonatypeComponent.getComponentRef() == null) {
          bomComponentFound = SbomCycloneDxUtils.findComponentByPackageUrl(sonatypeComponent.getPackageUrl(), bom);
        }

        if (bomComponentFound.isPresent()) {
          Component bomComponent = bomComponentFound.get();

          // Add or update bom component properties with sonatype data
          bomComponent.setProperties(addOrUpdateBomElementProperty(bomComponent.getProperties(),
              SbomTaxonomy.CDX_MATCH_STATE_PROPERTY_NAME, sonatypeComponent.getMatchStateId()));

          if (CollectionUtils.isNotEmpty(sonatypeComponent.getFilenamesList())) {
            bomComponent.setProperties(addOrUpdateBomElementProperty(bomComponent.getProperties(),
                SbomTaxonomy.CDX_MATCH_FILENAMES_PROPERTY_NAME,
                String.join(",", sonatypeComponent.getFilenamesList())));
          }
          String identificationSource = sonatypeComponent.getSource().equals(IDENTIFICATION_SOURCE_SONATYPE_CONTAINER)
              ? IDENTIFICATION_SOURCE_SONATYPE_CONTAINER
              : sonatypeComponent.getIdentificationSources();
          bomComponent.setProperties(addOrUpdateBomElementProperty(bomComponent.getProperties(),
              SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME, identificationSource));

          bomComponent.setProperties(addOrUpdateBomElementProperty(bomComponent.getProperties(),
              SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME, sonatypeComponent.getHash()));

          // Add original purl if not set and the original bom component has a purl value
          Property p = findPropertyWithName(bomComponent, SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME);
          if (p == null && StringUtils.isNotEmpty(bomComponent.getPurl())) {
            bomComponent.setProperties(addOrUpdateBomElementProperty(bomComponent.getProperties(),
                SbomTaxonomy.CDX_ORIGINAL_PURL_PROPERTY_NAME, bomComponent.getPurl()));
          }

          // Merge sonatype vulnerabilities into bom
          mergeSonatypeDataVulnerabilities(bomComponent, sonatypeComponentVulnerabilities, bomVulnerabilitiesList,
              newBomVulnerabilities, vexByCoordinateSecurityId);

          // If no new licenses were recovered from db, skip merge process (left current licenses unaltered)
          // Update any legacy property names in the current licenses
          if (CollectionUtils.isEmpty(resolvedLicenseDTOS)) {
            if (bomComponent.getLicenses() != null &&
                CollectionUtils.isNotEmpty(bomComponent.getLicenses().getLicenses()))
            {
              for (License license : bomComponent.getLicenses().getLicenses()) {
                updateCycloneDxLegacyPropertyIfPresent(license.getProperties(),
                    SbomTaxonomy.CDX_IDENTIFICATION_SOURCES_PROPERTY_NAME, null);
              }
            }
            continue;
          }
          LicenseChoice bomLicenseChoice = getBomComponentLicenses(bomComponent);

          // Merge new licenses here
          updateOrGenerateNewLicenseChoices(resolvedLicenseDTOS, bomLicenseChoice);

          // Final verification on resulting licenses
          if (bomLicenseChoice != null && CollectionUtils.isEmpty(bomLicenseChoice.getLicenses())) {
            // 1.6+ new library won't validate/generate an empty array of licenses and a null expression on a component
            bomComponent.setLicenses(null);
          }
        }
      }
    }
    bomVulnerabilitiesList.addAll(newBomVulnerabilities.values());

    // 1.6 requires properties tag to be a non-empty array for xml exports
    if (CollectionUtils.isEmpty(bom.getProperties())) {
      bom.setProperties(null);
    }

    return bom;
  }

  private Map<String, List<ThirdPartyVulnerabilityExploitabilityExchange>> prefetchVexData(
      Map<String, List<ThirdPartyCoordinateSecurity>> vulnerabilitiesByFileCoordinate)
  {
    Set<String> allCoordinateSecurityIds = vulnerabilitiesByFileCoordinate.values()
        .stream()
        .flatMap(List::stream)
        .map(ThirdPartyCoordinateSecurity::getId)
        .collect(Collectors.toSet());
    if (allCoordinateSecurityIds.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, List<ThirdPartyVulnerabilityExploitabilityExchange>> result = new HashMap<>();
    thirdPartyVulnerabilityExploitabilityExchangeDAO
        .getListByCoordinateSecurityIds(allCoordinateSecurityIds)
        .forEach(vex -> result
            .computeIfAbsent(vex.getCoordinateSecurityId(), k -> new ArrayList<>())
            .add(vex));
    return result;
  }

  protected Bom mergeCurrentDatabaseState(Bom bom) {
    return mergeCurrentDatabaseState(bom, null);
  }

  private Map<String, Component> generateComponentRefMap(Bom bom) {
    Map<String, Component> componentRefMap = new HashMap<>();
    bom.getComponents().forEach(component -> {
      String componentRef = SbomIdentityUtils.getComponentRef(component);
      if (componentRef != null) {
        componentRefMap.put(componentRef, component);
      }
    });
    return componentRefMap;
  }

  private void mergeSonatypeDataVulnerabilities(
      Component bomComponent,
      List<ThirdPartyCoordinateSecurity> sonatypeVulnerabilities,
      List<Vulnerability> bomVulnerabilities,
      MultiValuedMap<String, Vulnerability> newBomVulnerabilities,
      Map<String, List<ThirdPartyVulnerabilityExploitabilityExchange>> vexByCoordinateSecurityId)
  {
    for (ThirdPartyCoordinateSecurity sonatypeVulnerability : sonatypeVulnerabilities) {
      Optional<Vulnerability> vulnerabilityFromBom;
      vulnerabilityFromBom = findMatchingBomVulnerability(bomComponent, bomVulnerabilities, sonatypeVulnerability);

      ThirdPartyVulnerabilityExploitabilityExchange sonatypeVexInformation =
          findVexByCoordinateSecurityIdAndRefId(vexByCoordinateSecurityId,
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

  private static ThirdPartyVulnerabilityExploitabilityExchange findVexByCoordinateSecurityIdAndRefId(
      Map<String, List<ThirdPartyVulnerabilityExploitabilityExchange>> vexByCoordinateSecurityId,
      String coordinateSecurityId,
      String refId)
  {
    if (refId == null) {
      return null;
    }
    List<ThirdPartyVulnerabilityExploitabilityExchange> candidates =
        vexByCoordinateSecurityId.getOrDefault(coordinateSecurityId, Collections.emptyList());
    for (ThirdPartyVulnerabilityExploitabilityExchange vex : candidates) {
      if (refId.equals(vex.getRefId())) {
        return vex;
      }
    }
    return null;
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
        (sonatypeVexInformation == null && bomVulnerability.getAnalysis() == null))
    {
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
          // if there's no vex in either (bom, db) we can combine this
          Affect affect = SbomExportUtils.newAffectLinkingComponent(bomComponent);
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
          bomVulnerability.getId().equals(sonatypeVulnerability.getRefId()))
      {
        return Optional.of(bomVulnerability);
      }
    }
    return Optional.empty();
  }

  private void updateOrGenerateNewLicenseChoices(
      Set<ResolvedLicenseDTO> resolvedLicenses,
      LicenseChoice bomLicenseChoice)
  {
    boolean resetLicenseChoice = false;
    for (ResolvedLicenseDTO resolvedLicense : resolvedLicenses) {
      // in case we have overridden licenses for this component we should only export the overridden licenses
      // so need to reset/empty any existing collection once if there are
      if (CollectionUtils.isEmpty(bomLicenseChoice.getLicenses()) ||
          (!resetLicenseChoice && resolvedLicense.overrideStatus() != null))
      {
        resetLicenseChoice = true;
        bomLicenseChoice.setLicenses(new ArrayList<>());
      }

      // merge only if not overridden
      if (resolvedLicense.overrideStatus() == null) {
        Optional<License> licenseFromBom = bomLicenseChoice.getLicenses()
            .stream()
            .filter(it -> doLicensesMatch(resolvedLicense, it))
            .findFirst();
        if (licenseFromBom.isPresent()) {
          SbomExportUtils.updateCycloneDxLicenseAttributes(licenseFromBom.get(), resolvedLicense.licenseUrl(),
              resolvedLicense.identificationSources());
          continue;
        }
      }
      bomLicenseChoice.addLicense(createCycloneDxLicenseForResolvedLicense(resolvedLicense));
    }
  }

  private boolean doLicensesMatch(ResolvedLicenseDTO sonatypeComponentLicense, License bomComponentLicense) {
    return bomComponentLicense.getId() != null && bomComponentLicense.getId()
        .equals(sonatypeComponentLicense
            .licenseId())
        || (bomComponentLicense.getName() != null && bomComponentLicense.getName()
            .equals(sonatypeComponentLicense.licenseName()));
  }

  private LicenseChoice getBomComponentLicenses(Component bomComponent) {
    if (bomComponent.getLicenses() == null) {
      // Initialize proper empty data structures for holding licenses to avoid null exceptions
      LicenseChoice licenseChoice = new LicenseChoice();
      licenseChoice.setLicenses(Collections.emptyList());
      bomComponent.setLicenses(licenseChoice);
    }
    else if (bomComponent.getLicenses().getExpression() != null &&
        StringUtils.isNotEmpty(bomComponent.getLicenses().getExpression().getValue()))
    {
      Expression bomComponentLicenseExpression = bomComponent.getLicenses().getExpression();
      bomComponent.getLicenses().setLicenses(new ArrayList<>());
      String purl = bomComponent.getPurl() != null ? bomComponent.getPurl() : "";
      bomComponent.getLicenses()
          .getLicenses()
          .addAll(
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

  private void generateNewBomMetadata(Bom bom) {
    Metadata newBomMetadata = new Metadata();
    newBomMetadata.setTimestamp(new Date());
    String binaryFileName = exportParams.sbomMetadata.getOriginalBinaryFileName();
    if (StringUtils.isNotBlank(binaryFileName)) {
      newBomMetadata.setProperties(addOrUpdateBomElementProperty(newBomMetadata.getProperties(),
          SbomTaxonomy.CDX_ORIGINAL_FILE_PROPERTY_NAME, binaryFileName));
    }

    ToolInformation toolInformation = new ToolInformation();
    toolInformation.setComponents(Collections.singletonList(createComponent(
        "Sonatype SBOM Manager", versionService.getFullVersion())));
    newBomMetadata.setToolChoice(toolInformation);

    OrganizationalEntity organizationalEntity = new OrganizationalEntity();
    organizationalEntity.setName("Sonatype Inc.");
    organizationalEntity.setUrls(Collections.singletonList("https://www.sonatype.com/"));

    Component bomComponentInfo = createComponent(
        idUtils.getPublicOwnerId(OwnerType.APPLICATION, exportParams.sbomMetadata.getApplicationId()),
        exportParams.sbomMetadata.getSbomVersion());
    bomComponentInfo.setBomRef(UUID.randomUUID().toString());
    newBomMetadata.setComponent(bomComponentInfo);
    bom.setMetadata(newBomMetadata);
  }

  private Component createComponent(String name, String version) {
    Component component = new Component();
    component.setType(Type.APPLICATION);
    component.setName(name);
    component.setVersion(version);
    return component;
  }

  // Since we overwrote the original metadata and set a new parent component
  // we need to update the dependency tree with the new parent component ref
  private void updateDependenciesWithNewBomComponentRef(Bom bom, String oldBomComponentRef) {
    if (StringUtils.isNotEmpty(oldBomComponentRef)) {
      String newBomComponentRef = bom.getMetadata().getComponent().getBomRef();
      if (CollectionUtils.isNotEmpty(bom.getDependencies())) {
        Optional<Dependency> rootDependencyOptional = bom.getDependencies()
            .stream()
            .filter(it -> it.getRef().equals(oldBomComponentRef))
            .findFirst();
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

  protected PdfData convertToPdfData(Bom bom) {
    PdfData pdfData = new PdfData();
    pdfData.baseUrl = getBaseUrl();
    pdfData.title = getTitle();
    pdfData.createdDate = new Date();
    pdfData.analyzedDate = bom.getMetadata().getTimestamp();
    pdfData.productVersion = versionService.getShortVersion();
    pdfData.sbomMetadata = buildSbomMetadataDTO();
    MultiValuedMap<String, PdfComponentPolicyViolation> policyViolationsByPurlAndHash = getPolicyViolationsData();
    Map<String, List<ApiLicenseThreatDTOV2>> effectiveLicenseThreatsByPurl = mapEffectiveLicenseThreatsByPurl();

    pdfData.components = new ArrayList<>();
    MultiValuedMap<String, Vulnerability> vulnerabilitiesByBomRefs = getVulnerabilitiesMappedPerAffect(bom);

    if (bom.getComponents() != null) {
      for (Component component : bom.getComponents()) {
        // Although unlikely, it is possible to have duplicate components in an original SBOM with no bom-refs.
        // (see SBOM-1553)
        // In such cases SBOM Manager only keeps track of only 1 component for a given unique identity (purl/hash/etc)
        // and we should make sure only to use that component for PDF export. (the one with the sonatype truncated sha1)
        if (StringUtils.isNotBlank(SbomCycloneDxUtils.getSonatypeTruncatedSha1(component))) {
          PdfData.PdfComponent pdfComponent = new PdfData.PdfComponent();
          pdfComponent.displayName = getComponentDisplayName(component);
          pdfComponent.matchState = getComponentMatchState(component);
          pdfComponent.policyViolations = getPdfComponentPolicyViolations(component, policyViolationsByPurlAndHash);
          pdfComponent.securityIssues = getSecurityIssuesData(component, vulnerabilitiesByBomRefs);
          pdfComponent.effectiveLicenses = getLicensesData(component);
          pdfComponent.effectiveLicenseThreats = getEffectiveLicenseThreats(component, effectiveLicenseThreatsByPurl);
          pdfData.components.add(pdfComponent);
        }
      }
    }
    return pdfData;
  }

  private static MultiValuedMap<String, Vulnerability> getVulnerabilitiesMappedPerAffect(final Bom bom) {
    MultiValuedMap<String, Vulnerability> vulnerabilitiesPerAffect = new ArrayListValuedHashMap<>();
    List<Vulnerability> vulnerabilities = Optional.ofNullable(bom.getVulnerabilities()).orElse(Collections.emptyList());
    for (Vulnerability vulnerability : vulnerabilities) {
      for (Affect affect : vulnerability.getAffects()) {
        vulnerabilitiesPerAffect.put(affect.getRef(), vulnerability);
      }
    }
    return vulnerabilitiesPerAffect;
  }

  private Map<String, List<ApiLicenseThreatDTOV2>> mapEffectiveLicenseThreatsByPurl() {
    Map<String, List<ApiLicenseThreatDTOV2>> result = new HashMap<>();
    ApiReportRawDataDTOV2 reportRawData = exportParams.getReportRawData();
    if (reportRawData == null || CollectionUtils.isEmpty(reportRawData.components)) {
      return result;
    }

    for (ApiReportComponentDTOV2 component : reportRawData.components) {
      if (component.packageUrl != null && component.licenseData != null &&
          component.licenseData.effectiveLicenseThreats != null)
      {
        result.put(component.packageUrl, component.licenseData.effectiveLicenseThreats);
      }
    }
    return result;
  }

  private List<PdfComponentLicenseThreat> getEffectiveLicenseThreats(
      final Component component,
      Map<String, List<ApiLicenseThreatDTOV2>> effectiveLicenseThreatsByPurl)
  {
    List<PdfComponentLicenseThreat> result = new ArrayList<>();
    List<ApiLicenseThreatDTOV2> ltgs =
        effectiveLicenseThreatsByPurl.getOrDefault(component.getPurl(), Collections.emptyList());
    for (ApiLicenseThreatDTOV2 ltg : ltgs) {
      PdfComponentLicenseThreat componentLicenseThreat = new PdfComponentLicenseThreat();
      componentLicenseThreat.licenseThreatGroupLevel = ltg.licenseThreatGroupLevel;
      result.add(componentLicenseThreat);
    }
    return result;
  }

  protected BomPageMetadataDTO buildSbomMetadataDTO() {
    ThirdPartySbomMetadata metadataEntity = exportParams.sbomMetadata;
    ThirdPartyScan scanEntity = getThirdPartyScan();
    try {
      return SbomCycloneDxUtils.buildBomPageMetadataDTO(metadataEntity, scanEntity, migrationTrackerDAO);
    }
    catch (IllegalStateException e) {
      // in a most unlikely event of malformed metadata json
      log.debug("Failed to parse sbom metadata json for application {}, version {}, and scanId {}",
          metadataEntity.getApplicationId(), metadataEntity.getSbomVersion(),
          metadataEntity.getThirdPartyFileId());
      return null;
    }
  }

  private String getComponentDisplayName(final Component component) {
    if (component.getPurl() != null) {
      ComponentDisplayName displayName = ComponentDisplayNameUtil.fromIdentifier(
          new PackageUrlIdentifier(component.getPurl()).toComponentIdentifier());
      if (displayName != null) {
        return displayName.toString();
      }
    }
    return component.getName();
  }

  private List<PdfComponentPolicyViolation> getPdfComponentPolicyViolations(
      Component component,
      MultiValuedMap<String, PdfComponentPolicyViolation> policyViolationsByPurlAndHash)
  {
    List<PdfComponentPolicyViolation> componentPolicyViolations = new ArrayList<>();
    if (component.getPurl() != null) {
      componentPolicyViolations.addAll(policyViolationsByPurlAndHash.get(component.getPurl()));
    }
    if (org.apache.commons.collections4.CollectionUtils.isEmpty(componentPolicyViolations)) {
      String componentHash = getComponentHash(component);
      if (componentHash != null) {
        componentPolicyViolations.addAll(policyViolationsByPurlAndHash.get(componentHash));
      }
    }
    return componentPolicyViolations;
  }

  private String getComponentMatchState(final Component component) {
    Property prop = findPropertyWithName(component, SbomTaxonomy.CDX_MATCH_STATE_PROPERTY_NAME);
    if (prop != null) {
      return prop.getValue();
    }
    return null;
  }

  private String getComponentHash(final Component component) {
    Property prop = findPropertyWithName(component, SbomTaxonomy.CDX_SONATYPE_SHA1_PROPERTY_NAME);
    if (prop != null) {
      return prop.getValue();
    }
    // fallback to legacy prop
    prop = findPropertyWithName(component, SbomTaxonomy.LEGACY_SONATYPE_SHA1_PROPERTY_NAME);
    if (prop != null) {
      return prop.getValue();
    }
    return null;
  }

  private Property findPropertyWithName(Component component, String name) {
    List<Property> properties = component.getProperties();
    if (properties != null) {
      for (Property property : properties) {
        if (StringUtils.equals(property.getName(), name)) {
          return property;
        }
      }
    }
    return null;
  }

  private List<PdfComponentLicense> getLicensesData(final Component component) {
    LicenseChoice licenseChoice = component.getLicenses();
    if (licenseChoice != null &&
        org.apache.commons.collections4.CollectionUtils.isNotEmpty(licenseChoice.getLicenses()))
    {
      return licenseChoice.getLicenses()
          .stream()
          .map(l -> {
            PdfComponentLicense lic = new PdfComponentLicense();
            if (StringUtils.isNotEmpty(l.getName())) {
              lic.name = l.getName();
            }
            else {
              lic.name = l.getId();
            }
            return lic;
          })
          .collect(Collectors.toList());
    }
    else {
      return Collections.emptyList();
    }
  }

  private List<PdfComponentSecurityIssue> getSecurityIssuesData(
      final Component component,
      final MultiValuedMap<String, Vulnerability> vulnerabilitiesByBomRefs)
  {
    if (StringUtils.isEmpty(component.getBomRef())) {
      return Collections.emptyList();
    }

    Collection<Vulnerability> vulns = vulnerabilitiesByBomRefs.get(component.getBomRef());
    if (CollectionUtils.isEmpty(vulns)) {
      return Collections.emptyList();
    }

    List<PdfComponentSecurityIssue> pdfVulns = new ArrayList<>();
    for (Vulnerability vulnerability : vulns) {
      Optional.ofNullable(vulnerability.getRatings())
          .orElse(Collections.emptyList())
          .stream()
          .filter(rating -> rating.getScore() != null)
          .findFirst()
          .ifPresent(rating -> {
            PdfComponentSecurityIssue issue = new PdfComponentSecurityIssue();
            issue.reference = vulnerability.getId();
            issue.severity = rating.getScore().floatValue();
            issue.analysisState = getAnalysisState(vulnerability);
            pdfVulns.add(issue);
          });
    }
    return pdfVulns;
  }

  private String getAnalysisState(Vulnerability vulnerability) {
    if (vulnerability.getAnalysis() != null && vulnerability.getAnalysis().getState() != null) {
      return WordUtils.capitalizeFully(vulnerability.getAnalysis().getState().getStateName().replace("_", " "));
    }
    else {
      return "Unannotated";
    }
  }

  /**
   * Creates a multi-valued map that contains a mapping of component hash or purl to the policy violations data. The key
   * can be either a hash or a purl, or both pointing to the same violation. This helps to provide a fallback if in case
   * no match for purl to match based on hash
   */
  private MultiValuedMap<String, PdfComponentPolicyViolation> getPolicyViolationsData() {
    MultiValuedMap<String, PdfComponentPolicyViolation> mapped = new ArrayListValuedHashMap<>();
    ApiReportPolicyDataDTOV2 data = exportParams.getPolicyData();
    if (data == null) {
      Application app = getApplication();
      ThirdPartyScan tpScan = getThirdPartyScan();
      if (tpScan != null) {
        try {
          data = apiReportDataServiceV2.getPolicyViolationsDataNoAuth(app.getPublicId(), tpScan.getScanId(), false);
        }
        catch (Exception e) {
          log.debug("Failed to get policy violations data for application {} and scanId {}",
              app.getPublicId(), tpScan.getScanId(), e);
        }
      }
    }
    if (data != null && data.components != null) {
      for (ApiReportComponentPolicyViolationsDTOV2 reportViolationDto : data.components) {
        for (ApiReportPolicyViolationDTOV2 reportPolicyViolation : reportViolationDto.violations) {
          PdfComponentPolicyViolation pdfViolation = new PdfComponentPolicyViolation();
          pdfViolation.policyThreatLevel = reportPolicyViolation.policyThreatLevel;
          pdfViolation.policyName = reportPolicyViolation.policyName;
          pdfViolation.policyThreatCategory = reportPolicyViolation.policyThreatCategory;
          pdfViolation.waived = reportPolicyViolation.waived;
          pdfViolation.legacyViolation = reportPolicyViolation.legacyViolation;
          if (reportViolationDto.componentIdentifier != null) {
            String purl = PackageUrlIdentifier.fromComponentIdentifier(
                reportViolationDto.componentIdentifier.toComponentIdentifier()).getPackageUrl();
            mapped.put(purl, pdfViolation);
          }
          mapped.put(reportViolationDto.hash, pdfViolation);
        }
      }
    }
    return mapped;
  }

  private Application getApplication() {
    return applicationDAO.getById(exportParams.sbomMetadata.getApplicationId());
  }

  private String getTitle() {
    Application application = getApplication();
    return String.join(" ",
        Optional.ofNullable(application)
            .map(Application::getName)
            .orElse(""),
        REPORT_NAME).trim();
  }

  private ThirdPartyScan getThirdPartyScan() {
    return thirdPartyScanDAO.getByThirdPartyFileId(exportParams.sbomMetadata.getThirdPartyFileId());
  }

  protected void cleanupLegacyVulnerabilitiesFromBomComponents(Bom bom) {
    if (bom.getComponents() != null) {
      bom.getComponents()
          .stream()
          .filter(c -> c.getExtensions() != null && c.getExtensions().containsKey("vulnerabilities"))
          .forEach(c -> c.getExtensions().remove("vulnerabilities"));
    }
    else {
      bom.setComponents(Collections.emptyList());
    }
  }
}
