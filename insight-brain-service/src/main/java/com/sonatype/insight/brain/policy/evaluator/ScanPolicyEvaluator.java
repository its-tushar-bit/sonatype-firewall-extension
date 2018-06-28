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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.webhook.ApplicationEvaluationEventService;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

@Named
public class ScanPolicyEvaluator
{
  private static final Logger log = LoggerFactory.getLogger(ScanPolicyEvaluator.class);

  public static final String POLICY_ALERTS_FILENAME = "policyalerts.json";

  public static final String POLICY_THREATS_FILENAME = "policythreats.json";

  private static final ConcurrentMap<String, String> PERSISTENCE_LOCKS_BY_APPID = new ConcurrentHashMap<>();

  private static final String UNKNOWN = "unknown";

  private final InsightWork work;

  private final ReportService reportService;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private PolicyDAO policyDAO = new PolicyDAO();

  private PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private final PolicyThreatsAdapter policyThreatsAdapter;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  private final ApplicationEvaluationEventService applicationEvaluationEventService;

  private final TelemetrySender telemetrySender;

  @Inject
  public ScanPolicyEvaluator(final InsightWork insightWork,
                             final ReportService reportService,
                             final PolicyThreatsAdapter policyThreatsAdapter,
                             final ComponentPolicyEvaluator componentPolicyEvaluator,
                             final ApplicationEvaluationEventService applicationEvaluationEventService,
                             final TelemetrySender telemetrySender)
  {
    this.work = insightWork;
    this.reportService = reportService;
    this.policyThreatsAdapter = policyThreatsAdapter;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
    this.applicationEvaluationEventService = applicationEvaluationEventService;
    this.telemetrySender = telemetrySender;
  }

  public ScanPolicyEvaluatorResults evaluate(final String applicationPublicId, final String scanId, final Stage stage)
      throws IOException
  {
    return evaluate(applicationPublicId, scanId, stage, false /* forMonitoring */);
  }

  public ScanPolicyEvaluatorResults evaluateForMonitoring(String applicationPublicId, String scanId, Stage stage)
      throws IOException
  {
    return evaluate(applicationPublicId, scanId, stage, true /* forMonitoring */);
  }

  private ScanPolicyEvaluatorResults evaluate(final String applicationPublicId,
                                              final String scanId,
                                              final Stage stage,
                                              boolean forMonitoring)
      throws IOException
  {
    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException("Invalid stage id=" + stage.getStageTypeId());
    }

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final File reportFile = reportService.fetchReport(work, appId, scanId, true);

    final ReportEntry licenseReportEntry = Report.getEntry(reportFile, "licenses.json");
    final ReportEntry securityReportEntry = Report.getEntry(reportFile, "security.json");
    final ReportEntry bomReportEntry = Report.getEntry(reportFile, "bom.json");

    if (bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null) {
      throw new BadRequestException("Unable to evaluate policy, the scan " + scanId + " could not be processed.");
    }

    // Load data about components
    final List<Component> components = new ComponentDAO().getAll(application, licenseReportEntry.buf,
        securityReportEntry.buf, bomReportEntry.buf);

    sendApplicationStageComponentCounts(application.getId(), stage.getStageTypeId(), components);

    // Evaluate the policies
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(appId, stage, components, forMonitoring);

    // Save the policy evaluation and violations
    ScanPolicyEvaluatorResults scanPolicyEvaluatorResults = persistPolicyResults(appId, scanId, stage, forMonitoring,
        policyResults,
        components);

    createReportFiles(reportFile, scanPolicyEvaluatorResults, stage, forMonitoring);
    ReportService.flushReportChanges(appId, scanId); // ensure policy count is recalculated on fetch

    postEvaluateEvent(scanPolicyEvaluatorResults.evaluation, scanPolicyEvaluatorResults.activeViolations);

    return scanPolicyEvaluatorResults;
  }

  private void createReportFiles(File reportFile,
                                 ScanPolicyEvaluatorResults scanPolicyEvaluatorResults,
                                 Stage stage,
                                 boolean forMonitoring)
      throws IOException
  {
    List<PolicyAlert> alerts = PolicyAlertUtil.createPolicyAlerts(scanPolicyEvaluatorResults.activeViolations,
        stage.getStageTypeId(), forMonitoring);
    Report.putEntry(reportFile, POLICY_ALERTS_FILENAME, JsonUtils.generate(JsonUtils.aaData(alerts)));

    PolicyThreats policyThreats = policyThreatsAdapter.createPolicyThreats(scanPolicyEvaluatorResults.allViolations);
    Report.putEntry(reportFile, POLICY_THREATS_FILENAME, JsonUtils.generate(policyThreats));
  }

  private ScanPolicyEvaluatorResults persistPolicyResults(String appId,
                                                          String scanId,
                                                          Stage stage,
                                                          boolean forMonitoring,
                                                          PolicyResults policyResults,
                                                          List<Component> components)
  {
    Object lock = getPersistenceLock(appId);
    synchronized (lock) {
      long start = System.currentTimeMillis();
      PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
      try (TransactionContext tx = policyEvaluationDAO.createTransactionContext()) {
        tx.begin();

        // Persist the policy evaluation
        boolean isReevaluation = (policyEvaluationDAO.getLastByApplicationIdAndScanId(tx, appId, scanId) != null);
        PolicyEvaluation policyEvaluation = new PolicyEvaluation(appId, stage.getStageTypeId(), scanId, isReevaluation,
            forMonitoring);
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
        results.notifiableViolations = isReevaluation ? null : new ArrayList<>();

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

        setGrandfatheredPolicyViolations(tx, appId, results.allViolations);
        results.activeViolations = results.allViolations.stream().filter(PolicyViolation::isActive).collect(toList());

        // Persist the PolicyViolations and ApplicationComponents only if there isn't a more recent
        // primary policy evaluation, since any reevaluation (even for monitoring) may be for an older scan.
        if (isForLatestScan) {
          List<PolicyViolation> oldPolicyViolations = policyViolationDAO.getUnfixedByApplicationIdAndStageId(tx, appId,
              stage.getStageTypeId());
          PolicyViolationDiff<PolicyViolation> policyViolationDiff = PolicyViolationDigester
              .digestPolicyViolations(oldPolicyViolations, results.allViolations);

          // New policy violations.
          for (PolicyViolation newPolicyViolation : policyViolationDiff.getAppeared()) {
            if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
              results.notifiableViolations.add(newPolicyViolation);
            }
            policyViolationDAO.insert(tx, newPolicyViolation);
          }
          // Fixed policy violations.
          for (PolicyViolation oldPolicyViolation : policyViolationDiff.getCleared()) {
            oldPolicyViolation.setFixTime(policyEvaluation.getTime());
            policyViolationDAO.update(tx, oldPolicyViolation);
          }
          // Existing policy violations.
          for (Map.Entry<PolicyViolation, PolicyViolation> entry : policyViolationDiff.getSame().entrySet()) {
            PolicyViolation oldPolicyViolation = entry.getKey();
            PolicyViolation newPolicyViolation = entry.getValue();
            if (!newPolicyViolation.isWaived() && oldPolicyViolation.isWaived()) {
              // The policy violation was un-waived.
              oldPolicyViolation.setFixTime(policyEvaluation.getTime());
              policyViolationDAO.update(tx, oldPolicyViolation);
              if (isNotifiable(null, newPolicyViolation, forMonitoring, isReevaluation)) {
                results.notifiableViolations.add(newPolicyViolation);
              }
              policyViolationDAO.insert(tx, newPolicyViolation);
            }
            else {
              if (isNotifiable(oldPolicyViolation, newPolicyViolation, forMonitoring, isReevaluation)) {
                results.notifiableViolations.add(oldPolicyViolation);
              }
              oldPolicyViolation.setThreatCategory(newPolicyViolation.getThreatCategory());
              oldPolicyViolation.setActionTypeId(newPolicyViolation.getActionTypeId());
              oldPolicyViolation.setConstraintFactsJson(newPolicyViolation.getConstraintFactsJson());
              oldPolicyViolation.setFilename(newPolicyViolation.getFilename());
              if (!oldPolicyViolation.isWaived()) {
                oldPolicyViolation.setWaiveTime(newPolicyViolation.getWaiveTime());
                oldPolicyViolation.setPolicyWaiverId(newPolicyViolation.getPolicyWaiverId());
                oldPolicyViolation.setPolicyWaiverComment(newPolicyViolation.getPolicyWaiverComment());
              }
              policyViolationDAO.update(tx, oldPolicyViolation);
            }
          }

          persistApplicationComponents(tx, appId, stage, policyEvaluation.getTime(), components);
        }

        tx.commit();

        if (!isReevaluation && lastPrimaryPolicyEvaluation != null) {
          String previousScanId = lastPrimaryPolicyEvaluation.getScanId();
          deletePreviousScanFile(appId, stage, previousScanId);
        }

        log.debug(
            "Persisted policy evaluation results (active={}, waived={}) for application {} from stage {} in {} ms",
            policyResults.getActiveAlerts().size(), policyResults.getWaivedAlerts().size(), appId,
            stage.getStageTypeId(), System.currentTimeMillis() - start);
        return results;
      }
    }
  }

  /**
   * Updates the grandfathered policy violations based on the existing grandfathered violations (across all stages).
   */
  private void setGrandfatheredPolicyViolations(TransactionContext tx,
                                                String appId,
                                                List<PolicyViolation> policyViolations)
  {
    List<PolicyViolation> grandfatheredPolicyViolations = policyViolationDAO.getUnfixedGrandfatheredByApplicationId(tx,
        appId);
    PolicyViolationDiff<PolicyViolation> policyViolationDiff = PolicyViolationDigester
        .digestPolicyViolations(grandfatheredPolicyViolations, policyViolations);
    policyViolationDiff.getSame().forEach( //
        (grandfatheredPolicyViolation, newPolicyViolation) -> newPolicyViolation
            .setGrandfatherTime(grandfatheredPolicyViolation.getGrandfatherTime()));
  }

  private String getFilename(ComponentFact componentFact) {
    return new ComponentDisplayFilename().addPathnames(componentFact.getPathnames()).getFilename().orElse(null);
  }

  private boolean isNotifiable(PolicyViolation oldPolicyViolation,
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
      Files.delete(previousScanFile.toPath());
      log.debug("Deleted obsolete scan file for app ID {} and stage {}: {}.", appId, stage,
          previousScanFile.getAbsolutePath());
    }
    catch (Exception e) {
      log.error("Cannot delete previous scan file for app ID {} and stage {}: {}. Cause: {}", appId, stage,
          previousScanFile.getAbsolutePath(), e.getMessage(), e);
    }
  }

  private void persistApplicationComponents(TransactionContext tx,
                                            String appId,
                                            Stage stage,
                                            Date time,
                                            List<Component> components)
  {
    ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();

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
    }
  }

  private void calculateCounters(PolicyEvaluationResult policyEvaluationResult, List<PolicyViolation> policyViolations)
  {
    final Map<String, Integer> componentThreatLevels = new HashMap<>();
    for (PolicyViolation policyViolation : policyViolations) {
      final int policyThreatLevel = policyViolation.getThreatLevel();
      final String id = policyViolation.getHash();
      final Integer level = componentThreatLevels.get(id);
      if (level == null || level < policyThreatLevel) {
        componentThreatLevels.put(id, policyThreatLevel);
      }
    }

    int criticalCount = 0, severeCount = 0, moderateCount = 0;
    for (final int level : componentThreatLevels.values()) {
      if (level >= 8) {
        criticalCount++;
      }
      else if (level >= 4) {
        severeCount++;
      }
      else if (level >= 2) {
        moderateCount++;
      }
    }

    policyEvaluationResult.setAffectedComponentCount(criticalCount + severeCount + moderateCount);
    policyEvaluationResult.setCriticalComponentCount(criticalCount);
    policyEvaluationResult.setSevereComponentCount(severeCount);
    policyEvaluationResult.setModerateComponentCount(moderateCount);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(PolicyEvaluation policyEvaluation, boolean createAlerts) {
    List<PolicyViolation> policyViolations = policyViolationDAO
        .getActiveByApplicationIdAndStageId(policyEvaluation.getApplicationId(), policyEvaluation.getStageTypeId());
    return createPolicyEvaluationResult(policyEvaluation, policyViolations, createAlerts);
  }

  public PolicyEvaluationResult createPolicyEvaluationResult(PolicyEvaluation policyEvaluation,
                                                             List<PolicyViolation> policyViolations,
                                                             boolean createAlerts)
  {
    PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    calculateCounters(policyEvaluationResult, policyViolations);
    if (createAlerts) {
      List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyViolations,
          policyEvaluation.getStageTypeId(), policyEvaluation.isForMonitoring());
      policyEvaluationResult.setAlerts(policyAlerts);
    }
    return policyEvaluationResult;
  }

  /**
   * @since 1.25.0
   */
  private void postEvaluateEvent(PolicyEvaluation policyEvaluation, List<PolicyViolation> policyViolations) {
    PolicyEvaluationResult policyEvaluationResult = createPolicyEvaluationResult(policyEvaluation, policyViolations,
        true);
    applicationEvaluationEventService.postEvent(policyEvaluation, policyEvaluationResult);
  }

  private static Object getPersistenceLock(String appId) {
    Object lock = PERSISTENCE_LOCKS_BY_APPID.get(appId);
    if (lock == null) {
      lock = PERSISTENCE_LOCKS_BY_APPID.putIfAbsent(appId, appId);
      if (lock == null) {
        lock = appId;
      }
    }
    return lock;
  }

  @VisibleForTesting
  void sendApplicationStageComponentCounts(String applicationId, String stageId, Collection<Component> components) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS);
    telemetryData.setAttributes(getApplicationStageComponentCountsAttributes(applicationId, stageId, components));
    telemetrySender.send(telemetryData);
  }

  private Map<String, Object> getApplicationStageComponentCountsAttributes(String applicationId,
                                                                           String stageId,
                                                                           Collection<Component> components)
  {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", HdsClientAnalytics.obfuscate(applicationId));
    attributes.put("stage_id", stageId);
    Map<String, Long> componentCounts = getComponentCounts(components);
    for (String format : componentCounts.keySet()) {
      attributes
          .put("number_of_" + format.replace("-", "") + "_components", String.valueOf(componentCounts.get(format)));
    }
    attributes.put("number_of_components", String.valueOf(components.size()));
    return attributes;
  }

  private Map<String, Long> getComponentCounts(Collection<Component> components) {
    return components.stream().map(Component::getComponentIdentifier)
        .map(componentIdentifier -> componentIdentifier == null ? UNKNOWN : componentIdentifier.getFormat())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }
}
