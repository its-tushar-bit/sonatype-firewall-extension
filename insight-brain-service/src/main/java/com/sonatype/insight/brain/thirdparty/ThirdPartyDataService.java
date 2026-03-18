/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

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
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.sbom.SbomResultsMerger;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.telemetry.CpeResultsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.ThirdPartyHealthCheckReportSecurityRowDTO;
import com.sonatype.insight.scan.ThirdPartyVulnerabilityExploitabilityExchangeRowDTO;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.Swid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.thirdparty.ThirdPartyScanResultUtils.getResearchTypeForThirdPartyVulnerability;

@Named
@Singleton
public class ThirdPartyDataService
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyDataService.class);

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

  private final ProductLicense productLicense;

  private final SbomPersistenceService sbomPersistenceService;

  private final Provider<SbomResultsMerger> sbomResultsMergerProvider;

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
      final SearchIndexManager searchIndexManager,
      final ProductLicense productLicense,
      final Provider<SbomResultsMerger> sbomResultsMergerProvider,
      final SbomPersistenceService sbomPersistenceService)
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
    this.productLicense = productLicense;
    this.sbomResultsMergerProvider = sbomResultsMergerProvider;
    this.sbomPersistenceService = sbomPersistenceService;
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
      sbomPersistenceService.deleteSbom(sbomMetadata.getApplicationId(), sbomMetadata.getFilename());
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

    for (Entry<String, Collection<ThirdPartyFileCoordinate>> multimap : coordinates.asMap().entrySet()) {
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

  // Visible for testing
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
    thirdPartyCoordinateSecurityDAO.getByFileCoordinateId(coord.getId())
        .forEach(
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
    dto.identificationSource = coordinateSecurity.getIdentificationSources();
    dto.researchType = getResearchTypeForThirdPartyVulnerability(coordinateSecurity.getVulnerabilitySource(),
        coordinateSecurity.getRefId());
    dto.detectionType = SecurityVulnerabilityDetectionType.OTHER.getId();

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
    final ThirdPartyBillOfMaterialsRowDTO dto =
        new ThirdPartyBillOfMaterialsRowDTO(componentIdentifier, coordinate.getHash());
    dto.createTime = scanTime.getTime();
    dto.matchState = MatchState.EXACT.toString();
    dto.identificationSource = coordinate.getSource();
    dto.pathnames = coordinates.stream().parallel().map(c -> c.getPackageUrl()).collect(Collectors.toSet());
    dto.setPackageUrl(StringUtils.isNotEmpty(coordinate.getPackageUrl())
        ? coordinate.getPackageUrl()
        : PackageUrlIdentifier.toPackageUrl(componentIdentifier));
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

  public ThirdPartyApplicationReportDTO loadThirdPartyInfrastructureAsCodeData(
      final ApplicationReport applicationReport,
      final String appId)
  {
    // Collect data for telemetry within the loop
    Map<String, Integer> inputTypeCount = new HashMap<>();
    Map<String, Integer> providerCount = new HashMap<>();

    int numberOfIacComponents = 0;
    // End telemetry related fields

    ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = new ThirdPartyApplicationReportDTO();
    Map<String, ThirdPartyReportComponentDTO> data = thirdPartyComponentDAO.getData(applicationReport);
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

  public void mergeSonatypeDataWithSbomDataWithIndexing(
      final String scanId,
      final ApplicationReport applicationReport,
      final CpeResultsTelemetry cpeResultsTelemetry) throws IOException
  {
    if (!productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)) {
      return;
    }
    ThirdPartySbomMetadata sbomMetadata = thirdPartySbomMetadataDAO.getByScanId(scanId);
    if (sbomMetadata == null) {
      return;
    }

    sbomResultsMergerProvider.get().mergeResults(sbomMetadata, scanId, applicationReport, cpeResultsTelemetry);
    indexSbomForSearch(sbomMetadata);
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
