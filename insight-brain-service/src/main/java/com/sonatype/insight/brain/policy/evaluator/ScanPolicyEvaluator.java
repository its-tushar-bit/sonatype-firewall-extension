/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.FirstOccurrencePolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.WaivedPolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

@Named
public class ScanPolicyEvaluator
{
  public static final String POLICY_ALERTS_FILENAME = "policyalerts.json";

  public static final String POLICY_THREATS_FILENAME = "policythreats.json";

  private static final ConcurrentMap<String, String> PERSISTENCE_LOCKS_BY_APPID = new ConcurrentHashMap<>();

  private final InsightWork work;

  private final ReportService reportService;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private PolicyDAO policyDAO = new PolicyDAO();

  private PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();

  private final PolicyThreatsAdapter policyThreatsAdapter;

  private final ComponentPolicyEvaluator componentPolicyEvaluator;

  @Inject
  public ScanPolicyEvaluator(final InsightWork insightWork,
                             final ReportService reportService,
                             final PolicyThreatsAdapter policyThreatsAdapter,
                             final ComponentPolicyEvaluator componentPolicyEvaluator)
  {
    this.work = insightWork;
    this.reportService = reportService;
    this.policyThreatsAdapter = policyThreatsAdapter;
    this.componentPolicyEvaluator = componentPolicyEvaluator;
  }

  public PolicyEvaluation evaluate(final String applicationPublicId, final String scanId, final Stage stage)
      throws IOException
  {
    return evaluate(applicationPublicId, scanId, stage, false /* forMonitoring */);
  }

  public PolicyEvaluation evaluateForMonitoring(String applicationPublicId, String scanId, Stage stage)
      throws IOException
  {
    return evaluate(applicationPublicId, scanId, stage, true /* forMonitoring */);
  }

  private PolicyEvaluation evaluate(final String applicationPublicId,
                                    final String scanId,
                                    final Stage stage,
                                    boolean forMonitoring) throws IOException
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

    // Evaluate the policies
    PolicyResults policyResults = componentPolicyEvaluator.evaluate(appId, stage, components, forMonitoring);

    // Save the policy evaluation and violations
    PolicyEvaluation policyEvaluation = persistPolicyResults(appId, scanId, stage, forMonitoring, policyResults,
        components);
    final List<PolicyAlert> alerts = policyResults.getActiveAlerts();

    byte[] alertsFileContent = JsonUtils.generate(JsonUtils.aaData(alerts));
    if (!forMonitoring) {
      Report.putEntry(reportFile, POLICY_ALERTS_FILENAME, alertsFileContent);
    }

    Report.putEntry(reportFile, POLICY_THREATS_FILENAME, JsonUtils.generate(policyThreatsAdapter
        .createPolicyThreats(policyViolationDAO.getByEvaluationId(policyEvaluation.getId()))));

    ReportService.flushReportChanges(appId, scanId); // ensure policy count is recalculated on fetch

    return policyEvaluation;
  }

  private PolicyEvaluation persistPolicyResults(String appId,
                                                String scanId,
                                                Stage stage,
                                                boolean forMonitoring,
                                                PolicyResults policyResults,
                                                List<Component> components)
  {
    Object lock = getPersistenceLock(appId);
    synchronized (lock) {
      PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
      try (TransactionContext tx = policyEvaluationDAO.createTransactionContext()) {
        tx.begin();

        // Persist the policy evaluation
        boolean isReevaluation = (policyEvaluationDAO.getLastByApplicationIdAndScanId(tx, appId, scanId) != null);
        PolicyEvaluation policyEvaluation = new PolicyEvaluation(appId, stage.getStageTypeId(), scanId, isReevaluation,
            forMonitoring);
        boolean isForLatestScan = true;
        if (isReevaluation) {
          PolicyEvaluation lastPrimaryPolicyEvaluation = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(
              tx, appId, stage.getStageTypeId());
          isForLatestScan = lastPrimaryPolicyEvaluation.getScanId().equals(scanId);
          policyEvaluation.setForObsoleteScan(!isForLatestScan);
        }
        policyEvaluationDAO.insert(tx, policyEvaluation);

        // Persist policy violations
        List<PolicyViolation> newPolicyViolations = new ArrayList<>();
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
                componentFact.getPathnames());
            for (Action action : policyAlert.getActions()) {
              // Don't save notification data into policy violations here because at this point we don't really know if
              // the notifications will be sent or not.
              // The notifier component will take care of saving the notification data.
              if (!Action.ID_NOTIFY.equals(action.getActionTypeId())) {
                policyViolation.setActionTypeId(action.getActionTypeId());
                break;
              }
            }
            PolicyWaiver policyWaiver = policyResults.getPolicyWaiver(componentFact);
            policyViolation.setWaived(policyWaiver != null);
            policyViolationDAO.insert(tx, policyViolation);
            if (policyWaiver != null) {
              WaivedPolicyViolation waivedPolicyViolation = new WaivedPolicyViolation(policyViolation.getId(),
                  policyWaiver.getId(), policyWaiver.getComment());
              waivedPolicyViolationDAO.insert(tx, waivedPolicyViolation);
            }
            else {
              newPolicyViolations.add(policyViolation);
            }
          }
        }

        // Persist the FirstOccurrencePolicyViolations and ApplicationComponents only if there isn't a more recent
        // primary policy evaluation, since any reevaluation (even for monitoring) may be for an older scan.
        if (isForLatestScan) {
          // Calculate a diff between the current policy violations and the previous first occurrence policy violations
          List<PolicyViolation> oldPolicyViolations = policyViolationDAO
              .getFirstOccurrenceByApplicationIdAndStageTypeId(tx, appId, stage.getStageTypeId());
          PolicyViolationDiff policyViolationDiff = PolicyViolationDigester.digestPolicyViolations(oldPolicyViolations,
              newPolicyViolations);
          FirstOccurrencePolicyViolationDAO firstOccurrencePolicyViolationDAO = new FirstOccurrencePolicyViolationDAO();
          // Delete cleared first occurrence policy violations
          for (PolicyViolation clearedPolicyViolation : policyViolationDiff.getCleared()) {
            FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = firstOccurrencePolicyViolationDAO.getById(
                tx, clearedPolicyViolation.getId());
            firstOccurrencePolicyViolationDAO.delete(tx, firstOccurrencePolicyViolation);
          }
          // Add new first occurrence policy violations
          for (PolicyViolation appearedPolicyViolation : policyViolationDiff.getAppeared()) {
            FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = new FirstOccurrencePolicyViolation(
                appearedPolicyViolation.getId(), appId, stage.getStageTypeId());
            firstOccurrencePolicyViolationDAO.insert(tx, firstOccurrencePolicyViolation);
          }

          persistApplicationComponents(tx, appId, stage, policyEvaluation.getTime(), components);
        }

        tx.commit();

        return policyEvaluation;
      }
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

  private void calculateCounters(PolicyEvaluationResult policyEvaluationResult) {
    final Map<String, Integer> componentThreatLevels = new HashMap<>();
    for (final PolicyAlert alert : policyEvaluationResult.getAlerts()) {
      final PolicyFact trigger = alert.getTrigger();
      final int policyThreatLevel = trigger.getThreatLevel();
      for (final ComponentFact component : trigger.getComponentFacts()) {
        final String id = component.getComponentId();
        final Integer level = componentThreatLevels.get(id);
        if (level == null || level < policyThreatLevel) {
          componentThreatLevels.put(id, policyThreatLevel);
        }
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

  public PolicyEvaluationResult createPolicyEvaluationResult(PolicyEvaluation policyEvaluation) {
    List<PolicyAlert> policyAlerts = PolicyAlertUtil.createPolicyAlerts(policyEvaluation);
    PolicyEvaluationResult policyEvaluationResult = new PolicyEvaluationResult();
    policyEvaluationResult.setAlerts(policyAlerts);
    calculateCounters(policyEvaluationResult);
    policyEvaluationResult.setReevaluation(policyEvaluation.isReevaluation());
    return policyEvaluationResult;
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
}
