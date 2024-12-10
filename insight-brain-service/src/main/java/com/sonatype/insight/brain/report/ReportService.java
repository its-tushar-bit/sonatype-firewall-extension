/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.report.ApplicationReport.ReportType;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.brain.utils.JacksonNodeUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService.VULNERABILITY_SIGNATURE_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.DATA_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.DEPENDENCIES_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.LICENSES_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.POLICY_THREATS;
import static com.sonatype.insight.brain.report.ApplicationReport.SECURITY_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.SUMMARY_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

@Named
public class ReportService
{
  private static final Logger log = LoggerFactory.getLogger(ReportService.class);

  private static final String EXACTLY_MATCHED_COMPONENT_COUNT = "exactlyMatchedComponentCount";

  private static final String KNOWN_ARTIFACT_COUNT = "knownArtifactCount";

  private static final String CHILDREN_NODE = "children";

  private static final String DIRECT_DEPENDENCY_NODE = "directDependency";

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final Configuration configuration;

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final ThirdPartyDataService thirdPartyDataService;

  private final TelemetrySender telemetrySender;

  private final TelemetryUtils telemetryUtils;

  private final RepositoryMatcher repositoryMatcher;

  private final H2ApplicationRiskService applicationRiskService;

  private final ProductLicense productLicense;

  private final SbomMetadataUtils sbomMetadataUtils;

  private final LicenseDAO licenseDAO;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ThirdPartyComponentDAO thirdPartyComponentDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private final InnerSourceComponentDAO innerSourceComponentDAO;

  private final ProprietaryConfigService proprietaryConfigService;

  private ReportDataStore reportDataStore;

  @Inject
  public ReportService(
      final PolicyEvaluationDAO policyEvaluationDAO,
      final Configuration configuration,
      final ApplicationDAO applicationDAO,
      final OrganizationDAO organizationDAO,
      final ThirdPartyDataService thirdPartyDataService,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final RepositoryMatcher repositoryMatcher,
      final H2ApplicationRiskService applicationRiskService,
      final ProductLicense productLicense,
      final SbomMetadataUtils sbomMetadataUtils,
      final LicenseDAO licenseDAO,
      final ComponentLoaderFactory componentLoaderFactory,
      final ThirdPartyComponentDAO thirdPartyComponentDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final HashComponentIdentifierDAO hashComponentIdentifierDAO,
      final LicenseOverrideDAO licenseOverrideDAO,
      final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final InnerSourceComponentDAO innerSourceComponentDAO,
      final ProprietaryConfigService proprietaryConfigService,
      final ReportDataStore reportDataStore)
  {
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.configuration = configuration;
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.thirdPartyDataService = thirdPartyDataService;
    this.telemetrySender = telemetrySender;
    this.telemetryUtils = telemetryUtils;
    this.repositoryMatcher = repositoryMatcher;
    this.applicationRiskService = applicationRiskService;
    this.productLicense = productLicense;
    this.sbomMetadataUtils = sbomMetadataUtils;
    this.licenseDAO = licenseDAO;
    this.componentLoaderFactory = componentLoaderFactory;
    this.thirdPartyComponentDAO = thirdPartyComponentDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.innerSourceComponentDAO = innerSourceComponentDAO;
    this.proprietaryConfigService = proprietaryConfigService;
    this.reportDataStore = reportDataStore;
  }

  public ApplicationReport fetchReport(final Application app, final String scanId) throws IOException {
    ApplicationReport applicationReport =
        reportDataStore.downloadReport(app.getId(), scanId, this::processThirdPartyData);
    applyChanges(app, applicationReport, repositoryMatcher, telemetrySender, telemetryUtils, configuration);
    thirdPartyDataService.mergeSonatypeDataWithSbomDataWithIndexing(scanId, applicationReport);
    return applicationReport;
  }

  //visible for testing
  void includeThirdPartyData(final ApplicationReport applicationReport, final ThirdPartyApplicationReportDTO dto)
      throws IOException
  {
    if (dto != null) {
      applicationReport.appendToReport(dto);
    }
  }

  @VisibleForTesting
  void processThirdPartyData(final String scanId, final ApplicationReport tempApplicationReport, final String appId)
      throws IOException
  {
    ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = thirdPartyDataService.getScanData(scanId);
    ThirdPartyApplicationReportDTO thirdPartyApplicationReportForInfrastructureAsCodeDTO =
        thirdPartyDataService.loadThirdPartyInfrastructureAsCodeData(tempApplicationReport, appId);
    if (thirdPartyApplicationReportDTO != null) {
      thirdPartyApplicationReportDTO.billOfMaterials
          .addAll(thirdPartyApplicationReportForInfrastructureAsCodeDTO.billOfMaterials);
      thirdPartyApplicationReportDTO.securityRows
          .addAll(thirdPartyApplicationReportForInfrastructureAsCodeDTO.securityRows);
      includeThirdPartyData(tempApplicationReport, thirdPartyApplicationReportDTO);
      thirdPartyDataService.indexVulnerabilities(scanId);

      if (!productLicense.hasFeature(LicensedFeature.SBOM_MANAGER) ||
          !sbomMetadataUtils.hasSbomMetadata(scanId) ||
          sbomMetadataUtils.hasMaxActiveSbomLimitBeenReached()) {
        thirdPartyDataService.deleteByScanId(scanId);
      }
    }
  }

  private void auditBrowseReport(final String scanId, final String name) {
    if (name.endsWith(".json")) {
      AuditData.get().setReportId(scanId);
    }
    else {
      AuditData.get().setEvent(null);
    }
  }

  @Authorize(permission = Permission.READ)
  public ReportEntry processBrowseReport(
      final @AuthzContext(Key.APPLICATION_ID) String appPublicId,
      String scanId,
      String path)
  {
    final String name = toEntryName(path);
    auditBrowseReport(scanId, name);
    final ApplicationReport applicationReport = getReport(appPublicId, scanId);
    ReportEntry reportEntry = null;
    try {
      reportEntry = applicationReport.getEntry(name);
      if (SECURITY_JSON_FILENAME.equals(name)) {
        reportEntry = loadCombinedSecurityData(reportEntry, applicationReport);
      }
    }
    catch (final Exception e) {
      log.warn("Problem embedding report: " + e.getMessage(), e);
    }
    return reportEntry;
  }

  private String toEntryName(final String path) {
    if (null == path || path.isEmpty()) {
      return "index.html";
    }
    boolean seenSlash = true;
    StringBuilder buf = null;
    for (int i = 0, len = path.length(); i < len; i++) {
      final char c = path.charAt(i);
      final boolean isSlash = '/' == c;
      if (seenSlash && isSlash) {
        if (buf == null) {
          buf = new StringBuilder(path.subSequence(0, i));
        }
      }
      else if (buf != null) {
        buf.append(c);
      }
      seenSlash = isSlash;
    }
    if (seenSlash && buf != null) {
      buf.append("index.html");
    }
    return buf != null ? buf.toString() : path;
  }

  private ReportEntry loadCombinedSecurityData(ReportEntry reportEntry, ApplicationReport applicationReport)
      throws IOException
  {
    ReportEntry thirdPartyReportEntry = applicationReport.getEntry(THIRD_PARTY_SECURITY_JSON_FILENAME);
    if (reportEntry != null && thirdPartyReportEntry != null) {
      ContainerNode<?> thirdPartySecurityNode = JsonUtils.parse(thirdPartyReportEntry.buf);
      ContainerNode<?> securityNode = JsonUtils.parse(reportEntry.buf);
      ArrayNode thirdPartySecurityRootNode = (ArrayNode) thirdPartySecurityNode.get("aaData");
      ArrayNode securityRootNode = (ArrayNode) securityNode.get("aaData");
      securityRootNode.addAll(thirdPartySecurityRootNode);

      return new ReportEntry(SECURITY_JSON_FILENAME, reportEntry.time, JsonUtils.generate(securityNode));
    }
    return reportEntry;
  }

  public ApplicationReport getReport(final String appId, final String scanId) {
    ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);
    if (applicationReport.exists()) {
      return applicationReport;
    }

    if (policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId) != null) {
      throw new NotFoundException("The report for application ID " + appId + " and scan ID " + scanId
          + " does not exist. Usually this means the report was deemed obsolete"
          + " according to the data retention policies and hence purged to the trash.");
    }
    throw new NotFoundException("Could not find a report with ID " + scanId);
  }

  @Authorize(permission = Permission.READ)
  public ReportMetadataDTO getReportMetadata(
      final @AuthzContext(Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
      final String scanId) throws IOException
  {
    return getReportMetadataNoAuth(applicationPublicId, scanId);
  }

  public ReportMetadataDTO getReportMetadataNoAuth(
      final String applicationPublicId,
      final String scanId) throws IOException
  {
    Application application = getApplicationWithOrganizationInformation(applicationPublicId);

    ReportMetadataDTO metadata = new ReportMetadataDTO();
    metadata.setApplication(application);

    ApplicationReport applicationReport = getReport(application.getId(), scanId);
    final ContainerNode<?> data = JsonUtils.parse(applicationReport.getEntry(DATA_JSON_FILENAME).buf);
    boolean expandedCoverage = data.path("globals").path("expandedCoverage").booleanValue();
    if (expandedCoverage) {
      throw new BadRequestException(
          "Expanded Coverage (XC) is no longer supported. " +
              "We have incorporated support for all languages that were maintained in XC in Lifecycle");
    }
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(),
        scanId);

    metadata.setReportTime(evaluation.getTime());
    metadata.setReportTitle(StageTypes.getById(evaluation.getStageTypeId()).getName() + " Report");
    metadata.setStageId(evaluation.getStageTypeId());
    metadata.setCommitHash(evaluation.getCommitHash());
    metadata.setInitiator(evaluation.getInitiator());
    metadata.setScanTriggerType(evaluation.getScanTriggerType().getDisplayName());
    metadata.setReevaluation(evaluation.isReevaluation());
    metadata.setForMonitoring(evaluation.isForMonitoring());

    if (productLicense.hasFeature(LicensedFeature.DEVELOPER_DASHBOARD)) {
      final ApplicationRiskScoreDTO applicationRiskScoreDTO = applicationRiskService.getRiskForApp(application,
          Collections.singleton(StageTypes.getById(evaluation.getStageTypeId())));
      metadata.setTotalRisk(finalExtractTotalRiskOrDefault(applicationRiskScoreDTO));
    }

    // For NVS where a scanLabel is set for the application name and the stage name doesn't matter
    if (applicationReport.getEntry("template.properties") != null) {
      JsonNode scanLabelNode = data.path("scanLabel");
      if (scanLabelNode.isTextual()) {
        metadata.getApplication().setName(scanLabelNode.asText());
        metadata.setReportTitle("Report");
      }
    }

    return metadata;
  }

  private int finalExtractTotalRiskOrDefault(final ApplicationRiskScoreDTO applicationRiskScoreDTO) {
    if (applicationRiskScoreDTO == null) {
      return -1;
    }
    else if (applicationRiskScoreDTO.totalApplicationRisk == null) {
      return -1;
    }
    else {
      return applicationRiskScoreDTO.totalApplicationRisk.totalRisk;
    }
  }

  private Application getApplicationWithOrganizationInformation(final String applicationPublicId) {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    Organization organization = organizationDAO.getByIdNotNull(application.getOrganizationId());
    application.setOrganization(organization);
    return application;
  }

  public ReportEntry getBomForPolicyEvaluation(PolicyEvaluation policyEvaluation) throws IOException {
    if (policyEvaluation == null) {
      return null;
    }
    ApplicationReport applicationReport = getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());

    return applicationReport.getEntry("bom.json");
  }

  @Authorize(permission = Permission.WRITE)
  public void updateReportEntry(
      @AuthzContext(Key.APPLICATION_ID) String appInternalId,
      String scanId,
      String entryName,
      byte[] bufferData) throws IOException
  {
    ApplicationReport applicationReport = getReport(appInternalId, scanId);
    applicationReport.putEntry(entryName, bufferData);
  }

  public PolicyThreats getPolicyThreats(
      final String applicationPublicId,
      final String scanId)
  {
    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    final ApplicationReport applicationReport = getReport(application.getId(), scanId);

    try {
      final ReportEntry reportEntry = applicationReport.getEntry(POLICY_THREATS);

      if (reportEntry == null) {
        throw new NotFoundException(String.format("Report policy threats entry is missing for the requested " +
            "application [%s] and scan ID [%s]", applicationPublicId, scanId));
      }

      return JsonUtils.parse(reportEntry.buf, PolicyThreats.class);
    }
    catch (final IOException e) {
      throw new NotFoundException(e.getMessage());
    }
  }

  public ReportEntry getEntry(final ApplicationReport applicationReport, final String name) throws IOException {
    return applicationReport.getEntry(name);
  }

  @VisibleForTesting
  public void setReportUtils(final ReportDataStore reportDataStore) {
    this.reportDataStore = reportDataStore;
  }

  public ReportEntity getVulnerabilitySignatureJson(final String applicationId, final String scanId)
      throws IOException
  {
    return reportDataStore.getReportEntityByName(applicationId, scanId, VULNERABILITY_SIGNATURE_JSON_FILENAME);
  }

  private void applyChanges(
      final Application application,
      final ApplicationReport applicationReport,
      final RepositoryMatcher repositoryMatcher,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final Configuration configuration)
      throws IOException
  {
    long start = System.currentTimeMillis();

    final ReportType reportType = applicationReport.getType();

    if (ApplicationReport.ReportType.ERROR.equals(reportType)) {
      return;
    }

    // If this is called from a policy re-evaluation, some files may be cached.
    // Start fresh by deleting any cached files.
    applicationReport.deleteCacheDir();
    applicationReport.deletePdfReport();
    applicationReport.embedApplicationPublicId(application);

    applyComponentRelatedChanges(application, applicationReport, repositoryMatcher, telemetrySender, telemetryUtils);
    applicationReport.cacheThirdPartyData();

    // these data items have already had changes applied as part of applyComponentRelatedChanges above
    final ContainerNode<?> security = JsonUtils.parse(applicationReport.getEntry(SECURITY_JSON_FILENAME).buf);
    final ContainerNode<?> licenses = JsonUtils.parse(applicationReport.getEntry(LICENSES_JSON_FILENAME).buf);
    final ContainerNode<?> partialMatched = JsonUtils.parse(applicationReport.getEntry("partialmatched.json").buf);

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier =
        parseDependencyDepths(JsonUtils.parse(applicationReport.extractEntry(DEPENDENCIES_JSON_FILENAME).buf));

    final ObjectNode data = JsonUtils.parse(applicationReport.getEntry(DATA_JSON_FILENAME).buf);
    final int[] securityCounts = getSecurityCounts(data);
    final int[] licenseCounts = new int[11];

    int insecureArtifactCount = 0;
    boolean isALPObservedLicenseEnabled = configuration.isALPObservedLicenseDetectionEnabled();

    final ArrayList<int[]> securityPunchCard = new ArrayList<>();
    final ArrayList<int[]> licensePunchCard = new ArrayList<>();

    Set<ComponentIdentifier> components = new HashSet<>();
    for (final JsonNode row : security.get("aaData")) {
      final String status = row.path("status").asText();
      if (!SecurityVulnerabilityOverrideStatus.NOT_APPLICABLE.getName().equals(status)) {
        double severity = row.path("score").asDouble();
        updateSecurityCounts(severity, securityCounts);

        ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(row);
        if (components.add(componentIdentifier)) {
          insecureArtifactCount++;
        }

        final int counter = severity < 4 ? 2 : severity < 7 ? 1 : 0;
        updatePunchCard(securityPunchCard, componentIdentifier, depthsByIdentifier, counter);
      }
    }

    License notSupportedLicense = licenseDAO.getById(License.NOT_SUPPORTED_ID);

    ComponentLoader componentLoader = componentLoaderFactory.createComponentLoader(application);
    for (JsonNode licenseJsonNode : licenses.get("aaData")) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(licenseJsonNode);

      hideObservedLicenses(componentIdentifier,
          (ObjectNode) licenseJsonNode,
          isALPObservedLicenseEnabled,
          notSupportedLicense);

      final Component component = componentLoader.getComponent(licenseJsonNode);
      ObjectNode licenseNode = (ObjectNode) licenseJsonNode;
      Integer threatLevel = component.getLicenseThreatLevel();
      licenseNode.put("effectiveLicenseThreat", threatLevel);
      if (component.isLicenseOverridden()) {
        licenseNode.put("overriddenLicenseThreat", threatLevel);
      }

      if (threatLevel != null) {
        threatLevel = Math.min(10, Math.max(0, threatLevel));
        licenseCounts[threatLevel]++;
        if (threatLevel > 0) {
          // Punch card expects 0 to be the highest threat with 2 being the lowest
          final int threatDepth = threatLevel < 4 ? 2 : threatLevel < 8 ? 1 : 0;
          updatePunchCard(licensePunchCard, component.getComponentIdentifier(), depthsByIdentifier, threatDepth);
        }
      }
    }

    for (JsonNode licenseJsonNode : partialMatched.get("aaData")) {
      final ArrayNode matchedComponentNodes = (ArrayNode) licenseJsonNode.get("matchDetails");
      for (JsonNode matchedComponentJsonNode : matchedComponentNodes) {
        ObjectNode matchedComponentNode = (ObjectNode) matchedComponentJsonNode;

        final Component matchedComponent = componentLoader.getComponent(matchedComponentJsonNode);
        matchedComponentNode.put("effectiveLicenseThreat", matchedComponent.getLicenseThreatLevel());
        if (matchedComponent.isLicenseOverridden()) {
          matchedComponentNode.put("overriddenLicenseThreat", matchedComponent.getLicenseThreatLevel());
        }
      }
    }

    applicationReport.saveReportEntry(LICENSES_JSON_FILENAME, licenses);
    applicationReport.saveReportEntry("partialmatched.json", partialMatched);
    writeLicenseThreatsToReportFile(application, applicationReport);

    JacksonNodeUtils.fill(data.putArray("securityCounts"), securityCounts);
    data.put("insecureArtifactCount", insecureArtifactCount);
    JacksonNodeUtils.fill(data.putArray("effectiveLicenseCounts"), licenseCounts);
    JacksonNodeUtils.fill(data.putArray("securityPunchCard"), securityPunchCard);
    JacksonNodeUtils.fill(data.putArray("licensePunchCard"), licensePunchCard);

    applicationReport.saveReportEntry(DATA_JSON_FILENAME, data);

    log.debug("Applied changes to report in {} ms", System.currentTimeMillis() - start);
  }

  /**
   * Applies changes to component data (bom/license/security/partialmatched/dependencies) including claiming components
   */
  private void applyComponentRelatedChanges(
      final Application application,
      final ApplicationReport applicationReport,
      final RepositoryMatcher repositoryMatcher,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils) throws IOException
  {
    long start = System.currentTimeMillis();

    ContainerNode<?> bomJsonData = applicationReport.loadReportEntry(BOM_JSON_FILENAME);
    ContainerNode<?> dataJson = applicationReport.loadReportEntry(DATA_JSON_FILENAME);
    ContainerNode<?> summaryJsonData = applicationReport.loadReportEntry(SUMMARY_JSON_FILENAME);

    Map<String, HashComponentIdentifier> claimedComponentsByHash =
        applyClaimedComponents(bomJsonData, dataJson, summaryJsonData);

    // must start from un-edited data
    ContainerNode<?> licensesJsonData = applicationReport.loadReportEntry(LICENSES_JSON_FILENAME);
    ContainerNode<?> securityJsonData = applicationReport.loadReportEntry(SECURITY_JSON_FILENAME);
    ContainerNode<?> dependenciesJsonData = applicationReport.loadReportEntry(DEPENDENCIES_JSON_FILENAME);
    thirdPartyComponentDAO.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData,
        applicationReport);

    Set<ComponentIdentifier> componentIdentifiers = fixBomComponentIdentifiers(bomJsonData);

    // now apply any data edits (e.g. modified flag)
    augmentDependenciesGraph(dependenciesJsonData);
    applicationReport.saveReportEntry(DEPENDENCIES_JSON_FILENAME, dependenciesJsonData);

    DependencyResolver
        .getInstance(dependenciesJsonData, bomJsonData, dataJson, summaryJsonData, application, telemetrySender,
            telemetryUtils, innerSourceComponentDAO, applicationDAO, proprietaryConfigService)
        .resolve();

    componentIdentifiers.addAll(
        repositoryMatcher.match(application, bomJsonData, dataJson, summaryJsonData, licensesJsonData,
            securityJsonData));

    fixComponentIdentifiers(licensesJsonData, componentIdentifiers);
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = applyLicenseOverrides(licensesJsonData,
        application);
    ArrayNode licensesAaData = (ArrayNode) licensesJsonData.get("aaData");
    componentIdentifiersWithLicenseOverrides
        .addAll(addLicenseOverridesForClaimedComponents(licensesAaData, claimedComponentsByHash.values(), application));
    applicationReport.saveReportEntry(LICENSES_JSON_FILENAME, licensesJsonData);

    applicationReport.saveReportEntry(DATA_JSON_FILENAME, dataJson);
    applicationReport.saveReportEntry(SUMMARY_JSON_FILENAME, summaryJsonData);

    augmentModified(componentIdentifiersWithLicenseOverrides, bomJsonData);
    applicationReport.saveReportEntry(BOM_JSON_FILENAME, bomJsonData);

    fixComponentIdentifiers(securityJsonData, componentIdentifiers);
    applySecurityVulnerabilityOverrides(securityJsonData, application);
    applicationReport.saveReportEntry(SECURITY_JSON_FILENAME, securityJsonData);

    // must start from un-edited data
    ContainerNode<?> partialmatchedJsonData = applicationReport.loadReportEntry("partialmatched.json");
    removeClaimedComponentsFromPartialMatched(partialmatchedJsonData, claimedComponentsByHash);
    applicationReport.saveReportEntry("partialmatched.json", partialmatchedJsonData);

    log.debug("applyComponentRelatedChanges finished  in {} ms", System.currentTimeMillis() - start);
  }

  private Map<String, HashComponentIdentifier> applyClaimedComponents(
      ContainerNode<?> bomJsonData,
      ContainerNode<?> dataJson,
      ContainerNode<?> summaryJsonData)
  {
    int exactlyMatchedComponentCount = 0;
    int partiallyMatchedComponentCount = 0;
    int knownArtifactCount = 0;

    Map<String, HashComponentIdentifier> claimedComponentsByHash = new LinkedHashMap<>();
    JsonNode aaData = bomJsonData.get("aaData");
    for (JsonNode bomJsonNode : aaData) {
      String hash = bomJsonNode.get("hash").asText();
      HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hash);
      ObjectNode bomObjectNode = (ObjectNode) bomJsonNode;

      if (hashComponentIdentifier != null) {
        ComponentIdentifier componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
        if (componentIdentifier.isMaven()) {
          // reports generated before 1.13.0 still require separate GAV fields
          setMavenCoordinatesWithExtension(bomObjectNode, componentIdentifier);
        }
        // injectComponentIdentifier below is for legacy reports and does not help claimed components
        bomObjectNode.set("componentIdentifier", JsonUtils.asTree(componentIdentifier));
        bomObjectNode.put("matchState", MatchState.EXACT.getId());
        bomObjectNode.put("createTime", hashComponentIdentifier.getCreateTimeLong());
        bomObjectNode.set("relativePopularity", NullNode.getInstance());
        bomObjectNode.put("identificationSource", IdentificationSource.MANUAL.getId());
        bomObjectNode.put("comment", hashComponentIdentifier.getComment());
        claimedComponentsByHash.put(hash, hashComponentIdentifier);
      }

      String matchStateString = bomObjectNode.get("matchState").asText();
      MatchState matchState = MatchState.getById(matchStateString);

      if (!matchState.equals(MatchState.UNKNOWN)) {
        knownArtifactCount++;
        if (matchState.equals(MatchState.SIMILAR)) {
          partiallyMatchedComponentCount++;
        }
        else {
          exactlyMatchedComponentCount++;
        }
      }
    }

    ObjectNode data = (ObjectNode) dataJson;
    ObjectNode summary = (ObjectNode) summaryJsonData;

    data.put("partiallyMatchedComponentCount", partiallyMatchedComponentCount);
    data.put(EXACTLY_MATCHED_COMPONENT_COUNT, exactlyMatchedComponentCount);
    data.put(KNOWN_ARTIFACT_COUNT, knownArtifactCount);

    // the pdf report uses summary.json not data.json
    summary.put(KNOWN_ARTIFACT_COUNT, knownArtifactCount);

    log.debug("applyClaimedComponents: {} components, {} claimed.", aaData.size(), claimedComponentsByHash.size());

    return claimedComponentsByHash;
  }

  @VisibleForTesting
  static Map<ComponentIdentifier, Set<Integer>> parseDependencyDepths(JsonNode dependenciesJson) {
    long start = System.currentTimeMillis();

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier = new LinkedHashMap<>();
    JsonNode componentDepths = dependenciesJson.path("componentDepths");
    JsonNode gavDepths = dependenciesJson.path("gavDepths");
    if (componentDepths.isArray()) {
      // new structure: [ { "componentIdentifier" : {...}, "depths" : [1, 2, 3] }, ... ]
      for (JsonNode element : componentDepths) {
        ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(element);
        Set<Integer> depths = new LinkedHashSet<>();
        for (final JsonNode level : element.path("depths")) {
          depths.add(level.asInt());
        }
        depthsByIdentifier.put(componentIdentifier, depths);
      }
    }
    else if (gavDepths.isObject()) {
      // legacy structure: { "g:a:v" : [1, 2, 3], ... }
      for (Iterator<Entry<String, JsonNode>> it = gavDepths.fields(); it.hasNext(); ) {
        Entry<String, JsonNode> entry = it.next();
        String[] gav = entry.getKey().split(":");
        if (gav.length != 3) {
          continue;
        }
        ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(gav[0], gav[1], gav[2]);
        Set<Integer> depths = new LinkedHashSet<>();
        for (final JsonNode level : entry.getValue()) {
          depths.add(level.asInt());
        }
        depthsByIdentifier.put(componentIdentifier, depths);
      }
    }

    log.debug("parseDependencyDepths: {} depthsByIdentifier, {} ms.", depthsByIdentifier.size(),
        System.currentTimeMillis() - start);

    return depthsByIdentifier;
  }

  private int[] getSecurityCounts(ObjectNode dataJson) {
    int[] securityCounts = new int[10];
    JsonNode securityCountsNode = dataJson.get("securityCounts");
    if (securityCountsNode != null && !securityCountsNode.isEmpty()) {
      try {
        securityCounts = JsonUtils.asPojo(securityCountsNode, int[].class);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return securityCounts;
  }

  private void updateSecurityCounts(final double severity, int[] securityCounts) {
    final int threatIndex = 10 - (int) Math.floor(severity);
    securityCounts[threatIndex < 0 ? 0 : threatIndex < 10 ? threatIndex : 9]++;
  }

  private void updatePunchCard(
      List<int[]> punchCard,
      ComponentIdentifier componentIdentifier,
      Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier,
      int level)
  {
    Set<Integer> depths = depthsByIdentifier.get(componentIdentifier);
    if (depths == null) {
      return;
    }
    for (Integer depth : depths) {
      int index = depth - 1;
      while (index >= punchCard.size()) {
        punchCard.add(new int[3]);
      }
      punchCard.get(index)[level]++;
    }
  }

  private Set<ComponentIdentifier> fixBomComponentIdentifiers(ContainerNode<?> bomJsonData) {
    Set<ComponentIdentifier> componentIdentifiers = new LinkedHashSet<>();
    JsonNode aaData = bomJsonData.get("aaData");
    for (JsonNode bomJsonNode : aaData) {
      ObjectNode bomObjectNode = (ObjectNode) bomJsonNode;

      ComponentIdentifierAdapter.injectComponentIdentifier(bomObjectNode);
      ComponentDisplayNameUtil.injectDisplayName(bomObjectNode);
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(bomObjectNode);
      componentIdentifiers.add(componentIdentifier);
    }

    log.debug("fixBomComponentIdentifiers: {} components.", aaData.size());

    return componentIdentifiers;
  }

  private void fixComponentIdentifiers(
      ContainerNode<?> jsonData,
      Set<ComponentIdentifier> componentIdentifiers)
  {
    ArrayNode aaData = (ArrayNode) jsonData.get("aaData");
    Iterator<JsonNode> iterJsonData = aaData.iterator();
    int removedCount = 0;
    while (iterJsonData.hasNext()) {
      ObjectNode jsonNode = (ObjectNode) iterJsonData.next();
      ComponentIdentifierAdapter.injectComponentIdentifier(jsonNode);
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonNode);

      if (!componentIdentifiers.contains(componentIdentifier)) {
        // License/security data for a component that is not in this report. Remove it.
        iterJsonData.remove();
        removedCount++;
      }
      else {
        ComponentDisplayNameUtil.injectDisplayName(jsonNode);
      }
    }

    log.debug("fixComponentIdentifiers: {} components, {} removed.", aaData.size(), removedCount);
  }

  private Set<ComponentIdentifier> applyLicenseOverrides(
      ContainerNode<?> licensesJsonData,
      Application application)
  {
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = new HashSet<>();

    if (!hasAnyLicenseOverrides(licenseOverrideDAO, application.getId())) {
      return componentIdentifiersWithLicenseOverrides;
    }

    ArrayNode licensesAaData = (ArrayNode) licensesJsonData.get("aaData");
    Iterator<JsonNode> iterLicenseData = licensesAaData.iterator();
    int licenseOverrideCount = 0;
    while (iterLicenseData.hasNext()) {
      ObjectNode licenseJsonNode = (ObjectNode) iterLicenseData.next();
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(licenseJsonNode);
      LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
          application, componentIdentifier);
      if (licenseOverride != null) {
        licenseOverrideCount++;
        componentIdentifiersWithLicenseOverrides.add(componentIdentifier);
        licenseJsonNode.put("status", licenseOverride.getStatus().getName());
        if (!licenseOverride.getLicenseIds().isEmpty()) {
          ArrayNode licenseOverrideNode = licenseJsonNode.putArray("overriddenLicenses");

          for (String licenseId : licenseOverride.getLicenseIds()) {
            licenseOverrideNode.add(licenseDAO.getByIdNotNull(licenseId).getShortDisplayName());
          }
        }
        if (licenseOverride.getComment() != null) {
          licenseJsonNode.put("comment", licenseOverride.getComment());
        }
      }
    }

    log.debug("applyLicenseOverrides: {} components, {} overrides.", licensesAaData.size(), licenseOverrideCount);
    return componentIdentifiersWithLicenseOverrides;
  }

  private void applySecurityVulnerabilityOverrides(ContainerNode<?> securityJsonData, Application application) {
    ArrayNode securityAaData = (ArrayNode) securityJsonData.get("aaData");
    Iterator<JsonNode> iterSecurityData = securityAaData.iterator();
    int overrideCount = 0;
    while (iterSecurityData.hasNext()) {
      ObjectNode securityJsonNode = (ObjectNode) iterSecurityData.next();
      String hash = securityJsonNode.get("hash").asText();
      String source = securityJsonNode.get("source").asText();
      String referenceId = securityJsonNode.get("reference").asText();
      SecurityVulnerabilityOverride override =
          securityVulnerabilityOverrideDAO.getByOwnerIdHashSourceAndReferenceId(application.getId(),
              hash, source, referenceId);
      if (override != null) {
        overrideCount++;
        securityJsonNode.put("status", override.getStatus().getName());
        if (override.getComment() != null) {
          securityJsonNode.put("comment", override.getComment());
        }
      }
    }

    log.debug("applySecurityVulnerabilityOverrides: {} components, {} overrides.", securityJsonData.size(),
        overrideCount);
  }

  private Set<ComponentIdentifier> addLicenseOverridesForClaimedComponents(
      ArrayNode licensesAaData,
      Collection<HashComponentIdentifier> hashComponentIdentifiers,
      Application application)
  {
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = new HashSet<>();

    int licenseOverrideCount = 0;
    for (HashComponentIdentifier hashComponentIdentifier : hashComponentIdentifiers) {
      LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifierWithHierarchy(
          application, hashComponentIdentifier.getComponentIdentifier());
      if (licenseOverride != null) {
        licenseOverrideCount++;
        ObjectNode licenseJsonNode = licensesAaData.addObject();
        licenseJsonNode.put("hash", hashComponentIdentifier.getHash());
        ComponentIdentifier componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
        componentIdentifiersWithLicenseOverrides.add(componentIdentifier);
        licenseJsonNode.set("componentIdentifier", JsonUtils.asTree(componentIdentifier));
        if (componentIdentifier.isMaven()) {
          // reports generated before 1.13.0 still require separate GAV fields
          setMavenCoordinates(licenseJsonNode, componentIdentifier);
          licenseJsonNode.put("groupId", componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
          licenseJsonNode.put("artifactId", componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
          licenseJsonNode.put("version", componentIdentifier.get(ComponentIdentifier.VERSION));
          licenseJsonNode.put("classifier", componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER));
        }
        licenseJsonNode.put("matchState", MatchState.EXACT.getId());
        licenseJsonNode.put("catalogDate", hashComponentIdentifier.getCreateTimeLong());
        licenseJsonNode.put("status", licenseOverride.getStatus().getName());
        if (!licenseOverride.getLicenseIds().isEmpty()) {
          ArrayNode licenseOverrideNode = licenseJsonNode.putArray("overriddenLicenses");

          for (String licenseId : licenseOverride.getLicenseIds()) {
            licenseOverrideNode.add(licenseDAO.getByIdNotNull(licenseId).getShortDisplayName());
          }
        }
        if (licenseOverride.getComment() != null) {
          licenseJsonNode.put("comment", licenseOverride.getComment());
        }
      }
    }
    log.debug("addLicenseOverridesForClaimedComponents: {} overrides.", licenseOverrideCount);
    return componentIdentifiersWithLicenseOverrides;
  }

  private static void removeClaimedComponentsFromPartialMatched(
      ContainerNode<?> partialmatchedJsonData,
      Map<String, HashComponentIdentifier> claimedComponentsByHash)
  {
    JsonNode aaData = partialmatchedJsonData.get("aaData");
    Iterator<JsonNode> iterPartialMatchData = aaData.iterator();
    int removedCount = 0;
    while (iterPartialMatchData.hasNext()) {
      JsonNode jsonNode = iterPartialMatchData.next();
      String hash = jsonNode.path("hash").asText();
      if (claimedComponentsByHash.containsKey(hash)) {
        removedCount++;
        iterPartialMatchData.remove();
      }
      else {
        JsonNode matchDetails = jsonNode.get("matchDetails");
        for (JsonNode matchDetail : matchDetails) {
          ObjectNode detailsNode = (ObjectNode) matchDetail;
          ComponentIdentifierAdapter.injectComponentIdentifier(detailsNode);
          ComponentDisplayNameUtil.injectDisplayName(detailsNode);
        }
      }
    }

    log.debug("removeClaimedComponentsFromPartialMatched: {} partial matches, {} removed.", aaData.size(),
        removedCount);
  }

  @VisibleForTesting
  static void hideObservedLicenses(
      ComponentIdentifier matchedComponent,
      ObjectNode matchedComponentNode,
      boolean isALPObservedLicenseEnabled,
      License notSupportedLicense)
  {
    // we do no replacement for empty or only "Not-Supported" entry
    Set<String> currentObservedLicenses = JsonUtils.getStringSetFromArray(matchedComponentNode.get("observedLicenses"));
    if (CollectionUtils.isNotEmpty(currentObservedLicenses) &&
        !currentObservedLicenses.equals(Collections.singleton(notSupportedLicense.getShortDisplayName()))) {
      if (!isALPObservedLicenseEnabled && License.isAlpObservedLicenseFormatHidden(matchedComponent.getFormat())) {
        matchedComponentNode.putArray("observedLicenses")
            .add(notSupportedLicense.getShortDisplayName());
        matchedComponentNode.put("hiddenObservedLicenses", true);

        ArrayNode effectiveLicensesNode = matchedComponentNode.putArray("effectiveLicenses");
        JsonNode declaredLicenses = matchedComponentNode.get("declaredLicenses");
        if (declaredLicenses != null) {
          for (String declaredLicense : JsonUtils.getStringSetFromArray(declaredLicenses)) {
            effectiveLicensesNode.add(declaredLicense);
          }
        }
      }
      else {
        matchedComponentNode.put("hiddenObservedLicenses", false);
      }
    }
    else {
      matchedComponentNode.put("hiddenObservedLicenses", false);
    }
  }

  @VisibleForTesting
  void writeLicenseThreatsToReportFile(
      final Application application,
      final ApplicationReport applicationReport)
      throws IOException
  {
    Map<String, Integer> threatLevelsBySimpleLicenseId =
        licenseThreatGroupDAO.getLicenseThreatLevelsByApplication(application);

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode licenseTable = mapper.createObjectNode();
    for (MultiLicense multiLicense : multiLicenseDAO.getAll()) {
      Integer threatLevel = null;
      for (License license : multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicense.getId())) {
        Integer simpleLicenseThreatLevel = threatLevelsBySimpleLicenseId.get(license.getId());

        if (simpleLicenseThreatLevel != null) {
          if (threatLevel == null) {
            threatLevel = simpleLicenseThreatLevel;
          }
          else {
            threatLevel = Math.max(threatLevel, simpleLicenseThreatLevel);
          }
        }
      }
      licenseTable.put(multiLicense.getShortDisplayName(), threatLevel);
    }
    ObjectNode licenseThreatsJson = mapper.createObjectNode();
    licenseThreatsJson.set("aaData", licenseTable);
    applicationReport.saveReportEntry("licensethreats.json", licenseThreatsJson);
  }

  @VisibleForTesting
  static boolean hasAnyLicenseOverrides(LicenseOverrideDAO licenseOverrideDAO, String applicationId) {
    return licenseOverrideDAO.getCountByOwnerId(applicationId) > 0;
  }

  @VisibleForTesting
  static void augmentDependenciesGraph(final JsonNode dependenciesJsonData) {
    JsonNode dependencyGraphNode = dependenciesJsonData.get("dependencyGraph");
    if (dependencyGraphNode == null) {
      return;
    }

    // root node with component identifier 'null' contains all direct dependencies
    List<ComponentIdentifier> directComponentIdentifiers = new ArrayList<>();
    for (JsonNode child : dependencyGraphNode) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(child);
      if (componentIdentifier == null && child.has(CHILDREN_NODE)) {
        for (JsonNode rootChild : child.get(CHILDREN_NODE)) {
          ((ObjectNode) rootChild).put(DIRECT_DEPENDENCY_NODE, true);
          directComponentIdentifiers.add(ComponentIdentifierAdapter.getComponentIdentifier(rootChild));
        }
        break;
      }
    }

    // setting relevant component identifiers in the full component list
    for (JsonNode child : dependencyGraphNode) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(child);
      if (componentIdentifier != null) {
        ((ObjectNode) child).put(DIRECT_DEPENDENCY_NODE, directComponentIdentifiers.contains(componentIdentifier));
      }
    }
  }

  @VisibleForTesting
  static void augmentModified(Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides, JsonNode bomJsonData) {
    ArrayNode components = (ArrayNode) bomJsonData.get("aaData");
    for (int componentIndex = 0; componentIndex < components.size(); componentIndex++) {
      ObjectNode component = (ObjectNode) components.get(componentIndex);
      if (componentIdentifiersWithLicenseOverrides
          .contains(ComponentIdentifierAdapter.getComponentIdentifier(component))) {
        component.put("modified", true);
      }
    }
  }

  public ReportPdf getPdfReport(final String appId, final String scanId) {
    return reportDataStore.getReportPdf(appId, scanId);
  }

  public static void setMavenCoordinatesWithExtension(
      final ObjectNode objectNode,
      final ComponentIdentifier componentIdentifier)
  {
    setMavenCoordinates(objectNode, componentIdentifier);
    objectNode.put(ComponentIdentifier.MAVEN_EXTENSION, componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION));
  }

  public static void setMavenCoordinates(
      final ObjectNode objectNode,
      final ComponentIdentifier componentIdentifier)
  {
    objectNode.put(ComponentIdentifier.MAVEN_GROUP_ID, componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID));
    objectNode
        .put(ComponentIdentifier.MAVEN_ARTIFACT_ID, componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
    objectNode.put(ComponentIdentifier.VERSION, componentIdentifier.get(ComponentIdentifier.VERSION));
    objectNode.put(ComponentIdentifier.MAVEN_CLASSIFIER, componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER));
  }
}
