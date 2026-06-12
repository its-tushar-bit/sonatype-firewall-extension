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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.io.ByteArrayInputStream;
import java.nio.file.FileAlreadyExistsException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.innersource.InnerSourceCleanupPendingService;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.hosted.HostedReportFileBuilder;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.insight.brain.git.RemediationVersionDTO;
import com.sonatype.insight.brain.git.pullrequestcreationservice.AutomatedPullRequestCreationService;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.hds.ScanUploadService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.innersource.InnerSourceVersion;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.report.ApplicationReport.ReportType;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.scan.ScanContext;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.CpeResultsTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.brain.utils.JacksonNodeUtils;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.ws.rs.InternalServerErrorException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import datadog.trace.api.Trace;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.*;

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

  private final InnerSourceApplicationDAO innerSourceApplicationDAO;

  private final InnerSourceVersionDAO innerSourceVersionDAO;

  private final ProprietaryConfigService proprietaryConfigService;

  private final ReportDataStore reportDataStore;

  private final ScanUploadService scanUploadService;

  private final AutomatedPullRequestCreationService automatedPullRequestCreationService;

  private final CpeMatchingConfigurationService cpeMatchingConfigurationService;

  private final ScanPersistenceService scanPersistenceService;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final RepositoryDAO repositoryDAO;

  private final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider;

  private final ApplicationReportPersistenceService applicationReportPersistenceService;

  private final InnerSourceCleanupPendingService innerSourceCleanupPendingService;

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
      final InnerSourceApplicationDAO innerSourceApplicationDAO,
      final InnerSourceVersionDAO innerSourceVersionDAO,
      final ProprietaryConfigService proprietaryConfigService,
      final ReportDataStore reportDataStore,
      final ScanUploadService scanUploadService,
      final AutomatedPullRequestCreationService automatedPullRequestCreationService,
      final CpeMatchingConfigurationService cpeMatchingConfigurationService,
      final ScanPersistenceService scanPersistenceService,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final RepositoryDAO repositoryDAO,
      final Provider<RepositoryPolicyEvaluator> repositoryPolicyEvaluatorProvider,
      final ApplicationReportPersistenceService applicationReportPersistenceService,
      final InnerSourceCleanupPendingService innerSourceCleanupPendingService)
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
    this.innerSourceApplicationDAO = innerSourceApplicationDAO;
    this.innerSourceVersionDAO = innerSourceVersionDAO;
    this.proprietaryConfigService = proprietaryConfigService;
    this.reportDataStore = reportDataStore;
    this.scanUploadService = scanUploadService;
    this.automatedPullRequestCreationService = automatedPullRequestCreationService;
    this.cpeMatchingConfigurationService = cpeMatchingConfigurationService;
    this.scanPersistenceService = scanPersistenceService;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryPolicyEvaluatorProvider = repositoryPolicyEvaluatorProvider;
    this.applicationReportPersistenceService = applicationReportPersistenceService;
    this.innerSourceCleanupPendingService = innerSourceCleanupPendingService;
  }

  @Trace
  @WithSpan
  public ApplicationReport fetchReport(
      final Application app,
      final String scanId,
      final String stageTypeId) throws IOException
  {
    return fetchReport(app, scanId, stageTypeId, null);
  }

  @Trace
  @WithSpan
  public ApplicationReport fetchReport(
      final Application app,
      final String scanId,
      final String stageTypeId,
      final Map<String, ReportEntry> preservedThirdPartyEntries) throws IOException
  {
    ApplicationReport applicationReport =
        reportDataStore.downloadReport(app, scanId,
            (sid, report, appId) -> processThirdPartyDataWithFallback(sid, report, appId,
                preservedThirdPartyEntries));
    CpeResultsTelemetry cpeResultsTelemetry = new CpeResultsTelemetry();
    applyChanges(app, scanId, applicationReport, stageTypeId, cpeResultsTelemetry, repositoryMatcher, telemetrySender,
        telemetryUtils, configuration);
    thirdPartyDataService.mergeSonatypeDataWithSbomDataWithIndexing(scanId, applicationReport, cpeResultsTelemetry);
    sendCpeResultMetricsTelemetry(app.getId(), cpeResultsTelemetry);
    return applicationReport;
  }

  // visible for testing
  void includeThirdPartyData(
      final ApplicationReport applicationReport,
      final ThirdPartyApplicationReportDTO dto) throws IOException
  {
    if (dto != null) {
      applicationReport.appendToReport(dto);
    }
  }

  @VisibleForTesting
  void processThirdPartyData(
      final String scanId,
      final ApplicationReport tempApplicationReport,
      final String appId) throws IOException
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
          sbomMetadataUtils.hasMaxActiveSbomLimitBeenReached())
      {
        thirdPartyDataService.deleteByScanId(scanId);
      }
    }
  }

  private void processThirdPartyDataWithFallback(
      final String scanId,
      final ApplicationReport tempApplicationReport,
      final String appId,
      final Map<String, ReportEntry> preservedThirdPartyEntries) throws IOException
  {
    if (preservedThirdPartyEntries != null &&
        preservedThirdPartyEntries.values().stream().anyMatch(Objects::nonNull))
    {
      for (String entryName : List.of(
          THIRD_PARTY_BOM_JSON.getName(),
          THIRD_PARTY_SECURITY_JSON.getName(),
          THIRD_PARTY_LICENSE_JSON.getName()))
      {
        ReportEntry preserved = preservedThirdPartyEntries.get(entryName);
        if (preserved != null) {
          tempApplicationReport.putEntry(entryName, preserved.buf);
        }
      }
    }
    else {
      processThirdPartyData(scanId, tempApplicationReport, appId);
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

  public boolean isHostedRepositoryComponent(final String scanId) {
    return repositoryComponentDAO.getByScanId(scanId) != null;
  }

  public boolean isHostedScan(final String scanId, final String appId) {
    if (isHostedRepositoryComponent(scanId)) {
      return true;
    }
    PolicyEvaluation pe = policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId);
    return pe != null && ScanTriggerType.REPOSITORY_MANAGER == pe.getScanTriggerType();
  }

  public void reevaluateHostedComponent(final String appId, final String scanId) {
    RepositoryComponent component = repositoryComponentDAO.getByScanId(scanId);
    if (component == null) {
      throw new NotFoundException("No hosted component found for scanId: " + scanId);
    }
    Repository repository = repositoryDAO.getById(component.getRepositoryId());
    if (repository == null) {
      throw new NotFoundException("Repository not found for component scanId: " + scanId);
    }
    PolicyEvaluation lastEval = policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId);
    String stageTypeId = lastEval != null ? lastEval.getStageTypeId() : ComplianceStageType.ID;
    String format = component.getComponentIdentifier() != null
        ? component.getComponentIdentifier().getFormat()
        : repository.getFormat();
    RepositoryComponentEvaluationDataRequestList request =
        new RepositoryComponentEvaluationDataRequestList("INITIAL_SCAN");
    if (component.getPathname() == null || component.getHash() == null) {
      throw new NotFoundException("Component for scanId " + scanId + " is missing pathname or hash");
    }
    request.components.add(
        new RepositoryComponentEvaluationDataRequest(format, component.getPathname(), component.getHash()));
    // skipAutoWaivers is not forwarded — RepositoryPolicyEvaluator.evaluate does not support it.
    // Callers passing skipAutoWaivers=true via ReportResource will have it silently ignored for hosted scans.
    log.debug("reevaluateHostedComponent: skipAutoWaivers not supported for hosted scans, appId={} scanId={}", appId,
        scanId);
    repositoryPolicyEvaluatorProvider.get().evaluate(repository, request, false, null, stageTypeId);
    try {
      saveOverlayFiles(appId, scanId);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new InternalServerErrorException("Failed to save overlay files for scanId=" + scanId, e);
    }
  }

  @Authorize(permission = Permission.READ)
  public ReportEntry processBrowseReport(
      final @AuthzContext(Key.APPLICATION_ID) String appId,
      String scanId,
      String path)
  {
    final String name = toEntryName(path);
    auditBrowseReport(scanId, name);
    ApplicationReport applicationReport = getReport(appId, scanId);
    try {
      if (!applicationReport.exists() && isHostedScan(scanId, appId)) {
        applicationReport = reportDataStore.downloadReport(
            applicationDAO.getByIdNotNull(appId), scanId, (sid, r, aid) -> {
            });
      }
    }
    catch (Exception e) {
      log.debug("Could not download report for appId={} scanId={}: {}", appId, scanId, e.getMessage());
    }
    ReportEntry reportEntry = null;
    try {
      if (SECURITY_JSON.getName().equals(name)) {
        reportEntry = loadCombinedSecurityData(applicationReport);
      }
      else {
        reportEntry = applicationReport.getEntry(name);
      }
    }
    catch (final Exception e) {
      log.warn("Problem embedding report: " + e.getMessage(), e);
    }
    return reportEntry;
  }

  private String toEntryName(final String path) {
    if (null == path || path.isEmpty()) {
      return INDEX_HTML.getName();
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
      buf.append(INDEX_HTML.getName());
    }
    return buf != null ? buf.toString() : path;
  }

  private ReportEntry loadCombinedSecurityData(ApplicationReport applicationReport) throws IOException {
    Map<String, ReportEntry> entries = applicationReport.getEntries(List.of(
        SECURITY_JSON.getName(),
        THIRD_PARTY_SECURITY_JSON.getName()));
    ReportEntry securityReportEntry = entries.get(SECURITY_JSON.getName());
    ReportEntry thirdPartyReportEntry = entries.get(THIRD_PARTY_SECURITY_JSON.getName());
    if (securityReportEntry != null && thirdPartyReportEntry != null) {
      ContainerNode<?> thirdPartySecurityNode = JsonUtils.parse(thirdPartyReportEntry.buf);
      ContainerNode<?> securityNode = JsonUtils.parse(securityReportEntry.buf);
      ArrayNode thirdPartySecurityRootNode = (ArrayNode) thirdPartySecurityNode.get("aaData");
      ArrayNode securityRootNode = (ArrayNode) securityNode.get("aaData");
      securityRootNode.addAll(thirdPartySecurityRootNode);

      return new ReportEntry(SECURITY_JSON.getName(), securityReportEntry.time, JsonUtils.generate(securityNode));
    }
    return securityReportEntry;
  }

  @Trace
  @WithSpan
  public ApplicationReport getReport(final String appId, final String scanId) {
    return getReport(applicationDAO.getByIdNotNull(appId), scanId);
  }

  public ApplicationReport getReport(final Application app, final String scanId) {
    ApplicationReport applicationReport = reportDataStore.getApplicationReport(app, scanId);

    boolean exists;
    try {
      exists = applicationReport.exists();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    if (exists) {
      // Consumer pre-generates overlay files — if policythreats.json is still missing
      // (e.g. consumer failed partway), regenerate it from DB without re-downloading the zip.
      return applicationReport;
    }

    PolicyEvaluation lastEval = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), scanId);
    if (lastEval != null) {
      // Report zip missing — consumer failed before downloading it. Download now as recovery.
      if (isHostedScan(scanId, app.getId())) {
        try {
          reportDataStore.downloadReport(app, scanId, (sid, r, aid) -> {
          });
        }
        catch (FileAlreadyExistsException ignored) {
          // concurrent recovery request already downloaded it
        }
        catch (Exception e) {
          log.debug("HDS report unavailable for recovery scanId={}: {}", scanId, e.getMessage());
        }
        try {
          saveOverlayFiles(app.getId(), scanId);
        }
        catch (Exception e) {
          log.warn("Recovery: failed to save overlay files for scanId={}: {}", scanId, e.getMessage());
        }
        applicationReport = reportDataStore.getApplicationReport(app, scanId);
        try {
          if (applicationReport.exists()) {
            return applicationReport;
          }
        }
        catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
      throw new NotFoundException("The report for application ID " + app.getId() + " and scan ID " + scanId
          + " does not exist. Usually this means the report was deemed obsolete"
          + " according to the data retention policies and hence purged to the trash.");
    }
    throw new NotFoundException("Could not find a report with ID " + scanId);
  }

  private void saveOverlayFiles(final String appId, final String scanId) throws Exception {
    RepositoryComponent comp = repositoryComponentDAO.getByScanId(scanId);
    List<RepositoryPolicyViolation> violations = comp != null && comp.getPathname() != null
        ? repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(
            comp.getRepositoryId(), comp.getPathname())
        : List.of();
    for (String fileName : List.of("policythreats.json", "summary.json")) {
      byte[] content = HostedReportFileBuilder.build(fileName, comp, violations);
      applicationReportPersistenceService.saveReportFile(appId, scanId, fileName,
          new ByteArrayInputStream(content));
    }
    // Patch bom.json displayName — required by PDF generator (ApiReportDataServiceV2:289 NPE)
    Application application = applicationDAO.getByIdNotNull(appId);
    ApplicationReport report = reportDataStore.getApplicationReport(application, scanId);
    ReportEntry bomEntry = report != null ? report.getEntry("bom.json") : null;
    if (bomEntry != null) {
      byte[] patched = HostedReportFileBuilder.patchBomDisplayName(bomEntry.buf, comp);
      applicationReportPersistenceService.saveReportFile(appId, scanId, "bom.json",
          new ByteArrayInputStream(patched));
    }
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

    ApplicationReport applicationReport = getReport(application, scanId);
    Map<String, ReportEntry> entries = applicationReport.getEntries(List.of(
        DATA_JSON.getName(),
        TEMPLATE_PROPERTIES.getName(),
        SUMMARY_JSON.getName()));
    ContainerNode<?> data = JsonUtils.parse(entries.get(DATA_JSON.getName()).buf);
    if (data.path("policyComponentCount").isMissingNode() && isHostedScan(scanId, application.getId())) {
      try {
        saveOverlayFiles(application.getId(), scanId);
      }
      catch (Exception e) {
        log.warn("Recovery: failed to save overlay files for scanId={}: {}", scanId, e.getMessage());
      }
      applicationReport = getReport(application, scanId);
      entries = applicationReport.getEntries(List.of(
          DATA_JSON.getName(),
          TEMPLATE_PROPERTIES.getName(),
          SUMMARY_JSON.getName()));
      data = JsonUtils.parse(entries.get(DATA_JSON.getName()).buf);
    }
    boolean expandedCoverage = data.path("globals").path("expandedCoverage").booleanValue();
    if (expandedCoverage) {
      throw new BadRequestException(
          "Expanded Coverage (XC) is no longer supported. " +
              "We have incorporated support for all languages that were maintained in XC in Lifecycle");
    }
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(),
        scanId);
    if (evaluation == null) {
      return metadata;
    }

    metadata.setReportTime(evaluation.getTime());
    metadata.setReportTitle(StageTypes.getById(evaluation.getStageTypeId()).getName() + " Report");
    metadata.setStageId(evaluation.getStageTypeId());
    metadata.setCommitHash(evaluation.getCommitHash());
    metadata.setInitiator(evaluation.getInitiator());
    metadata.setScanTriggerType(evaluation.getScanTriggerType().getDisplayName());
    metadata.setReevaluation(evaluation.isReevaluation());
    metadata.setForMonitoring(evaluation.isForMonitoring());
    metadata.setBranchName(evaluation.getBranchName());

    if (ScanTriggerType.REPOSITORY_MANAGER == evaluation.getScanTriggerType()) {
      RepositoryComponent comp = repositoryComponentDAO.getByScanId(scanId);
      if (comp != null) {
        List<RepositoryPolicyViolation> violations =
            repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathname(comp.getRepositoryId(), comp.getPathname());
        int totalRisk = violations.stream()
            .filter(v -> !v.isWaived())
            .mapToInt(RepositoryPolicyViolation::getThreatLevel)
            .sum();
        metadata.setTotalRisk(totalRisk);
      }
    }
    else if (productLicense.hasFeature(LicensedFeature.DEVELOPER_DASHBOARD)) {
      final ApplicationRiskScoreDTO applicationRiskScoreDTO = applicationRiskService.getRiskForApp(application,
          Collections.singleton(StageTypes.getById(evaluation.getStageTypeId())));
      metadata.setTotalRisk(finalExtractTotalRiskOrDefault(applicationRiskScoreDTO));
    }

    // For NVS where a scanLabel is set for the application name and the stage name doesn't matter
    if (entries.get(TEMPLATE_PROPERTIES.getName()) != null) {
      JsonNode scanLabelNode = data.path("scanLabel");
      if (scanLabelNode.isTextual()) {
        metadata.getApplication().setName(scanLabelNode.asText());
        metadata.setReportTitle("Report");
      }
    }

    setContainerScannerMode(entries.get(SUMMARY_JSON.getName()), metadata);

    return metadata;
  }

  // visible for testing
  void setContainerScannerMode(ReportEntry reportSummary, ReportMetadataDTO metadata) throws IOException {
    if (reportSummary == null) {
      return;
    }

    ContainerNode<?> summaryNode = JsonUtils.parse(reportSummary.buf);
    if (summaryNode == null) {
      return;
    }

    JsonNode containerScanningModeNode = summaryNode.get("containerScanningMode");
    if (containerScanningModeNode == null) {
      return;
    }

    metadata.setContainerScanningMode(containerScanningModeNode.asText());
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

    return applicationReport.getEntry(BOM_JSON.getName());
  }

  public PolicyThreats getPolicyThreats(
      final String applicationPublicId,
      final String scanId)
  {
    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    final ApplicationReport applicationReport = getReport(application, scanId);

    try {
      final ReportEntry reportEntry = applicationReport.getEntry(POLICY_THREATS.getName());

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

  public BaseReportEntity getVulnerabilitySignatureJson(final String applicationId, final String scanId) {
    return reportDataStore.getVulnerabilitySignatureJson(applicationId, scanId);
  }

  private void applyChanges(
      final Application application,
      final String scanId,
      final ApplicationReport applicationReport,
      final String stageTypeId,
      final CpeResultsTelemetry cpeResultsTelemetry,
      final RepositoryMatcher repositoryMatcher,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils,
      final Configuration configuration) throws IOException
  {
    long start = System.currentTimeMillis();

    final ReportType reportType = applicationReport.getType();

    if (ApplicationReport.ReportType.ERROR.equals(reportType)) {
      return;
    }

    applicationReport.embedApplicationPublicId();

    applyComponentRelatedChanges(application, scanId, applicationReport, stageTypeId, cpeResultsTelemetry,
        repositoryMatcher, telemetrySender, telemetryUtils);

    // these data items have already had changes applied as part of applyComponentRelatedChanges above
    final ContainerNode<?> security = JsonUtils.parse(applicationReport.getEntry(SECURITY_JSON.getName()).buf);
    final ContainerNode<?> licenses = JsonUtils.parse(applicationReport.getEntry(LICENSES_JSON.getName()).buf);
    final ContainerNode<?> partialMatched =
        JsonUtils.parse(applicationReport.getEntry(PARTIAL_MATCHED_JSON.getName()).buf);

    Map<ComponentIdentifier, Set<Integer>> depthsByIdentifier =
        parseDependencyDepths(JsonUtils.parse(applicationReport.getEntry(DEPENDENCIES_JSON.getName()).buf));

    final ObjectNode data = JsonUtils.parse(applicationReport.getEntry(DATA_JSON.getName()).buf);
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

    applicationReport.saveReportEntry(LICENSES_JSON.getName(), licenses);
    applicationReport.saveReportEntry(PARTIAL_MATCHED_JSON.getName(), partialMatched);
    writeLicenseThreatsToReportFile(application, applicationReport);

    JacksonNodeUtils.fill(data.putArray("securityCounts"), securityCounts);
    data.put("insecureArtifactCount", insecureArtifactCount);
    JacksonNodeUtils.fill(data.putArray("effectiveLicenseCounts"), licenseCounts);
    JacksonNodeUtils.fill(data.putArray("securityPunchCard"), securityPunchCard);
    JacksonNodeUtils.fill(data.putArray("licensePunchCard"), licensePunchCard);

    applicationReport.saveReportEntry(DATA_JSON.getName(), data);

    log.debug("Applied changes to report in {} ms", System.currentTimeMillis() - start);
  }

  /**
   * Applies changes to component data (bom/license/security/partialmatched/dependencies) including claiming components
   */
  private void applyComponentRelatedChanges(
      final Application application,
      final String scanId,
      final ApplicationReport applicationReport,
      final String stageTypeId,
      final CpeResultsTelemetry cpeResultsTelemetry,
      final RepositoryMatcher repositoryMatcher,
      final TelemetrySender telemetrySender,
      final TelemetryUtils telemetryUtils) throws IOException
  {
    long start = System.currentTimeMillis();

    // Load all required report entries in parallel for improved performance
    Map<String, ContainerNode<?>> entries = applicationReport.loadReportEntries(List.of(
        BOM_JSON.getName(),
        DATA_JSON.getName(),
        SUMMARY_JSON.getName(),
        LICENSES_JSON.getName(),
        SECURITY_JSON.getName(),
        DEPENDENCIES_JSON.getName(),
        PARTIAL_MATCHED_JSON.getName()));

    ContainerNode<?> bomJsonData = entries.get(BOM_JSON.getName());
    ContainerNode<?> dataJson = entries.get(DATA_JSON.getName());
    ContainerNode<?> summaryJsonData = entries.get(SUMMARY_JSON.getName());

    Map<String, HashComponentIdentifier> claimedComponentsByHash =
        applyClaimedComponents(bomJsonData, dataJson, summaryJsonData, cpeResultsTelemetry);

    // must start from un-edited data
    ContainerNode<?> licensesJsonData = entries.get(LICENSES_JSON.getName());
    ContainerNode<?> securityJsonData = entries.get(SECURITY_JSON.getName());
    ContainerNode<?> dependenciesJsonData = entries.get(DEPENDENCIES_JSON.getName());
    thirdPartyComponentDAO.updateReport(bomJsonData, licensesJsonData, securityJsonData, dataJson, summaryJsonData,
        applicationReport);

    Set<ComponentIdentifier> componentIdentifiers = fixBomComponentIdentifiers(bomJsonData);

    // now apply any data edits (e.g. modified flag)
    augmentDependenciesGraph(dependenciesJsonData);
    applicationReport.saveReportEntry(DEPENDENCIES_JSON.getName(), dependenciesJsonData);

    innerSourceCleanupPendingService.cleanupRecordsIfPending(application.getId(), scanId);

    DependencyResolver
        .getInstance(dependenciesJsonData, bomJsonData, dataJson, summaryJsonData, stageTypeId, application,
            telemetrySender, telemetryUtils, innerSourceApplicationDAO, innerSourceVersionDAO, applicationDAO,
            proprietaryConfigService)
        .resolve();

    triggerInnerSourceAutomatedRemediation(application, scanId, stageTypeId, dependenciesJsonData);

    componentIdentifiers.addAll(
        repositoryMatcher.match(application, bomJsonData, dataJson, summaryJsonData, licensesJsonData,
            securityJsonData));

    fixComponentIdentifiers(licensesJsonData, componentIdentifiers);
    Set<ComponentIdentifier> componentIdentifiersWithLicenseOverrides = applyLicenseOverrides(licensesJsonData,
        application);
    ArrayNode licensesAaData = (ArrayNode) licensesJsonData.get("aaData");
    componentIdentifiersWithLicenseOverrides
        .addAll(addLicenseOverridesForClaimedComponents(licensesAaData, claimedComponentsByHash.values(), application));
    applicationReport.saveReportEntry(LICENSES_JSON.getName(), licensesJsonData);

    applicationReport.saveReportEntry(DATA_JSON.getName(), dataJson);
    applicationReport.saveReportEntry(SUMMARY_JSON.getName(), summaryJsonData);

    augmentModified(componentIdentifiersWithLicenseOverrides, bomJsonData);
    applicationReport.saveReportEntry(BOM_JSON.getName(), bomJsonData);

    fixComponentIdentifiers(securityJsonData, componentIdentifiers);
    applySecurityVulnerabilityOverrides(securityJsonData, application, cpeResultsTelemetry);
    applicationReport.saveReportEntry(SECURITY_JSON.getName(), securityJsonData);

    // must start from un-edited data
    ContainerNode<?> partialmatchedJsonData = entries.get(PARTIAL_MATCHED_JSON.getName());
    removeClaimedComponentsFromPartialMatched(partialmatchedJsonData, claimedComponentsByHash);
    applicationReport.saveReportEntry(PARTIAL_MATCHED_JSON.getName(), partialmatchedJsonData);

    log.debug("applyComponentRelatedChanges finished  in {} ms", System.currentTimeMillis() - start);
  }

  private void triggerInnerSourceAutomatedRemediation(
      final Application application,
      final String scanId,
      final String stageTypeId,
      final JsonNode dependenciesJsonData)
  {
    try {
      if (dependenciesJsonData == null) {
        return;
      }

      JsonNode dependencyTreeNode = dependenciesJsonData.path("dependencyTree");
      if (dependencyTreeNode.isMissingNode()) {
        return;
      }

      DependencyNode tree = JsonUtils.asPojo(dependencyTreeNode, DependencyNode.class);
      if (tree == null) {
        return;
      }

      Set<ComponentIdentifier> directDeps = extractDirectDependencies(tree);
      Map<String, Set<ComponentIdentifier>> directInnerSourceDeps = getInnerSourceComponents(directDeps);
      if (directInnerSourceDeps.isEmpty()) {
        return;
      }

      for (Entry<String, Set<ComponentIdentifier>> entry : directInnerSourceDeps.entrySet()) {
        String innerSourceApplicationId = entry.getKey();
        Set<ComponentIdentifier> innerSourceComponents = entry.getValue();

        if (CollectionUtils.isEmpty(innerSourceComponents)) {
          continue;
        }

        ComponentIdentifier innerSourceComponentWithHighestVersion;
        if (innerSourceComponents.size() > 1) {
          innerSourceComponentWithHighestVersion = innerSourceComponents.stream()
              .max(Comparator.comparing(componentIdentifier -> InnerSourceUtils.createCompositeComparableVersion(
                  componentIdentifier.get(ComponentIdentifier.VERSION), componentIdentifier.getFormat())))
              .orElse(null);
          log.debug("Found {} versions of the same InnerSource component {}.", innerSourceComponents.size(),
              innerSourceComponentWithHighestVersion.createAlternativeVersion(null));
        }
        else {
          innerSourceComponentWithHighestVersion = innerSourceComponents.iterator().next();
        }

        if (innerSourceComponentWithHighestVersion == null) {
          continue;
        }

        InnerSourceVersion latestVersion = innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(
            innerSourceApplicationId,
            StageTypes.RELEASE.getId());

        if (latestVersion == null ||
            !InnerSourceUtils.isValidAutomatedVersionUpdate(innerSourceComponentWithHighestVersion,
                latestVersion.getLatestVersion()))
        {
          continue;
        }

        createAutomatedRemediationPullRequest(application, scanId, stageTypeId, latestVersion,
            innerSourceComponentWithHighestVersion);
      }
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private void createAutomatedRemediationPullRequest(
      final Application application,
      final String scanId,
      final String stageTypeId,
      final InnerSourceVersion latestReleaseVersion,
      final ComponentIdentifier innerSourceComponent)
  {
    RemediationVersionDTO remediationVersionDTO =
        new RemediationVersionDTO(latestReleaseVersion.getLatestVersion(),
            ApiVersionChangeOptionType.INNER_SOURCE_LATEST_NON_BREAKING);
    try {
      PolicyEvaluation evaluation =
          policyEvaluationDAO.getLastByApplicationIdAndScanId(application.getId(), scanId);
      String scannedBranchName = evaluation != null ? evaluation.getBranchName() : null;
      automatedPullRequestCreationService.createAutomatedRemediationPullRequest(application, scanId,
          new Stage(stageTypeId, StageTypes.getById(stageTypeId).getName()),
          innerSourceComponent, () -> Optional.of(remediationVersionDTO), Collections.emptyList(), true,
          scannedBranchName);
    }
    catch (Exception e) {
      log.error("Failed to create automated remediation pull request for InnerSource component {} for application {}.",
          innerSourceComponent, application.getPublicId(), e);
    }
  }

  private Map<String, Set<ComponentIdentifier>> getInnerSourceComponents(final Set<ComponentIdentifier> directDeps) {
    Map<String, Set<ComponentIdentifier>> result = new HashMap<>();

    if (directDeps.isEmpty()) {
      return result;
    }

    Set<PackageUrlIdentifier> directDepsWithoutVersion = directDeps.stream()
        .map(componentIdentifier -> componentIdentifier.createAlternativeVersion(null))
        .map(PackageUrlIdentifier::fromComponentIdentifier)
        .collect(Collectors.toSet());

    Map<String, Set<ComponentIdentifier>> purlToComponents = directDeps.stream()
        .collect(Collectors.groupingBy(
            componentIdentifier -> PackageUrlIdentifier
                .fromComponentIdentifier(componentIdentifier)
                .createAlternativeVersion(null)
                .getPackageUrl(),
            Collectors.toSet()));

    List<InnerSourceApplication> innerSourceApps = innerSourceApplicationDAO.getByPackageUrls(directDepsWithoutVersion);

    for (InnerSourceApplication app : innerSourceApps) {
      String packageUrl = app.getPackageUrl();
      if (purlToComponents.containsKey(packageUrl)) {
        result.put(app.getId(), purlToComponents.get(packageUrl));
      }
    }

    return result;
  }

  private Set<ComponentIdentifier> extractDirectDependencies(final DependencyNode tree) {
    Set<ComponentIdentifier> directDependencies = new HashSet<>();
    for (DependencyNode child : tree.getChildren()) {
      if (!child.isModule() && child.isDirect()) {
        directDependencies.add(child.getComponentIdentifier());
      }
      for (DependencyNode firstLevel : child.getChildren()) {
        if (firstLevel.isDirect()) {
          directDependencies.add(firstLevel.getComponentIdentifier());
        }
      }
    }
    directDependencies.remove(null);
    return directDependencies;
  }

  private Map<String, HashComponentIdentifier> applyClaimedComponents(
      ContainerNode<?> bomJsonData,
      ContainerNode<?> dataJson,
      ContainerNode<?> summaryJsonData,
      CpeResultsTelemetry cpeResultsTelemetry)
  {
    int exactlyMatchedComponentCount = 0;
    int partiallyMatchedComponentCount = 0;
    int knownArtifactCount = 0;

    Map<String, HashComponentIdentifier> claimedComponentsByHash = new LinkedHashMap<>();
    JsonNode aaData = bomJsonData.get("aaData");
    for (JsonNode bomJsonNode : aaData) {
      processCpeComponentTelemetry(bomJsonNode, cpeResultsTelemetry);
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

      if (!MatchState.UNKNOWN.equals(matchState)) {
        knownArtifactCount++;
        if (MatchState.EXACT.equals(matchState)) {
          exactlyMatchedComponentCount++;
        }
        else {
          partiallyMatchedComponentCount++;
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

  private void processCpeComponentTelemetry(final JsonNode bomJsonNode, final CpeResultsTelemetry cpeResultsTelemetry) {
    if (bomJsonNode.hasNonNull("componentIdentifier")) {
      ComponentIdentifier componentIdentifier =
          ComponentIdentifierAdapter.getComponentIdentifier(bomJsonNode);
      if (componentIdentifier != null) {
        // for total, count only the components with any valid component identifier. unknown components are not counted
        cpeResultsTelemetry.incrementReportComponentTotal();
        if (ComponentIdentifier.isFormatValidForCpeMatching(componentIdentifier.getFormat())) {
          cpeResultsTelemetry.incrementCandidateFormatsCount();
        }
      }
    }

    if (bomJsonNode.hasNonNull("identificationSource") && bomJsonNode.hasNonNull("analyzerFeatures")) {
      try {
        AnalyzerFeatures analyzerFeatures =
            JsonUtils.asPojo(bomJsonNode.get("analyzerFeatures"), AnalyzerFeatures.class);
        String identificationSource = bomJsonNode.get("identificationSource").asText();
        if (IdentificationSource.SBOM.getId().equals(identificationSource) &&
            AnalysisType.CPE.equals(analyzerFeatures.getAnalysisType()))
        {
          cpeResultsTelemetry.incrementCpeMatchedComponentCount();
        }
      }
      catch (IOException e) {
        log.debug("error parsing analyzerFeatures object", e);
      }
    }
  }

  private void processCpeSecurityTelemetry(
      final JsonNode securityJsonNode,
      final CpeResultsTelemetry cpeResultsTelemetry)
  {
    if (securityJsonNode != null && securityJsonNode.hasNonNull("detectionType")) {
      String detectionType = securityJsonNode.get("detectionType").asText();
      if (SecurityVulnerabilityDetectionType.CPE_MATCH.getId().equals(detectionType)) {
        cpeResultsTelemetry.incrementCpeMatchedVulnerabilityCount();
      }
    }
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
      for (Entry<String, JsonNode> entry : gavDepths.properties()) {
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

  private void applySecurityVulnerabilityOverrides(
      ContainerNode<?> securityJsonData,
      Application application,
      CpeResultsTelemetry cpeResultsTelemetry)
  {
    ArrayNode securityAaData = (ArrayNode) securityJsonData.get("aaData");
    Iterator<JsonNode> iterSecurityData = securityAaData.iterator();
    int overrideCount = 0;
    while (iterSecurityData.hasNext()) {
      ObjectNode securityJsonNode = (ObjectNode) iterSecurityData.next();
      processCpeSecurityTelemetry(securityJsonNode, cpeResultsTelemetry);
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

  private void sendCpeResultMetricsTelemetry(
      final String applicationId,
      final CpeResultsTelemetry cpeResultsTelemetry)
  {
    if (cpeMatchingConfigurationService.isCpeDataMatchingEnabled(applicationId) &&
        cpeResultsTelemetry.getCpeMatchedComponentCount() > 0)
    {
      TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.CPE_RESULTS_METRICS);
      telemetryData.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
      telemetryData.put(CpeResultsTelemetry.ATTRIBUTE_NAME, cpeResultsTelemetry);
      telemetrySender.send(telemetryData);
    }
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
        !currentObservedLicenses.equals(Collections.singleton(notSupportedLicense.getShortDisplayName())))
    {
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
      final ApplicationReport applicationReport) throws IOException
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
    applicationReport.saveReportEntry(LICENSE_THREATS_JSON.getName(), licenseThreatsJson);
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
          .contains(ComponentIdentifierAdapter.getComponentIdentifier(component)))
      {
        component.put("modified", true);
      }
    }
  }

  public ReportPdfEntity getPdfReport(final String appId, final String scanId) {
    return reportDataStore.getReportPdf(appId, scanId);
  }

  @Trace
  @WithSpan
  public PolicyEvaluation reUploadScanToHds(String appId, String scanId, String clientUserAgent) throws IOException {
    // First call to ensure the scanId is audited even on failure.
    AuditData.get().setScanId(scanId);
    Application application = applicationDAO.getById(appId);
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(appId, scanId);

    if (policyEvaluation == null) {
      throw new BadRequestException("Policy evaluation for scan " + scanId + " does not exist on the server.");
    }

    final ScanEntity scanEntity = scanPersistenceService.getScan(appId, scanId);
    final String stageTypeId = policyEvaluation.getStageTypeId();
    final Stage stage = new Stage(stageTypeId);
    final ScanTriggerType scanTriggerType = policyEvaluation.getScanTriggerType();
    final ClientScanType clientScanType = policyEvaluation.getClientScanType();
    final SbomSpecification sbomSpecification = scanTriggerType == ScanTriggerType.SONATYPE_CONTAINER_IMAGE_SCANNER_API
        ? SbomSpecification.CYCLONEDX
        : null;
    final ScanContext scanContext =
        new ScanContext.Builder().containerImageSbomSpecification(sbomSpecification).build();

    ScanReceipt scanReceipt = scanUploadService.upload(scanEntity, application, stage.getStageTypeId(),
        clientScanType,
        clientUserAgent,
        telemetryUtils.buildThirdPartyScanTelemetryData(application.getId(), stage, stageTypeId,
            scanTriggerType, clientUserAgent),
        null /* scanRequestId */, scanContext, true);
    // Call again after upload to ensure the scanId is set to the original value, not the temporary new one.
    AuditData.get().setScanId(scanId);

    try {
      scanReceipt.waitForReport();
    }
    catch (InterruptedException e) {
      AuditData.get().setException(e);
      Thread.currentThread().interrupt();
      throw new RuntimeException("Scan " + scanId + " interrupted while waiting for report re-generation.", e);
    }

    Map<String, ReportEntry> preservedThirdPartyEntries = readThirdPartyEntriesFromReport(application, scanId);

    String tempScanId = scanReceipt.getScanId();
    fetchReport(application, tempScanId, stageTypeId, preservedThirdPartyEntries);
    reportDataStore.moveApplicationReport(appId, tempScanId, scanId);
    return policyEvaluation;
  }

  private Map<String, ReportEntry> readThirdPartyEntriesFromReport(
      final Application application,
      final String scanId)
  {
    try {
      ApplicationReport originalReport = reportDataStore.getApplicationReport(application, scanId);
      if (originalReport.exists()) {
        List<String> entryNames = List.of(
            THIRD_PARTY_BOM_JSON.getName(),
            THIRD_PARTY_SECURITY_JSON.getName(),
            THIRD_PARTY_LICENSE_JSON.getName());
        Map<String, ReportEntry> entries = originalReport.getEntries(entryNames);
        if (entries.values().stream().anyMatch(Objects::nonNull)) {
          return entries;
        }
      }
    }
    catch (IOException e) {
      log.warn("Could not read third-party entries from original report for scan {}", scanId, e);
    }
    return null;
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
