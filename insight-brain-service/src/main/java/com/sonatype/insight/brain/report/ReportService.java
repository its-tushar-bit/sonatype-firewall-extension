/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dashboard.ApplicationRiskScoreDTO;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ReportMetadataDTO;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sbom.utils.SbomMetadataUtils;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.thirdparty.ThirdPartyDataService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ReportUtils.DATA_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ReportUtils.SECURITY_JSON_FILENAME;
import static com.sonatype.insight.brain.thirdparty.ThirdPartyComponentDAO.THIRD_PARTY_SECURITY_JSON_FILENAME;

@Named
public class ReportService
{
  private final InsightWork work;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private static final Logger log = LoggerFactory.getLogger(ReportService.class);

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

  private ReportUtils reportUtils;

  @Inject
  public ReportService(
      InsightWork work,
      PolicyEvaluationDAO policyEvaluationDAO,
      Configuration configuration,
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      ThirdPartyDataService thirdPartyDataService,
      TelemetrySender telemetrySender,
      TelemetryUtils telemetryUtils,
      RepositoryMatcher repositoryMatcher,
      H2ApplicationRiskService applicationRiskService,
      ProductLicense productLicense,
      SbomMetadataUtils sbomMetadataUtils,
      ReportUtils reportUtils)
  {
    this.work = work;
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
    this.reportUtils = reportUtils;
  }

  public Report fetchReport(final Application app, final String scanId)
      throws IOException
  {
    String appId = app.getId();
    final Report reportFile = reportUtils.getFileReport(appId, scanId);
    if (!reportFile.exists()) {
      int reportTimeoutInSeconds = configuration.getReportTimeoutInSeconds();
      Report tempFile = reportUtils.tempReport(reportFile);

      if (!reportUtils.downloadReport(scanId, tempFile, reportTimeoutInSeconds, 5)) {
        throw new NotFoundException("Could not download the report for scan ID " + scanId);
      }
      processThirdPartyData(scanId, tempFile, appId);
      reportUtils.rename(tempFile, reportFile);
    }

    reportUtils.applyChanges(app, reportFile, repositoryMatcher, telemetrySender, telemetryUtils, configuration);
    thirdPartyDataService.mergeSonatypeDataWithSbomDataWithIndexing(scanId, reportFile);

    return reportFile;
  }

  //visible for testing
  void includeThirdPartyData(final Report reportFile, final ThirdPartyApplicationReportDTO dto)
      throws IOException
  {
    if (dto != null) {
      reportUtils.appendToReport(reportFile, dto);
    }
  }

  @VisibleForTesting
  void processThirdPartyData(final String scanId, final Report tempFile, final String appId)
      throws IOException
  {
    ThirdPartyApplicationReportDTO thirdPartyApplicationReportDTO = thirdPartyDataService.getScanData(scanId);
    ThirdPartyApplicationReportDTO thirdPartyApplicationReportForInfrastructureAsCodeDTO =
        thirdPartyDataService.loadThirdPartyInfrastructureAsCodeData(tempFile, appId);
    if (thirdPartyApplicationReportDTO != null) {
      thirdPartyApplicationReportDTO.billOfMaterials
          .addAll(thirdPartyApplicationReportForInfrastructureAsCodeDTO.billOfMaterials);
      thirdPartyApplicationReportDTO.securityRows
          .addAll(thirdPartyApplicationReportForInfrastructureAsCodeDTO.securityRows);
      includeThirdPartyData(tempFile, thirdPartyApplicationReportDTO);
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
    final String name = reportUtils.toEntryName(path);
    auditBrowseReport(scanId, name);
    final Report reportFile = getReport(appPublicId, scanId);
    ReportEntry reportEntry = null;
    try {
      reportEntry = reportUtils.getEntry(reportFile, name);
      if (SECURITY_JSON_FILENAME.equals(name)) {
        reportEntry = loadCombinedSecurityData(reportEntry, reportFile);
      }
    }
    catch (final Exception e) {
      log.warn("Problem embedding report: " + e.getMessage(), e);
    }
    return reportEntry;
  }

  private ReportEntry loadCombinedSecurityData(ReportEntry reportEntry, Report reportFile) throws IOException {
    ReportEntry thirdPartyReportEntry =
        reportUtils.getEntry(reportFile, THIRD_PARTY_SECURITY_JSON_FILENAME);
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

  public Report getReport(final String appId, final String scanId) {
    Report reportFile = reportUtils.getFileReport(appId, scanId);
    if (reportFile.exists()) {
      return reportFile;
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

    Report reportFile = getReport(application.getId(), scanId);
    final ContainerNode<?> data = JsonUtils.parse(reportUtils.getEntry(reportFile, DATA_JSON_FILENAME).buf);
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
      final ApplicationRiskScoreDTO applicationRiskScoreDTO = applicationRiskService.getRiskForApp(
          application,
          Collections.singleton(StageTypes.getById(evaluation.getStageTypeId())
      ));
      metadata.setTotalRisk(finalExtractTotalRiskOrDefault(applicationRiskScoreDTO));
    }

    // For NVS where a scanLabel is set for the application name and the stage name doesn't matter
    if (reportUtils.getEntry(reportFile, "template.properties") != null) {
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
    Report reportFile = getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());

    return reportUtils.getEntry(reportFile, "bom.json");
  }

  @Authorize(permission = Permission.WRITE)
  public void updateReportEntry(
      @AuthzContext(Key.APPLICATION_ID) String appInternalId,
      String scanId,
      String entryName,
      byte[] bufferData) throws IOException
  {
    Report reportFile = getReport(appInternalId, scanId);
    reportUtils.putEntry(reportFile, entryName, bufferData);
  }

  public PolicyThreats getPolicyThreats(
      final String applicationPublicId,
      final String scanId)
  {
    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    final Report reportFile = getReport(application.getId(), scanId);

    try {
      final ReportEntry reportEntry = reportUtils.getEntry(reportFile, ReportUtils.POLICY_THREATS);

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

  public ReportEntry getEntry(final Report reportFile, final String name) throws IOException {
    return reportUtils.getEntry(reportFile, name);
  }

  @VisibleForTesting
  public void setReportUtils(final ReportUtils reportUtils) {
    this.reportUtils = reportUtils;
  }
}
