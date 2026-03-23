/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.signature.VulnerabilitySignatureAnalysisDTO;
import com.sonatype.insight.brain.api.experimental.ApiVulnerabilityReachabilityStatusService;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.api.v2.service.autowaivers.AutoPolicyWaiverUtil;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.db.IdUtil;
import com.sonatype.insight.brain.dataaccess.AggregateFileDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritizationRemediationService;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.policy.AutoPolicyWaiverExclusionMatcherWrapper;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.PathForwardInspector;
import com.sonatype.insight.brain.policy.utils.EvaluationUtils;
import com.sonatype.insight.brain.policy.violation.ApplicationPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.telemetry.autowaivers.AutoPolicyWaiverTelemetryCollector;
import com.sonatype.insight.brain.utils.JacksonNodeUtils;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEventService;
import com.sonatype.insight.brain.webhook.PolicyAlertEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.updateReachabilityStatus;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DATA_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_ALERTS;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.POLICY_THREATS;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SUMMARY_JSON;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Named
public class ScanPolicyEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(ScanPolicyEvaluator.class);

  private static final String UNKNOWN = "unknown";

  public static final String REEVALUATE_NOT_ALLOWED_FOR_OUT_OF_DATE_SCAN_MESSAGE =
      "Could not Re-Evaluate this report because it is out of date. Navigate to the latest evaluation for this stage.";

  public static final String SKIPPING_AUTO_WAIVERS_NOT_ALLOWED_FOR_PRIMARY_EVALUATIONS =
      "Auto-waivers can only be skipped in re-evaluations not primary policy evaluations.";

  private final ReportService reportService;

  private final PolicyDAO policyDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final AggregateFileDAO aggregateFileDAO;

  private final ApplicationComponentLicenseDAO applicationComponentLicenseDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  private final OwnerDAO ownerDAO;

  private final OrganizationDAO organizationDAO;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ApplicationEvaluationEventService applicationEvaluationEventService;

  private final LegacyViolationService legacyViolationService;

  private final PolicyAlertEventService policyAlertEventService;

  private final TelemetrySender telemetrySender;

  private final ProductLicense productLicense;

  private final PolicyViolationLoggerFactory policyViolationLoggerFactory;

  private final SourceControlUtils sourceControlUtils;

  private final CurrentUser currentUser;

  private final Configuration configuration;

  private final ClusterLockManager clusterLockManager;

  private final PolicyAlertUtil policyAlertUtil;

  private final TelemetryUtils telemetryUtils;

  private final DevelopmentPrioritizationRemediationService developmentPrioritizationRemediationService;

  private final FeaturesService featuresService;

  private final ReportComponentService reportComponentService;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final PathForwardInspector pathForwardInspector;

  private final ApiVulnerabilityReachabilityStatusService apiVulnerabilityReachabilityStatusService;

  private final LicenseNameProvider licenseNameProvider;

  private final ScanPersistenceService scanPersistenceService;

  private final ComponentHelper componentHelper;

  @Inject
  public ScanPolicyEvaluator(
      final ReportService reportService,
      final PolicyDAO policyDAO,
      final PolicyViolationDAO policyViolationDAO,
      final AggregateFileDAO aggregateFileDAO,
      final ApplicationComponentLicenseDAO applicationComponentLicenseDAO,
      final ApplicationComponentDAO applicationComponentDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO,
      final OwnerDAO ownerDAO,
      final OrganizationDAO organizationDAO,
      final ComponentPolicyEvaluator componentPolicyEvaluator,
      final ApplicationEvaluationEventService applicationEvaluationEventService,
      final LegacyViolationService legacyViolationService,
      final PolicyAlertEventService policyAlertEventService,
      final TelemetrySender telemetrySender,
      final PolicyViolationLoggerFactory policyViolationLoggerFactory,
      final ProductLicense productLicense,
      final SourceControlUtils sourceControlUtils,
      final CurrentUser currentUser,
      final Configuration configuration,
      final ClusterLockManager clusterLockManager,
      final PolicyAlertUtil policyAlertUtil,
      final TelemetryUtils telemetryUtils,
      final DevelopmentPrioritizationRemediationService developmentPrioritizationRemediationService,
      final FeaturesService featuresService,
      final ComponentInfoService componentInfoService,
      final ReportComponentService reportComponentService,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final PathForwardInspector pathForwardInspector,
      final ApiVulnerabilityReachabilityStatusService apiVulnerabilityReachabilityStatusService,
      final LicenseNameProvider licenseNameProvider,
      final ScanPersistenceService scanPersistenceService,
      final ComponentHelper componentHelper)
  {
    this.reportService = reportService;
    this.policyDAO = policyDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.aggregateFileDAO = aggregateFileDAO;
    this.applicationComponentLicenseDAO = applicationComponentLicenseDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.autoPolicyWaiverExclusionDAO = autoPolicyWaiverExclusionDAO;
    this.ownerDAO = ownerDAO;
    this.organizationDAO = organizationDAO;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.applicationEvaluationEventService = applicationEvaluationEventService;
    this.legacyViolationService = legacyViolationService;
    this.policyAlertEventService = policyAlertEventService;
    this.telemetrySender = telemetrySender;
    this.policyViolationLoggerFactory = policyViolationLoggerFactory;
    this.productLicense = productLicense;
    this.sourceControlUtils = sourceControlUtils;
    this.currentUser = currentUser;
    this.configuration = configuration;
    this.clusterLockManager = clusterLockManager;
    this.policyAlertUtil = policyAlertUtil;
    this.telemetryUtils = telemetryUtils;
    this.developmentPrioritizationRemediationService = developmentPrioritizationRemediationService;
    this.featuresService = featuresService;
    this.reportComponentService = reportComponentService;
    componentInfoService.setToolName("ci");
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.pathForwardInspector = pathForwardInspector;
    this.apiVulnerabilityReachabilityStatusService = apiVulnerabilityReachabilityStatusService;
    this.licenseNameProvider = licenseNameProvider;
    this.scanPersistenceService = scanPersistenceService;
    this.componentHelper = componentHelper;
  }

  public ScanPolicyEvaluatorResults evaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final ClientScanType clientScanType,
      final boolean skipAutoWaivers) throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, null, null, false /* forMonitoring */,
        clientScanType, null, skipAutoWaivers);
  }

  public ScanPolicyEvaluatorResults evaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final ClientScanType clientScanType,
      final VulnerabilitySignatureAnalysisDTO analysisDTO,
      final boolean skipAutoWaivers) throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, null, null, false /* forMonitoring */,
        clientScanType, analysisDTO, skipAutoWaivers);
  }

  public ScanPolicyEvaluatorResults evaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final String clientUserAgent,
      final String clientInstanceId,
      final ClientScanType clientScanType) throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
        false /* forMonitoring */, clientScanType, null, false /* skipAutoWaivers */);
  }

  public ScanPolicyEvaluatorResults evaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final String clientUserAgent,
      final String clientInstanceId,
      final ClientScanType clientScanType,
      final VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
        false /* forMonitoring */, clientScanType, analysisDTO, false /* skipAutoWaivers */);
  }

  public ScanPolicyEvaluatorResults evaluateForMonitoring(
      Application application,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType,
      ClientScanType clientScanType) throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, null, null, true /* forMonitoring */,
        clientScanType, null, false /* skipAutoWaivers */);
  }

  /*
   * please note: this method was renamed from 'evaluate' so as to facilitate instrumentation by a java agent
   * that captures metrics during a load test; the agent cannot instrument overloaded methods
   */
  private ScanPolicyEvaluatorResults doEvaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final String clientUserAgent,
      final String clientInstanceId,
      boolean forMonitoring,
      final ClientScanType clientScanType,
      final VulnerabilitySignatureAnalysisDTO analysisDTO,
      final boolean skipAutoWaivers) throws IOException
  {
    log.debug(
        "Evaluating policies for application ID {}, scan ID {}, stage {}, scan trigger type {}, for monitoring {}.",
        application.getId(), scanId, stage.getStageTypeId(), scanTriggerType.name(), forMonitoring);

    boolean isContainerImageEval = isEvaluationForContainerImage(stage);

    // Only validate stage type when it is not a container image evaluation
    if (!isContainerImageEval && !Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException(stage.getStageTypeId());
    }

    /*
     * Re-evaluations are being disallowed in response to https://sonatype.atlassian.net/browse/CLM-25312.
     *
     * When re-evaluating policy against the non-latest scan, we can not persist data to the database. This would
     * overwrite newer, more relevant state about the application (what violations are present, which have been closed,
     * meantime to remediate, etc..)
     *
     * Unfortunately the report page is no longer purely driven by the Report files. It incorporates information
     * from the policy_violation tables. By generating new policy violations during a re-evaluation, but not persisting
     * them to the policy_violation table, we would leave the report in state where the policy_violations have no ids.
     * This prevents portions of the report from rendering, such as violation details.
     */
    throwErrorIfReEvaluatingAnOldScan(application.getId(), scanId, stage.getStageTypeId());

    AuditData.get().setStageId(stage.getStageTypeId());

    ReportComponentData reportComponentData =
        reportComponentService.fetchReportAndComponents(application, scanId, stage.getStageTypeId());

    return performPolicyEvaluation(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
        forMonitoring, clientScanType, reportComponentData, analysisDTO, skipAutoWaivers);
  }

  private void fetchAndPersistRemediationRecommendations(
      final String scanId,
      final Stage stage,
      final List<Component> components,
      final String appId)
  {
    try {
      List<ComponentIdentifier> componentIdentifiers = components.stream()
          .map(Component::getComponentIdentifier)
          .filter(Objects::nonNull) // Some components, like local jars, don't have any identifier
          .collect(toList());
      developmentPrioritizationRemediationService.fetchAndPersistRemediationRecommendations(
          componentIdentifiers, scanId, appId, stage);
      log.debug("Subroutine fetchAndPersistRemediationRecommendations() finished.");
    }
    catch (Throwable ex) {
      log.error("Subroutine fetchAndPersistRemediationRecommendations() failed. " +
          "Propagation prevented because it shouldn't affect the main routine.", ex);
    }
  }

  private List<PolicyAlert> createPolicyAlerts(
      String applicationId,
      String scanId,
      String stageTypeId,
      boolean forMonitoring,
      List<Component> components,
      List<PolicyViolation> violations)
  {
    boolean isContainerImageEvaluation = Stage.ID_PROXY.equals(stageTypeId) &&
        productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION) &&
        SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled();

    boolean enableActions = productLicense.hasFeature(LicensedFeature.ENFORCEMENT) || isContainerImageEvaluation;
    if (!enableActions) {
      log.debug("Ignoring actions in policy alerts for application {} and scan {} in stage {}, "
          + "license does not support enforcement.", applicationId, scanId, stageTypeId);
    }
    return policyAlertUtil.createPolicyAlerts(components, violations, stageTypeId, applicationId, forMonitoring,
        enableActions, scanId);
  }

  private void updateReportFiles(
      ApplicationReport applicationReport,
      ScanPolicyEvaluatorResults scanPolicyEvaluatorResults,
      Stage stage,
      Map<String, Owner> policyIdPolicyOwnerMap,
      boolean forMonitoring,
      List<Component> components) throws IOException
  {
    List<PolicyAlert> alerts = createPolicyAlerts(scanPolicyEvaluatorResults.evaluation.getApplicationId(),
        scanPolicyEvaluatorResults.evaluation.getScanId(), stage.getStageTypeId(), forMonitoring,
        components, scanPolicyEvaluatorResults.activeViolations);
    applicationReport.putEntry(POLICY_ALERTS.getName(), JsonUtils.generate(JsonUtils.aaData(alerts)));
    PolicyThreats policyThreats = PolicyThreatsAdapter.createPolicyThreats(scanPolicyEvaluatorResults.allViolations,
        stage.getStageTypeId(),
        policyIdPolicyOwnerMap);
    applicationReport.putEntry(POLICY_THREATS.getName(), JsonUtils.generate(policyThreats));

    updateDataJson(applicationReport, policyThreats);
  }

  private void updateDataJson(ApplicationReport applicationReport, PolicyThreats policyThreats) throws IOException {
    int[] policyCounts = new int[11];
    int policyComponentCount = 0;
    int legacyViolationCount = 0;

    for (PolicyThreats.Component component : policyThreats.aaData) {
      int level = component.policyThreatLevel;
      policyCounts[level < 0 ? 0 : level < 11 ? level : 10]++;
      if (level >= 2) {
        policyComponentCount++;
      }
      for (PolicyThreats.PolicyViolation policyViolation : component.allViolations) {
        if (policyViolation.legacyViolation) {
          legacyViolationCount++;
        }
      }
    }

    ObjectNode data = JsonUtils.parse(applicationReport.getEntry(DATA_JSON.getName()).buf);
    JacksonNodeUtils.fill(data.putArray("policyCounts"), policyCounts);
    data.put("policyComponentCount", policyComponentCount);
    data.put("grandfatheredPolicyViolationCount", legacyViolationCount);
    data.put("legacyViolationCount", legacyViolationCount);
    applicationReport.putEntry(DATA_JSON.getName(), JsonUtils.generate(data));
  }

  /**
   * Processes the raw policy evaluation results: - persists policy evaluation - persists policy violation data and
   * component data if this is an evaluation for the most recent scan for the specified stage - sets or updates the
   * legacy status on policy violations - determines the policy violations for which notifications should be sent
   */
  private ScanPolicyEvaluatorResults processPolicyResults(
      Application app,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType,
      List<Policy> policies,
      boolean forMonitoring,
      PolicyResults policyResults,
      List<Component> components,
      PolicyViolationTelemetryCollector telemetryCollector,
      AutoPolicyWaiverTelemetryCollector autoPolicyWaiverTelemetryCollector,
      ApplicationReport applicationReport,
      ClientScanType clientScanType,
      VulnerabilitySignatureAnalysisDTO analysisDTO,
      final boolean skipAutoWaivers) throws IOException
  {
    String appId = app.getId();
    long start = System.currentTimeMillis();
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolations(app);
        TransactionContext tx = policyEvaluationDAO.createTransactionContext())
    {
      clusterLock.lock();
      tx.begin();
      boolean isLegacyViolationApplicable = stage.getStageTypeId().equals(Stage.ID_COMPLIANCE) ? false : true;
      boolean isLegacyViolationEnabled = legacyViolationService.isLegacyViolationEnabled(tx, app.getId(),
          stage.getStageTypeId());
      // Persist the policy evaluation
      boolean isReevaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(tx, appId, scanId) != null;
      AuditData.get().setIsReevaluation(isReevaluation);
      PolicyEvaluation policyEvaluation = new PolicyEvaluation(appId, stage.getStageTypeId(), scanId, isReevaluation,
          forMonitoring, currentUser.getUsernameOrSystem(), scanTriggerType, clientScanType);
      final ReportEntry dataJsonEntry = applicationReport.getEntry(DATA_JSON.getName());
      policyEvaluation.setCommitHash(extractField(dataJsonEntry, "commitHash"));
      policyEvaluation.setBranchName(extractField(dataJsonEntry, "branchName"));
      PolicyEvaluation lastPrimaryPolicyEvaluation = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(tx,
          appId, stage.getStageTypeId());
      boolean isForLatestScan = true;
      if (isReevaluation) {
        isForLatestScan = lastPrimaryPolicyEvaluation.getScanId().equals(scanId);
        policyEvaluation.setForObsoleteScan(!isForLatestScan);
      }

      policyEvaluationDAO.insert(tx, policyEvaluation);

      ScanPolicyEvaluatorResults results = new ScanPolicyEvaluatorResults();
      results.evaluation = policyEvaluation;
      results.allViolations = new ArrayList<>();
      results.notifiableViolations = new ArrayList<>();
      results.fixedViolations = new ArrayList<>();
      results.waivedViolations = new ArrayList<>();
      results.autoWaivedViolations = new ArrayList<>();

      List<String> ownerIds = getOwnerIds(appId);

      List<PolicyViolation> autoWaivedPolicyViolations = Collections.emptyList();
      boolean skipAutoWaiversForReevaluation = skipAutoWaivers && isReevaluation;
      if (skipAutoWaiversForReevaluation) {
        autoWaivedPolicyViolations =
            policyViolationDAO.getAutoWaivedByApplicationIdAndStageId(appId,
                stage.getStageTypeId());
        policyViolationDAO.loadConstraintFacts(autoWaivedPolicyViolations);
      }
      else if (!isReevaluation && skipAutoWaivers) {
        throw new BadRequestException(SKIPPING_AUTO_WAIVERS_NOT_ALLOWED_FOR_PRIMARY_EVALUATIONS);
      }

      PurlIdentifiersWithVulnerabilities reachablePurlIdentifiersWithVulnerabilities =
          getReachablePurlIdentifiersWithVulnerabilities(app.getId(), scanId, analysisDTO);

      // Convert the policy alerts into policy violations
      List<PolicyAlert> allPolicyAlerts = new ArrayList<>();
      allPolicyAlerts.addAll(policyResults.getActiveAlerts());
      allPolicyAlerts.addAll(policyResults.getWaivedAlerts());
      Map<String, Owner> policyIdPolicyOwnerMap = new HashMap<>();

      List<PolicyViolation> existingViolationsForReachability = Collections.emptyList();
      if (isReevaluation && reachablePurlIdentifiersWithVulnerabilities == null) {
        existingViolationsForReachability = policyViolationDAO.getUnfixedByApplicationIdAndStageId(tx, appId,
            stage.getStageTypeId());
        policyViolationDAO.loadConstraintFacts(existingViolationsForReachability);
      }

      for (PolicyAlert policyAlert : allPolicyAlerts) {
        PolicyFact policyFact = policyAlert.getTrigger();
        Policy policy = policyDAO.getByIdNotNull(policyFact.getPolicyId());
        Owner ownerPolicy = ownerDAO.getById(policy.getOwnerId());
        policyIdPolicyOwnerMap.put(policy.getId(), ownerPolicy);
        PolicyThreatCategory threatCategory = policy.getThreatCategory();

        for (ComponentFact componentFact : policyFact.getComponentFacts()) {
          PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policy.getId(), policy.getName(),
              policyFact.getThreatLevel(), threatCategory, componentFact.getHash(),
              componentFact.getComponentIdentifier(), componentFact.getConstraintFacts(),
              getFilename(componentFact));
          for (Action action : policyAlert.getActions()) {
            // Don't save notification data into policy violations here because at this point we don't really know if
            // the notifications will be sent or not.
            // The notifier component will take care of saving the notification data.
            if (!Action.ID_NOTIFY.equals(action.getActionTypeId())) {
              policyViolation.setActionTypeId(action.getActionTypeId());
              break;
            }
          }
          if (forMonitoring) {
            policyViolation.setSeenByMonitoringEvaluation(true);
          }
          else if (!isReevaluation) {
            policyViolation.setSeenByPrimaryEvaluation(true);
          }
          PolicyWaiver policyWaiver = policyResults.getPolicyWaiver(componentFact);
          if (policyWaiver != null) {
            policyViolation.setWaiveTime(policyEvaluation.getTime());
            policyViolation.setPolicyWaiverId(policyWaiver.getId());
            policyViolation.setPolicyWaiverComment(policyWaiver.getComment());
          }

          Component component =
              findComponentByComponentIdentifier(components, policyViolation.getComponentIdentifier());

          updateReachabilityStatus(policyViolation, reachablePurlIdentifiersWithVulnerabilities);

          if (!existingViolationsForReachability.isEmpty()) {
            existingViolationsForReachability.stream()
                .filter(
                    oldViolation -> PolicyViolationComparator.COMPARATOR.compare(oldViolation, policyViolation) == 0)
                .findFirst()
                .ifPresent(oldViolation -> policyViolation.setReachabilityStatus(
                    determineReachabilityStatus(oldViolation, policyViolation, isReevaluation)));
          }

          // send telemetry information only after we updated the status, this will provide accurate
          // reporting on how well call flow information for reachability is available.
          telemetryCollector.addTelemetryForReachableViolation(
              policyViolation,
              component,
              reachablePurlIdentifiersWithVulnerabilities);

          if (skipAutoWaiversForReevaluation) {
            autoWaivedPolicyViolations.stream()
                .filter(violation -> PolicyViolationComparator.COMPARATOR.compare(violation, policyViolation) == 0)
                .forEach(violation -> {
                  final AutoPolicyWaiver autoPolicyWaiver =
                      autoPolicyWaiverDAO.getById(violation.getAutoPolicyWaiverId());
                  final Owner owner = ownerDAO.getById(autoPolicyWaiver.getOwnerId());
                  policyViolation.setWaiveTime(violation.getWaiveTime());
                  policyViolation.setAutoPolicyWaiverId(violation.getAutoPolicyWaiverId());
                  autoPolicyWaiverTelemetryCollector.addTelemetryForApplyAutoWaiver(autoPolicyWaiver,
                      policyViolation, owner);
                });
          }
          else {
            List<AutoPolicyWaiver> autoPolicyWaivers = getApplicableAutoPolicyWaivers(ownerIds);
            List<AutoPolicyWaiverExclusion> autoPolicyWaiverExclusions =
                getApplicableAutoPolicyWaiverExclusions(ownerIds);

            boolean hasReachabilityData = reachablePurlIdentifiersWithVulnerabilities != null;
            for (AutoPolicyWaiver autoPolicyWaiver : autoPolicyWaivers) {
              if (canEvaluateWithAutoWaiver(autoPolicyWaiver, policyViolation)) {
                boolean violationShouldBeAutoWaived = evaluateAutoPolicyWaiver(
                    appId,
                    component,
                    policyViolation,
                    autoPolicyWaiver,
                    autoPolicyWaiverExclusions,
                    stage.getStageTypeId(),
                    scanId,
                    hasReachabilityData);
                if (violationShouldBeAutoWaived) {
                  policyViolation.setWaiveTime(policyEvaluation.getTime());
                  policyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

                  Owner owner = ownerDAO.getById(autoPolicyWaiver.getOwnerId());
                  autoPolicyWaiverTelemetryCollector.addTelemetryForApplyAutoWaiver(autoPolicyWaiver,
                      policyViolation, owner);

                  // Do not evaluate further auto waivers when one has been applied
                  break;
                }
              }
            }
          }

          results.allViolations.add(policyViolation);
        }
      }
      pathForwardInspector.cleanUp();

      if (isLegacyViolationApplicable) {
        setLegacyViolations(tx, isLegacyViolationEnabled, app, policies, policyEvaluation.getTime(),
            results.allViolations);
      }
      ApplicationPolicyViolationLogger policyViolationLogger =
          policyViolationLoggerFactory.newLogger(policyEvaluation.getTime(), app);

      // Persist the PolicyViolations and ApplicationComponents only if there isn't a more recent
      // primary policy evaluation, since any reevaluation (even for monitoring) may be for an older scan.
      if (isForLatestScan) {
        List<PolicyViolation> oldPolicyViolations = policyViolationDAO.getUnfixedByApplicationIdAndStageId(tx, appId,
            stage.getStageTypeId());
        policyViolationDAO.loadConstraintFacts(oldPolicyViolations);
        PolicyViolationDiff<PolicyViolation> policyViolationDiff = PolicyViolationDigester
            .digestPolicyViolations(oldPolicyViolations, results.allViolations);

        telemetryCollector.setTimeOfPolicyEvaluation(policyEvaluation.getTime());

        // New policy violations.
        List<PolicyViolation> newPolicyViolations = policyViolationDiff.getAppeared();
        logPolicyViolations(newPolicyViolations, "new");

        for (PolicyViolation newPolicyViolation : newPolicyViolations) {
          if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
            results.notifiableViolations.add(newPolicyViolation);
          }
          if (newPolicyViolation.isLegacyViolation()) {
            newPolicyViolation.setLegacyViolationApplied(true);
          }

          policyViolationDAO.insert(tx, newPolicyViolation);

          recordConditionTypeViolationTelemetry(telemetryCollector, newPolicyViolation, components);

          policyViolationLogger.add(PolicyViolationLogEvent.CREATE, newPolicyViolation);
          Component component =
              findComponentByComponentIdentifier(components, newPolicyViolation.getComponentIdentifier());

          if (newPolicyViolation.isWaived() && !newPolicyViolation.isAutoWaived()) {
            policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
            telemetryCollector.addTelemetryForWaivedViolation(newPolicyViolation, component);
            results.waivedViolations.add(newPolicyViolation);
          }

          if (newPolicyViolation.isAutoWaived()) {
            policyViolationLogger.add(PolicyViolationLogEvent.AUTOWAIVE, newPolicyViolation);
            telemetryCollector.addTelemetryForAutoWaivedViolation(newPolicyViolation, component);
            results.autoWaivedViolations.add(newPolicyViolation);
          }

          if (newPolicyViolation.isLegacyViolation()) {
            telemetryCollector.addTelemetryForLegacyViolation(newPolicyViolation, component);
            policyViolationLogger.add(PolicyViolationLogEvent.GRANDFATHER, newPolicyViolation);
            policyViolationLogger.add(PolicyViolationLogEvent.GRANT_LEGACY_STATUS, newPolicyViolation);
          }
        }
        // Fixed policy violations.
        for (PolicyViolation oldPolicyViolation : policyViolationDiff.getCleared()) {
          oldPolicyViolation.setFixTime(policyEvaluation.getTime());

          List<Component> found = findComponentsByComponentIdentifierElseVersionless(components,
              oldPolicyViolation.getComponentIdentifier());

          oldPolicyViolation.setIsRemediatedByVersionChange(
              EvaluationUtils.isRemediatedByVersionChange(found, oldPolicyViolation));

          policyViolationDAO.update(tx, oldPolicyViolation);
          policyViolationLogger.add(PolicyViolationLogEvent.FIX, oldPolicyViolation);

          telemetryCollector.addTelemetryForFixedViolation(oldPolicyViolation, found);
          results.fixedViolations.add(oldPolicyViolation);
        }
        // Existing policy violations.
        List<PolicyViolation> existing = new ArrayList<>();
        for (Map.Entry<PolicyViolation, PolicyViolation> entry : policyViolationDiff.getSame().entrySet()) {
          PolicyViolation oldPolicyViolation = entry.getKey();
          existing.add(oldPolicyViolation);
          PolicyViolation newPolicyViolation = entry.getValue();

          recordConditionTypeViolationAuditTelemetry(telemetryCollector, oldPolicyViolation, components);

          if (!newPolicyViolation.isWaived() && oldPolicyViolation.isWaived()) {
            // The policy violation was un-waived or un-auto-waived
            oldPolicyViolation.setFixTime(policyEvaluation.getTime());
            policyViolationDAO.update(tx, oldPolicyViolation);
            if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
              results.notifiableViolations.add(newPolicyViolation);
            }
            policyViolationDAO.insert(tx, newPolicyViolation);

            policyViolationLogger.add(
                oldPolicyViolation.isAutoWaived()
                    ? PolicyViolationLogEvent.UNAUTOWAIVE
                    : PolicyViolationLogEvent.UNWAIVE,
                newPolicyViolation);
            var component = findComponentByComponentIdentifier(components, oldPolicyViolation.getComponentIdentifier());
            telemetryCollector.addTelemetryForUnwaivedViolation(
                oldPolicyViolation,
                newPolicyViolation,
                component);
          }
          else {
            if (isNotifiable(oldPolicyViolation, newPolicyViolation, forMonitoring, isReevaluation)) {
              results.notifiableViolations.add(oldPolicyViolation);
            }
            oldPolicyViolation.setThreatCategory(newPolicyViolation.getThreatCategory());
            oldPolicyViolation.setActionTypeId(newPolicyViolation.getActionTypeId());
            oldPolicyViolation.setConstraintFacts(newPolicyViolation.getConstraintFacts());
            oldPolicyViolation.setFilename(newPolicyViolation.getFilename());
            oldPolicyViolation.setPolicyName(newPolicyViolation.getPolicyName());

            Component component =
                findComponentByComponentIdentifier(components, oldPolicyViolation.getComponentIdentifier());

            boolean keepExistingViolation = true;

            if (newPolicyViolation.isAutoWaived()) {
              if (oldPolicyViolation.isAutoWaived()) {
                /*
                 * The existing violation was auto waived. Between scans, the old waiver was removed and replaced by
                 * a new one. Alternatively, the old waiver was removed and a waiver configured on a parent
                 * organization now applies. Either way, the violation should continue to be auto waived with the
                 * new waiver.
                 */
                if (!oldPolicyViolation.getAutoPolicyWaiverId().equals(newPolicyViolation.getAutoPolicyWaiverId())) {
                  oldPolicyViolation.setAutoPolicyWaiverId(newPolicyViolation.getAutoPolicyWaiverId());
                  oldPolicyViolation.setWaiveTime(newPolicyViolation.getWaiveTime());
                }
                results.autoWaivedViolations.add(oldPolicyViolation);
              }
              else if (oldPolicyViolation.isWaived()) {
                // Old violation had a policy waiver which has been removed, new violation has an auto waiver
                oldPolicyViolation.setFixTime(policyEvaluation.getTime());
                if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
                  results.notifiableViolations.add(newPolicyViolation);
                }
                policyViolationDAO.insert(tx, newPolicyViolation);

                policyViolationLogger.add(PolicyViolationLogEvent.UNWAIVE, newPolicyViolation);
                telemetryCollector.addTelemetryForUnwaivedViolation(
                    oldPolicyViolation,
                    newPolicyViolation,
                    component);

                policyViolationLogger.add(PolicyViolationLogEvent.AUTOWAIVE, newPolicyViolation);
                telemetryCollector.addTelemetryForAutoWaivedViolation(newPolicyViolation, component);
                results.waivedViolations.remove(oldPolicyViolation);
                results.autoWaivedViolations.add(newPolicyViolation);
                results.allViolations.remove(oldPolicyViolation);
                keepExistingViolation = false;
              }
              else {
                oldPolicyViolation.setAutoPolicyWaiverId(newPolicyViolation.getAutoPolicyWaiverId());
                oldPolicyViolation.setWaiveTime(newPolicyViolation.getWaiveTime());
                policyViolationLogger.add(PolicyViolationLogEvent.AUTOWAIVE, oldPolicyViolation);
                telemetryCollector.addTelemetryForAutoWaivedViolation(oldPolicyViolation, component);
                results.autoWaivedViolations.add(oldPolicyViolation);
              }
            }
            else if (newPolicyViolation.isWaived()) {
              if (oldPolicyViolation.isAutoWaived()) {
                // Old policy violation had an auto waiver which has been removed/no longer applies
                oldPolicyViolation.setFixTime(policyEvaluation.getTime());
                if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
                  results.notifiableViolations.add(newPolicyViolation);
                }
                policyViolationDAO.insert(tx, newPolicyViolation);
                policyViolationLogger.add(PolicyViolationLogEvent.UNAUTOWAIVE, oldPolicyViolation);
                telemetryCollector.addTelemetryForUnwaivedViolation(
                    oldPolicyViolation,
                    newPolicyViolation,
                    component);

                policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
                telemetryCollector.addTelemetryForWaivedViolation(newPolicyViolation, component);

                results.autoWaivedViolations.remove(oldPolicyViolation);
                results.waivedViolations.add(newPolicyViolation);
                results.allViolations.remove(oldPolicyViolation);
                keepExistingViolation = false;
              }
              else if (oldPolicyViolation.isWaived()) {
                // The policy waiver ID has changed (CLM-19768)
                if (!newPolicyViolation.getPolicyWaiverId().equals(oldPolicyViolation.getPolicyWaiverId())) {
                  oldPolicyViolation.setPolicyWaiverId(newPolicyViolation.getPolicyWaiverId());
                  oldPolicyViolation.setPolicyWaiverComment(newPolicyViolation.getPolicyWaiverComment());
                }
              }
              else {
                oldPolicyViolation.setWaiveTime(newPolicyViolation.getWaiveTime());
                oldPolicyViolation.setPolicyWaiverId(newPolicyViolation.getPolicyWaiverId());
                oldPolicyViolation.setPolicyWaiverComment(newPolicyViolation.getPolicyWaiverComment());
                policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, oldPolicyViolation);
                telemetryCollector.addTelemetryForWaivedViolation(oldPolicyViolation, component);
                results.waivedViolations.add(oldPolicyViolation);
              }
            }

            if (oldPolicyViolation.isLegacyViolation() && !oldPolicyViolation.isLegacyViolationApplied() &&
                !oldPolicyViolation.getStageTypeId().equals(Stage.ID_COMPLIANCE))
            {
              oldPolicyViolation.setLegacyViolationApplied(true);
              telemetryCollector.addTelemetryForLegacyViolation(oldPolicyViolation, component);
            }
            else if (oldPolicyViolation.isLegacyViolation() && oldPolicyViolation.isLegacyViolationApplied() &&
                !oldPolicyViolation.getStageTypeId().equals(Stage.ID_COMPLIANCE))
            {
              // Send audit telemetry for unchanged legacy violations to detect missing data
              telemetryCollector.addTelemetryForLegacyViolationAudit(oldPolicyViolation, component);
            }
            else if (!oldPolicyViolation.isLegacyViolation() &&
                oldPolicyViolation.isLegacyViolationApplied())
            {
              // legacy violation was revoked
              oldPolicyViolation.setLegacyViolationApplied(false);
            }
            if (!isLegacyViolationApplicable) {
              oldPolicyViolation.setLegacyViolationTime(null);
            }
            // Firewall (proxy stage) violations should not have legacy status
            if (Stage.ID_PROXY.equals(oldPolicyViolation.getStageTypeId())) {
              oldPolicyViolation.setLegacyViolationTime(null);
            }

            if (keepExistingViolation) {
              // CLM-35315 - make sure after replacement to keep the latest reachability status
              oldPolicyViolation.setReachabilityStatus(
                  determineReachabilityStatus(oldPolicyViolation, newPolicyViolation, isReevaluation));
              results.allViolations.remove(newPolicyViolation);
              results.allViolations.add(oldPolicyViolation);
            }

            policyViolationDAO.update(tx, oldPolicyViolation);
          }
        }
        logPolicyViolations(existing, "previously seen");
        persistApplicationComponents(tx, appId, stage, policyEvaluation.getTime(), components);
      }

      tx.commit();

      policyViolationLogger.log();

      results.activeViolations = filterActivePolicyViolations(results.allViolations, policyEvaluation.getStageTypeId());

      String purgeScanFiles = configuration.getPurgeScanFiles();
      if (purgeScanFiles == null) {
        if (!isReevaluation && lastPrimaryPolicyEvaluation != null) {
          String previousScanId = lastPrimaryPolicyEvaluation.getScanId();
          deletePreviousScanFile(appId, stage, previousScanId);
        }
      }

      log.debug(
          "Persisted policy evaluation results (active={}, waived={}) for application {} from stage {} in {} ms",
          policyResults.getActiveAlerts().size(), policyResults.getWaivedAlerts().size(), appId,
          stage.getStageTypeId(), System.currentTimeMillis() - start);

      updateReportFiles(applicationReport, results, stage, policyIdPolicyOwnerMap, forMonitoring, components);

      return results;
    }
  }

  private static boolean canEvaluateWithAutoWaiver(
      final AutoPolicyWaiver autoPolicyWaiver,
      final PolicyViolation policyViolation)
  {
    return autoPolicyWaiver != null && policyViolation.isActive() &&
        policyViolation.getThreatCategory().equals(PolicyThreatCategory.SECURITY);
  }

  /**
   * Builds a list of constraints formatted for telemetry from the policy violation's constraint facts,
   * specifically for condition type violation telemetry.
   *
   * @param telemetryCollector Collector for formatting telemetry data.
   * @param policyViolation Policy violation containing constraint facts to format.
   * @return List of constraints formatted for condition type violation telemetry.
   */
  private List<Constraint> buildConditionTypeViolationTelemetryConstraints(
      final PolicyViolationTelemetryCollector telemetryCollector,
      final PolicyViolation policyViolation)
  {
    List<Constraint> policyViolationTelemetryConstraints = new ArrayList<>();
    if (policyViolation.getConstraintFacts() != null) {
      for (ConstraintFact constraintFact : policyViolation.getConstraintFacts()) {
        List<Condition> policyViolationTelemetryConditions = new ArrayList<>();
        if (constraintFact.getConditionFacts() != null) {
          for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
            policyViolationTelemetryConditions.add(
                telemetryCollector.formatConditionForTelemetryData(conditionFact,
                    constraintFact.getOperatorName()));
          }
        }
        policyViolationTelemetryConstraints.add(
            telemetryCollector.formatConstraintForTelemetryData(constraintFact,
                policyViolationTelemetryConditions));
      }
    }
    return policyViolationTelemetryConstraints;
  }

  /**
   * Records Condition Type policy violations as telemetry.
   *
   * @param telemetryCollector Collector for adding telemetry.
   * @param newPolicyViolation Policy violation to include in telemetry.
   */
  private void recordConditionTypeViolationTelemetry(
      final PolicyViolationTelemetryCollector telemetryCollector,
      final PolicyViolation newPolicyViolation,
      final List<Component> components)
  {
    List<Component> found = findComponentsByComponentIdentifierElseVersionless(components,
        newPolicyViolation.getComponentIdentifier());

    List<Constraint> policyViolationTelemetryConstraints =
        buildConditionTypeViolationTelemetryConstraints(telemetryCollector, newPolicyViolation);

    telemetryCollector.addTelemetryForConditionTypeViolation(newPolicyViolation, found,
        policyViolationTelemetryConstraints);
  }

  /**
   * Records audit telemetry for existing condition type violations that remain unchanged.
   *
   * @param telemetryCollector Collector for adding telemetry.
   * @param policyViolation Existing policy violation from database (must have an ID).
   * @param components List of components to search for associated component.
   */
  private void recordConditionTypeViolationAuditTelemetry(
      final PolicyViolationTelemetryCollector telemetryCollector,
      final PolicyViolation policyViolation,
      final List<Component> components)
  {
    List<Component> foundComponents = findComponentsByComponentIdentifierElseVersionless(components,
        policyViolation.getComponentIdentifier());

    List<Constraint> policyViolationTelemetryConstraints =
        buildConditionTypeViolationTelemetryConstraints(telemetryCollector, policyViolation);

    telemetryCollector.addTelemetryForConditionTypeViolationAudit(
        policyViolation,
        foundComponents,
        policyViolationTelemetryConstraints);
  }

  /**
   * If this is the first policy evaluation and legacy violations are enabled for the application, then policy
   * violations are marked as legacy for the policies that are enabled for legacy. If this is not the first policy
   * evaluation, then it marks policy violations as legacy based on the existing legacy violations (across all stages).
   */
  private void setLegacyViolations(
      TransactionContext tx,
      boolean areLegacyViolationsEnabled,
      Application app,
      List<Policy> policies,
      Date policyEvaluationTime,
      List<PolicyViolation> policyViolations)
  {
    // The check if this is the first evaluation can be expensive. Do it only if legacy violations are enabled.
    if (areLegacyViolationsEnabled && isFirstEvaluation(tx, app)) {
      if (!productLicense.hasFeature(LicensedFeature.POLICY_GRANDFATHERING)) {
        log.debug("No legacy violations in the first evaluation for application {}, "
            + "license does not support legacy violations.", app.getId());
        return;
      }
      Map<String, Policy> policiesById = policies.stream().collect(toMap(Policy::getId, Function.identity()));
      policyViolations.stream() //
          .filter(policyViolation -> policiesById.get(policyViolation.getPolicyId())
              .isLegacyViolationAllowed())
          .forEach(policyViolation -> {
            // Firewall (proxy stage) violations should not be marked as legacy
            if (!Stage.ID_PROXY.equals(policyViolation.getStageTypeId())) {
              policyViolation.setLegacyViolationTime(policyEvaluationTime);
            }
          });
    }
    else {
      List<PolicyViolation> legacyViolations =
          policyViolationDAO.getUnfixedLegacyViolationByApplicationId(tx, app.getId());
      policyViolationDAO.loadConstraintFacts(legacyViolations);
      if (!legacyViolations.isEmpty()) {
        PolicyViolationDiff<PolicyViolation> policyViolationDiff = PolicyViolationDigester
            .digestPolicyViolations(legacyViolations, policyViolations);
        policyViolationDiff.getSame()
            .forEach( //
                (legacyViolation, newPolicyViolation) -> {
                  // Firewall (proxy stage) violations should not inherit legacy status
                  // and should not propagate legacy status to other violations
                  if (!Stage.ID_PROXY.equals(newPolicyViolation.getStageTypeId()) &&
                      !Stage.ID_PROXY.equals(legacyViolation.getStageTypeId()))
                {
                    newPolicyViolation.setLegacyViolationTime(legacyViolation.getLegacyViolationTime());
                  }
                });
      }
    }
  }

  private boolean isFirstEvaluation(TransactionContext tx, Application app) {
    // The record for the current policy evaluation was already created, so we have to check with 1, not 0.
    return policyEvaluationDAO.getCountByApplicationId(tx, app.getId()) == 1;
  }

  private String getFilename(ComponentFact componentFact) {
    return new ComponentDisplayFilename().addPathnames(componentFact.getPathnames()).getFilename().orElse(null);
  }

  private boolean isNotifiable(
      PolicyViolation oldPolicyViolation,
      PolicyViolation newPolicyViolation,
      boolean forMonitoring,
      boolean isReevaluation)
  {
    if (isReevaluation && !forMonitoring) {
      return false;
    }
    boolean active = newPolicyViolation.isActive();
    boolean wasSeen;
    if (oldPolicyViolation == null) {
      wasSeen = false;
    }
    else if (forMonitoring) {
      wasSeen = oldPolicyViolation.isSeenByMonitoringEvaluation() || oldPolicyViolation.isSeenByPrimaryEvaluation();
      oldPolicyViolation.setSeenByMonitoringEvaluation(active);
    }
    else {
      wasSeen = oldPolicyViolation.isSeenByPrimaryEvaluation();
      oldPolicyViolation.setSeenByPrimaryEvaluation(active);
    }
    return active && !wasSeen;
  }

  private void deletePreviousScanFile(String appId, Stage stage, String previousScanId) {
    ScanEntity previousScanEntity = scanPersistenceService.getScan(appId, previousScanId);
    try {
      if (scanPersistenceService.deleteScan(previousScanEntity)) {
        log.debug("Deleted obsolete scan file for app ID {} and stage {}: {}.", appId, stage,
            previousScanEntity.getLocation());
      }
    }
    catch (Exception e) {
      log.error("Cannot delete previous scan file for app ID {} and stage {}: {}. Cause: {}", appId, stage,
          previousScanEntity.getLocation(), e.getMessage(), e);
    }
  }

  private void persistApplicationComponents(
      TransactionContext tx,
      String appId,
      Stage stage,
      Date time,
      List<Component> components)
  {
    // Delete all app->component associations for the specified stage
    List<ApplicationComponent> oldApplicationComponents = applicationComponentDAO.getByApplicationIdAndStageTypeId(tx,
        appId, stage.getStageTypeId());
    for (ApplicationComponent oldApplicationComponent : oldApplicationComponents) {
      applicationComponentDAO.delete(tx, oldApplicationComponent);
    }

    // Collect all entities in memory first to enable batch inserts by table
    List<ApplicationComponent> newApplicationComponents = new ArrayList<>();
    List<AggregateFile> newAggregateFiles = new ArrayList<>();
    List<ApplicationComponentLicense> newLicenses = new ArrayList<>();

    // Build all entities
    for (Component component : components) {
      if (component.getHash() == null) {
        continue;
      }

      ApplicationComponent applicationComponent = new ApplicationComponent(appId, stage.getStageTypeId(), time,
          component.getHash(), component.getComponentIdentifier(), component.getMatchState().getId(), component
              .getIdentificationSource()
              .getId(),
          component.isProprietary(), component.getPathnames());
      applicationComponent.setId(IdUtil.newUUID());
      newApplicationComponents.add(applicationComponent);

      for (com.sonatype.clm.dto.model.component.AggregateFile aggregateFile : component.getAggregateFiles()) {
        newAggregateFiles.add(
            new AggregateFile(applicationComponent.getId(), aggregateFile.hash, aggregateFile.pathnames));
      }

      Set<String> effectiveLicenseIds = ComponentDetailsLoader.calculateEffectiveLicenses(
          component.getDeclaredMultiLicenseIds(),
          component.getObservedMultiLicenseIds(),
          component.getLicenseOverrideIds());
      for (String effectiveLicenseId : effectiveLicenseIds) {
        newLicenses.add(new ApplicationComponentLicense(applicationComponent.getId(), effectiveLicenseId));
      }
    }

    // Insert by table type for clarity - consider using jOOQ batch API if performance is critical
    for (ApplicationComponent applicationComponent : newApplicationComponents) {
      applicationComponentDAO.insert(tx, applicationComponent);
    }

    for (AggregateFile aggregateFile : newAggregateFiles) {
      aggregateFileDAO.insert(tx, aggregateFile);
    }

    for (ApplicationComponentLicense license : newLicenses) {
      applicationComponentLicenseDAO.insert(tx, license);
    }
  }

  private void calculateCounters(
      PolicyEvaluationResult policyEvaluationResult,
      List<PolicyViolation> policyViolations)
  {
    final Map<String, Integer> componentThreatLevels = new HashMap<>();
    int criticalPolicyViolationCount = 0;
    int severePolicyViolationCount = 0;
    int moderatePolicyViolationCount = 0;
    int legacyViolationCount = 0;
    for (PolicyViolation policyViolation : policyViolations) {
      if (policyViolation.isActive()) {
        final int policyThreatLevelNumber = policyViolation.getThreatLevel();
        final String id = policyViolation.getHash();
        final Integer policyThreatLevelForComponent = componentThreatLevels.get(id);
        if (policyThreatLevelForComponent == null || policyThreatLevelForComponent < policyThreatLevelNumber) {
          componentThreatLevels.put(id, policyThreatLevelNumber);
        }

        ThreatLevel policyThreatLevel = ThreatLevel.from(policyThreatLevelNumber);
        switch (policyThreatLevel) {
          case CRITICAL:
            criticalPolicyViolationCount++;
            break;
          case SEVERE:
            severePolicyViolationCount++;
            break;
          case MODERATE:
            moderatePolicyViolationCount++;
            break;
          case LOW:
            // We don't count for LOW
            break;
          default:
            throw new IllegalArgumentException("Unknown threat level " + policyThreatLevel);
        }
      }
      else if (policyViolation.isLegacyViolation()) {
        legacyViolationCount++;
      }
    }

    int criticalComponentCount = 0;
    int severeComponentCount = 0;
    int moderateComponentCount = 0;
    for (final int policyThreatLevelForComponent : componentThreatLevels.values()) {
      ThreatLevel policyThreatLevel = ThreatLevel.from(policyThreatLevelForComponent);
      switch (policyThreatLevel) {
        case CRITICAL:
          criticalComponentCount++;
          break;
        case SEVERE:
          severeComponentCount++;
          break;
        case MODERATE:
          moderateComponentCount++;
          break;
        case LOW:
          // We don't count for LOW
          break;
        default:
          throw new IllegalArgumentException("Unknown threat level " + policyThreatLevel);
      }
    }

    policyEvaluationResult
        .setAffectedComponentCount(criticalComponentCount + severeComponentCount + moderateComponentCount);
    policyEvaluationResult.setCriticalComponentCount(criticalComponentCount);
    policyEvaluationResult.setSevereComponentCount(severeComponentCount);
    policyEvaluationResult.setModerateComponentCount(moderateComponentCount);

    policyEvaluationResult.setCriticalPolicyViolationCount(criticalPolicyViolationCount);
    policyEvaluationResult.setSeverePolicyViolationCount(severePolicyViolationCount);
    policyEvaluationResult.setModeratePolicyViolationCount(moderatePolicyViolationCount);
    policyEvaluationResult.setLegacyViolationCount(legacyViolationCount);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(PolicyEvaluation policyEvaluation, boolean createAlerts) {
    List<PolicyViolation> policyViolations = policyViolationDAO
        .getActiveByApplicationIdAndStageId(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId());
    return createPolicyEvaluationResult(policyEvaluation, policyViolations, createAlerts);
  }

  /**
   * Creates a policy evaluation result with optional total component count loading.
   * This overload fetches policy violations from the database.
   *
   * @param policyEvaluation the policy evaluation
   * @param createAlerts whether to create alerts
   * @param loadTotalComponentCount whether to load the total component count (reads summary.json if not provided)
   * @return the policy evaluation result
   */
  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      boolean createAlerts,
      boolean loadTotalComponentCount)
  {
    List<PolicyViolation> policyViolations = policyViolationDAO
        .getActiveByApplicationIdAndStageId(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId());
    return createPolicyEvaluationResult(policyEvaluation, Collections.emptyList(), policyViolations, createAlerts,
        null, loadTotalComponentCount);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      List<PolicyViolation> policyViolations,
      boolean createAlerts)
  {
    return createPolicyEvaluationResult(policyEvaluation, Collections.emptyList(), policyViolations, createAlerts,
        null);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      List<PolicyViolation> policyViolations,
      boolean createAlerts,
      ReportEntry summaryReportEntry)
  {
    return createPolicyEvaluationResult(policyEvaluation, Collections.emptyList(), policyViolations, createAlerts,
        summaryReportEntry);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      List<Component> components,
      List<PolicyViolation> policyViolations,
      boolean createAlerts)
  {
    return createPolicyEvaluationResult(policyEvaluation, components, policyViolations, createAlerts, null);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      List<Component> components,
      List<PolicyViolation> policyViolations,
      boolean createAlerts,
      ReportEntry summaryReportEntry)
  {
    return createPolicyEvaluationResult(policyEvaluation, components, policyViolations, createAlerts,
        summaryReportEntry, true);
  }

  /**
   * Creates a policy evaluation result with optional total component count loading.
   *
   * @param policyEvaluation the policy evaluation
   * @param components the components
   * @param policyViolations the policy violations
   * @param createAlerts whether to create alerts
   * @param summaryReportEntry optional pre-loaded summary report entry
   * @param loadTotalComponentCount whether to load the total component count (reads summary.json if not provided)
   * @return the policy evaluation result
   */
  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      List<Component> components,
      List<PolicyViolation> policyViolations,
      boolean createAlerts,
      ReportEntry summaryReportEntry,
      boolean loadTotalComponentCount)
  {
    PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    calculateCounters(policyEvaluationResult, policyViolations);
    if (createAlerts) {
      List<PolicyViolation> activePolicyViolations = filterActivePolicyViolations(policyViolations,
          policyEvaluation.getStageTypeId());
      List<PolicyAlert> policyAlerts = createPolicyAlerts(policyEvaluation.getApplicationId(),
          policyEvaluation.getScanId(), policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring(),
          components, activePolicyViolations);
      policyEvaluationResult.setAlerts(policyAlerts);
    }
    if (loadTotalComponentCount) {
      policyEvaluationResult.setTotalComponentCount(getTotalComponentCount(policyEvaluation, summaryReportEntry));
    }
    return policyEvaluationResult;
  }

  /**
   * Stage-aware filtering: Firewall ignores legacy violations completely,
   * Lifecycle excludes legacy violations.
   */
  private List<PolicyViolation> filterActivePolicyViolations(
      List<PolicyViolation> policyViolations,
      String stageTypeId)
  {
    boolean isFirewallContext = Stage.ID_PROXY.equals(stageTypeId);

    if (isFirewallContext) {
      return policyViolations.stream()
          .filter(PolicyViolation::isActiveForFirewall)
          .collect(toList());
    }
    else {
      return policyViolations.stream()
          .filter(PolicyViolation::isActive)
          .collect(toList());
    }
  }

  /**
   * @since 1.25.0
   */
  private void postEvents(
      ScanPolicyEvaluatorResults scanPolicyEvaluatorResults,
      Application application,
      List<Component> components)
  {
    final PolicyEvaluation policyEvaluation = scanPolicyEvaluatorResults.evaluation;
    final List<PolicyViolation> activeViolations = scanPolicyEvaluatorResults.activeViolations;
    final List<PolicyViolation> waivedViolations = scanPolicyEvaluatorResults.waivedViolations;
    final List<PolicyViolation> fixedViolations = scanPolicyEvaluatorResults.fixedViolations;

    PolicyEvaluationResult policyEvaluationResult =
        createPolicyEvaluationResult(policyEvaluation, components, activeViolations, true);

    List<PolicyAlert> waivedAlerts = createPolicyAlerts(policyEvaluation.getApplicationId(),
        policyEvaluation.getScanId(), policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring(),
        components, waivedViolations);

    List<PolicyAlert> fixedAlerts = createPolicyAlerts(policyEvaluation.getApplicationId(),
        policyEvaluation.getScanId(), policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring(),
        components, fixedViolations);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult, application);
    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, application, waivedAlerts, fixedAlerts);
  }

  private int getTotalComponentCount(PolicyEvaluation policyEvaluation, ReportEntry summaryReportEntry) {
    try {
      ReportEntry summaryEntry = summaryReportEntry;
      if (summaryEntry == null) {
        ApplicationReport applicationReport =
            reportService.getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
        summaryEntry = applicationReport.getEntry(SUMMARY_JSON.getName());
      }
      if (summaryEntry != null) {
        JsonNode content = JsonUtils.parse(summaryEntry.buf);
        return content.get("totalArtifactCount").asInt();
      }
    }
    catch (IOException | NotFoundException e) {
      log.error("Could not get report for applicationId={} and scanId={}", policyEvaluation.getApplicationId(),
          policyEvaluation.getScanId(), e);
    }
    return 0;
  }

  @VisibleForTesting
  void sendEvaluationTelemetry(
      String scanId,
      String applicationId,
      String stageId,
      ScanTriggerType scanTriggerType,
      Collection<Component> components,
      String clientUserAgent,
      String clientInstanceId)
  {
    Map<String, Long> componentCounts = getComponentCounts(components);
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        scanId, applicationId, stageId, scanTriggerType, clientUserAgent, clientInstanceId,
        Collections.singletonMap("component_counts", componentCounts));
    telemetrySender.send(telemetryData);
  }

  private Map<String, Long> getComponentCounts(Collection<Component> components) {
    return components.stream()
        .map(Component::getComponentIdentifier)
        .map(componentIdentifier -> componentIdentifier == null ? UNKNOWN : componentIdentifier.getFormat())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  /**
   * @since 1.50
   */
  void sendLegacyViolationTelemetryData(
      String applicationId,
      List<PolicyViolation> policyViolations,
      String stageTypeId)
  {
    TelemetryData telemetryData = new TelemetryData(
        TelemetryPurpose.APPLICATION_EVALUATION_LEGACY_VIOLATION_COUNTS);
    telemetryData.setAttributes(getLegacyViolationCountsAttributes(applicationId, policyViolations, stageTypeId));
    telemetrySender.send(telemetryData);
  }

  /**
   * @since 1.50
   */
  private Map<String, Object> getLegacyViolationCountsAttributes(
      String applicationId,
      List<PolicyViolation> policyViolations,
      String stageTypeId)
  {
    Map<ThreatLevel, Long> threatLevels = new HashMap<>();
    for (ThreatLevel threatLevel : ThreatLevel.values()) {
      threatLevels.put(threatLevel, 0L);
    }
    Map<PolicyThreatCategory, Long> policyThreatCategories = new HashMap<>();
    for (PolicyThreatCategory policyThreatCategory : PolicyThreatCategory.values()) {
      policyThreatCategories.put(policyThreatCategory, 0L);
    }

    int legacyViolationCount = 0;
    for (PolicyViolation policyViolation : policyViolations) {
      if (policyViolation.isLegacyViolation()) {
        ThreatLevel threatLevel = ThreatLevel.from(policyViolation.getThreatLevel());
        threatLevels.put(threatLevel, threatLevels.get(threatLevel) + 1);

        PolicyThreatCategory policyThreatCategory = policyViolation.getThreatCategory();
        policyThreatCategories.put(policyThreatCategory, policyThreatCategories.get(policyThreatCategory) + 1);

        legacyViolationCount++;
      }
    }

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    telemetryUtils.includeRealApplicationId(attributes, applicationId);
    attributes.put("grandfathering_enabled",
        String.valueOf(legacyViolationService.isLegacyViolationEnabled(applicationId, stageTypeId)));
    attributes.put("number_of_grandfathered_violations", String.valueOf(legacyViolationCount));
    if (legacyViolationCount > 0) {
      for (Entry<ThreatLevel, Long> entry : threatLevels.entrySet()) {
        attributes.put("number_of_grandfathered_violations_with_" + entry.getKey().name().toLowerCase(Locale.ENGLISH)
            + "_threat_level", String.valueOf(entry.getValue()));
      }
      for (Entry<PolicyThreatCategory, Long> entry : policyThreatCategories.entrySet()) {
        attributes.put("number_of_grandfathered_violations_in_" + entry.getKey().name().toLowerCase(Locale.ENGLISH)
            + "_policy_threat_category", String.valueOf(entry.getValue()));
      }
    }
    return attributes;
  }

  private String extractField(ReportEntry dataReportEntry, String fieldName) throws IOException {
    if (null == dataReportEntry) {
      return null;
    }
    return JsonUtils.parse(dataReportEntry.buf).path(fieldName).asText(null);
  }

  private void logPolicyViolations(List<PolicyViolation> policyViolations, String policyProperty) {
    if (policyViolations.isEmpty()) {
      log.debug("No {} policies violated.", policyProperty);
    }
    else {
      Map<String, Long> policyViolationCount = policyViolations.stream()
          .collect(Collectors.groupingBy(AbstractPolicyViolation::getPolicyName, Collectors.counting()));
      String stringified = policyViolationCount.keySet()
          .stream()
          .sorted()
          .map(s -> s + "(" + policyViolationCount.get(s).toString() + ")")
          .collect(Collectors.joining(", "));
      // 6 new policies violated: My-First-Policy(4), My-Second-Policy(2).
      // 6 previously seen policies violated: My-First-Policy(4), My-Second-Policy(2).
      log.debug("{} {} policies violated: {}.", policyViolations.size(), policyProperty, stringified);
    }
  }

  private Component findComponentByComponentIdentifier(
      List<Component> components,
      ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier == null) {
      return null;
    }
    return components.stream()
        .filter(c -> c.getComponentIdentifier() != null)
        .filter(c -> c.getComponentIdentifier().equals(componentIdentifier))
        .findFirst()
        .orElse(null);
  }

  private List<Component> findComponentsByComponentIdentifierElseVersionless(
      List<Component> components,
      ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier == null) {
      return Collections.emptyList();
    }
    Component component = findComponentByComponentIdentifier(components, componentIdentifier);
    if (component != null) {
      return Collections.singletonList(component);
    }
    ComponentIdentifier versionlessComponentIdentifier = componentIdentifier.createAlternativeVersion(null);
    return components.stream()
        .filter(c -> c.getComponentIdentifier() != null)
        .filter(c -> c.getComponentIdentifier()
            .createAlternativeVersion(null)
            .equals(versionlessComponentIdentifier))
        .collect(toList());
  }

  /**
   * @since 1.180
   */
  private void sendStaleReportEvaluationTelemetryData(final String privateAppId, final String scanId) {
    TelemetryData telemetryData = new TelemetryData(
        TelemetryPurpose.STALE_REPORT_REEVALUATION);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", HdsClientAnalytics.obfuscate(privateAppId));
    attributes.put("scan_id", HdsClientAnalytics.obfuscate(scanId));
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private void throwErrorIfReEvaluatingAnOldScan(
      final String privateAppId,
      final String scanId,
      final String stageTypeId)
  {
    final boolean isReevaluation =
        policyEvaluationDAO.getLastByApplicationIdAndScanId(privateAppId, scanId) != null;

    if (isReevaluation) {
      final PolicyEvaluation lastPrimaryPolicyEvaluation = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(
          privateAppId, stageTypeId);
      final boolean isNotForLatestScan = !lastPrimaryPolicyEvaluation.getScanId().equals(scanId);

      if (isNotForLatestScan) {
        sendStaleReportEvaluationTelemetryData(privateAppId, scanId);
        throw new BadRequestException(REEVALUATE_NOT_ALLOWED_FOR_OUT_OF_DATE_SCAN_MESSAGE);
      }
    }
  }

  public ScanPolicyEvaluatorResults performPolicyEvaluation(
      final Application application,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final String clientUserAgent,
      final String clientInstanceId,
      final boolean forMonitoring,
      final ClientScanType clientScanType,
      final ReportComponentData reportComponentData,
      final boolean skipAutoWaivers) throws IOException
  {
    return performPolicyEvaluation(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
        forMonitoring, clientScanType, reportComponentData, null, skipAutoWaivers);
  }

  public ScanPolicyEvaluatorResults performPolicyEvaluation(
      final Application application,
      final String scanId,
      final Stage stage,
      final ScanTriggerType scanTriggerType,
      final String clientUserAgent,
      final String clientInstanceId,
      final boolean forMonitoring,
      final ClientScanType clientScanType,
      final ReportComponentData reportComponentData,
      final VulnerabilitySignatureAnalysisDTO analysisDTO,
      final boolean skipAutoWaivers) throws IOException
  {
    sendEvaluationTelemetry(scanId, application.getId(), stage.getStageTypeId(), scanTriggerType,
        reportComponentData.components,
        clientUserAgent, clientInstanceId);

    String appId = application.getId();
    String ownerId = getPolicyOwnerIdForEvaluation(application, stage);
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(ownerId);
    PolicyResults policyResults =
        componentPolicyEvaluator.evaluate(appId, stage, policies, reportComponentData.components,
            forMonitoring);

    PolicyViolationTelemetryCollector telemetryCollector = new PolicyViolationTelemetryCollector(
        policyWaiverDAO,
        sourceControlEventDAO,
        telemetryUtils,
        licenseNameProvider,
        sourceControlUtils.isScmEnabled(appId),
        componentHelper);

    AutoPolicyWaiverTelemetryCollector autoPolicyWaiverTelemetryCollector =
        new AutoPolicyWaiverTelemetryCollector(telemetryUtils);

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        processPolicyResults(application, scanId, stage, scanTriggerType, policies, forMonitoring, policyResults,
            reportComponentData.components, telemetryCollector, autoPolicyWaiverTelemetryCollector,
            reportComponentData.applicationReport, clientScanType, analysisDTO, skipAutoWaivers);

    sendAggregatePolicyViolationAndAutoWaiverTelemetry(telemetryCollector, autoPolicyWaiverTelemetryCollector);

    sendLegacyViolationTelemetryData(application.getId(), scanPolicyEvaluatorResults.allViolations,
        stage.getStageTypeId());

    sendMissingEpssScoreTelemetry(application.getId(), scanId, stage.getStageTypeId(),
        reportComponentData.components);

    final Set<Feature> features = featuresService.getFeatures();
    if (features.contains(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS)) {
      fetchAndPersistRemediationRecommendations(scanId, stage, reportComponentData.components, appId);
    }

    postEvents(scanPolicyEvaluatorResults, application, reportComponentData.components);
    thirdPartySbomMetadataDAO.makeSbomActiveIfExist(scanId);
    return scanPolicyEvaluatorResults;
  }

  String getPolicyOwnerIdForEvaluation(final Application application, final Stage stage) {
    boolean isContainerImageEval = isEvaluationForContainerImage(stage);

    if (isContainerImageEval) {
      return organizationDAO.getById(application.getOrganizationId()).getRelatedRepositoryId();
    }

    return application.getId();
  }

  private boolean isEvaluationForContainerImage(final Stage stage) {
    return (stage.getStageTypeId().equals(Stage.ID_PROXY) &&
        productLicense.hasFeature(LicensedFeature.CONTAINER_IMAGES_EVALUATION) &&
        SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.isEnabled());
  }

  private boolean evaluateAutoPolicyWaiver(
      final String appId,
      final Component component,
      final PolicyViolation policyViolation,
      final AutoPolicyWaiver autoPolicyWaiver,
      final List<AutoPolicyWaiverExclusion> autoPolicyWaiverExclusions,
      final String stageId,
      final String scanId,
      final boolean hasReachabilityData)
  {
    if (violationHasPolicyWaiver(policyViolation)) {
      return false;
    }
    if (violationHasApplicableAutoWaiverExclusion(policyViolation, autoPolicyWaiverExclusions)) {
      return false;
    }
    if (!violationMeetsThreatLevelCriteria(policyViolation, autoPolicyWaiver)) {
      return false;
    }

    return shouldAutoWaiveBasedOnScopesAndOperator(appId, component, policyViolation, autoPolicyWaiver, stageId, scanId,
        hasReachabilityData);
  }

  private boolean shouldAutoWaiveBasedOnScopesAndOperator(
      final String appId,
      final Component component,
      final PolicyViolation policyViolation,
      final AutoPolicyWaiver autoPolicyWaiver,
      final String stageId,
      final String scanId,
      final boolean hasReachabilityData)
  {
    final boolean isReachabilityConfigured =
        violationMeetsReachabilityConfigurationCriteria(hasReachabilityData, autoPolicyWaiver, policyViolation);
    boolean isNotReachable = false;
    if (isReachabilityConfigured) {
      isNotReachable = violationIsNonReachable(policyViolation);
      if (autoPolicyWaiver.getScopesOperatorAny() && isNotReachable) {
        return true;
      }
    }

    final boolean isNoPathForwardConfigured = isNoPathForwardConfigured(autoPolicyWaiver, component);
    boolean hasNoPathForward = false;
    if (isNoPathForwardConfigured) {
      hasNoPathForward = !componentHasPathForward(appId, component, stageId, scanId);
      if (autoPolicyWaiver.getScopesOperatorAny() && hasNoPathForward) {
        return true;
      }
    }

    if (!autoPolicyWaiver.getScopesOperatorAny()) {
      return isNotReachable && hasNoPathForward;
    }

    return false;
  }

  private boolean violationHasPolicyWaiver(final PolicyViolation policyViolation) {
    return policyViolation.getPolicyWaiverId() != null && policyViolation.getWaiveTime() != null;
  }

  private boolean violationMeetsThreatLevelCriteria(
      final PolicyViolation policyViolation,
      final AutoPolicyWaiver autoPolicyWaiver)
  {
    return policyViolation.getThreatLevel() <= autoPolicyWaiver.getThreatLevel();
  }

  private boolean violationMeetsReachabilityConfigurationCriteria(
      final boolean hasReachabilityData,
      final AutoPolicyWaiver autoPolicyWaiver,
      final PolicyViolation policyViolation)
  {
    if (!autoPolicyWaiver.hasReachability()) {
      return false;
    }

    if (hasReachabilityData) {
      return true;
    }

    // CLM-37144 - Allow reachability-based auto-waivers even when there's no new reachability data
    // if the violation already has reachability data from a previous evaluation
    ReachabilityStatus existingStatus = policyViolation.getReachabilityStatus();
    return existingStatus != null && existingStatus != ReachabilityStatus.UNKNOWN;
  }

  private boolean violationIsNonReachable(final PolicyViolation policyViolation) {
    return ReachabilityStatus.NON_REACHABLE.equals(policyViolation.getReachabilityStatus());
  }

  private boolean isNoPathForwardConfigured(final AutoPolicyWaiver autoPolicyWaiver, final Component component) {
    return autoPolicyWaiver.hasPathForward() && component.getComponentIdentifier() != null;
  }

  private boolean componentHasPathForward(
      final String appId,
      final Component component,
      final String stageId,
      final String scanId)
  {
    return pathForwardInspector.containsUpgradeableVersion(component.getComponentIdentifier(), appId, stageId, scanId);
  }

  private List<AutoPolicyWaiver> getApplicableAutoPolicyWaivers(final List<String> ownerIds) {
    final List<AutoPolicyWaiver> autoPolicyWaivers = new ArrayList<>();
    ownerIds.forEach(id -> autoPolicyWaivers.addAll(autoPolicyWaiverDAO.getByOwnerId(id)));

    return AutoPolicyWaiverUtil.getApplicableAutoPolicyWaivers(autoPolicyWaivers);
  }

  private List<AutoPolicyWaiverExclusion> getApplicableAutoPolicyWaiverExclusions(final List<String> ownerIds) {
    final List<AutoPolicyWaiverExclusion> autoPolicyWaiverExclusions = new ArrayList<>();

    for (String id : ownerIds) {
      autoPolicyWaiverDAO.getByOwnerId(id)
          .stream()
          .filter(Objects::nonNull)
          .forEach(autoPolicyWaiver -> autoPolicyWaiverExclusions.addAll(
              autoPolicyWaiverExclusionDAO
                  .getByOwnerIdAndAutoPolicyWaiverId(autoPolicyWaiver.getOwnerId(), autoPolicyWaiver.getId())));
    }

    return autoPolicyWaiverExclusions;
  }

  private List<String> getOwnerIds(final String applicationId) {
    final Set<Feature> features = featuresService.getFeatures();
    if (features.contains(SystemConfigurationPropertyFeature.AUTO_WAIVERS)) {
      return ownerDAO.getOwnerIds(applicationId);
    }

    return Collections.emptyList();
  }

  private boolean violationHasApplicableAutoWaiverExclusion(
      PolicyViolation policyViolation,
      List<AutoPolicyWaiverExclusion> autoPolicyWaiverExclusions)
  {
    policyViolationDAO.loadConstraintFacts(Collections.singletonList(policyViolation));
    for (AutoPolicyWaiverExclusion exclusion : autoPolicyWaiverExclusions) {
      AutoPolicyWaiverExclusionMatcherWrapper wrapper =
          new AutoPolicyWaiverExclusionMatcherWrapper(exclusion);
      if (wrapper.matchesViolation(policyViolation)) {
        return true;
      }
    }
    return false;
  }

  private PurlIdentifiersWithVulnerabilities getReachablePurlIdentifiersWithVulnerabilities(
      final String applicationId,
      final String scanId,
      final VulnerabilitySignatureAnalysisDTO analysisDTO) throws IOException
  {
    if (analysisDTO == null) {
      return null;
    }

    return apiVulnerabilityReachabilityStatusService.getPurlIdentifiersWithVulnerabilities(
        applicationId,
        scanId,
        analysisDTO);
  }

  @VisibleForTesting
  void sendMissingEpssScoreTelemetry(
      final String applicationId,
      final String scanId,
      final String stageTypeId,
      final List<Component> components)
  {
    long vulnerabilitiesWithMissingEpss = components.stream()
        .flatMap(component -> component.getSecurityVulnerabilities().stream())
        .filter(vulnerability -> vulnerability.getEpssData() == null ||
            vulnerability.getEpssData().getCurrentScore() == null)
        .count();

    TelemetryData telemetryData = new TelemetryData(
        TelemetryPurpose.APPLICATION_EVALUATION_MISSING_EPSS_SCORE_COUNT);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    telemetryUtils.includeRealApplicationId(attributes, applicationId);
    attributes.put("scan_id", HdsClientAnalytics.obfuscate(scanId));
    attributes.put("stage_id", stageTypeId);
    attributes.put("vulnerabilities_with_missing_epss", String.valueOf(vulnerabilitiesWithMissingEpss));
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private void sendAggregatePolicyViolationAndAutoWaiverTelemetry(
      final PolicyViolationTelemetryCollector policyViolationTelemetryCollector,
      final AutoPolicyWaiverTelemetryCollector autoPolicyWaiverTelemetryCollector)
  {
    final List<TelemetryData> aggregateTelemetryDataList = new ArrayList<>();
    aggregateTelemetryDataList.addAll(policyViolationTelemetryCollector.getTelemetryData());
    aggregateTelemetryDataList.addAll(autoPolicyWaiverTelemetryCollector.getTelemetryData());
    telemetrySender.send(aggregateTelemetryDataList);
  }

  private ReachabilityStatus determineReachabilityStatus(
      final PolicyViolation oldViolation,
      final PolicyViolation newViolation,
      final boolean isReevaluation)
  {
    if (!isReevaluation) {
      return newViolation.getReachabilityStatus();
    }

    // During reevaluations, preserve old reachability data if it exists (not null and not UNKNOWN)
    ReachabilityStatus oldStatus = oldViolation.getReachabilityStatus();
    if (oldStatus != null && oldStatus != ReachabilityStatus.UNKNOWN) {
      return oldStatus;
    }

    return newViolation.getReachabilityStatus();
  }
}
