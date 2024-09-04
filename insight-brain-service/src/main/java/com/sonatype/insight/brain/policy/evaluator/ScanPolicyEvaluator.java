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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.development.prioritization.DevelopmentPrioritizationRemediationService;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.AggregateFile;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AbstractPolicyViolation;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.conditions.ComponentCategoryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.DependencyTypeConditionType;
import com.sonatype.insight.brain.model.policy.conditions.HygieneRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityCategoryConditionType;
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

  public static final Set<String> TELEMETRY_CONDITION_TYPES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(
          HygieneRatingConditionType.ID,
          IntegrityRatingConditionType.ID,
          ComponentCategoryConditionType.ID,
          DependencyTypeConditionType.ID,
          SecurityVulnerabilityCategoryConditionType.ID
      )));

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

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ClusterLockManager clusterLockManager;

  private final PolicyAlertUtil policyAlertUtil;

  private final TelemetryUtils telemetryUtils;

  private final DevelopmentPrioritizationRemediationService developmentPrioritizationRemediationService;

  private final FeaturesService featuresService;

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
      final ComponentLoaderFactory componentLoaderFactory,
      final ClusterLockManager clusterLockManager,
      final PolicyAlertUtil policyAlertUtil,
      final TelemetryUtils telemetryUtils,
      final DevelopmentPrioritizationRemediationService developmentPrioritizationRemediationService,
      final FeaturesService featuresService)
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
    this.componentLoaderFactory = componentLoaderFactory;
    this.clusterLockManager = clusterLockManager;
    this.policyAlertUtil = policyAlertUtil;
    this.telemetryUtils = telemetryUtils;
    this.developmentPrioritizationRemediationService = developmentPrioritizationRemediationService;
    this.featuresService = featuresService;
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
    final File reportFile;
    final List<Component> components;
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(application, scanId)) {
      clusterLock.lock();
      reportFile = reportService.fetchReport(application, scanId);

      final ReportEntry licenseReportEntry = Report.getEntry(reportFile, Report.LICENSES_JSON_FILENAME);
      final ReportEntry securityReportEntry = Report.getEntry(reportFile, Report.SECURITY_JSON_FILENAME);
      final ReportEntry bomReportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
      final ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);

      if (bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null
          || dependenciesReportEntry == null) {
        throw new BadRequestException("Unable to evaluate policy, the scan " + scanId + " could not be processed.");
      }

      // Load data about components
      components = componentLoaderFactory.createComponentLoader(application).getAll(licenseReportEntry.buf,
          securityReportEntry.buf, bomReportEntry.buf, dependenciesReportEntry.buf);
    }

    sendEvaluationTelemetry(application.getId(), stage.getStageTypeId(), scanTriggerType, components, clientUserAgent,
        clientInstanceId);

    // Evaluate the policies
    String appId = application.getId();
    List<Policy> policies = policyDAO.getApplicableByOwnerIdWithHierarchy(appId);
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(appId, stage, policies, components, forMonitoring);

    PolicyViolationTelemetryCollector telemetryCollector =
        new PolicyViolationTelemetryCollector(policyWaiverDAO, telemetryUtils, sourceControlUtils.isScmEnabled(appId));

    // Save the policy evaluation and violations
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults =
        processPolicyResults(application, scanId, stage, scanTriggerType, policies, forMonitoring,
            policyResults, components, telemetryCollector, reportFile, clientScanType);

    telemetrySender.send(telemetryCollector.getTelemetryData());

    sendLegacyViolationTelemetryData(application.getId(), scanPolicyEvaluatorResults.allViolations);

    final Set<Feature> features = featuresService.getFeatures();
    if (features.contains(SystemConfigurationPropertyFeature.DEVELOPER_BULK_RECOMMENDATIONS)) {
      fetchAndPersistRemediationRecommendations(scanId, stage, components, appId);
    }

    postEvents(scanPolicyEvaluatorResults, application, components);

    return scanPolicyEvaluatorResults;
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
      boolean forMonitoring,
      List<Component> components) throws IOException
  {
    List<PolicyAlert> alerts = createPolicyAlerts(scanPolicyEvaluatorResults.evaluation.getApplicationId(),
        scanPolicyEvaluatorResults.evaluation.getScanId(), stage.getStageTypeId(), forMonitoring,
        components, scanPolicyEvaluatorResults.activeViolations);
    Report.putEntry(reportFile, POLICY_ALERTS_FILENAME, JsonUtils.generate(JsonUtils.aaData(alerts)));

    PolicyThreats policyThreats = PolicyThreatsAdapter.createPolicyThreats(scanPolicyEvaluatorResults.allViolations);
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

      // Convert the policy alerts into policy violations
      List<PolicyAlert> allPolicyAlerts = new ArrayList<>();
      allPolicyAlerts.addAll(policyResults.getActiveAlerts());
      allPolicyAlerts.addAll(policyResults.getWaivedAlerts());
      for (PolicyAlert policyAlert : allPolicyAlerts) {
        PolicyFact policyFact = policyAlert.getTrigger();
        Policy policy = policyDAO.getByIdNotNull(policyFact.getPolicyId());
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
          results.allViolations.add(policyViolation);
        }
      }

      setLegacyViolations(tx, isLegacyViolationEnabled, app, policies, policyEvaluation.getTime(),
          results.allViolations);

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

          if (newPolicyViolation.isWaived()) {
            policyViolationLogger.add(PolicyViolationLogEvent.WAIVE, newPolicyViolation);
            telemetryCollector.addTelemetryForWaivedViolation(newPolicyViolation, component);
            results.waivedViolations.add(newPolicyViolation);
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
            // The policy violation was un-waived.
            oldPolicyViolation.setFixTime(policyEvaluation.getTime());
            policyViolationDAO.update(tx, oldPolicyViolation);
            if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
              results.notifiableViolations.add(newPolicyViolation);
            }
            policyViolationDAO.insert(tx, newPolicyViolation);

            policyViolationLogger.add(PolicyViolationLogEvent.UNWAIVE, newPolicyViolation);
            Component component =
                findComponentByComponentIdentifier(components, oldPolicyViolation.getComponentIdentifier());
            telemetryCollector.addTelemetryForUnwaivedViolation(
                newPolicyViolation,
                component,
                oldPolicyViolation.getPolicyWaiverId());
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

            if (newPolicyViolation.isWaived() &&
                !newPolicyViolation.getPolicyWaiverId().equals(oldPolicyViolation.getPolicyWaiverId())) {
              // The policy violation was waived, or waiverID has changed (CLM-19768)
              oldPolicyViolation.setPolicyWaiverId(newPolicyViolation.getPolicyWaiverId());
              oldPolicyViolation.setPolicyWaiverComment(newPolicyViolation.getPolicyWaiverComment());

              if (!oldPolicyViolation.isWaived()) {
                oldPolicyViolation.setWaiveTime(newPolicyViolation.getWaiveTime());
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

            // Update the violation in the list of all violation to be the one actually saved to the db.
            results.allViolations.remove(newPolicyViolation);
            results.allViolations.add(oldPolicyViolation);
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

      updateReportFiles(reportFile, results, stage, forMonitoring, components);

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
    for (ConstraintFact constraintFact : newPolicyViolation.getConstraintFacts()) {
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        String conditionTypeId = conditionFact.getConditionTypeId();

        if (TELEMETRY_CONDITION_TYPES.contains(conditionTypeId)) {
          telemetryCollector.addTelemetryForConditionTypeViolation(newPolicyViolation, conditionTypeId, components);

        }
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
}
