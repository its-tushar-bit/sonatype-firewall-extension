/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.search.SearchIndexManager;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateLicenseDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchangeDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.vulnerability.SecurityVulnerabilityDataService;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.ThirdPartyVulnerabilityExploitabilityExchangeRowDTO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Swid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.DependencyResolver.MATCH_STATE;

@Named
public class ThirdPartyDataService
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyDataService.class);

  public static final String FIELD_EFFECTIVE_LICENSES = "effectiveLicenses";

  public static final String FIELD_REFERENCE = "reference";

  public static final String FIELD_LICENSE_NAME = "name";

  public static final String FIELD_LICENSE_URL = "url";

  public static final int MAX_RECURSION_DEPTH = 100000;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  private final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private final SearchIndexManager searchIndexManager;

  private final SecurityVulnerabilityDataService securityVulnerabilityDataService;

  private final ProductLicense productLicense;

  private final InsightWork insightWork;

  @Inject
  public ThirdPartyDataService(
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO,
      final ThirdPartyComponentDAO thirdPartyComponentDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      SearchIndexManager searchIndexManager,
      final SecurityVulnerabilityDataService securityVulnerabilityDataService,
      final ProductLicense productLicense,
      final InsightWork insightWork)
  {
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyVulnerabilityExploitabilityExchangeDAO = thirdPartyVulnerabilityExploitabilityExchangeDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.thirdPartyCoordinateLicenseDAO = thirdPartyCoordinateLicenseDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.thirdPartyVulnerabilityDAO = thirdPartyVulnerabilityDAO;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.searchIndexManager = searchIndexManager;
    this.securityVulnerabilityDataService = securityVulnerabilityDataService;
    this.productLicense = productLicense;
    this.insightWork = insightWork;
  }

  public ThirdPartyApplicationReportDTO getScanData(final String scanId) {
    List<ThirdPartyScan> scanData = thirdPartyScanDAO.getByScanId(scanId);
    if (!scanData.isEmpty()) {
      log.debug("Found {} third party scan data files for scanId {}", scanData.size(), scanId);
      return loadThirdPartyDataForScan(scanId, scanData.get(0).getCreateTime());
    }
    return null;
  }

  public List<ThirdPartyCoordinateSecurity> getSecurityVulnerabilitiesForScanId(final String scanId) {
    List<ThirdPartyFileCoordinate> coordsByScanId = thirdPartyFileCoordinateDAO.getByScanId(scanId);
    if (coordsByScanId.isEmpty()) {
      return Collections.emptyList();
    }
    return thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(
        coordsByScanId.stream().map(ThirdPartyFileCoordinate::getId).collect(Collectors.toList()));
  }

  public void deleteByScanId(String scanId) throws IOException {
    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByScanId(scanId);
    if (sbomMetadata != null) {
      Files.deleteIfExists(
          insightWork.getSbomDir(sbomMetadata.getApplicationId()).toPath().resolve(sbomMetadata.getFilename()));
    }
    thirdPartyFileDAO.deleteByScanId(scanId);
  }

  private ThirdPartyApplicationReportDTO loadThirdPartyDataForScan(
      String scanId,
      final Date scanTime)
  {
    ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = new ThirdPartyApplicationReportDTO();

    List<ThirdPartyFile> scanFiles = thirdPartyFileDAO.getByScanId(scanId);

    Multimap<String, ThirdPartyFileCoordinate> coordinates = ArrayListMultimap.create();
    for (ThirdPartyFile scanFile : scanFiles) {
      thirdPartyFileCoordinateDAO.getByThirdPartyFileId(scanFile.getId())
          .forEach(coord -> coordinates.put(coord.getHash(), coord));
    }

    for (Entry<String,Collection<ThirdPartyFileCoordinate>> multimap : coordinates.asMap().entrySet()) {
      try {
        List<ThirdPartyFileCoordinate> mapValues = (List<ThirdPartyFileCoordinate>) multimap.getValue();
        ThirdPartyFileCoordinate coord = mapValues.get(0);
        ComponentIdentifier componentIdentifier = getComponentIdentifier(coord);
        thirdPartyApplicationReportDTO.billOfMaterials.add(toBomRow(mapValues, componentIdentifier, scanTime));
        populateSecurityVulnerabilities(coord, componentIdentifier, thirdPartyApplicationReportDTO);
        populateLicenseInformation(coord, componentIdentifier, thirdPartyApplicationReportDTO);
      }
      catch (InvalidComponentIdentifierException | InvalidPackageURLException e) {
        log.error("Error creating component identifier from third-party data component", e);
      }
    }

    log.debug("Found {} third party components, {} vulnerabilities and {} licenses for scanId {}",
        thirdPartyApplicationReportDTO.billOfMaterials.size(), thirdPartyApplicationReportDTO.securityRows.size(),
        thirdPartyApplicationReportDTO.licenseRows.size(), scanId);
    return thirdPartyApplicationReportDTO;
  }

  //Visible for testing
  ComponentIdentifier getComponentIdentifier(final ThirdPartyFileCoordinate coord) {
    ComponentIdentifier componentIdentifier;
    if (StringUtils.isNotBlank(coord.getPackageUrl())) {
      componentIdentifier = ComponentIdentifierAdapter.toComponentIdentifier(coord.getPackageUrl());
      componentIdentifier.ensureComplete();
    }
    else {
      componentIdentifier =
          ComponentIdentifierAdapter.toComponentIdentifier(coord.getFormat(), coord.getName(), coord.getVersion());
    }
    return componentIdentifier;
  }

  private void populateSecurityVulnerabilities(
      final ThirdPartyFileCoordinate coord,
      final ComponentIdentifier componentIdentifier,
      final ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO)
  {
    thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(coord.getId()).forEach(
        sec -> thirdPartyApplicationReportDTO.securityRows.add(toSecurityRow(sec, componentIdentifier, coord)));
  }

  private ThirdPartyHealthCheckReportSecurityRowDTO toSecurityRow(
      final ThirdPartyCoordinateSecurity coordinateSecurity,
      final ComponentIdentifier componentIdentifier,
      final ThirdPartyFileCoordinate coordinate)
  {
    final ThirdPartyHealthCheckReportSecurityRowDTO dto =
        new ThirdPartyHealthCheckReportSecurityRowDTO(componentIdentifier, coordinate.getHash());
    dto.matchState = MatchState.EXACT.toString();
    dto.reference = coordinateSecurity.getRefId();
    dto.description = coordinateSecurity.getDescription();
    dto.score = BigDecimal.valueOf(coordinateSecurity.getSeverity()).setScale(2, RoundingMode.UNNECESSARY).floatValue();
    dto.url = coordinateSecurity.getLink();
    dto.fixedVersion = coordinateSecurity.getFixedBy();
    dto.source = coordinateSecurity.getVulnerabilitySource();
    dto.cwe = coordinateSecurity.getCwes();
    dto.cvssVectorString = coordinateSecurity.getAttackVector();
    dto.cvssVectorSource = coordinateSecurity.getRatingMethod();
    dto.severity = coordinateSecurity.getSeverityDescription();
    dto.ratingMethod = coordinateSecurity.getRatingMethod();
    dto.recommendations = coordinateSecurity.getRecommendations();
    dto.advisories = coordinateSecurity.getAdvisories();

    ThirdPartyVulnerabilityExploitabilityExchange vex = getVex(coordinateSecurity);
    if (vex != null) {
      dto.analysis = new ThirdPartyVulnerabilityExploitabilityExchangeRowDTO();
      dto.analysis.response = vex.getResponse();
      dto.analysis.justification = vex.getJustification();
      dto.analysis.state = vex.getState();
      dto.analysis.detail = vex.getDetail();
    }

    return dto;
  }

  private ThirdPartyVulnerabilityExploitabilityExchange getVex(
      final ThirdPartyCoordinateSecurity coordinateSecurity)
  {
    return thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(
        coordinateSecurity.getId(),
        coordinateSecurity.getRefId());
  }

  private ThirdPartyBillOfMaterialsRowDTO toBomRow(
      final List<ThirdPartyFileCoordinate> coordinates,
      final ComponentIdentifier componentIdentifier,
      final Date scanTime)
  {
    ThirdPartyFileCoordinate coordinate = coordinates.get(0);
    final ThirdPartyBillOfMaterialsRowDTO dto
        = new ThirdPartyBillOfMaterialsRowDTO(componentIdentifier, coordinate.getHash());
    dto.createTime = scanTime.getTime();
    dto.matchState = MatchState.EXACT.toString();
    dto.identificationSource = coordinate.getSource();
    dto.pathnames = coordinates.stream().parallel().map(c -> c.getPackageUrl()).collect(Collectors.toSet());
    dto.setPackageUrl(StringUtils.isNotEmpty(coordinate.getPackageUrl()) ?
        coordinate.getPackageUrl() : PackageUrlIdentifier.toPackageUrl(componentIdentifier));
    dto.cpe = coordinate.getCpe();
    String swid = coordinate.getSwid();
    if (swid != null) {
      try {
        dto.swid = ThirdPartyComponentDAO.MAPPER.readValue(swid, Swid.class);
      }
      catch (JsonProcessingException e) {
        log.debug("Cannot read SWID value from DB", e);
      }
    }
    return dto;
  }

  private void populateLicenseInformation(
      final ThirdPartyFileCoordinate coord,
      final ComponentIdentifier componentIdentifier,
      final ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO)
  {
    List<ThirdPartyCoordinateLicense> licenses = thirdPartyCoordinateLicenseDAO.getByFileCoordinateId(coord.getId());
    final ThirdPartyLicenseRowDTO dto = new ThirdPartyLicenseRowDTO(componentIdentifier, coord.getHash());
    if (!licenses.isEmpty()) {
      licenses.forEach(thirdPartyCoordinateLicense -> addLicense(thirdPartyCoordinateLicense, dto));
    }
    else {
      licenseNotProvided(dto);
    }
    thirdPartyApplicationReportDTO.licenseRows.add(dto);
  }

  private void addLicense(
      final ThirdPartyCoordinateLicense thirdPartyCoordinateLicense,
      final ThirdPartyLicenseRowDTO dto)
  {
    try {
      multiLicenseDAO.getByIdNoReloadNotNull(thirdPartyCoordinateLicense.getLicenseId());
      final ThirdPartyLicenseDTO licenseThirdParty = new ThirdPartyLicenseDTO();
      licenseThirdParty.id = thirdPartyCoordinateLicense.getLicenseId();
      licenseThirdParty.name = thirdPartyCoordinateLicense.getName();
      licenseThirdParty.url = thirdPartyCoordinateLicense.getUrl();
      dto.declaredLicenses.add(licenseThirdParty);
    }
    catch (NotFoundException e) {
      log.debug(e.getMessage());
    }
  }

  private void licenseNotProvided(final ThirdPartyLicenseRowDTO dto) {
    final MultiLicense licenseNotProvided = multiLicenseDAO.getByIdNotNull(License.UNSPECIFIED_ID);

    final ThirdPartyLicenseDTO licenseThirdParty = new ThirdPartyLicenseDTO();
    licenseThirdParty.id = licenseNotProvided.getId();
    licenseThirdParty.name = licenseNotProvided.getShortDisplayName();
    dto.declaredLicenses.add(licenseThirdParty);
  }

  public void indexVulnerabilities(final String scanId) {
    List<ThirdPartyCoordinateSecurity> secVulnerabilities = getSecurityVulnerabilitiesForScanId(scanId);
    Set<ThirdPartyVulnerability> vulnerabilityList =
        secVulnerabilities.stream().map(ThirdPartyVulnerability::new).collect(Collectors.toSet());
    saveOrUpdate(vulnerabilityList);
  }

  public void saveOrUpdate(final Set<ThirdPartyVulnerability> vulnerabilityList) {
    thirdPartyVulnerabilityDAO.saveOrUpdate(vulnerabilityList);
  }

  public ThirdPartyApplicationReportDTO loadThirdPartyInfrastructureAsCodeData(final File report, final String appId) {
    // Collect data for telemetry within the loop
    Map<String, Integer> inputTypeCount = new HashMap<>();
    Map<String, Integer> providerCount = new HashMap<>();

    int numberOfIacComponents = 0;
    // End telemetry related fields

    ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = new ThirdPartyApplicationReportDTO();
    Map<String, ThirdPartyReportComponentDTO> data = thirdPartyComponentDAO.getData(report);
    if (data == null) {
      return thirdPartyApplicationReportDTO;
    }
    Set<ThirdPartyVulnerability> vulnerabilities = new HashSet<>();
    Collection<ThirdPartyReportComponentDTO> thirdPartyReportComponentDtos = data.values();
    for (ThirdPartyReportComponentDTO componentDTO : thirdPartyReportComponentDtos) {
      if (IdentificationSource.SONATYPE_IAC.getName().equals(componentDTO.bomRow.identificationSource)) {
        // Collect telemetry data
        numberOfIacComponents++;
        collectTelemetryData(inputTypeCount, providerCount, componentDTO);

        // Create security rows
        for (ThirdPartyHealthCheckReportSecurityRowDTO securityRow : componentDTO.securityRows) {
          ThirdPartyVulnerability thirdPartyVulnerability = new ThirdPartyVulnerability();
          thirdPartyVulnerability.setRefId(securityRow.reference);
          thirdPartyVulnerability.setDescription(securityRow.description);
          thirdPartyVulnerability.setSeverity(securityRow.score);
          thirdPartyVulnerability.setAdvisories(securityRow.advisories);
          thirdPartyVulnerability.setVulnerabilitySource(componentDTO.bomRow.identificationSource);
          thirdPartyVulnerability.setUpdateTime(new Date());
          vulnerabilities.add(thirdPartyVulnerability);
        }
        thirdPartyApplicationReportDTO.billOfMaterials.add(componentDTO.bomRow);
        thirdPartyApplicationReportDTO.securityRows.addAll(componentDTO.securityRows);
      }
    }
    saveOrUpdate(vulnerabilities);

    // Send telemetry from collected data
    if (numberOfIacComponents > 0) {
      sendIacMetricsTelemetry(appId, inputTypeCount, providerCount, numberOfIacComponents);
    }

    return thirdPartyApplicationReportDTO;
  }

  @VisibleForTesting
  void indexSbomForSearch(ThirdPartySbomMetadata sbomMetadata) {
    SearchIndexChange searchIndexChange = thirdPartySbomMetadataDAO.newSearchIndexChange(sbomMetadata);
    searchIndexManager.insert(searchIndexChange);
  }

  public void mergeSonatypeDataWithSbomDataWithIndexing(final String scanId, final File reportFile) throws IOException {
    if (!productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)) {
      return;
    }
    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByScanId(scanId);
    if (sbomMetadata == null) {
      return;
    }

    ContainerNode<?> bomJsonData =
        JsonUtils.parse(Objects.requireNonNull(Report.getEntry(reportFile, Report.BOM_JSON_FILENAME)).buf);
    ContainerNode<?> securityJsonData =
        JsonUtils.parse(Objects.requireNonNull(Report.getEntry(reportFile, Report.SECURITY_JSON_FILENAME)).buf);
    ContainerNode<?> licensesJsonData =
        JsonUtils.parse(Objects.requireNonNull(Report.getEntry(reportFile, Report.LICENSES_JSON_FILENAME)).buf);
    final ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);
    ContainerNode<?> dependenciesJsonData =
        dependenciesReportEntry != null ? JsonUtils.parse(dependenciesReportEntry.buf) : null;

    Map<ComponentIdentifier, String> componentDependencyTypeMap = new HashMap<>();
    // populate component dependency type map by walking dependency tree if dependency data is not present in bom.json
    if (bomJsonData.get("dependencyDataIncluded") != null &&
        !bomJsonData.get("dependencyDataIncluded").booleanValue()) {
      populateComponentDependencyTypeMap(dependenciesJsonData, componentDependencyTypeMap);
    }
    mergeSonatypeDataWithSbomData(sbomMetadata, scanId, bomJsonData, securityJsonData, licensesJsonData,
        componentDependencyTypeMap);
    indexSbomForSearch(sbomMetadata);
  }

  private void mergeSonatypeDataWithSbomData(
      ThirdPartySbomMetadata sbomMetadata,
      String scanId,
      ContainerNode<?> bomJsonData,
      ContainerNode<?> securityJsonData,
      ContainerNode<?> licensesJsonData,
      Map<ComponentIdentifier, String> componentDependencyTypeMap)
  {
    Map<ComponentIdentifier, List<String>> sonatypeVulnerabilityResults = readSonatypeSecurityResults(securityJsonData);
    Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults =
        readSonatypeLicenseResults(licensesJsonData);
    if (MapUtils.isEmpty(sonatypeVulnerabilityResults) && MapUtils.isEmpty(sonatypeLicenseResults)) {
      makeSbomActive(sbomMetadata);
      return;
    }

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

      PackageUrlIdentifier bomPurl = PackageUrlIdentifier.fromComponentIdentifier(bomComponentIdentifier);

      ThirdPartyFileCoordinate sbomComponent = thirdPartyFileCoordinateDAO
          .getByFormatNameVersionAndScanID(bomComponentIdentifier.getFormat(), bomPurl.getName(), bomPurl.getVersion(),
              scanId);
      if (sbomComponent == null) {
        log.debug("Could not locate matching third party coordinate entry for component identifier {} and scanId {}",
            bomComponentIdentifier, scanId);
        continue;
      }
      // use directDependency if present in bom, if not, only then walk tree
      if (bomNode.get("directDependency") != null) {
        sbomComponent.setDependencyType(bomNode.get("directDependency").booleanValue() ? "D" : "T");
      }
      else {
        updateComponentDependencyType(sbomComponent, componentDependencyTypeMap);
      }
      updateComponentIdentifiedAsSonatype(sbomComponent);
      mergeSecurityData(sonatypeVulnerabilityResults, bomComponentIdentifier, sbomComponent);
      mergeLicenseData(sonatypeLicenseResults, bomComponentIdentifier, sbomComponent);
    }
    makeSbomActive(sbomMetadata);
  }

  private void makeSbomActive(final ThirdPartySbomMetadata sbomMetadata) {
    sbomMetadata.setStatus(SbomStatus.ACTIVE.toString());
    thirdPartySbomMetadataDAO.update(sbomMetadata);
  }

  private void updateComponentIdentifiedAsSonatype(final ThirdPartyFileCoordinate sbomComponent) {
    sbomComponent.addIdentificationSource(IdentificationSource.SONATYPE.getId());
    thirdPartyFileCoordinateDAO.update(sbomComponent);
  }

  private void updateComponentDependencyType(
      final ThirdPartyFileCoordinate sbomComponent,
      Map<ComponentIdentifier, String> componentDependencyTypeMap)
  {
    ComponentIdentifier sbomComponentIdentifier =
        ComponentIdentifierAdapter.toComponentIdentifier(sbomComponent.getPackageUrl());
    sbomComponent.setDependencyType(componentDependencyTypeMap.get(sbomComponentIdentifier));
  }

  private void populateComponentDependencyTypeMap(
      ContainerNode<?> dependenciesJsonData,
      Map<ComponentIdentifier, String> componentDependencyTypeMap) throws IOException
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

  private void mergeSecurityData(
      final Map<ComponentIdentifier, List<String>> sonatypeSecResults,
      final ComponentIdentifier bomComponentIdentifier,
      final ThirdPartyFileCoordinate sbomComponent)
  {
    List<String> sonatypeVulns = sonatypeSecResults.get(bomComponentIdentifier);
    if (CollectionUtils.isNotEmpty(sonatypeVulns)) {
      for (String sonatypeVuln : sonatypeVulns) {
        ThirdPartyCoordinateSecurity sbomVulnerability =
            thirdPartyCoordinateSecurityDAO.getByCoordinateFileIdAndRefId(sbomComponent.getId(), sonatypeVuln);
        SecurityVulnerabilityData sonatypeVulnerabilityData =
            securityVulnerabilityDataService.getSecurityVulnerabilityDetailsFromHDS(sonatypeVuln,
                bomComponentIdentifier, true);
        if (sbomVulnerability != null) {
          //matching sbom vulnerability found, update record
          if (sonatypeVulnerabilityData != null && sonatypeVulnerabilityData.mainSeverity != null &&
              sonatypeVulnerabilityData.mainSeverity.score > 0) {
            populateMissingThirdPartyCoordinateSecurityWithSonatypeData(sbomVulnerability, sonatypeVulnerabilityData);
            thirdPartyCoordinateSecurityDAO.update(sbomVulnerability);
          }
        }
        else {
          //no matching sbom vulnerability, insert sonatype data
          ThirdPartyCoordinateSecurity newThirdPartySecurity = new ThirdPartyCoordinateSecurity();
          newThirdPartySecurity.setFileCoordinateId(sbomComponent.getId());
          newThirdPartySecurity.setRefId(sonatypeVuln);
          populateMissingThirdPartyCoordinateSecurityWithSonatypeData(newThirdPartySecurity, sonatypeVulnerabilityData);
          thirdPartyCoordinateSecurityDAO.insert(newThirdPartySecurity);
        }
      }
    }
  }

  private void mergeLicenseData(
      final Map<ComponentIdentifier, Map<String, JsonNode>> sonatypeLicenseResults,
      final ComponentIdentifier bomComponentIdentifier,
      final ThirdPartyFileCoordinate sbomComponent)
  {
    Map<String, JsonNode> sonatypeLicenses = sonatypeLicenseResults.get(bomComponentIdentifier);
    if (MapUtils.isNotEmpty(sonatypeLicenses)) {
      for (Entry<String, JsonNode> sonatypeLicenseEntry : sonatypeLicenses.entrySet()) {
        ThirdPartyCoordinateLicense sbomLicense =
            thirdPartyCoordinateLicenseDAO.getByFileCoordinateIdAndLicenseId(sbomComponent.getId(),
                sonatypeLicenseEntry.getKey());
        if (sbomLicense != null) {
          //matching sbom license found, update record
          populateMissingThirdPartyCoordinateLicenseWithSonatypeData(sbomLicense, sonatypeLicenseEntry.getValue());
          thirdPartyCoordinateLicenseDAO.update(sbomLicense);
        }
        else {
          //no matching sbom license, insert sonatype data
          ThirdPartyCoordinateLicense newThirdPartyLicense = new ThirdPartyCoordinateLicense();
          newThirdPartyLicense.setFileCoordinateId(sbomComponent.getId());
          newThirdPartyLicense.setLicenseId(sonatypeLicenseEntry.getKey());
          populateMissingThirdPartyCoordinateLicenseWithSonatypeData(newThirdPartyLicense,
              sonatypeLicenseEntry.getValue());
          thirdPartyCoordinateLicenseDAO.insert(newThirdPartyLicense);
        }
      }
    }
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

  private Map<ComponentIdentifier, List<String>> readSonatypeSecurityResults(final ContainerNode<?> securityJsonData) {
    Map<ComponentIdentifier, List<String>> secResults = new HashMap<>();
    ArrayNode securityJsonArray = (ArrayNode) securityJsonData.get("aaData");
    for (JsonNode securityJsonNode : securityJsonArray) {
      ComponentIdentifier securityComponentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(securityJsonNode);
      String refId = JsonUtils.getNullableString(securityJsonNode.get(FIELD_REFERENCE));
      secResults.computeIfAbsent(securityComponentIdentifier, componentIdentifier -> new ArrayList<>()).add(refId);
    }
    return secResults;
  }

  private void populateMissingThirdPartyCoordinateSecurityWithSonatypeData(
      final ThirdPartyCoordinateSecurity thirdPartySecurity,
      final SecurityVulnerabilityData sonatypeVulnerabilityData)
  {
    if (sonatypeVulnerabilityData == null) {
      return;
    }

    if (CollectionUtils.isNotEmpty(sonatypeVulnerabilityData.advisories)) {
      thirdPartySecurity.setAdvisories(
          sonatypeVulnerabilityData.advisories.stream().map(a -> a.url).collect(Collectors.joining(",")));
    }
    if (sonatypeVulnerabilityData.customData != null &&
        StringUtils.isNotBlank(sonatypeVulnerabilityData.customData.cvssVector)) {
      thirdPartySecurity.setAttackVector(sonatypeVulnerabilityData.customData.cvssVector);
    }
    if (sonatypeVulnerabilityData.customData != null &&
        StringUtils.isNotBlank(sonatypeVulnerabilityData.customData.cweId)) {
      thirdPartySecurity.setCwes(sonatypeVulnerabilityData.customData.cweId);
    }
    if (StringUtils.isNotBlank(sonatypeVulnerabilityData.description)) {
      thirdPartySecurity.setDescription(sonatypeVulnerabilityData.description);
    }
    else if (StringUtils.isNotBlank(sonatypeVulnerabilityData.explanationMarkdown)) {
      thirdPartySecurity.setDescription(sonatypeVulnerabilityData.explanationMarkdown);
    }
    if (sonatypeVulnerabilityData.vulnerabilityLink != null) {
      thirdPartySecurity.setLink(sonatypeVulnerabilityData.vulnerabilityLink.toString());
    }
    if (StringUtils.isNotBlank(sonatypeVulnerabilityData.recommendationMarkdown)) {
      thirdPartySecurity.setRecommendations(sonatypeVulnerabilityData.recommendationMarkdown);
    }
    if (sonatypeVulnerabilityData.mainSeverity != null && sonatypeVulnerabilityData.mainSeverity.score > 0) {
      thirdPartySecurity.setSeverity(sonatypeVulnerabilityData.mainSeverity.score);
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

  private void collectTelemetryData(
      final Map<String, Integer> inputTypeCount,
      final Map<String, Integer> providerCount,
      final ThirdPartyReportComponentDTO iacComponent)
  {
    final Set<String> knownInputTypes = ImmutableSet.of("tf", "tf_plan", "cfn", "k8s", "arm");
    final Set<String> knownProviders = ImmutableSet.of("aws", "kubernetes", "azureerm");

    if (iacComponent.securityRows == null || iacComponent.securityRows.isEmpty()) {
      return;
    }

    String inputType = iacComponent.securityRows.get(0).inputType;
    if (!knownInputTypes.contains(inputType)) {
      log.info("Unknown inputType: {}", inputType);
      inputType = "unknown";
    }
    if (!inputTypeCount.containsKey(inputType)) {
      inputTypeCount.put(inputType, 0);
    }
    inputTypeCount.put(inputType, inputTypeCount.get(inputType) + 1);

    String provider = iacComponent.securityRows.get(0).provider;
    if (!knownProviders.contains(provider)) {
      log.info("Unknown provider: {}", provider);
      provider = "unknown";
    }
    if (!providerCount.containsKey(provider)) {
      providerCount.put(provider, 0);
    }
    providerCount.put(provider, providerCount.get(provider) + 1);
  }

  @VisibleForTesting
  void sendIacMetricsTelemetry(
      String applicationId,
      Map<String, Integer> inputTypeCount,
      Map<String, Integer> providerCount,
      int numberOfIacComponents)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.IAC_METRICS);

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    telemetryUtils.includeRealApplicationId(attributes, applicationId);

    for (String provider : providerCount.keySet()) {
      attributes.put("number_of_components_with_provider_" + provider, String.valueOf(providerCount.get(provider)));
    }

    for (String inputType : inputTypeCount.keySet()) {
      attributes.put("number_of_components_with_input_type_" + inputType,
          String.valueOf(inputTypeCount.get(inputType)));
    }

    attributes.put("number_of_iac_components", String.valueOf(numberOfIacComponents));
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }
}
