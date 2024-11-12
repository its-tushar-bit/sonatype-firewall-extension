/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.dataaccess.AggregateFileDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentLicenseDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritizationRemediationService;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.*;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.violation.ApplicationPolicyViolationLogger;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLogEvent;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.telemetry.AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction;
import com.sonatype.insight.brain.telemetry.AutoPolicyWaiverTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Named
public class ScanPolicyEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(ScanPolicyEvaluator.class);

  public static final String POLICY_ALERTS_FILENAME = "policyalerts.json";

  public static final String POLICY_THREATS_FILENAME = "policythreats.json";

  private static final String UNKNOWN = "unknown";

  public static final String REEVALUATE_NOT_ALLOWED_FOR_OUT_OF_DATE_SCAN_MESSAGE =
      "Could not Re-Evaluate this report because it is out of date. Navigate to the latest evaluation for this stage.";

  private final InsightWork work;

  private final ReportService reportService;

  private final PolicyDAO policyDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final AggregateFileDAO aggregateFileDAO;

  private final ApplicationComponentLicenseDAO applicationComponentLicenseDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final OwnerDAO ownerDAO;

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

  private final ComponentInfoService componentInfoService;

  private final ReportComponentService reportComponentService;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final AutoPolicyWaiverTelemetryMetrics autoPolicyWaiverTelemetryMetrics;

  @Inject
  public ScanPolicyEvaluator(
      final InsightWork insightWork,
      final ReportService reportService,
      final PolicyDAO policyDAO,
      final PolicyViolationDAO policyViolationDAO,
      final AggregateFileDAO aggregateFileDAO,
      final ApplicationComponentLicenseDAO applicationComponentLicenseDAO,
      final ApplicationComponentDAO applicationComponentDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final OwnerDAO ownerDAO,
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
      final AutoPolicyWaiverTelemetryMetrics autoPolicyWaiverTelemetryMetrics)
  {
    this.work = insightWork;
    this.reportService = reportService;
    this.policyDAO = policyDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.aggregateFileDAO = aggregateFileDAO;
    this.applicationComponentLicenseDAO = applicationComponentLicenseDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.ownerDAO = ownerDAO;
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
    this.componentInfoService = componentInfoService;
    this.reportComponentService = reportComponentService;
    componentInfoService.setToolName("ci");
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.autoPolicyWaiverTelemetryMetrics = autoPolicyWaiverTelemetryMetrics;
  }

  public ScanPolicyEvaluatorResults evaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      ScanTriggerType scanTriggerType,
      ClientScanType clientScanType)
      throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, null, null, false /* forMonitoring */,
        clientScanType);
  }

  public ScanPolicyEvaluatorResults evaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      ClientScanType clientScanType)
      throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
        false /* forMonitoring */, clientScanType);
  }

  public ScanPolicyEvaluatorResults evaluateForMonitoring(
      Application application,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType,
      ClientScanType clientScanType)
      throws IOException
  {
    return doEvaluate(application, scanId, stage, scanTriggerType, null, null, true /* forMonitoring */,
        clientScanType);
  }

  /*
    please note:  this method was renamed from 'evaluate' so as to facilitate instrumentation by a java agent
    that captures metrics during a load test;  the agent cannot instrument overloaded methods
   */
  private ScanPolicyEvaluatorResults doEvaluate(
      final Application application,
      final String scanId,
      final Stage stage,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      boolean forMonitoring,
      ClientScanType clientScanType) throws IOException
  {
    log.debug(
        "Evaluating policies for application ID {}, scan ID {}, stage {}, scan trigger type {}, for monitoring {}.",
        application.getId(), scanId, stage.getStageTypeId(), scanTriggerType.name(), forMonitoring);

    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException(stage.getStageTypeId());
    }

    /*
      Re-evaluations are being disallowed in response to https://sonatype.atlassian.net/browse/CLM-25312.

      When re-evaluating policy against the non-latest scan, we can not persist data to the database. This would
      overwrite newer, more relevant state about the application (what violations are present, which have been closed,
      meantime to remediate, etc..)

      Unfortunately the report page is no longer purely driven by the Report files. It incorporates information
      from the policy_violation tables. By generating new policy violations during a re-evaluation, but not persisting
      them to the policy_violation table, we would leave the report in state where the policy_violations have no ids.
      This prevents portions of the report from rendering, such as violation details.
     */
    throwErrorIfReEvaluatingAnOldScan(application.getId(), scanId, stage.getStageTypeId());

    AuditData.get().setStageId(stage.getStageTypeId());

    ReportComponentData reportComponentData = reportComponentService.fetchReportAndComponents(application, scanId);

    return performPolicyEvaluation(application, scanId, stage, scanTriggerType, clientUserAgent, clientInstanceId,
        forMonitoring, clientScanType, reportComponentData);
  }

  private void fetchAndPersistRemediationRecommendations(
      final String scanId, final Stage stage, final List<Component> components, final String appId)
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
    boolean enableActions = productLicense.hasFeature(LicensedFeature.ENFORCEMENT);
    if (!enableActions) {
      log.debug("Ignoring actions in policy alerts for application {} and scan {} in stage {}, "
          + "license does not support enforcement.", applicationId, scanId, stageTypeId);
    }
    return policyAlertUtil.createPolicyAlerts(components, violations, stageTypeId, applicationId, forMonitoring,
        enableActions);
  }

  private void updateReportFiles(
      File reportFile,
      ScanPolicyEvaluatorResults scanPolicyEvaluatorResults,
      Stage stage,
      Map<String, String> policyIdPolicyOwnerIdMap,
      boolean forMonitoring,
      List<Component> components) throws IOException
  {
    List<PolicyAlert> alerts = createPolicyAlerts(scanPolicyEvaluatorResults.evaluation.getApplicationId(),
        scanPolicyEvaluatorResults.evaluation.getScanId(), stage.getStageTypeId(), forMonitoring,
        components, scanPolicyEvaluatorResults.activeViolations);
    Report.putEntry(reportFile, POLICY_ALERTS_FILENAME, JsonUtils.generate(JsonUtils.aaData(alerts)));

    PolicyThreats policyThreats = PolicyThreatsAdapter.createPolicyThreats(scanPolicyEvaluatorResults.allViolations,
        stage.getStageTypeId(),
        policyIdPolicyOwnerIdMap);
    Report.putEntry(reportFile, POLICY_THREATS_FILENAME, JsonUtils.generate(policyThreats));

    updateDataJson(reportFile, policyThreats);
  }

  private void updateDataJson(File reportFile, PolicyThreats policyThreats) throws IOException {
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

    ObjectNode data = JsonUtils.parse(Report.getEntry(reportFile, Report.DATA_JSON_FILENAME).buf);
    Report.fill(data.putArray("policyCounts"), policyCounts);
    data.put("policyComponentCount", policyComponentCount);
    data.put("grandfatheredPolicyViolationCount", legacyViolationCount);
    data.put("legacyViolationCount", legacyViolationCount);
    Report.putEntry(reportFile, Report.DATA_JSON_FILENAME, JsonUtils.generate(data));
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
      File reportFile,
      ClientScanType clientScanType) throws IOException
  {
    String appId = app.getId();
    long start = System.currentTimeMillis();
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolations(app);
         TransactionContext tx = policyEvaluationDAO.createTransactionContext()) {
      clusterLock.lock();
      tx.begin();
      boolean isLegacyViolationEnabled = legacyViolationService.isLegacyViolationEnabled(tx, app.getId());
      // Persist the policy evaluation
      boolean isReevaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(tx, appId, scanId) != null;
      AuditData.get().setIsReevaluation(isReevaluation);
      PolicyEvaluation policyEvaluation = new PolicyEvaluation(appId, stage.getStageTypeId(), scanId, isReevaluation,
          forMonitoring, currentUser.getUsernameOrSystem(), scanTriggerType, clientScanType);
      policyEvaluation.setCommitHash(extractCommitHash(Report.getEntry(reportFile, Report.DATA_JSON_FILENAME)));
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

      AutoPolicyWaiver autoPolicyWaiver = getApplicableAutoPolicyWaiver(appId);

      // Convert the policy alerts into policy violations
      List<PolicyAlert> allPolicyAlerts = new ArrayList<>();
      allPolicyAlerts.addAll(policyResults.getActiveAlerts());
      allPolicyAlerts.addAll(policyResults.getWaivedAlerts());
      Map<String, String> policyIdPolicyOwnerIdMap = new HashMap();
      for (PolicyAlert policyAlert : allPolicyAlerts) {
        PolicyFact policyFact = policyAlert.getTrigger();
        Policy policy = policyDAO.getByIdNotNull(policyFact.getPolicyId());
        policyIdPolicyOwnerIdMap.put(policy.getId(), policy.getOwnerId());
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

          if (autoPolicyWaiver != null && policyViolation.isActive()) {
            boolean violationShouldBeAutoWaived = evaluateAutoPolicyWaiver(
                appId,
                policyViolation,
                autoPolicyWaiver,
                stage.getStageTypeId());
            if (violationShouldBeAutoWaived) {
              policyViolation.setWaiveTime(policyEvaluation.getTime());
              policyViolation.setAutoPolicyWaiverId(autoPolicyWaiver.getId());

              Owner owner = ownerDAO.getById(autoPolicyWaiver.getOwnerId());

              if (owner != null) {
                autoPolicyWaiverTelemetryMetrics.collect(autoPolicyWaiver, owner.getType(),
                    AutoPolicyWaiverAction.APPLY, policyViolation);
              }
              else {
                autoPolicyWaiverTelemetryMetrics.collect(autoPolicyWaiver, null,
                    AutoPolicyWaiverAction.APPLY, policyViolation);
              }
            }
          }

          results.allViolations.add(policyViolation);
        }
      }

      if (!stage.getStageTypeId().equals(Stage.ID_COMPLIANCE)) {
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
          policyViolationDAO.update(tx, oldPolicyViolation);
          policyViolationLogger.add(PolicyViolationLogEvent.FIX, oldPolicyViolation);

          List<Component> found = findComponentsByComponentIdentifierElseVersionless(components,
              oldPolicyViolation.getComponentIdentifier());
          telemetryCollector.addTelemetryForFixedViolation(oldPolicyViolation, found);
          results.fixedViolations.add(oldPolicyViolation);
        }
        // Existing policy violations.
        List<PolicyViolation> existing = new ArrayList<>();
        for (Map.Entry<PolicyViolation, PolicyViolation> entry : policyViolationDiff.getSame().entrySet()) {
          PolicyViolation oldPolicyViolation = entry.getKey();
          existing.add(oldPolicyViolation);
          PolicyViolation newPolicyViolation = entry.getValue();

          if (!newPolicyViolation.isWaived() && oldPolicyViolation.isWaived()) {
            // The policy violation was un-waived or un-auto-waived
            oldPolicyViolation.setFixTime(policyEvaluation.getTime());
            policyViolationDAO.update(tx, oldPolicyViolation);
            if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
              results.notifiableViolations.add(newPolicyViolation);
            }
            policyViolationDAO.insert(tx, newPolicyViolation);

            if (oldPolicyViolation.isAutoWaived()) {
              policyViolationLogger.add(PolicyViolationLogEvent.UNAUTOWAIVE, newPolicyViolation);
              Component component =
                  findComponentByComponentIdentifier(components, oldPolicyViolation.getComponentIdentifier());
              telemetryCollector.addTelemetryForUnAutoWaivedViolation(
                  newPolicyViolation,
                  component,
                  oldPolicyViolation.getPolicyWaiverId());
            }
            else {
              policyViolationLogger.add(PolicyViolationLogEvent.UNWAIVE, newPolicyViolation);
              Component component =
                  findComponentByComponentIdentifier(components, oldPolicyViolation.getComponentIdentifier());
              telemetryCollector.addTelemetryForUnwaivedViolation(
                  newPolicyViolation,
                  component,
                  oldPolicyViolation.getPolicyWaiverId());
            }
          }
          else {
            if (isNotifiable(oldPolicyViolation, newPolicyViolation, forMonitoring, isReevaluation)) {
              results.notifiableViolations.add(oldPolicyViolation);
            }
            oldPolicyViolation.setThreatCategory(newPolicyViolation.getThreatCategory());
            oldPolicyViolation.setActionTypeId(newPolicyViolation.getActionTypeId());
            oldPolicyViolation.setConstraintFactsJson(newPolicyViolation.getConstraintFactsJson());
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
                    newPolicyViolation,
                    component,
                    oldPolicyViolation.getPolicyWaiverId());

                policyViolationLogger.add(PolicyViolationLogEvent.AUTOWAIVE, newPolicyViolation);
                telemetryCollector.addTelemetryForAutoWaivedViolation(newPolicyViolation, component);
                results.waivedViolations.remove(oldPolicyViolation);
                results.autoWaivedViolations.add(newPolicyViolation);
                results.allViolations.remove(oldPolicyViolation);
                results.allViolations.add(newPolicyViolation);
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
                telemetryCollector.addTelemetryForUnAutoWaivedViolation(
                    oldPolicyViolation,
                    component,
                    oldPolicyViolation.getAutoPolicyWaiverId());

                policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, oldPolicyViolation);
                telemetryCollector.addTelemetryForWaivedViolation(oldPolicyViolation, component);

                results.autoWaivedViolations.remove(oldPolicyViolation);
                results.waivedViolations.add(newPolicyViolation);
                results.allViolations.remove(oldPolicyViolation);
                results.allViolations.add(newPolicyViolation);
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

            if (oldPolicyViolation.isLegacyViolation() && !oldPolicyViolation.isLegacyViolationApplied()) {
              oldPolicyViolation.setLegacyViolationApplied(true);
              telemetryCollector.addTelemetryForLegacyViolation(oldPolicyViolation, component);
            }
            else if (!oldPolicyViolation.isLegacyViolation() &&
                oldPolicyViolation.isLegacyViolationApplied()) {
              // legacy violation was revoked
              oldPolicyViolation.setLegacyViolationApplied(false);
            }
            policyViolationDAO.update(tx, oldPolicyViolation);

            if (keepExistingViolation) {
              results.allViolations.remove(newPolicyViolation);
              results.allViolations.add(oldPolicyViolation);
            }
          }
        }
        logPolicyViolations(existing, "previously seen");
        persistApplicationComponents(tx, appId, stage, policyEvaluation.getTime(), components);
      }

      tx.commit();

      policyViolationLogger.log();

      results.activeViolations = filterActivePolicyViolations(results.allViolations);

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

      updateReportFiles(reportFile, results, stage, policyIdPolicyOwnerIdMap, forMonitoring, components);

      return results;
    }
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

    for (ConstraintFact constraintFact : newPolicyViolation.getConstraintFacts()) {
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        String conditionTypeId = conditionFact.getConditionTypeId();
        telemetryCollector.addTelemetryForConditionTypeViolation(newPolicyViolation, conditionTypeId, found);
      }
    }
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
          .forEach(policyViolation -> policyViolation.setLegacyViolationTime(policyEvaluationTime));
    }
    else {
      List<PolicyViolation> legacyViolations =
          policyViolationDAO.getUnfixedLegacyViolationByApplicationId(tx, app.getId());
      if (!legacyViolations.isEmpty()) {
        PolicyViolationDiff<PolicyViolation> policyViolationDiff = PolicyViolationDigester
            .digestPolicyViolations(legacyViolations, policyViolations);
        policyViolationDiff.getSame().forEach( //
            (legacyViolation, newPolicyViolation) -> newPolicyViolation
                .setLegacyViolationTime(legacyViolation.getLegacyViolationTime()));
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
    File previousScanFile = work.getScanFile(appId, previousScanId);
    try {
      if (Files.deleteIfExists(previousScanFile.toPath())) {
        log.debug("Deleted obsolete scan file for app ID {} and stage {}: {}.", appId, stage,
            previousScanFile.getAbsolutePath());
      }
    }
    catch (Exception e) {
      log.error("Cannot delete previous scan file for app ID {} and stage {}: {}. Cause: {}", appId, stage,
          previousScanFile.getAbsolutePath(), e.getMessage(), e);
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

    // Add new app->component associations for the specified stage
    for (Component component : components) {
      if (component.getHash() == null) {
        continue;
      }

      ApplicationComponent applicationComponent = new ApplicationComponent(appId, stage.getStageTypeId(), time,
          component.getHash(), component.getComponentIdentifier(), component.getMatchState().getId(), component
          .getIdentificationSource().getId(), component.isProprietary(), component.getPathnames());
      applicationComponentDAO.insert(tx, applicationComponent);
      for (com.sonatype.clm.dto.model.component.AggregateFile aggregateFile : component.getAggregateFiles()) {
        aggregateFileDAO
            .insert(tx, new AggregateFile(applicationComponent.getId(), aggregateFile.hash, aggregateFile.pathnames));
      }

      Set<String> effectiveLicenseIds = ComponentDetailsLoader.calculateEffectiveLicenses(
          component.getDeclaredMultiLicenseIds(),
          component.getObservedMultiLicenseIds(),
          component.getLicenseOverrideIds());
      for (String effectiveLicenseId : effectiveLicenseIds) {
        ApplicationComponentLicense applicationComponentLicense =
            new ApplicationComponentLicense(applicationComponent.getId(), effectiveLicenseId);
        applicationComponentLicenseDAO.insert(tx, applicationComponentLicense);
      }
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

  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      List<PolicyViolation> policyViolations,
      boolean createAlerts)
  {
    return createPolicyEvaluationResult(policyEvaluation, Collections.emptyList(), policyViolations, createAlerts);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(
      PolicyEvaluation policyEvaluation,
      List<Component> components,
      List<PolicyViolation> policyViolations,
      boolean createAlerts)
  {
    PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    calculateCounters(policyEvaluationResult, policyViolations);
    if (createAlerts) {
      List<PolicyViolation> activePolicyViolations = filterActivePolicyViolations(policyViolations);
      List<PolicyAlert> policyAlerts = createPolicyAlerts(policyEvaluation.getApplicationId(),
          policyEvaluation.getScanId(), policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring(),
          components, activePolicyViolations);
      policyEvaluationResult.setAlerts(policyAlerts);
    }
    policyEvaluationResult.setTotalComponentCount(getTotalComponentCount(policyEvaluation));
    return policyEvaluationResult;
  }

  private List<PolicyViolation> filterActivePolicyViolations(List<PolicyViolation> policyViolations) {
    return policyViolations.stream().filter(PolicyViolation::isActive).collect(toList());
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
    String commitHash = policyEvaluation.getCommitHash();

    PolicyEvaluationResult policyEvaluationResult =
        createPolicyEvaluationResult(policyEvaluation, components, activeViolations, true);

    List<PolicyAlert> waivedAlerts = createPolicyAlerts(policyEvaluation.getApplicationId(),
        policyEvaluation.getScanId(), policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring(),
        components, waivedViolations);

    List<PolicyAlert> fixedAlerts = createPolicyAlerts(policyEvaluation.getApplicationId(),
        policyEvaluation.getScanId(), policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring(),
        components, fixedViolations);

    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult, commitHash, application);
    policyAlertEventService
        .postEvent(policyEvaluation, policyEvaluationResult, commitHash, application, waivedAlerts, fixedAlerts);
  }

  private int getTotalComponentCount(PolicyEvaluation policyEvaluation) {
    try {
      File reportFile = reportService.getReport(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
      ReportEntry summaryEntry = Report.getEntry(reportFile, Report.SUMMARY_JSON_FILENAME);
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
      String applicationId,
      String stageId,
      ScanTriggerType scanTriggerType,
      Collection<Component> components,
      String clientUserAgent,
      String clientInstanceId)
  {
    Map<String, Long> componentCounts = getComponentCounts(components);
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        applicationId, stageId, scanTriggerType, clientUserAgent, clientInstanceId,
        Collections.singletonMap("component_counts", componentCounts));
    telemetrySender.send(telemetryData);
  }

  private Map<String, Long> getComponentCounts(Collection<Component> components) {
    return components.stream().map(Component::getComponentIdentifier)
        .map(componentIdentifier -> componentIdentifier == null ? UNKNOWN : componentIdentifier.getFormat())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }

  /**
   * @since 1.50
   */
  void sendLegacyViolationTelemetryData(String applicationId, List<PolicyViolation> policyViolations) {
    TelemetryData telemetryData = new TelemetryData(
        TelemetryPurpose.APPLICATION_EVALUATION_LEGACY_VIOLATION_COUNTS);
    telemetryData.setAttributes(getLegacyViolationCountsAttributes(applicationId, policyViolations));
    telemetrySender.send(telemetryData);
  }

  /**
   * @since 1.50
   */
  private Map<String, Object> getLegacyViolationCountsAttributes(
      String applicationId,
      List<PolicyViolation> policyViolations)
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
        String.valueOf(legacyViolationService.isLegacyViolationEnabled(applicationId)));
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

  private String extractCommitHash(ReportEntry dataReportEntry) throws IOException {
    if (null == dataReportEntry) {
      return null;
    }
    return JsonUtils.parse(dataReportEntry.buf).path("commitHash").asText(null);
  }

  private void logPolicyViolations(List<PolicyViolation> policyViolations, String policyProperty) {
    if (policyViolations.isEmpty()) {
      log.debug("No {} policies violated.", policyProperty);
    }
    else {
      Map<String, Long> policyViolationCount = policyViolations.stream()
          .collect(Collectors.groupingBy(AbstractPolicyViolation::getPolicyName, Collectors.counting()));
      String stringified = policyViolationCount.keySet().stream().sorted()
          .map(s -> s + "(" + policyViolationCount.get(s).toString() + ")").collect(Collectors.joining(", "));
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
        .filter(c -> c.getComponentIdentifier().createAlternativeVersion(null)
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
      final String stageTypeId
  )
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
      Application application,
      String scanId,
      Stage stage,
      ScanTriggerType scanTriggerType,
      String clientUserAgent,
      String clientInstanceId,
      boolean forMonitoring,
      ClientScanType clientScanType,
      ReportComponentData reportComponentData) throws IOException
  {

    sendEvaluationTelemetry(application.getId(), stage.getStageTypeId(), scanTriggerType,
        reportComponentData.components,
        clientUserAgent, clientInstanceId);

    String appId = application.getId();
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(appId);
    PolicyResults policyResults =
        componentPolicyEvaluator.evaluate(appId, stage, policies, reportComponentData.components,
            forMonitoring);

    PolicyViolationTelemetryCollector telemetryCollector =
        new PolicyViolationTelemetryCollector(policyWaiverDAO, telemetryUtils, sourceControlUtils.isScmEnabled(appId));

    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        processPolicyResults(application, scanId, stage, scanTriggerType, policies, forMonitoring, policyResults,
            reportComponentData.components, telemetryCollector, reportComponentData.reportFile, clientScanType);

    telemetrySender.send(telemetryCollector.getTelemetryData());

    sendLegacyViolationTelemetryData(application.getId(), scanPolicyEvaluatorResults.allViolations);

    final Set<Feature> features = featuresService.getFeatures();
    if (features.contains(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS)) {
      fetchAndPersistRemediationRecommendations(scanId, stage, reportComponentData.components, appId);
    }

    postEvents(scanPolicyEvaluatorResults, application, reportComponentData.components);
    thirdPartySbomMetadataDAO.makeSbomActiveIfExist(scanId);
    return scanPolicyEvaluatorResults;
  }

  private boolean evaluateAutoPolicyWaiver(
      String appId,
      PolicyViolation policyViolation,
      AutoPolicyWaiver autoPolicyWaiver,
      String stageId)
  {

    boolean hasPolicyWaiver = hasPolicyWaiver(policyViolation);
    boolean threatLevel = doesViolationMeetThreatLevelCriteria(policyViolation, autoPolicyWaiver);
    boolean hasPathForward = doesViolationHavePathForward(appId, policyViolation, autoPolicyWaiver, stageId);

    return threatLevel && !hasPolicyWaiver && !hasPathForward;
  }

  private boolean hasPolicyWaiver(PolicyViolation policyViolation) {
    return policyViolation.getPolicyWaiverId() != null && policyViolation.getWaiveTime() != null;
  }

  private boolean doesViolationMeetThreatLevelCriteria(
      PolicyViolation policyViolation, AutoPolicyWaiver autoPolicyWaiver)
  {
    return policyViolation.getThreatLevel() <= autoPolicyWaiver.getThreatLevel();
  }

  private boolean doesViolationHavePathForward(
      String appId,
      PolicyViolation policyViolation,
      AutoPolicyWaiver autoPolicyWaiver,
      String stageId)
  {
    Boolean hasPathForward = autoPolicyWaiver.hasPathForward();
    if (hasPathForward == null || !hasPathForward || policyViolation.getComponentIdentifier() == null) {
      // Auto waiver does not have path forward enabled. No need to evaluate this condition.
      return false;
    }
    componentInfoService.setToolName("ci");
    ComponentVersionInfoDTO dto = componentInfoService.getComponentVersionInfoNoAuth(
        OwnerType.APPLICATION,
        appId,
        policyViolation.getComponentIdentifier(),
        stageId,
        null,
        null,
        null,
        SourceEndpoint.SCAN_POLICY_EVALUATOR
    );

    if (dto == null || dto.remediation == null) {
      // if either of these are null, we can't be sure if there is a path forward; don't auto waive in that case
      return true;
    }
    return !dto.remediation.versionChanges.isEmpty() || dto.remediation.suggestedVersionChange != null;
  }

  private AutoPolicyWaiver getApplicableAutoPolicyWaiver(String applicationId) {
    final Set<Feature> features = featuresService.getFeatures();
    if (features.contains(SystemConfigurationPropertyFeature.AUTO_WAIVERS)) {
      List<String> ownerIds = ownerDAO.getOwnerIds(applicationId);
      for (String id : ownerIds) {
        List<AutoPolicyWaiver> autoPolicyWaivers = autoPolicyWaiverDAO.getByOwnerId(id);
        if (!autoPolicyWaivers.isEmpty()) {
          return autoPolicyWaivers.get(0);
        }
      }
    }
    return null;
  }
}
