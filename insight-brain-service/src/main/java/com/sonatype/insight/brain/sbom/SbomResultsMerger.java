/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPOutputStream;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.sbom.utils.SbomCommonUtils;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.SbomStatus;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Swid;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.model.vulnerability.Vulnerability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.DependencyResolver.MATCH_STATE;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxLicenseFromDbData;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxVulnerabilityFromDbData;
import static com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils.resolveRatingMethodFromSeveritySource;
import static com.sonatype.insight.brain.utils.CvssV3Severity.resolveRatingSeverity;

@Named
public class SbomResultsMerger
{
  private static final Logger log = LoggerFactory.getLogger(ReportService.class);

  public static final int MAX_RECURSION_DEPTH = 100000;

  public static final String NVD = "NVD";

  public static final String CVE = "CVE";

  public static final String FIELD_MATCH_STATE = "matchState";

  public static final String FIELD_EFFECTIVE_LICENSES = "effectiveLicenses";

  public static final String FIELD_LICENSE_NAME = "name";

  public static final String FIELD_LICENSE_URL = "url";

  public static final Set<String> UNSUPPORTED_LICENSE_IDS = ImmutableSet.of("Not Provided", "Non-Standard");

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ApplicationDAO applicationDAO;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private final InsightWork insightWork;

  private ContainerNode<?> bomJsonData;

  private ContainerNode<?> securityJsonData;

  private ContainerNode<?> licensesJsonData;

  private ContainerNode<?> dependenciesJsonData;

  private SbomPostImportMetricsTelemetry sbomPostImportMetricsTelemetry;

  private List<SbomResultsMatcherTelemetry> bestMatchResultsTelemetries = new ArrayList<>();

  private Map<ComponentIdentifier, String> componentDependencyTypeMap = new HashMap<>();

  private Bom originalBom = null;

  private Bom filteredBom = null;

  @Inject
  public SbomResultsMerger(
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final SbomMetadataUtils sbomMetadataUtils,
      final ApplicationDAO applicationDAO,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final InsightWork insightWork)
  {
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyVulnerabilityExploitabilityExchangeDAO = thirdPartyVulnerabilityExploitabilityExchangeDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.applicationDAO = applicationDAO;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.insightWork = insightWork;
  }

  @VisibleForTesting
  Bom getOriginalBom() {
    return originalBom;
  }

  @VisibleForTesting
  Bom getFilteredBom() {
    return filteredBom;
  }

  public void mergeResults(
      final ThirdPartySbomMetadata sbomMetadata,
      final String scanId,
      final File reportFile)
      throws IOException
  {
    initializeMerge(sbomMetadata, reportFile);
    mergeSonatypeDataWithSbomData(sbomMetadata, scanId);
    sendTelemetries();
    createAndSaveOriginalSbom(sbomMetadata);
    createAndSaveFilteredScanFile(sbomMetadata);
    cleanUpPreviousReport(sbomMetadata.getApplicationId(), sbomMetadata.getThirdPartyFileId(), scanId);
  }

  private void initializeMerge(final ThirdPartySbomMetadata sbomMetadata, final File reportFile) throws IOException {
    bomJsonData =
        JsonUtils.parse(Objects.requireNonNull(Report.getEntry(reportFile, Report.BOM_JSON_FILENAME)).buf);
    securityJsonData =
        JsonUtils.parse(Objects.requireNonNull(Report.getEntry(reportFile, Report.SECURITY_JSON_FILENAME)).buf);
    licensesJsonData =
        JsonUtils.parse(Objects.requireNonNull(Report.getEntry(reportFile, Report.LICENSES_JSON_FILENAME)).buf);
    final ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);
    dependenciesJsonData =
        dependenciesReportEntry != null ? JsonUtils.parse(dependenciesReportEntry.buf) : null;
    sbomPostImportMetricsTelemetry = new SbomPostImportMetricsTelemetry();

    // populate component dependency type map by walking dependency tree if dependency data is not present in bom.json
    if (bomJsonData.get("dependencyDataIncluded") != null &&
        !bomJsonData.get("dependencyDataIncluded").booleanValue()) {
      populateComponentDependencyTypeMap(componentDependencyTypeMap);
    }

    // create an original SBOM and filtered scan file for continuous monitoring in the case of binary scans
    if (SbomStatus.PENDING.toString().equals(sbomMetadata.getStatus()) &&
        SbomScanType.BINARY.toString().equals(sbomMetadata.getScanType())) {
      originalBom = createNewBom();
      filteredBom = createNewBom();
    }
  }

  private void mergeSonatypeDataWithSbomData(ThirdPartySbomMetadata sbomMetadata, String scanId) {
    //required for backward compatibility until sonatypeIdentifier is in place
    Map<ComponentIdentifier, JsonNode> resultsNotConsideringSonatypeId = new LinkedHashMap<>();
    MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> resultsConsideringSonatypeId =
        new ArrayListValuedHashMap<>();
    groupHdsResultsConsideringSonatypeId(bomJsonData, resultsConsideringSonatypeId, resultsNotConsideringSonatypeId);
    Map<ComponentIdentifier, Set<SecurityVulnerability>> sonatypeVulnerabilityResults =
        readSonatypeSecurityResults(securityJsonData);
    Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults =
        readSonatypeLicenseResults(licensesJsonData);
    if (MapUtils.isEmpty(sonatypeVulnerabilityResults) && MapUtils.isEmpty(sonatypeLicenseResults)) {
      // Scenario: In this case no vulnerabilities or licenses were found in HDS so the merging process is skipped.
      // We still need to gather telemetry data.
      generateTelemetryForUnverifiedVulnerabilitiesOnlyScenario(scanId);
    }

    if (!resultsConsideringSonatypeId.isEmpty()) {
      mergeResultsConsideringSonatypeIdentifier(scanId, componentDependencyTypeMap, resultsConsideringSonatypeId,
          sonatypeVulnerabilityResults, sonatypeLicenseResults, sbomMetadata, originalBom, filteredBom);
    }

    if (resultsConsideringSonatypeId.isEmpty() || (!resultsNotConsideringSonatypeId.isEmpty() &&
        SbomScanType.BINARY.toString().equals(sbomMetadata.getScanType()))) {
      mergeResultsNotConsideringSonatypeIdentifier(scanId, componentDependencyTypeMap, resultsNotConsideringSonatypeId,
          sonatypeVulnerabilityResults, sonatypeLicenseResults, sbomMetadata, originalBom, filteredBom);
    }
    makeSbomActive(sbomMetadata);
  }

  private void mergeResultsConsideringSonatypeIdentifier(
      final String scanId,
      final Map<ComponentIdentifier, String> componentDependencyTypeMap,
      final MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> resultsWithSonatypeId,
      final Map<ComponentIdentifier, Set<SecurityVulnerability>> sonatypeVulnerabilityResults,
      final Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final Bom originalBom,
      final Bom filteredBom)
  {
    for (String sonatypeId : resultsWithSonatypeId.keySet()) {
      ThirdPartyFileCoordinate sbomComponent = thirdPartyFileCoordinateDAO.getById(sonatypeId);
      if (sbomComponent == null) {
        log.debug("Internal Error: no ThirdPartyCoordinate record for {}", sonatypeId);
        continue;
      }

      Collection<Pair<ComponentIdentifier, JsonNode>> identityResults = resultsWithSonatypeId.get(sonatypeId);
      if (CollectionUtils.size(identityResults) == 1) {
        //no multi results. perform merge using this result.
        Pair<ComponentIdentifier, JsonNode> idResult = identityResults.iterator().next();
        doMergeConsideringSonatypeIdentifier(scanId, componentDependencyTypeMap, sonatypeVulnerabilityResults,
            sonatypeLicenseResults, thirdPartySbomMetadata, originalBom, filteredBom, idResult, sbomComponent);
      }
      else {
        SbomResultsMatcherTelemetry bestMatchResultsTelemetry = new SbomResultsMatcherTelemetry();
        bestMatchResultsTelemetries.add(bestMatchResultsTelemetry);
        //more than 1 hds result for the thirdparty component. perform best match
        Pair<ComponentIdentifier, JsonNode> idResult =
            SbomResultsMatcher.bestMatch(sbomComponent, identityResults, bestMatchResultsTelemetry);
        doMergeConsideringSonatypeIdentifier(scanId, componentDependencyTypeMap, sonatypeVulnerabilityResults,
            sonatypeLicenseResults, thirdPartySbomMetadata, originalBom, filteredBom, idResult, sbomComponent);
      }
    }
  }

  private void mergeResultsNotConsideringSonatypeIdentifier(
      final String scanId,
      final Map<ComponentIdentifier, String> componentDependencyTypeMap,
      final Map<ComponentIdentifier, JsonNode> resultsWithNoSonatypeId,
      final Map<ComponentIdentifier, Set<SecurityVulnerability>> sonatypeVulnerabilityResults,
      final Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final Bom originalBom,
      final Bom filteredBom)
  {
    for (Entry<ComponentIdentifier, JsonNode> resultEntry : resultsWithNoSonatypeId.entrySet()) {
      ComponentIdentifier bomComponentIdentifier = resultEntry.getKey();
      JsonNode bomNode = resultEntry.getValue();
      PackageUrlIdentifier bomPurl = PackageUrlIdentifier.fromComponentIdentifier(bomComponentIdentifier);
      ThirdPartyFileCoordinate sbomComponent = thirdPartyFileCoordinateDAO.getByPackageUrlAndHashAndScanId(
          bomPurl.getPackageUrl(), bomNode.get("hash").asText(), scanId);
      if (sbomComponent == null) {
        //fallback to coordinate matching
        sbomComponent = thirdPartyFileCoordinateDAO
            .getByFormatNameVersionAndScanID(bomComponentIdentifier.getFormat(), bomPurl.getName(),
                bomPurl.getVersion(), scanId);
      }
      sbomComponent = addNewComponentsForBinaryScan(bomNode, originalBom, filteredBom, sbomComponent,
          thirdPartySbomMetadata.getThirdPartyFileId());
      mergeResultComponentToDatabase(scanId, componentDependencyTypeMap, bomNode, bomPurl.getPackageUrl(),
          sbomComponent, bomComponentIdentifier, sonatypeVulnerabilityResults, sonatypeLicenseResults,
          thirdPartySbomMetadata);
    }
  }

  private void doMergeConsideringSonatypeIdentifier(
      final String scanId,
      final Map<ComponentIdentifier, String> componentDependencyTypeMap,
      final Map<ComponentIdentifier, Set<SecurityVulnerability>> sonatypeVulnerabilityResults,
      final Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final Bom originalBom,
      final Bom filteredBom,
      final Pair<ComponentIdentifier, JsonNode> idResult,
      final ThirdPartyFileCoordinate sbomComponent)
  {
    JsonNode bomNode = idResult.getValue();
    ComponentIdentifier bomComponentIdentifier = idResult.getKey();
    addNewComponentsForBinaryScan(bomNode, originalBom, filteredBom, sbomComponent,
        sbomComponent.getThirdPartyFileId());
    mergeResultComponentToDatabase(scanId,
        componentDependencyTypeMap, bomNode, getBomPurl(bomNode, bomComponentIdentifier),
        sbomComponent, bomComponentIdentifier, sonatypeVulnerabilityResults, sonatypeLicenseResults,
        thirdPartySbomMetadata);
  }

  private void sendTelemetries() {
    List<TelemetryData> telemetryDataList = new ArrayList<>();
    telemetryDataList.add(telemetryUtils.buildThirdPartyScanSbomImportTelemetryData(sbomPostImportMetricsTelemetry));
    if (CollectionUtils.isNotEmpty(bestMatchResultsTelemetries)) {
      addBestResultsMatcherStatTelemetries(telemetryDataList);
    }
    telemetrySender.send(telemetryDataList);
  }

  private void addBestResultsMatcherStatTelemetries(List<TelemetryData> telemetryDataList) {
    for (SbomResultsMatcherTelemetry telemetry : bestMatchResultsTelemetries) {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SBOM_RESULT_BEST_MATCH_METRICS);
      telemetryData.put(SbomResultsMatcherTelemetry.ATTRIBUTE_NAME, telemetry);
      telemetryDataList.add(telemetryData);
    }
  }

  private void mergeResultComponentToDatabase(
      final String scanId,
      final Map<ComponentIdentifier, String> componentDependencyTypeMap,
      final JsonNode bomNode,
      final String bomPurl,
      final ThirdPartyFileCoordinate sbomComponent,
      final ComponentIdentifier bomComponentIdentifier,
      final Map<ComponentIdentifier, Set<SecurityVulnerability>> sonatypeVulnerabilityResults,
      final Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults,
      final ThirdPartySbomMetadata thirdPartySbomMetadata)
  {
    if (sbomComponent == null) {
      log.debug("Could not locate matching third party coordinate entry for component identifier {} and scanId {}",
          bomComponentIdentifier, scanId);
      return;
    }
    // use directDependency if present in bom, if not, only then walk tree
    if (bomNode.get("directDependency") != null) {
      sbomComponent.setDependencyType(bomNode.get("directDependency").booleanValue() ? "D" : "T");
    }
    else {
      updateComponentDependencyType(sbomComponent, componentDependencyTypeMap);
    }
    if (bomNode.get("website") != null && !bomNode.get("website").isNull()) {
      sbomComponent.setWebsite(bomNode.get("website").asText());
    }
    if (bomNode.get("componentCategories") != null && !bomNode.get("componentCategories").isNull()) {
      JsonNode componentCategoryArrayNode = bomNode.get("componentCategories");
      if (componentCategoryArrayNode.isArray()) {
        String categories = StreamSupport.stream(componentCategoryArrayNode.spliterator(), false)
            .map(node -> node.get("componentCategoryId").asText())
            .collect(Collectors.joining(","));
        sbomComponent.setCategoryIds(categories);
      }
    }
    if (bomPurl != null && !StringUtils.equals(sbomComponent.getPackageUrl(), bomPurl)) {
      //in certain cases the purl from HDS matched results may be different to the original purl
      // in such cases update the purl to the result purl for consistency with Sonatype data
      sbomComponent.setPackageUrl(bomPurl);
    }
    sbomComponent.setMatchStateId(JsonUtils.getNullableString(bomNode.get(FIELD_MATCH_STATE)));
    updateComponentIdentifiedAsSonatype(sbomComponent);
    mergeSecurityData(sonatypeVulnerabilityResults, bomComponentIdentifier, sbomComponent, thirdPartySbomMetadata);
    mergeLicenseData(sonatypeLicenseResults, bomComponentIdentifier, sbomComponent);
  }

  private ThirdPartyFileCoordinate addNewComponentsForBinaryScan(
      JsonNode bomNode,
      Bom originalBom,
      Bom filteredBom,
      ThirdPartyFileCoordinate sbomDbComponent,
      String thirdPartyFileId)
  {
    if (ObjectUtils.allNotNull(originalBom, filteredBom)) {
      List<ThirdPartyCoordinateSecurity> disclosedVulns = null;
      List<ThirdPartyCoordinateLicense> disclosedLicenses = null;
      if (sbomDbComponent == null) {
        sbomDbComponent = createAndSaveComponentInThirdPartyDatabase(bomNode, thirdPartyFileId);
      }
      else {
        disclosedVulns = thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomDbComponent.getId());
        disclosedLicenses = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(sbomDbComponent.getId());
      }
      createAndSaveComponentInBom(sbomDbComponent, disclosedVulns, disclosedLicenses, bomNode, originalBom,
          filteredBom);
    }
    return sbomDbComponent;
  }

  private void createAndSaveComponentInBom(
      ThirdPartyFileCoordinate thirdPartyFileCoordinate,
      List<ThirdPartyCoordinateSecurity> disclosedVulns,
      List<ThirdPartyCoordinateLicense> disclosedLicenses,
      JsonNode bomNode,
      Bom bom,
      Bom filteredBom)
  {
    String bomRef = UUID.randomUUID().toString().replace("-", "");
    Component component = thirdPartyFileCoordinateToBomComponent(thirdPartyFileCoordinate, bomRef);
    addOccurenceEvidenceForComponent(bomNode, component);
    bom.addComponent(component);
    //merge has not happened yet, so at this point only disclosed vulnerabilities and licenses can exist
    //  include them as disclosed
    addDisclosedVulnerabilities(disclosedVulns, bom, component);
    addDisclosedLicenses(disclosedLicenses, component);

    Component clone = thirdPartyFileCoordinateToBomComponent(thirdPartyFileCoordinate, bomRef);
    //this might not be needed after SBOM-749 is implemented
    Property sonatypeIdentifierComponentProperty = new Property();
    sonatypeIdentifierComponentProperty.setName("sonatypeIdentifier");
    sonatypeIdentifierComponentProperty.setValue(thirdPartyFileCoordinate.getId());
    clone.addProperty(sonatypeIdentifierComponentProperty);
    filteredBom.addComponent(clone);
  }

  private void addOccurenceEvidenceForComponent(JsonNode bomNode, Component component) {
    List<String> pathnames = JsonUtils.getStringListFromArray(bomNode.get("pathnames"));
    if (pathnames == null) {
      return;
    }
    List<Occurrence> occurrences = pathnames.stream()
        .map(p -> {
          Occurrence o = new Occurrence();
          o.setLocation(p);
          return o;
        }).collect(Collectors.toList());
    Evidence evidence = new Evidence();
    evidence.setOccurrences(occurrences);
    component.setEvidence(evidence);
  }

  private void addDisclosedVulnerabilities(
      List<ThirdPartyCoordinateSecurity> disclosedVulns,
      Bom bom,
      Component component)
  {
    if (CollectionUtils.isNotEmpty(disclosedVulns)) {
      initVulnerabilities(bom);
      List<Vulnerability> newBomVulnerabilities = new ArrayList<>();
      disclosedVulns.forEach(vuln -> {
        ThirdPartyVulnerabilityExploitabilityExchange vex =
            thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(vuln.getId(),
                vuln.getRefId());
        newBomVulnerabilities.add(createCycloneDxVulnerabilityFromDbData(component, vuln, vex));
      });
      bom.getVulnerabilities().addAll(newBomVulnerabilities);
    }
  }

  private void initVulnerabilities(final Bom bom) {
    List<Vulnerability> vulnerabilities = bom.getVulnerabilities();
    if (vulnerabilities == null) {
      vulnerabilities = new ArrayList<>();
      bom.setVulnerabilities(vulnerabilities);
    }
  }

  private void addDisclosedLicenses(List<ThirdPartyCoordinateLicense> disclosedLicenses, Component component) {
    if (CollectionUtils.isNotEmpty(disclosedLicenses)) {
      LicenseChoice licenseChoice = new LicenseChoice();
      licenseChoice.setLicenses(new ArrayList<>());
      disclosedLicenses.forEach(license -> {
        licenseChoice.addLicense(createCycloneDxLicenseFromDbData(license));
      });
      component.setLicenses(licenseChoice);
    }
  }

  private Component thirdPartyFileCoordinateToBomComponent(ThirdPartyFileCoordinate fileCoordinate, String bomRef) {
    Component component = new Component();
    component.setBomRef(bomRef);
    component.setName(fileCoordinate.getName());
    component.setVersion(fileCoordinate.getVersion());
    component.setCpe(fileCoordinate.getCpe());
    component.setPurl(fileCoordinate.getPackageUrl());
    component.setType(Component.Type.LIBRARY);
    if (StringUtils.isNotEmpty(fileCoordinate.getSwid())) {
      Swid swid = new Swid();
      swid.setName(fileCoordinate.getSwid());
      swid.setTagId(fileCoordinate.getSwid());
    }
    return component;
  }

  private void populateMissingThirdPartyCoordinateSecurityWithSonatypeData(
      final ThirdPartyCoordinateSecurity thirdPartySecurity,
      final SecurityVulnerability sonatypeVulnerabilityData)
  {
    if (sonatypeVulnerabilityData == null) {
      return;
    }
    if (StringUtils.isNotBlank(sonatypeVulnerabilityData.getVector())) {
      thirdPartySecurity.setAttackVector(sonatypeVulnerabilityData.getVector());
    }
    if (StringUtils.isNotBlank(sonatypeVulnerabilityData.getUrl())) {
      thirdPartySecurity.setLink(sonatypeVulnerabilityData.getUrl());
    }
    if (sonatypeVulnerabilityData.getSeverity() != null && sonatypeVulnerabilityData.getSeverity() > 0) {
      thirdPartySecurity.setSeverity(sonatypeVulnerabilityData.getSeverity());
      thirdPartySecurity.setSeverityDescription(resolveRatingSeverity(sonatypeVulnerabilityData.getSeverity()).name());
    }
    if (StringUtils.isNotBlank(sonatypeVulnerabilityData.getSource())) {
      if (CVE.equals(sonatypeVulnerabilityData.getSource().toUpperCase())) {
        thirdPartySecurity.setVulnerabilitySource(NVD);
      }
      else {
        thirdPartySecurity.setVulnerabilitySource(sonatypeVulnerabilityData.getSource().toUpperCase(Locale.ROOT));
      }
    }
    if (StringUtils.isNotBlank(sonatypeVulnerabilityData.getVectorSource())) {
      thirdPartySecurity.setRatingMethod(
          resolveRatingMethodFromSeveritySource(sonatypeVulnerabilityData.getVectorSource()).name());
    }
    if (StringUtils.isNotBlank(sonatypeVulnerabilityData.getCwe())) {
      if (!SbomMetadataUtils.convertCwesStringToIntegerList(sonatypeVulnerabilityData.getCwe()).isEmpty()) {
        thirdPartySecurity.setCwes(sonatypeVulnerabilityData.getCwe());
      }
    }
    thirdPartySecurity.addIdentificationSource(IdentificationSource.SONATYPE.getId());
  }

  private void populateMissingThirdPartyCoordinateLicenseWithSonatypeData(
      final ThirdPartyCoordinateLicense thirdPartyLicense,
      final JsonNode licenseJsonNode)
  {
    if (licenseJsonNode == null) {
      return;
    }

    if (StringUtils.isNotBlank(JsonUtils.getNullableString(licenseJsonNode.get(FIELD_LICENSE_NAME)))) {
      thirdPartyLicense.setName(JsonUtils.getNullableString(licenseJsonNode.get(FIELD_LICENSE_NAME)));
    }
    if (StringUtils.isNotBlank(JsonUtils.getNullableString(licenseJsonNode.get(FIELD_LICENSE_URL)))) {
      thirdPartyLicense.setUrl(JsonUtils.getNullableString(licenseJsonNode.get(FIELD_LICENSE_URL)));
    }

    thirdPartyLicense.addIdentificationSource(IdentificationSource.SONATYPE.getId());
  }

  private ThirdPartyFileCoordinate createAndSaveComponentInThirdPartyDatabase(
      JsonNode componentNode,
      String thirdPartyFileId)
  {
    ThirdPartyFileCoordinate component = new ThirdPartyFileCoordinate();
    component.setThirdPartyFileId(thirdPartyFileId);
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(componentNode);
    if (componentIdentifier != null) {
      Map<String, String> componentCoordinates = componentIdentifier.getCoordinates();
      String name = String.join(":", componentIdentifier.getProprietaryCoordinates());
      component.setName(name);
      component.setVersion(componentCoordinates.get("version"));
      String componentFormat = componentIdentifier.getFormat();
      if (StringUtils.isNotEmpty(componentFormat)) {
        component.setFormat(componentFormat);
      }
    }
    component.setHash(JsonUtils.getNullableString(componentNode.get("hash")));
    component.setCpe(JsonUtils.getNullableString(componentNode.get("cpe")));
    component.setSwid(JsonUtils.getNullableString(componentNode.get("swid")));
    String bomPurl = getBomPurl(componentNode, componentIdentifier);
    component.setPackageUrl(bomPurl);
    component.setSource("Sonatype");
    component.setIdentificationSources("Sonatype");
    thirdPartyFileCoordinateDAO.insert(component);
    return component;
  }

  private String getBomPurl(JsonNode bomNode, ComponentIdentifier bomComponentIdentifier) {
    if (bomNode.get("packageUrl") != null) {
      return bomNode.get("packageUrl").asText();
    }
    return PackageUrlIdentifier.fromComponentIdentifier(bomComponentIdentifier).getPackageUrl();
  }

  private Map<ComponentIdentifier, Set<SecurityVulnerability>> readSonatypeSecurityResults(
      final ContainerNode<?> securityJsonData)
  {
    Map<ComponentIdentifier, Set<SecurityVulnerability>> secResults = new HashMap<>();
    ArrayNode securityJsonArray = (ArrayNode) securityJsonData.get("aaData");
    for (JsonNode securityJsonNode : securityJsonArray) {
      ComponentIdentifier securityComponentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(securityJsonNode);
      if (JsonUtils.getNullableString(securityJsonNode.get(FIELD_MATCH_STATE)) != null) {
        SecurityVulnerability securityVulnerability = loadSecurityJson(securityJsonNode);
        if (securityVulnerability != null) {
          secResults.computeIfAbsent(securityComponentIdentifier, componentIdentifier ->
              new HashSet<>()).add(securityVulnerability);
        }
      }
    }
    return secResults;
  }

  private Map<ComponentIdentifier, Map<String, JsonNode>> readSonatypeLicenseResults(
      final ContainerNode<?> licensesJsonData)
  {
    Map<ComponentIdentifier, Map<String, JsonNode>> licenseResults = new HashMap<>();
    ArrayNode licenseJsonArray = (ArrayNode) licensesJsonData.get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonArray) {
      ComponentIdentifier componentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(licenseJsonNode);
      List<String> licenseIds = JsonUtils.getStringListFromArray(licenseJsonNode.get(FIELD_EFFECTIVE_LICENSES));
      // later we may need to fall back to declared licenses if effective licenses is empty
      if (CollectionUtils.isNotEmpty(licenseIds)) {
        for (String licenseId : licenseIds) {
          licenseResults.computeIfAbsent(componentIdentifier, identifier -> new HashMap<>())
              .put(licenseId, licenseJsonNode);
        }
      }
    }
    return licenseResults;
  }

  private void groupHdsResultsConsideringSonatypeId(
      final ContainerNode<?> bomJsonData,
      final MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> resultsConsideringSonatypeId,
      final Map<ComponentIdentifier, JsonNode> resultsNotConsideringSonatypeId)
  {
    ArrayNode bomArray = (ArrayNode) bomJsonData.get("aaData");
    for (JsonNode bomNode : bomArray) {
      String matchStateString = bomNode.get(MATCH_STATE).asText();
      MatchState matchState = MatchState.getById(matchStateString);
      if (MatchState.UNKNOWN.equals(matchState)) {
        continue;
      }
      ComponentIdentifier bomComponentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(bomNode);
      if (bomComponentIdentifier == null) {
        log.debug("matched bom.json entry found without a component identifier {}", bomNode);
        continue;
      }
      String sonatypeId = bomNode.get("sonatypeIdentifier") != null ? bomNode.get("sonatypeIdentifier").asText() : null;
      if (sonatypeId != null) {
        resultsConsideringSonatypeId.put(sonatypeId, Pair.of(bomComponentIdentifier, bomNode));
      }
      else {
        resultsNotConsideringSonatypeId.put(bomComponentIdentifier, bomNode);
      }
    }
  }

  private void makeSbomActive(final ThirdPartySbomMetadata sbomMetadata) {
    sbomMetadata.setStatus(SbomStatus.ACTIVE.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);
  }

  private void createAndSaveFilteredScanFile(final ThirdPartySbomMetadata sbomMetadata) {
    if (filteredBom == null) {
      return;
    }

    String bomString = generateBomString(filteredBom);
    Application app = applicationDAO.getById(sbomMetadata.getApplicationId());
    ThirdPartyScan tpScan = thirdPartyScanDAO.getByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());
    ScanResult scanResult =
        sbomMetadataUtils.scanSbomContent(app, bomString, insightWork.getScanDir(sbomMetadata.getApplicationId()),
            SbomFormat.JSON, ItemContentType.SBOM, ScannerDriver.SBOM_API);

    String filteredScanFileName = SbomCommonUtils.newFilteredScanFileName(tpScan.getScanId());
    File filteredScanFile = new File(insightWork.getScanDir(sbomMetadata.getApplicationId()), filteredScanFileName);
    try {
      Files.copy(scanResult.getScanFile().toPath(), filteredScanFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      tpScan.setFilteredScanFile(filteredScanFile.getName());
      thirdPartyScanDAO.update(tpScan);
    }
    catch (IOException e) {
      log.error("Error saving filtered scan file {}", filteredScanFile.getName(), e);
    }
  }

  private void createAndSaveOriginalSbom(ThirdPartySbomMetadata thirdPartySbomMetadata) throws IOException {
    if (originalBom == null) {
      return;
    }

    String bomAsString = generateBomString(originalBom);
    String binaryFileName = thirdPartySbomMetadata.getFilename();
    String compressedBinaryFileName = binaryFileName.substring(0, binaryFileName.lastIndexOf(".")) + ".json.gz";
    File sbomDirectory = insightWork.getSbomDir(thirdPartySbomMetadata.getApplicationId());
    File compressedSbom = new File(sbomDirectory, compressedBinaryFileName);
    try (InputStream inputStream = new ByteArrayInputStream(bomAsString.getBytes());
         OutputStream outputStream = new GZIPOutputStream(new FileOutputStream(compressedSbom))) {
      IOUtils.copy(inputStream, outputStream);
      // We need to do this to seamlessly integrate into our SBOM exporter logic
      thirdPartySbomMetadata.setFilename(compressedBinaryFileName);
      thirdPartySbomMetadataDAO.update(thirdPartySbomMetadata);
    }
  }

  private void updateComponentIdentifiedAsSonatype(final ThirdPartyFileCoordinate sbomComponent) {
    sbomComponent.addIdentificationSource(IdentificationSource.SONATYPE.getId());
    thirdPartyFileCoordinateDAO.update(sbomComponent);
  }

  private void updateComponentDependencyType(
      final ThirdPartyFileCoordinate sbomComponent,
      Map<ComponentIdentifier, String> componentDependencyTypeMap)
  {
    try {
      ComponentIdentifier sbomComponentIdentifier =
          ComponentIdentifierAdapter.toComponentIdentifier(sbomComponent.getPackageUrl());
      sbomComponent.setDependencyType(componentDependencyTypeMap.get(sbomComponentIdentifier));
    }
    catch (InvalidPackageURLException e) {
      log.debug(
          "There was an error while trying to convert the purl into a component identifier - {purl: {}, " +
              "componentName: {}, componentHash: {}, componentVersion: {}}",
          sbomComponent.getPackageUrl(), sbomComponent.getName(), sbomComponent.getHash(), sbomComponent.getVersion(),
          e);
    }
  }

  private void mergeSecurityData(
      final Map<ComponentIdentifier, Set<SecurityVulnerability>> sonatypeSecResults,
      final ComponentIdentifier bomComponentIdentifier,
      final ThirdPartyFileCoordinate sbomComponent,
      final ThirdPartySbomMetadata thirdPartySbomMetadata)
  {
    Set<SecurityVulnerability> sonatypeVulns = sonatypeSecResults.get(bomComponentIdentifier);
    if (CollectionUtils.isNotEmpty(sonatypeVulns)) {
      // Get all the coordinate securities from the DB for all the vulnerabilities. This list wil
      Map<String, ThirdPartyCoordinateSecurity> coordinateSecuritiesFromDBForComponentMap =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomComponent.getId()).stream()
              .collect(Collectors.toMap(ThirdPartyCoordinateSecurity::getRefId, t -> t));
      sbomPostImportMetricsTelemetry.addToTotalVulnerabilitiesCount(coordinateSecuritiesFromDBForComponentMap.size());

      for (SecurityVulnerability sonatypeVuln : sonatypeVulns) {
        try {
          ThirdPartyCoordinateSecurity sbomVulnerability = coordinateSecuritiesFromDBForComponentMap
              .get(sonatypeVuln.getRefId());
          if (sbomVulnerability != null) {
            //matching sbom vulnerability found, update record
            if (sonatypeVuln.getSeverity() != null && sonatypeVuln.getSeverity() > 0) {
              populateMissingThirdPartyCoordinateSecurityWithSonatypeData(sbomVulnerability, sonatypeVuln);
              thirdPartyCoordinateSecurityDAO.update(sbomVulnerability);
              coordinateSecuritiesFromDBForComponentMap.remove(sonatypeVuln.getRefId());
              sbomPostImportMetricsTelemetry.incrementVerifiedVulnerabilityCount();
            }
          }
          else {
            //no matching sbom vulnerability, insert sonatype data
            ThirdPartyCoordinateSecurity newThirdPartySecurity = new ThirdPartyCoordinateSecurity();
            newThirdPartySecurity.setFileCoordinateId(sbomComponent.getId());
            newThirdPartySecurity.setRefId(sonatypeVuln.getRefId());
            newThirdPartySecurity.setSbomMetadataId(thirdPartySbomMetadata.getId());
            populateMissingThirdPartyCoordinateSecurityWithSonatypeData(newThirdPartySecurity, sonatypeVuln);
            thirdPartyCoordinateSecurityDAO.insert(newThirdPartySecurity);
            sbomPostImportMetricsTelemetry.incrementAdditionalVulnerabilitiesCount();
          }
        }
        catch (NotFoundException exception) {
          log.warn("Vulnerability {} not found", sonatypeVuln);
        }
      }

      // Walk through the remaining coordinate securities in this list. These are the orphan ones.
      for (String refId : coordinateSecuritiesFromDBForComponentMap.keySet()) {
        ThirdPartyCoordinateSecurity coordinateSecurity = coordinateSecuritiesFromDBForComponentMap.get(refId);
        if (coordinateSecurity.getIdentificationSources().contains(IdentificationSource.SONATYPE.getId()) &&
            coordinateSecurity.getIdentificationSources().contains(IdentificationSource.SBOM.getId())) {
          // if the vulnerability is found in DB but no HDS result, and has both identification sources,
          // SBOM and SONATYPE, remove SONATYPE.
          coordinateSecurity.setIdentificationSources(IdentificationSource.SBOM.getId());
          thirdPartyCoordinateSecurityDAO.update(coordinateSecurity);
          sbomPostImportMetricsTelemetry.incrementUnverifiedVulnerabilityCount();
        }
        else if (coordinateSecurity.getIdentificationSources().equals(IdentificationSource.SONATYPE.getId())) {
          // else if it only has SONATYPE, delete it from the DB along with any VEX annotation associated with it.
          thirdPartyCoordinateSecurityDAO.delete(coordinateSecurity);
        }
      }
    }
    else {
      // Scenario: There are no vulnerabilities found in HDS for this component. We still have to gather telemetry data.
      Map<String, ThirdPartyCoordinateSecurity> coordinateSecuritiesFromDBForComponentMap =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomComponent.getId()).stream()
              .collect(Collectors.toMap(ThirdPartyCoordinateSecurity::getRefId, t -> t));
      sbomPostImportMetricsTelemetry.addToTotalVulnerabilitiesCount(
          coordinateSecuritiesFromDBForComponentMap.size());
      sbomPostImportMetricsTelemetry.addToUnverifiedVulnerabilityCount(
          coordinateSecuritiesFromDBForComponentMap.size());
    }
  }

  private void mergeLicenseData(
      final Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults,
      final ComponentIdentifier bomComponentIdentifier,
      final ThirdPartyFileCoordinate sbomComponent)
  {
    Map<String, JsonNode> sonatypeLicenses = sonatypeLicenseResults.get(bomComponentIdentifier);
    if (MapUtils.isNotEmpty(sonatypeLicenses)) {
      List<ThirdPartyCoordinateLicense> sbomComponentLicenses =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(sbomComponent.getId());
      ArrayList<ThirdPartyCoordinateLicense> allLicensesFromDBForComponent = new ArrayList<>(sbomComponentLicenses);
      Map<String, ThirdPartyCoordinateLicense> byLicenseIds =
          sbomComponentLicenses.stream().collect(Collectors.toMap(ThirdPartyCoordinateLicense::getLicenseId, cl -> cl));
      Map<String, ThirdPartyCoordinateLicense> byLicenseNames = sbomComponentLicenses.stream().collect(
          Collectors.toMap(ThirdPartyCoordinateLicense::getName, Function.identity(), (first, second) -> first));
      for (Entry<String, JsonNode> sonatypeLicenseEntry : sonatypeLicenses.entrySet()) {
        String resultEntryLicense = sonatypeLicenseEntry.getKey();
        if (UNSUPPORTED_LICENSE_IDS.contains(resultEntryLicense)) {
          //there is no valid license identified by sonatype, so no point storing it in database
          continue;
        }
        ThirdPartyCoordinateLicense sbomLicense = byLicenseIds.get(resultEntryLicense);
        if (sbomLicense == null) {
          sbomLicense = byLicenseNames.get(resultEntryLicense);
        }

        if (sbomLicense != null) {
          // SBOM license found in DB and in json file, update record.
          populateMissingThirdPartyCoordinateLicenseWithSonatypeData(sbomLicense, sonatypeLicenseEntry.getValue());
          thirdPartyCoordinateLicenseDAO.update(sbomLicense);
          // Remove from list so I know which ones from the DB were not in the json file.
          allLicensesFromDBForComponent.remove(sbomLicense);
        }
        else {
          // No SBOM license found in DB, add new record with Sonatype identification sources.
          ThirdPartyCoordinateLicense newThirdPartyLicense = new ThirdPartyCoordinateLicense();
          newThirdPartyLicense.setFileCoordinateId(sbomComponent.getId());
          newThirdPartyLicense.setLicenseId(resultEntryLicense);
          populateMissingThirdPartyCoordinateLicenseWithSonatypeData(newThirdPartyLicense,
              sonatypeLicenseEntry.getValue());
          thirdPartyCoordinateLicenseDAO.insert(newThirdPartyLicense);
        }
      }

      // Walk through the licenses that were left in this list. These are the orphan ones.
      for (ThirdPartyCoordinateLicense licenseFromDB : allLicensesFromDBForComponent) {
        if (licenseFromDB.getIdentificationSources().contains(IdentificationSource.SONATYPE.getId()) &&
            licenseFromDB.getIdentificationSources().contains(IdentificationSource.SBOM.getId())) {
          // If the license was found in the DB but NOT in the json file, and the identification source is
          // SBOM,Sonatype, remove Sonatype.
          licenseFromDB.setIdentificationSources(IdentificationSource.SBOM.getId());
          thirdPartyCoordinateLicenseDAO.update(licenseFromDB);
        }
        else if (licenseFromDB.getIdentificationSources().equals(IdentificationSource.SONATYPE.getId())) {
          // If the license was found in the DB but NOT in the json file, and the identification source is only SBOM,
          // delete it from the DB.
          thirdPartyCoordinateLicenseDAO.delete(licenseFromDB);
        }
      }
    }
  }

  private static String generateBomString(final Bom originalBom) {
    return BomGeneratorFactory.createJson(Version.VERSION_16, originalBom).toJsonString();
  }

  private SecurityVulnerability loadSecurityJson(final JsonNode securityJsonNode) {
    if (securityJsonNode != null) {
      SecurityVulnerability securityVulnerability = new SecurityVulnerability();
      securityVulnerability.setSource(securityJsonNode.get("source").asText());
      securityVulnerability.setRefId(securityJsonNode.get("reference").asText());
      securityVulnerability.setSeverity(JsonUtils.getNullableFloat(securityJsonNode.get("score")));
      securityVulnerability.setUrl(JsonUtils.getNullableString(securityJsonNode.get("url")));
      securityVulnerability.setCwe(JsonUtils.getNullableString(securityJsonNode.get("cwe")));
      securityVulnerability.setVector(JsonUtils.getNullableString(securityJsonNode.get("cvssVectorString")));
      securityVulnerability.setVectorSource(JsonUtils.getNullableString(securityJsonNode.get("cvssVectorSource")));

      final List<String> aliases = JsonUtils.getStringListFromArray(securityJsonNode.get("aliases"));
      if (aliases != null) {
        for (String alias : aliases) {
          securityVulnerability.addAlias(alias);
        }
      }

      final List<String> vulnerabilityCategories =
          JsonUtils.getStringListFromArray(securityJsonNode.get("vulnerabilityCategories"));
      if (vulnerabilityCategories != null) {
        for (String categoryStr : vulnerabilityCategories) {
          SecurityVulnerabilityCategory category = SecurityVulnerabilityCategory.getById(categoryStr);
          securityVulnerability.addVulnerabilityCategory(category);
        }
      }
      return securityVulnerability;
    }
    else {
      return null;
    }
  }

  @VisibleForTesting
  void cleanUpPreviousReport(String applicationId, String thirdPartyFileId, String scanId) throws IOException {
    ThirdPartyScan thirdPartyScan = thirdPartyScanDAO.getByThirdPartyFileIdAndScanId(thirdPartyFileId, scanId);
    if (thirdPartyScan != null && thirdPartyScan.getPreviousScanId() != null) {
      if (SystemConfigurationPropertyFeature.CLEAN_UP_SBOM_CONTINUOUS_MONITORING_REPORT.isEnabled()) {
        // Delete previous scan report folder
        log.debug("Deleting previous scan report folder for applicationId {}, previousScanId {}. The new scan id is {}",
            applicationId, thirdPartyScan.getPreviousScanId(), scanId);
        File previousReportDir = insightWork.getReportDir(applicationId, thirdPartyScan.getPreviousScanId());
        FileUtils.deleteDirectory(previousReportDir);
      }
      thirdPartyScan.setPreviousScanId(null);
      thirdPartyScanDAO.update(thirdPartyScan);
    }
  }

  private void generateTelemetryForUnverifiedVulnerabilitiesOnlyScenario(final String scanId) {
    List<ThirdPartyFileCoordinate> components = thirdPartyFileCoordinateDAO.getByScanId(scanId);
    for (ThirdPartyFileCoordinate component : components) {
      Map<String, ThirdPartyCoordinateSecurity> coordinateSecuritiesFromDBForComponentMap =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(component.getId()).stream()
              .collect(Collectors.toMap(ThirdPartyCoordinateSecurity::getRefId, t -> t));
      sbomPostImportMetricsTelemetry.addToTotalVulnerabilitiesCount(
          coordinateSecuritiesFromDBForComponentMap.size());
      sbomPostImportMetricsTelemetry.addToUnverifiedVulnerabilityCount(
          coordinateSecuritiesFromDBForComponentMap.size());
    }
  }

  private void populateComponentDependencyTypeMap(Map<ComponentIdentifier, String> componentDependencyTypeMap)
      throws IOException
  {
    if (dependenciesJsonData == null) {
      return;
    }
    JsonNode dependencyTreeNode = dependenciesJsonData.path("dependencyTree");
    if (!dependencyTreeNode.isMissingNode()) {
      DependencyNode tree = JsonUtils.asPojo(dependencyTreeNode, DependencyNode.class);
      if (tree == null) {
        return;
      }
      walkTreeAndPopulateDirectDependencyType(Collections.singletonList(tree), componentDependencyTypeMap, 0);
    }
  }

  @VisibleForTesting
  Bom createNewBom() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();
    ToolInformation toolInformation = new ToolInformation();
    Component sbomManagerComponent = new Component();
    sbomManagerComponent.setType(Component.Type.APPLICATION);
    sbomManagerComponent.setName("Sonatype SBOM Manager");
    toolInformation.setComponents(Collections.singletonList(sbomManagerComponent));
    metadata.setToolChoice(toolInformation);
    bom.setMetadata(metadata);
    return bom;
  }

  private void walkTreeAndPopulateDirectDependencyType(
      List<DependencyNode> children,
      Map<ComponentIdentifier, String> componentDependencyTypeMap,
      int recursionDepth)
  {
    for (DependencyNode child : children) {
      componentDependencyTypeMap.putIfAbsent(child.getComponentIdentifier(), child.isDirect() ? "D" : "T");
      if (++recursionDepth <= MAX_RECURSION_DEPTH) {
        walkTreeAndPopulateDirectDependencyType(child.getChildren(), componentDependencyTypeMap, recursionDepth);
      }
      else {
        log.warn("Dependency tree depth exceeded {}, skipping child dependencies", recursionDepth);
      }
    }
  }
}
