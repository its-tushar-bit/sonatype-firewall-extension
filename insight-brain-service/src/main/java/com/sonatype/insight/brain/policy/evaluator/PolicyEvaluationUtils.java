/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.NewestPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.ActionTypes;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

@Named
public class PolicyEvaluationUtils
{
  public static final String POLICY_ALERTS_FILENAME = "policyalerts.json";

  public static final String POLICY_THREATS_FILENAME = "policythreats.json";

  private static final ConcurrentMap<String, String> PERSISTENCE_LOCKS_BY_APPID = new ConcurrentHashMap<>();

  private final InsightWork work;

  private final ReportDownloader reportDownloader;

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Inject
  public PolicyEvaluationUtils(final InsightWork insightWork, final ReportDownloader reportDownloader) {
    this.work = insightWork;
    this.reportDownloader = reportDownloader;
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

  private PolicyEvaluation evaluate(final String applicationPublicId, final String scanId, final Stage stage,
      boolean forMonitoring) throws IOException
  {
    if (!Stage.isValidStageTypeId(stage.getStageTypeId())) {
      throw new InvalidStageException("Invalid stage id=" + stage.getStageTypeId());
    }

    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    String appId = application.getId();

    final File reportFile = ReportResource.fetchReport(reportDownloader, work, appId, scanId, true, false);

    final PolicyDAO policyDAO = new PolicyDAO();

    final ReportEntry licenseReportEntry = Report.getEntry(reportFile, "licenses.json");
    final ReportEntry securityReportEntry = Report.getEntry(reportFile, "security.json");
    final ReportEntry bomReportEntry = Report.getEntry(reportFile, "bom.json");

    if (bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null) {
      throw new BadRequestException("Unable to evaluate policy, the scan " + scanId + " could not be processed");
    }

    // Load data about components
    final List<Component> components = new ComponentDAO().getAll(application, licenseReportEntry.buf,
        securityReportEntry.buf, bomReportEntry.buf);

    // Evaluate the policies
    PolicyResults policyResults = new PolicyEvaluator().evaluate(appId, stage, policyDAO, components, forMonitoring);

    // Save the policy evaluation and violations
    PolicyEvaluation policyEvaluation = persistPolicyResults(appId, scanId, stage, forMonitoring, policyResults);
    final List<PolicyAlert> alerts = policyResults.getActiveAlerts();

    byte[] alertsFileContent = JsonUtils.generate(JsonUtils.aaData(alerts));
    if (!forMonitoring) {
      Report.putEntry(reportFile, POLICY_ALERTS_FILENAME, alertsFileContent);
    }

    Report.putEntry(reportFile, POLICY_THREATS_FILENAME, JsonUtils.generate(toPolicyThreats(policyResults)));

    ReportResource.flushReportChanges(appId, scanId); // ensure policy count is recalculated on fetch

    return policyEvaluation;
  }

  private PolicyEvaluation persistPolicyResults(String appId, String scanId, Stage stage, boolean forMonitoring,
      PolicyResults policyResults)
  {
    Object lock = getPersistenceLock(appId);
    synchronized (lock) {
      PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();
      EntityManager em = policyEvaluationDAO.createEntityManager();
      try {
        em.getTransaction().begin();
  
        boolean isReevaluation = (policyEvaluationDAO.getLastByApplicationIdAndScanId(em, appId, scanId) != null);
  
        // Persist the policy evaluation
        PolicyEvaluation policyEvaluation = new PolicyEvaluation(appId, stage.getStageTypeId(), scanId, isReevaluation,
            forMonitoring);
        policyEvaluationDAO.insert(em, policyEvaluation);
  
        // Persist the policy violations
        List<PolicyViolation> newPolicyViolations = new ArrayList<>();
        PolicyDAO policyDAO = new PolicyDAO();
        PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
        for (PolicyAlert policyAlert : policyResults.getActiveAlerts()) {
          PolicyFact policyFact = policyAlert.getTrigger();
          Policy policy = policyDAO.getByIdNotNull(policyFact.getPolicyId());
          PolicyThreatCategory threatCategory = policy.getThreatCategory();
          for (ComponentFact componentFact : policyFact.getComponentFacts()) {
            PolicyViolation policyViolation = new PolicyViolation(policyEvaluation.getId(), policy.getId(),
                policy.getName(), policyFact.getThreatLevel(), threatCategory, componentFact.getHash(),
                componentFact.getGroupId(), componentFact.getArtifactId(), componentFact.getVersion(),
                componentFact.getConstraintFacts(), componentFact.getPathnames());
            policyViolationDAO.insert(em, policyViolation);
            newPolicyViolations.add(policyViolation);
          }
        }

        // Persist the "newest" policy violations only if there isn't a more recent primary policy evaluation, since any
        // reevaluation (even for monitoring) may be for an older scan.
        boolean persistNewestPolicyViolations = true;
        if (isReevaluation) {
          PolicyEvaluation lastPrimaryPolicyEvaluation = policyEvaluationDAO.getLastPrimaryByApplicationIdAndStageId(
              em, appId, stage.getStageTypeId());
          persistNewestPolicyViolations = lastPrimaryPolicyEvaluation.getScanId().equals(scanId);
        }
        if (persistNewestPolicyViolations) {
          // Calculate a diff between the current policy violations and the previous "newest" policy violations
          List<PolicyViolation> oldPolicyViolations = policyViolationDAO.getNewestByApplicationId(em, appId);
          PolicyViolationDiff policyViolationDiff = PolicyViolationDigester.digestPolicyViolations(newPolicyViolations,
              oldPolicyViolations);
          NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();
          // Delete cleared "newest" policy violations
          for (PolicyViolation clearedPolicyViolation : policyViolationDiff.getCleared()) {
            NewestPolicyViolation newestPolicyViolation = newestPolicyViolationDAO.getById(em,
                clearedPolicyViolation.getId());
            newestPolicyViolationDAO.delete(em, newestPolicyViolation);
          }
          // Add new "newest" policy violations
          for (PolicyViolation appearedPolicyViolation : policyViolationDiff.getAppeared()) {
            NewestPolicyViolation newestPolicyViolation = new NewestPolicyViolation(appearedPolicyViolation.getId(),
                appId, stage.getStageTypeId());
            newestPolicyViolationDAO.insert(em, newestPolicyViolation);
          }
        }
  
        em.getTransaction().commit();
  
        return policyEvaluation;
      }
      finally {
        PolicyEvaluationDAO.close(em);
      }
    }
  }

  public void calculateCounters(PolicyEvaluationResult policyEvaluationResult) {
    final Map<String, Integer> componentThreatLevels = new HashMap<String, Integer>();
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

    policyEvaluationResult.setAffectedComponentCount(componentThreatLevels.size());
    policyEvaluationResult.setCriticalComponentCount(criticalCount);
    policyEvaluationResult.setSevereComponentCount(severeCount);
    policyEvaluationResult.setModerateComponentCount(moderateCount);
  }

  PolicyThreats toPolicyThreats(final PolicyResults policyResults) {
    final Map<String, PolicyThreats.Component> components = new LinkedHashMap<>();
    processAlerts(components, policyResults.getActiveAlerts(), false);
    processAlerts(components, policyResults.getWaivedAlerts(), true);
    for (PolicyThreats.Component component : components.values()) {
      if (component.policyThreatLevel < 0) {
        component.policyThreatLevel = 0;
        component.policyName = "None";
      }
    }
    PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.version = 1;
    policyThreats.aaData = new ArrayList<>(components.values());
    return policyThreats;
  }

  private void processAlerts(Map<String, PolicyThreats.Component> components, List<PolicyAlert> alerts, boolean waived)
  {
    for (final PolicyAlert alert : alerts) {
      final PolicyFact trigger = alert.getTrigger();
      final int threatLevel = trigger.getThreatLevel();
      for (final ComponentFact componentFact : trigger.getComponentFacts()) {
        final String id = componentFact.getComponentId();
        PolicyThreats.Component component = components.get(id);
        if (component == null) {
          component = new PolicyThreats.Component();
          component.hash = componentFact.getHash();
          component.groupId = componentFact.getGroupId();
          component.artifactId = componentFact.getArtifactId();
          component.version = componentFact.getVersion();
          component.policyThreatLevel = -1;
          components.put(id, component);
        }
        PolicyThreats.PolicyViolation violation = toPolicyViolation(alert, componentFact);
        if (!waived) {
          component.activeViolations.add(violation);
          if (threatLevel > component.policyThreatLevel) {
            component.policyId = trigger.getPolicyId();
            component.policyName = trigger.getPolicyName();
            component.policyThreatLevel = threatLevel;
          }
        }
        else {
          component.waivedViolations.add(violation);
        }
      }
    }
  }

  private PolicyThreats.PolicyViolation toPolicyViolation(PolicyAlert alert, ComponentFact componentFact) {
    PolicyThreats.PolicyViolation violation = new PolicyThreats.PolicyViolation();
    violation.policyId = alert.getTrigger().getPolicyId();
    violation.policyName = alert.getTrigger().getPolicyName();
    violation.policyThreatLevel = alert.getTrigger().getThreatLevel();
    for (Action action : alert.getActions()) {
      PolicyThreats.PolicyAction act = new PolicyThreats.PolicyAction();
      act.actionType = action.getActionTypeId();
      act.actionSummary = ActionTypes.getById(action.getActionTypeId()).getSummary();
      violation.actions.add(act);
    }
    for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
      PolicyThreats.PolicyConstraint constraint = new PolicyThreats.PolicyConstraint();
      constraint.constraintId = constraintFact.getConstraintId();
      constraint.constraintName = constraintFact.getConstraintName();
      constraint.constraintOperator = constraintFact.getOperatorName();
      for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
        PolicyThreats.PolicyCondition condition = new PolicyThreats.PolicyCondition();
        condition.conditionType = conditionFact.getConditionTypeId();
        condition.conditionSummary = conditionFact.getSummary();
        condition.conditionReason = conditionFact.getReason();
        constraint.conditions.add(condition);
      }
      violation.constraints.add(constraint);
    }
    return violation;
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
