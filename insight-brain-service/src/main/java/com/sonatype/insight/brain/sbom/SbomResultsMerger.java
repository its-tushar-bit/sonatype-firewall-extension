/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.SbomTaxonomy;

import org.apache.commons.codec.digest.DigestUtils;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
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
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.sbom.export.SbomExportException;
import com.sonatype.insight.brain.sbom.export.SbomExportUtils;
import com.sonatype.insight.brain.sbom.utils.SbomCommonUtils;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.telemetry.CpeResultsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.DuplicateAwareThirdPartyFileCoordinatePersister;
import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.brain.thirdparty.ThirdPartyPersistenceService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
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

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;
import static com.sonatype.insight.brain.report.DependencyResolver.FIELD_ANALYZER_FEATURES;
import static com.sonatype.insight.brain.report.DependencyResolver.MATCH_STATE;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxLicenseForThirdpartyLicense;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxProperty;
import static com.sonatype.insight.brain.sbom.export.SbomExportUtils.createCycloneDxVulnerabilityFromDbData;
import static com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils.PROPERTY_COMPONENT_REF;
import static com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils.PROPERTY_COMPONENT_REFS;
import static com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils.resolveRatingMethodFromSeveritySource;
import static com.sonatype.insight.brain.utils.CvssV3Severity.resolveRatingSeverity;

@Named
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SbomResultsMerger
{
  private static final Logger log = LoggerFactory.getLogger(SbomResultsMerger.class);

  public static final String NVD = "NVD";

  public static final String CVE = "CVE";

  public static final String FIELD_MATCH_STATE = "matchState";

  public static final String FIELD_EFFECTIVE_LICENSES = "effectiveLicenses";

  public static final String FIELD_LICENSE_NAME = "name";

  public static final String FIELD_LICENSE_URL = "url";

  public static final String FIELD_PATHNAMES = "pathnames";

  public static final String FIELD_FILENAMES = "filenames";

  private final DuplicateAwareThirdPartyFileCoordinatePersister thirdPartyFileCoordinatePersister;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final ThirdPartyPersistenceService thirdPartyPersistenceService;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final ApplicationDAO applicationDAO;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private ContainerNode<?> bomJsonData;

  private ContainerNode<?> dependenciesJsonData;

  private final Map<ComponentIdentifier, Set<SecurityVulnerability>> hdsSecurityResults = new HashMap<>();

  private final Map<ComponentIdentifier, Map<String, JsonNode>> hdsLicenseResults = new HashMap<>();

  private SbomPostImportMetricsTelemetry sbomPostImportMetricsTelemetry;

  private CpeResultsTelemetry cpeResultsTelemetry;

  private final ApplicationReportPersistenceService applicationReportPersistenceService;

  private final List<SbomResultsMatcherTelemetry> bestMatchResultsTelemetries = new ArrayList<>();

  private final DependencyTreeParser dependencyTreeParser = new DependencyTreeParser();

  private Bom originalBom;

  private Bom filteredBom;

  private final ScanPersistenceService scanPersistenceService;

  @Inject
  public SbomResultsMerger(
      final DuplicateAwareThirdPartyFileCoordinatePersister thirdPartyFileCoordinatePersister,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartyPersistenceService thirdPartyPersistenceService,
      final SbomMetadataUtils sbomMetadataUtils,
      final ApplicationDAO applicationDAO,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final ApplicationReportPersistenceService applicationReportPersistenceService,
      final ScanPersistenceService scanPersistenceService)
  {
    this.thirdPartyFileCoordinatePersister = thirdPartyFileCoordinatePersister;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyVulnerabilityExploitabilityExchangeDAO = thirdPartyVulnerabilityExploitabilityExchangeDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartyPersistenceService = thirdPartyPersistenceService;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.applicationDAO = applicationDAO;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.applicationReportPersistenceService = applicationReportPersistenceService;
    this.scanPersistenceService = scanPersistenceService;
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
      final ApplicationReport applicationReport,
      final CpeResultsTelemetry cpeResultsTelemetry) throws IOException
  {
    initializeMergeProcessDependencies(sbomMetadata, applicationReport, cpeResultsTelemetry);
    mergeSonatypeDataWithSbomData(sbomMetadata, scanId);
    updateReportJsons(sbomMetadata, applicationReport);
    addDependencyDataForOriginalSbom();
    sendTelemetries();
    createAndSaveOriginalSbom(sbomMetadata);
    createAndSaveFilteredScanFile(sbomMetadata);
    cleanUpPreviousReport(sbomMetadata.getApplicationId(), sbomMetadata.getThirdPartyFileId(), scanId);
  }

  private void addDependencyDataForOriginalSbom() {
    if (originalBom == null || this.dependenciesJsonData == null ||
        CollectionUtils.isEmpty(originalBom.getComponents()))
    {
      return;
    }

    Map<String, Set<String>> componentPurlToBomRef = new HashMap<>();

    originalBom.getComponents().forEach(component -> {
      if (componentPurlToBomRef.containsKey(component.getPurl())) {
        componentPurlToBomRef.get(component.getPurl()).add(component.getBomRef());
      }
      else {
        Set<String> bomRefSet = new HashSet<>();
        bomRefSet.add(component.getBomRef());
        componentPurlToBomRef.put(component.getPurl(), bomRefSet);
      }
    });

    for (Component component : originalBom.getComponents()) {
      Optional<Set<String>> childrenDependenciesOptional =
          dependencyTreeParser.getComponentDependencies(component.getPurl());
      if (childrenDependenciesOptional.isPresent() && CollectionUtils.isNotEmpty(childrenDependenciesOptional.get())) {
        Dependency dependency = new Dependency(component.getBomRef());
        childrenDependenciesOptional.get()
            .stream()
            .filter(childDependencyPurl -> CollectionUtils.isNotEmpty(componentPurlToBomRef.get(childDependencyPurl)))
            .map(
                childDependencyPurl -> new Dependency(componentPurlToBomRef.get(childDependencyPurl).iterator().next()))
            .toList()
            .forEach(dependency::addDependency);
        originalBom.addDependency(dependency);
      }
    }
  }

  private void initializeMergeProcessDependencies(
      final ThirdPartySbomMetadata sbomMetadata,
      final ApplicationReport applicationReport,
      final CpeResultsTelemetry cpeResultsTelemetry) throws IOException
  {
    bomJsonData = JsonUtils.parse(Objects.requireNonNull(applicationReport.getEntry(BOM_JSON.getName())).buf);
    readSonatypeSecurityResults(applicationReport);
    readSonatypeLicenseResults(applicationReport);
    final ReportEntry dependenciesReportEntry = applicationReport.getEntry(DEPENDENCIES_JSON.getName());
    dependenciesJsonData =
        dependenciesReportEntry != null ? JsonUtils.parse(dependenciesReportEntry.buf) : null;
    sbomPostImportMetricsTelemetry = new SbomPostImportMetricsTelemetry();
    // initialize non null cpe telemetry to avoid null checks later
    this.cpeResultsTelemetry = cpeResultsTelemetry != null ? cpeResultsTelemetry : new CpeResultsTelemetry();
    dependencyTreeParser.parse(dependenciesJsonData);

    // create an original SBOM and filtered scan file for continuous monitoring in the case of binary scans
    if (ThirdPartySbomMetadataStatus.PENDING.equals(sbomMetadata.getStatus()) &&
        SbomScanType.BINARY.toString().equals(sbomMetadata.getScanType()))
    {
      originalBom = createNewBom();
      filteredBom = createNewBom();
    }
  }

  private void mergeSonatypeDataWithSbomData(ThirdPartySbomMetadata sbomMetadata, String scanId) {
    MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> hdsComponentResultsByComponentRef =
        new ArrayListValuedHashMap<>();
    MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> hdsComponentResultsBySonatypeId =
        new ArrayListValuedHashMap<>();
    Map<ComponentIdentifier, JsonNode> hdsResultsWithoutUniqueIdentifier = new LinkedHashMap<>();
    groupHdsResultsByComponentRefOrSonatypeId(bomJsonData, hdsComponentResultsByComponentRef,
        hdsComponentResultsBySonatypeId, hdsResultsWithoutUniqueIdentifier, sbomMetadata.getThirdPartyFileId());
    if (MapUtils.isEmpty(hdsSecurityResults) && MapUtils.isEmpty(hdsLicenseResults)) {
      // Scenario: In this case no vulnerabilities or licenses were found in HDS so the merging process is skipped.
      // We still need to gather telemetry data.
      generateTelemetryForUnverifiedVulnerabilitiesOnlyScenario(scanId);
    }
    doMerge(hdsComponentResultsByComponentRef, hdsComponentResultsBySonatypeId, hdsResultsWithoutUniqueIdentifier,
        scanId, sbomMetadata);
  }

  private void doMerge(
      MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> hdsResultsByComponentRef,
      MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> hdsResultsBySonatypeId,
      Map<ComponentIdentifier, JsonNode> resultsWithoutUniqueIdentifier,
      String scanId,
      ThirdPartySbomMetadata sbomMetadata)
  {
    boolean useComponentRef = !hdsResultsByComponentRef.isEmpty();

    MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> hdsComponentResults =
        hdsResultsByComponentRef.isEmpty() ? hdsResultsBySonatypeId : hdsResultsByComponentRef;

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      tx.begin();
      mergeResultsWithComponentRefOrSonatypeIdentifier(scanId, hdsComponentResults, sbomMetadata, useComponentRef, tx);
      if ((hdsResultsBySonatypeId.isEmpty() && !useComponentRef) || !resultsWithoutUniqueIdentifier.isEmpty() &&
          SbomScanType.BINARY.toString().equals(sbomMetadata.getScanType()))
      {
        mergeResultsWithoutUniqueIdentifier(scanId, resultsWithoutUniqueIdentifier, sbomMetadata, tx);
      }
      tx.commit();
    }
  }

  private void updateReportJsons(final ThirdPartySbomMetadata sbomMetadata, final ApplicationReport applicationReport) {
    // update bom.json only in the case of binary scans
    if (originalBom != null) {
      try {
        applicationReport.saveReportEntry(BOM_JSON.getName(), bomJsonData);
      }
      catch (IOException e) {
        log.debug("Failed to update bom.json in the binary scan application report for thirdPartyFileId {} ",
            sbomMetadata.getThirdPartyFileId(), e);
      }
    }
  }

  private void mergeResultsWithComponentRefOrSonatypeIdentifier(
      final String scanId,
      final MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> hdsComponentResults,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final boolean useComponentRef,
      final TransactionContext tx)
  {

    for (String uniqueIdentifier : hdsComponentResults.keySet()) {
      ThirdPartyFileCoordinate thirdPartyFileCoordinateMatch =
          findFileCoordinateByUniqueIdentifier(useComponentRef, uniqueIdentifier,
              thirdPartySbomMetadata.getThirdPartyFileId());
      if (thirdPartyFileCoordinateMatch == null) {
        log.debug("Internal Error: no ThirdPartyCoordinate record for {} {}",
            useComponentRef ? "componentReference" : "sonatypeIdentifier", uniqueIdentifier);
        continue;
      }

      Collection<Pair<ComponentIdentifier, JsonNode>> hdsComponentMatchesByUniqueIdentifier =
          hdsComponentResults.get(uniqueIdentifier);
      Pair<ComponentIdentifier, JsonNode> hdsComponentMatch;
      if (CollectionUtils.size(hdsComponentMatchesByUniqueIdentifier) == 1) {
        // single result from HDS for a unique identifier
        hdsComponentMatch = hdsComponentMatchesByUniqueIdentifier.iterator().next();
      }
      else {
        SbomResultsMatcherTelemetry bestMatchResultsTelemetry = new SbomResultsMatcherTelemetry();
        bestMatchResultsTelemetries.add(bestMatchResultsTelemetry);
        // more than 1 hds result for unique identifier, perform best match
        hdsComponentMatch = SbomResultsMatcher.bestMatch(thirdPartyFileCoordinateMatch,
            hdsComponentMatchesByUniqueIdentifier, bestMatchResultsTelemetry);
      }
      mergeThirdPartyComponentMatchWithHdsResults(scanId, thirdPartySbomMetadata, hdsComponentMatch,
          thirdPartyFileCoordinateMatch, tx);
    }
  }

  private ThirdPartyFileCoordinate findFileCoordinateByUniqueIdentifier(
      final boolean useComponentRef,
      final String uniqueIdentifier,
      final String thirdPartyFileId)
  {
    if (useComponentRef) {
      return thirdPartyFileCoordinateDAO.getByComponentRef(uniqueIdentifier, thirdPartyFileId);
    }
    else {
      return thirdPartyFileCoordinateDAO.getById(uniqueIdentifier);
    }
  }

  private void mergeResultsWithoutUniqueIdentifier(
      final String scanId,
      final Map<ComponentIdentifier, JsonNode> resultsWithoutUniqueIdentifier,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final TransactionContext tx)
  {
    Set<String> alreadyInsertedComponentIds = new HashSet<>();
    boolean isBinaryScan = thirdPartySbomMetadata.getScanType().equals(SbomScanType.BINARY.toString());
    for (Entry<ComponentIdentifier, JsonNode> resultEntry : resultsWithoutUniqueIdentifier.entrySet()) {
      ComponentIdentifier bomComponentIdentifier = resultEntry.getKey();
      JsonNode bomNode = resultEntry.getValue();
      PackageUrlIdentifier bomPurl = PackageUrlIdentifier.fromComponentIdentifier(bomComponentIdentifier);

      ThirdPartyFileCoordinate sbomComponent = null;
      if (isBinaryScan) {
        sbomComponent = addNewComponentForBinaryScan(bomNode, sbomComponent,
            thirdPartySbomMetadata.getThirdPartyFileId(), tx);
      }
      else {
        sbomComponent = thirdPartyFileCoordinateDAO.getByPackageUrlAndHashAndScanId(
            bomPurl.getPackageUrl(), bomNode.get("hash").asText(), scanId);
        if (sbomComponent == null) {
          // fallback to coordinate matching
          sbomComponent = findExistingComponentUsingCoordinates(bomComponentIdentifier, bomPurl, scanId,
              alreadyInsertedComponentIds);
        }
      }

      mergeComponentData(scanId, bomNode, bomPurl.getPackageUrl(), sbomComponent, bomComponentIdentifier,
          thirdPartySbomMetadata, tx);
    }
  }

  private void mergeThirdPartyComponentMatchWithHdsResults(
      final String scanId,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final Pair<ComponentIdentifier, JsonNode> hdsComponentMatch,
      final ThirdPartyFileCoordinate thirdPartyFileCoordinate,
      final TransactionContext tx)
  {
    JsonNode hdsComponentJsonNode = hdsComponentMatch.getValue();
    ComponentIdentifier componentIdentifier = hdsComponentMatch.getKey();
    addNewComponentForBinaryScan(hdsComponentJsonNode, thirdPartyFileCoordinate,
        thirdPartyFileCoordinate.getThirdPartyFileId(), tx);
    mergeComponentData(scanId, hdsComponentJsonNode, getPurl(hdsComponentJsonNode, componentIdentifier),
        thirdPartyFileCoordinate, componentIdentifier, thirdPartySbomMetadata, tx);
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

  private void mergeComponentData(
      final String scanId,
      final JsonNode componentJsonNode,
      final String bomPurl,
      final ThirdPartyFileCoordinate sbomComponent,
      final ComponentIdentifier bomComponentIdentifier,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final TransactionContext tx)
  {
    if (sbomComponent == null) {
      log.debug("Could not locate matching third party coordinate entry for component identifier {} and scanId {}",
          bomComponentIdentifier, scanId);
      return;
    }
    // use directDependency if present in bom, if not, only then walk tree
    if (componentJsonNode.get("directDependency") != null) {
      sbomComponent.setDependencyType(componentJsonNode.get("directDependency").booleanValue() ? "D" : "T");
    }
    else {
      updateComponentDependencyType(sbomComponent);
    }
    if (bomPurl != null && !StringUtils.equals(sbomComponent.getPackageUrl(), bomPurl)) {
      // in certain cases the purl from HDS matched results may be different to the original purl
      // in such cases update the purl to the result purl for consistency with Sonatype data
      sbomComponent.setPackageUrl(bomPurl);
    }

    JsonNode bomHashNode = componentJsonNode.get("hash");
    if (bomHashNode != null && !StringUtils.equals(sbomComponent.getHash(), bomHashNode.asText())) {
      // Update the original hash to the result hash for consistency with Sonatype data
      sbomComponent.setHash(bomHashNode.asText());
    }

    if (StringUtils.isEmpty(sbomComponent.getMatchStateId())) {
      sbomComponent.setMatchStateId(JsonUtils.getNullableString(componentJsonNode.get(FIELD_MATCH_STATE)));
    }
    if (CollectionUtils.isEmpty(sbomComponent.getOccurrencesList())) {
      sbomComponent.setOccurrencesList(JsonUtils.getStringListFromArray(componentJsonNode.get(FIELD_PATHNAMES)));
    }
    if (CollectionUtils.isEmpty(sbomComponent.getFilenamesList())) {
      final List<String> filenames = JsonUtils.getStringListFromArray(componentJsonNode.get(FIELD_FILENAMES));
      if (CollectionUtils.isNotEmpty(filenames)) {
        sbomComponent.setFilenamesList(filenames);
      }
    }
    setSonatypeIdentificationSourceIfApplicable(componentJsonNode, sbomComponent);
    thirdPartyFileCoordinateDAO.update(tx, sbomComponent);
    mergeSecurityData(bomComponentIdentifier, sbomComponent, thirdPartySbomMetadata, tx);
    mergeLicenseData(bomComponentIdentifier, sbomComponent, tx);
  }

  private ThirdPartyFileCoordinate addNewComponentForBinaryScan(
      final JsonNode hdsComponentJsonNode,
      ThirdPartyFileCoordinate sbomDbComponent,
      final String thirdPartyFileId,
      final TransactionContext tx)
  {
    if (ObjectUtils.allNotNull(originalBom, filteredBom)) {
      List<ThirdPartyCoordinateSecurity> disclosedVulnerabilities = null;
      List<ThirdPartyCoordinateLicense> disclosedLicenses = null;
      String newComponentBomRef = UUID.randomUUID().toString().replace("-", "");
      String newComponentRef = DigestUtils.sha1Hex(newComponentBomRef);
      if (sbomDbComponent == null) {
        sbomDbComponent = createAndSaveComponentInThirdPartyDatabase(newComponentBomRef, hdsComponentJsonNode,
            thirdPartyFileId, tx);
      }
      else {
        disclosedVulnerabilities = thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomDbComponent.getId());
        disclosedLicenses = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(sbomDbComponent.getId());
        sbomDbComponent.setComponentRef(newComponentRef);
        thirdPartyFileCoordinateDAO.update(tx, sbomDbComponent);
      }
      // replace any existing component refs with the new component ref
      ArrayNode componentRefs = ((ObjectNode) hdsComponentJsonNode).putArray(PROPERTY_COMPONENT_REFS);
      componentRefs.add(newComponentRef);
      createAndSaveComponentInBom(newComponentBomRef, sbomDbComponent, disclosedVulnerabilities, disclosedLicenses,
          hdsComponentJsonNode);
    }
    return sbomDbComponent;
  }

  private void createAndSaveComponentInBom(
      String bomRef,
      ThirdPartyFileCoordinate thirdPartyFileCoordinate,
      List<ThirdPartyCoordinateSecurity> disclosedVulns,
      List<ThirdPartyCoordinateLicense> disclosedLicenses,
      JsonNode bomNode)
  {
    Component component = thirdPartyFileCoordinateToBomComponent(thirdPartyFileCoordinate, bomRef);
    addOccurrenceEvidenceForComponent(bomNode, component);
    originalBom.addComponent(component);
    // merge has not happened yet, so at this point only disclosed vulnerabilities and licenses can exist
    // include them as disclosed
    addDisclosedVulnerabilities(disclosedVulns, component);
    addDisclosedLicenses(disclosedLicenses, component);

    Component clone = thirdPartyFileCoordinateToBomComponent(thirdPartyFileCoordinate, bomRef);
    clone.addProperty(
        SbomExportUtils.createCycloneDxProperty(PROPERTY_COMPONENT_REF, DigestUtils.sha1Hex(bomRef)));
    // Deprecated. this should be removed after SBOM-1208 is done
    Property sonatypeIdentifierComponentProperty = new Property();
    sonatypeIdentifierComponentProperty.setName("sonatypeIdentifier");
    sonatypeIdentifierComponentProperty.setValue(thirdPartyFileCoordinate.getId());
    clone.addProperty(sonatypeIdentifierComponentProperty);
    filteredBom.addComponent(clone);
  }

  private void addOccurrenceEvidenceForComponent(JsonNode bomNode, Component component) {
    List<String> pathnames = JsonUtils.getStringListFromArray(bomNode.get(FIELD_PATHNAMES));
    if (CollectionUtils.isEmpty(pathnames)) {
      return;
    }
    List<Occurrence> occurrences = pathnames.stream()
        .map(p -> {
          Occurrence o = new Occurrence();
          o.setLocation(SbomCycloneDxUtils.getFilteredPathname(p));
          return o;
        })
        .collect(Collectors.toList());
    Evidence evidence = new Evidence();
    evidence.setOccurrences(occurrences);
    component.setEvidence(evidence);
  }

  private void addDisclosedVulnerabilities(
      List<ThirdPartyCoordinateSecurity> disclosedVulns,
      Component component)
  {
    if (CollectionUtils.isNotEmpty(disclosedVulns)) {
      initVulnerabilities(originalBom);
      List<Vulnerability> newBomVulnerabilities = new ArrayList<>();
      disclosedVulns.forEach(vuln -> {
        ThirdPartyVulnerabilityExploitabilityExchange vex =
            thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(vuln.getId(),
                vuln.getRefId());
        newBomVulnerabilities.add(createCycloneDxVulnerabilityFromDbData(component, vuln, vex));
      });
      originalBom.getVulnerabilities().addAll(newBomVulnerabilities);
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
        licenseChoice.addLicense(createCycloneDxLicenseForThirdpartyLicense(license));
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
    SbomCycloneDxUtils.addSonatypeTruncatedSha1(fileCoordinate.getHash(), component);
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
      if (CVE.equalsIgnoreCase(sonatypeVulnerabilityData.getSource())) {
        thirdPartySecurity.setVulnerabilitySource(NVD);
      }
      else {
        thirdPartySecurity.setVulnerabilitySource(sonatypeVulnerabilityData.getSource().toUpperCase(Locale.ROOT));
      }
    }
    if (sonatypeVulnerabilityData.getResearchType() != null) {
      thirdPartySecurity.setResearchType(sonatypeVulnerabilityData.getResearchType().name());
    }
    if (sonatypeVulnerabilityData.getDetectionType() != null) {
      thirdPartySecurity.setDetectionType(sonatypeVulnerabilityData.getDetectionType().getId());
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
    // When HDS supplies aliases, they replace any vuln_ids previously persisted by
    // SbomResultHandler.parseVulnerability from the user-uploaded SBOM's <references>.
    // If HDS returns no aliases, the SBOM-sourced ids are preserved (we treat HDS as
    // authoritative only when it has data to assert).
    thirdPartySecurity.setVulnIdsFromList(sonatypeVulnerabilityData.getAliases());
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
      final String bomRef,
      final JsonNode componentNode,
      final String thirdPartyFileId,
      final TransactionContext tx)
  {
    ThirdPartyFileCoordinate component = new ThirdPartyFileCoordinate();
    component.setComponentRef(DigestUtils.sha1Hex(bomRef));
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
    String bomPurl = getPurl(componentNode, componentIdentifier);
    component.setPackageUrl(bomPurl);
    component.setSource("Sonatype");
    setSonatypeIdentificationSourceIfApplicable(componentNode, component);
    return thirdPartyFileCoordinatePersister.persist(tx, component);
  }

  private String getPurl(JsonNode bomNode, ComponentIdentifier bomComponentIdentifier) {
    if (bomNode.get("packageUrl") != null) {
      return bomNode.get("packageUrl").asText();
    }
    return PackageUrlIdentifier.fromComponentIdentifier(bomComponentIdentifier).getPackageUrl();
  }

  private void readSonatypeSecurityResults(final ApplicationReport applicationReport) throws IOException {
    ContainerNode<?> securityJsonData =
        JsonUtils.parse(Objects.requireNonNull(applicationReport.getEntry(SECURITY_JSON.getName())).buf);
    ArrayNode securityJsonArray = (ArrayNode) securityJsonData.get("aaData");
    for (JsonNode securityJsonNode : securityJsonArray) {
      ComponentIdentifier securityComponentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(securityJsonNode);
      if (JsonUtils.getNullableString(securityJsonNode.get(FIELD_MATCH_STATE)) != null) {
        SecurityVulnerability securityVulnerability = loadSecurityJson(securityJsonNode);
        if (securityVulnerability != null) {
          hdsSecurityResults.computeIfAbsent(securityComponentIdentifier, componentIdentifier -> new HashSet<>())
              .add(securityVulnerability);
        }
      }
    }
  }

  private void readSonatypeLicenseResults(final ApplicationReport applicationReport) throws IOException {
    ContainerNode<?> licensesJsonData =
        JsonUtils.parse(Objects.requireNonNull(applicationReport.getEntry(LICENSES_JSON.getName())).buf);
    ArrayNode licenseJsonArray = (ArrayNode) licensesJsonData.get("aaData");
    for (JsonNode licenseJsonNode : licenseJsonArray) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(licenseJsonNode);
      List<String> licenseIds = JsonUtils.getStringListFromArray(licenseJsonNode.get(FIELD_EFFECTIVE_LICENSES));
      // later we may need to fall back to declared licenses if effective licenses is empty
      if (CollectionUtils.isNotEmpty(licenseIds)) {
        for (String licenseId : licenseIds) {
          hdsLicenseResults.computeIfAbsent(componentIdentifier, identifier -> new HashMap<>())
              .put(licenseId, licenseJsonNode);
        }
      }
    }
  }

  private void groupHdsResultsByComponentRefOrSonatypeId(
      final ContainerNode<?> bomJsonData,
      final MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> resultsWithComponentRef,
      final MultiValuedMap<String, Pair<ComponentIdentifier, JsonNode>> resultsWithSonatypeId,
      final Map<ComponentIdentifier, JsonNode> resultsWithoutUniqueIdentifier,
      final String thirdPartyFileId)
  {
    ArrayNode bomJsonArray = (ArrayNode) bomJsonData.get("aaData");
    for (JsonNode bomComponentNode : bomJsonArray) {
      String matchStateString = bomComponentNode.get(MATCH_STATE).asText();
      MatchState matchState = MatchState.getById(matchStateString);
      if (MatchState.UNKNOWN.equals(matchState)) {
        continue;
      }
      ComponentIdentifier bomComponentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(bomComponentNode);
      if (bomComponentIdentifier == null) {
        log.debug("matched bom.json entry found without a component identifier {}", bomComponentNode);
        continue;
      }
      List<String> bomNodeComponentRefs =
          JsonUtils.getStringListFromArray(bomComponentNode.get(PROPERTY_COMPONENT_REFS));
      String bomNodeComponentRef = JsonUtils.getNullableString(bomComponentNode.get("componentRef"));
      String sonatypeIdentifier = JsonUtils.getNullableString(bomComponentNode.get("sonatypeIdentifier"));
      if (CollectionUtils.isNotEmpty(bomNodeComponentRefs)) {
        Optional<String> mergedComponentRef =
            thirdPartyFileCoordinatePersister.consolidate(bomNodeComponentRefs, thirdPartyFileId);
        mergedComponentRef.ifPresent(componentRef -> resultsWithComponentRef.put(componentRef,
            Pair.of(bomComponentIdentifier, bomComponentNode)));
      }
      else if (StringUtils.isNotBlank(bomNodeComponentRef)) {
        resultsWithComponentRef.put(bomNodeComponentRef, Pair.of(bomComponentIdentifier, bomComponentNode));
      }
      else if (StringUtils.isNotBlank(sonatypeIdentifier)) {
        resultsWithSonatypeId.put(sonatypeIdentifier, Pair.of(bomComponentIdentifier, bomComponentNode));
      }
      else {
        resultsWithoutUniqueIdentifier.put(bomComponentIdentifier, bomComponentNode);
      }
    }
  }

  private void createAndSaveFilteredScanFile(final ThirdPartySbomMetadata sbomMetadata) {
    if (filteredBom == null) {
      return;
    }

    String bomString = generateBomString(filteredBom);
    Application app = applicationDAO.getById(sbomMetadata.getApplicationId());
    ThirdPartyScan tpScan = thirdPartyScanDAO.getByThirdPartyFileId(sbomMetadata.getThirdPartyFileId());
    ScanResult scanResult = sbomMetadataUtils.scanSbomContent(app, bomString, SbomFormat.JSON, ItemContentType.SBOM,
        ScannerDriver.SBOM_API);

    String filteredScanFileName = SbomCommonUtils.newFilteredScanFileName(tpScan.getScanId());
    ScanEntity filteredScanEntity =
        scanPersistenceService.getScanByName(sbomMetadata.getApplicationId(), filteredScanFileName);
    // in the case of cli/container scans the filtered scan file may already exist
    if (!filteredScanEntity.exists()) {
      try {
        scanPersistenceService.copyScanFile(scanResult.getScanEntity(), filteredScanEntity);
        tpScan.setFilteredScanFile(filteredScanEntity.getName());
        thirdPartyScanDAO.update(tpScan);
      }
      catch (IOException e) {
        log.error("Error saving filtered scan file {}", filteredScanEntity.getName(), e);
      }
    }
  }

  private void createAndSaveOriginalSbom(ThirdPartySbomMetadata thirdPartySbomMetadata) throws IOException {
    if (originalBom == null) {
      return;
    }

    // Add the binary file name as a property in the original SBOM
    String binaryFileName = thirdPartySbomMetadata.getOriginalBinaryFileName();
    if (StringUtils.isNotBlank(binaryFileName)) {
      originalBom.getMetadata()
          .addProperty(createCycloneDxProperty(SbomTaxonomy.CDX_ORIGINAL_FILE_PROPERTY_NAME, binaryFileName));
    }

    String bomAsString = generateBomString(originalBom);
    try (InputStream inputStream = new ByteArrayInputStream(bomAsString.getBytes())) {
      thirdPartyPersistenceService.saveSbomForBinary(inputStream, thirdPartySbomMetadata);
    }
  }

  private void updateComponentDependencyType(final ThirdPartyFileCoordinate sbomComponent) {
    if (StringUtils.isBlank(sbomComponent.getPackageUrl())) {
      return;
    }
    try {
      ComponentIdentifier sbomComponentIdentifier =
          ComponentIdentifierAdapter.toComponentIdentifier(sbomComponent.getPackageUrl());
      Optional<String> dependencyType = dependencyTreeParser.getDependencyType(sbomComponentIdentifier);
      dependencyType.ifPresent(sbomComponent::setDependencyType);
    }
    catch (InvalidComponentIdentifierException e) {
      log.debug(
          "There was an error while trying to convert the purl into a component identifier - {purl: {}, " +
              "componentName: {}, componentHash: {}, componentVersion: {}}",
          sbomComponent.getPackageUrl(), sbomComponent.getName(), sbomComponent.getHash(), sbomComponent.getVersion(),
          e);
    }
  }

  private void mergeSecurityData(
      final ComponentIdentifier bomComponentIdentifier,
      final ThirdPartyFileCoordinate sbomComponent,
      final ThirdPartySbomMetadata thirdPartySbomMetadata,
      final TransactionContext tx)
  {
    Set<SecurityVulnerability> sonatypeVulns = hdsSecurityResults.get(bomComponentIdentifier);
    if (CollectionUtils.isNotEmpty(sonatypeVulns)) {
      boolean isCpeMatchedComponent = false;
      // Get all the coordinate securities from the DB for all the vulnerabilities.
      Map<String, ThirdPartyCoordinateSecurity> coordinateSecuritiesFromDBForComponentMap =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomComponent.getId())
              .stream()
              .collect(Collectors.toMap(ThirdPartyCoordinateSecurity::getRefId, t -> t));
      sbomPostImportMetricsTelemetry.addToTotalVulnerabilitiesCount(coordinateSecuritiesFromDBForComponentMap.size());

      for (SecurityVulnerability sonatypeVuln : sonatypeVulns) {
        try {
          ThirdPartyCoordinateSecurity sbomVulnerability = coordinateSecuritiesFromDBForComponentMap
              .get(sonatypeVuln.getRefId());
          if (sbomVulnerability != null) {
            // matching sbom vulnerability found, update record
            if (sonatypeVuln.getSeverity() != null && sonatypeVuln.getSeverity() > 0) {
              populateMissingThirdPartyCoordinateSecurityWithSonatypeData(sbomVulnerability, sonatypeVuln);
              thirdPartyCoordinateSecurityDAO.update(tx, sbomVulnerability);
              coordinateSecuritiesFromDBForComponentMap.remove(sonatypeVuln.getRefId());
              sbomPostImportMetricsTelemetry.incrementVerifiedVulnerabilityCount();
            }
          }
          else {
            // no matching sbom vulnerability, insert sonatype data
            ThirdPartyCoordinateSecurity newThirdPartySecurity = new ThirdPartyCoordinateSecurity();
            newThirdPartySecurity.setFileCoordinateId(sbomComponent.getId());
            newThirdPartySecurity.setRefId(sonatypeVuln.getRefId());
            newThirdPartySecurity.setSbomMetadataId(thirdPartySbomMetadata.getId());
            populateMissingThirdPartyCoordinateSecurityWithSonatypeData(newThirdPartySecurity, sonatypeVuln);
            thirdPartyCoordinateSecurityDAO.insertSafely(tx, newThirdPartySecurity);
            sbomPostImportMetricsTelemetry.incrementAdditionalVulnerabilitiesCount();
          }
        }
        catch (NotFoundException exception) {
          log.warn("Vulnerability {} not found", sonatypeVuln);
        }
        if (!isCpeMatchedComponent &&
            SecurityVulnerabilityDetectionType.CPE_MATCH.equals(sonatypeVuln.getDetectionType()))
        {
          isCpeMatchedComponent = true;
        }
      }

      // Walk through the remaining coordinate securities in this list. These are the orphan ones.
      // NOTE: identification source can be SONATYPE, SBOM or both. Frontend code will need to handle this.
      for (String refId : coordinateSecuritiesFromDBForComponentMap.keySet()) {
        ThirdPartyCoordinateSecurity coordinateSecurity = coordinateSecuritiesFromDBForComponentMap.get(refId);
        if (coordinateSecurity.getIdentificationSources().contains(IdentificationSource.SONATYPE.getId()) &&
            coordinateSecurity.getIdentificationSources().contains(IdentificationSource.SBOM.getId()))
        {
          // if the vulnerability is found in DB but no HDS result, and has both identification sources,
          // SBOM and SONATYPE, remove SONATYPE.
          coordinateSecurity.setIdentificationSources(IdentificationSource.SBOM.getId());
          thirdPartyCoordinateSecurityDAO.update(tx, coordinateSecurity);
          sbomPostImportMetricsTelemetry.incrementUnverifiedVulnerabilityCount();
        }
        else if (coordinateSecurity.getIdentificationSources().equals(IdentificationSource.SONATYPE.getId())) {
          // else if it only has SONATYPE, delete it from the DB along with any VEX annotation associated with it.
          thirdPartyCoordinateSecurityDAO.delete(tx, coordinateSecurity);
          // we skip the loop to avoid incrementing the telemetry count for unmatched vulnerabilities for this case
          continue;
        }
        if (isCpeMatchedComponent) {
          this.cpeResultsTelemetry.incrementCpeUnMatchedVulnerabilityCount();
        }
      }
    }
    else {
      // Scenario: There are no vulnerabilities found in HDS for this component. We still have to gather telemetry data.
      Map<String, ThirdPartyCoordinateSecurity> coordinateSecuritiesFromDBForComponentMap =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(sbomComponent.getId())
              .stream()
              .collect(Collectors.toMap(ThirdPartyCoordinateSecurity::getRefId, t -> t));
      sbomPostImportMetricsTelemetry.addToTotalVulnerabilitiesCount(
          coordinateSecuritiesFromDBForComponentMap.size());
      sbomPostImportMetricsTelemetry.addToUnverifiedVulnerabilityCount(
          coordinateSecuritiesFromDBForComponentMap.size());
    }
  }

  private void mergeLicenseData(
      final ComponentIdentifier bomComponentIdentifier,
      final ThirdPartyFileCoordinate sbomComponent,
      final TransactionContext tx)
  {
    Map<String, JsonNode> sonatypeLicenses = hdsLicenseResults.get(bomComponentIdentifier);
    if (MapUtils.isNotEmpty(sonatypeLicenses)) {
      List<ThirdPartyCoordinateLicense> sbomComponentLicenses =
          thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(sbomComponent.getId());
      ArrayList<ThirdPartyCoordinateLicense> allLicensesFromDBForComponent = new ArrayList<>(sbomComponentLicenses);
      Map<String, ThirdPartyCoordinateLicense> byLicenseIds =
          sbomComponentLicenses.stream().collect(Collectors.toMap(ThirdPartyCoordinateLicense::getLicenseId, cl -> cl));
      Map<String, ThirdPartyCoordinateLicense> byLicenseNames = sbomComponentLicenses.stream()
          .collect(
              Collectors.toMap(ThirdPartyCoordinateLicense::getName, Function.identity(), (first, second) -> first));
      for (Entry<String, JsonNode> sonatypeLicenseEntry : sonatypeLicenses.entrySet()) {
        String resultEntryLicense = sonatypeLicenseEntry.getKey();
        if (SbomCommonUtils.isUnsupportedLicenseId(resultEntryLicense)) {
          // there is no valid license identified by sonatype, so no point storing it in database
          continue;
        }
        ThirdPartyCoordinateLicense sbomLicense = byLicenseIds.get(resultEntryLicense);
        if (sbomLicense == null) {
          sbomLicense = byLicenseNames.get(resultEntryLicense);
        }

        if (sbomLicense != null) {
          // SBOM license found in DB and in json file, update record.
          populateMissingThirdPartyCoordinateLicenseWithSonatypeData(sbomLicense, sonatypeLicenseEntry.getValue());
          thirdPartyCoordinateLicenseDAO.update(tx, sbomLicense);
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
          thirdPartyCoordinateLicenseDAO.insertSafely(tx, newThirdPartyLicense);
        }
      }

      // Walk through the licenses that were left in this list. These are the orphan ones.
      for (ThirdPartyCoordinateLicense licenseFromDB : allLicensesFromDBForComponent) {
        if (licenseFromDB.getIdentificationSources().contains(IdentificationSource.SONATYPE.getId()) &&
            licenseFromDB.getIdentificationSources().contains(IdentificationSource.SBOM.getId()))
        {
          // If the license was found in the DB but NOT in the json file, and the identification source is
          // SBOM,Sonatype, remove Sonatype.
          licenseFromDB.setIdentificationSources(IdentificationSource.SBOM.getId());
          thirdPartyCoordinateLicenseDAO.update(tx, licenseFromDB);
        }
        else if (licenseFromDB.getIdentificationSources().equals(IdentificationSource.SONATYPE.getId())) {
          // If the license was found in the DB but NOT in the json file, and the identification source is only SBOM,
          // delete it from the DB.
          thirdPartyCoordinateLicenseDAO.delete(tx, licenseFromDB);
        }
      }
    }
  }

  private String generateBomString(final Bom bom) {
    try {
      return BomGeneratorFactory.createJson(Version.VERSION_16, bom).toJsonString();
    }
    catch (GeneratorException e) {
      throw new SbomExportException("An error occurred while trying to parse the SBOM's content to JSON string", e);
    }
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
      SecurityVulnerabilityResearchType researchType = SecurityVulnerabilityResearchType.getResearchType(
          JsonUtils.getNullableString(securityJsonNode.get("researchType")));
      securityVulnerability.setResearchType(researchType);
      SecurityVulnerabilityDetectionType detectionType = SecurityVulnerabilityDetectionType.getDetectionType(
          JsonUtils.getNullableString(securityJsonNode.get("detectionType")));
      securityVulnerability.setDetectionType(detectionType);

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
        applicationReportPersistenceService.deleteReport(applicationId, thirdPartyScan.getPreviousScanId());
      }
      thirdPartyScan.setPreviousScanId(null);
      thirdPartyScanDAO.update(thirdPartyScan);
    }
  }

  private void generateTelemetryForUnverifiedVulnerabilitiesOnlyScenario(final String scanId) {
    List<ThirdPartyFileCoordinate> components = thirdPartyFileCoordinateDAO.getByScanId(scanId);
    for (ThirdPartyFileCoordinate component : components) {
      Map<String, ThirdPartyCoordinateSecurity> coordinateSecuritiesFromDBForComponentMap =
          thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(component.getId())
              .stream()
              .collect(Collectors.toMap(ThirdPartyCoordinateSecurity::getRefId, t -> t));
      sbomPostImportMetricsTelemetry.addToTotalVulnerabilitiesCount(
          coordinateSecuritiesFromDBForComponentMap.size());
      sbomPostImportMetricsTelemetry.addToUnverifiedVulnerabilityCount(
          coordinateSecuritiesFromDBForComponentMap.size());
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

  private ThirdPartyFileCoordinate findExistingComponentUsingCoordinates(
      ComponentIdentifier bomComponentIdentifier,
      PackageUrlIdentifier bomPurl,
      String scanId,
      Set<String> alreadyInsertedComponentIds)
  {
    ThirdPartyFileCoordinate sbomComponent = thirdPartyFileCoordinateDAO.getByFormatNameVersionAndScanID(
        bomComponentIdentifier.getFormat(), bomPurl.getName(), bomPurl.getVersion(), scanId);
    // Find if the found db record is in the set of inserted id.
    if (sbomComponent != null && alreadyInsertedComponentIds.contains(sbomComponent.getId())) {
      // We set it to null to force create a new record instead of updating the other similar record
      sbomComponent = null;
    }
    return sbomComponent;
  }

  private void setSonatypeIdentificationSourceIfApplicable(
      JsonNode componentJsonNode,
      ThirdPartyFileCoordinate thirdPartyFileCoordinate)
  {
    try {
      AnalyzerFeatures analyzerFeatures =
          JsonUtils.asPojo(componentJsonNode.get(FIELD_ANALYZER_FEATURES), AnalyzerFeatures.class);
      if (analyzerFeatures != null && analyzerFeatures.getAnalysisSource() == AnalysisSource.SDS) {
        thirdPartyFileCoordinate.addIdentificationSource("Sonatype");
      }
    }
    catch (IOException e) {
      log.debug("Unable to read analysis features for component", e);
    }
  }
}
