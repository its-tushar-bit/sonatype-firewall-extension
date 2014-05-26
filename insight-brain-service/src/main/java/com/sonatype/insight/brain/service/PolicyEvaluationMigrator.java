/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.policy.NewestPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDigester;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.trending.TrendingReportCache;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates the policy evaluations and associated violations from the file system to the ODS database.
 *
 * @since 1.11
 */
@Named
public class PolicyEvaluationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationMigrator.class);

  private static final String MARKER_FILE_NAME = "policyevaluations-migrated";

  private static final String MONITOR_POLICY_ALERTS_FILE = "monitorpolicyalerts.json";

  private static final Function<ConditionFact, PolicyThreatCategory> CONDITION_FACT_TRANSFORMER = new Function<ConditionFact, PolicyThreatCategory>()
  {
    @Override
    public PolicyThreatCategory apply(final ConditionFact conditionFact) {
      ConditionType<?> conditionType = ConditionTypes.getById(conditionFact.getConditionTypeId());
      if (conditionType == null) {
        return PolicyThreatCategory.OTHER;
      }
      return conditionType.getThreatCategory();
    }
  };

  private static final Predicate<PolicyEvaluation> IS_PRIMARY_EVALUATION = new Predicate<PolicyEvaluation>()
  {
    @Override
    public boolean apply(final PolicyEvaluation input) {
      return !input.isForMonitoring() && !input.isReevaluation();
    }
  };

  private static final Function<PolicyEvaluation, String> GROUP_BY_SCAN = new Function<PolicyEvaluation, String>()
  {
    @Override
    public String apply(final PolicyEvaluation input) {
      return input.getScanId();
    }
  };

  private final InsightWork insightWork;

  private final TrendingReportCache trendingReportCache;

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private final NewestPolicyViolationDAO newestPolicyViolationDAO = new NewestPolicyViolationDAO();

  private final ApplicationComponentDAO applicationComponentDAO = new ApplicationComponentDAO();

  @Inject
  public PolicyEvaluationMigrator(InsightWork insightWork, TrendingReportCache trendingReportCache) {
    this.insightWork = insightWork;
    this.trendingReportCache = trendingReportCache;
  }

  public void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.info("Migrating policy evaluation data...");

    File markerFile = new File(insightWork.getWorkDir(), MARKER_FILE_NAME);
    if (markerFile.exists()) {
      log.debug("Policy evaluations already migrated.");
      return;
    }

    int ownerCount = 0;
    int evaluationCount = 0;

    EntityManager em = appDAO.createEntityManager();

    try {
      em.getTransaction().begin();

      for (Application application : appDAO.getAll(em)) {
        long appStart = System.currentTimeMillis();

        Set<String> monitoringScans = new HashSet<>();
        List<PolicyEvaluation> policyEvaluationsCache = new ArrayList<>();
        Map<String, List<PolicyViolation>> policyViolationsByEvaluationCache = new LinkedHashMap<>();
        Map<String, List<Component>> componentsByScanCache = new LinkedHashMap<>();
        
        ownerCount++;
        File auditDir = insightWork.getAuditDir(application.getId());
        JsonStore auditStore = JsonUtils.fileStore(auditDir);

        for (StageType stageType : StageTypes.getAll()) {
          String stageId = stageType.getId();
          List<PolicyEvaluation> policyEvaluations = allByStage(stageId, auditStore);

          if (policyEvaluations.isEmpty()) {
            continue;
          }

          log.debug("Migrating {} policy evaluations for Application named: {} and Stage: {}",
              policyEvaluations.size(), application.getName(), stageId);

          // Migrate all primary policy evaluations
          Collection<PolicyEvaluation> primaryEvaluations = Collections2.filter(policyEvaluations,
              IS_PRIMARY_EVALUATION);
          savePolicyEvaluations(em, application, stageId, primaryEvaluations, monitoringScans, policyEvaluationsCache,
              policyViolationsByEvaluationCache, componentsByScanCache);
          evaluationCount += primaryEvaluations.size();

          // Migrate only the most recent re-evaluation for each scan
          policyEvaluations.removeAll(primaryEvaluations);
          ImmutableListMultimap<String, PolicyEvaluation> groupedByScanId = Multimaps.index(policyEvaluations,
              GROUP_BY_SCAN);
          for (Entry<String, Collection<PolicyEvaluation>> scanIdToPolicyEvaluation : groupedByScanId.asMap().entrySet()) {
            List<PolicyEvaluation> reevaluations = Lists.newArrayList(Iterables.limit(scanIdToPolicyEvaluation.getValue(), 1));
            savePolicyEvaluations(em, application, stageId, reevaluations, monitoringScans, policyEvaluationsCache,
                policyViolationsByEvaluationCache, componentsByScanCache);
            evaluationCount += reevaluations.size();
          }

          // Migrate the components in use in each application, by stage.
          saveApplicationComponents(em, application.getId(), stageId, componentsByScanCache);
        }
        evaluationCount += monitoringScans.size();

        saveNewestPolicyViolations(em, policyEvaluationsCache, policyViolationsByEvaluationCache);

        log.debug("Migration of policy evaluations for Application named: {} complete in {} ms.", application.getName(),
            System.currentTimeMillis() - appStart);
      }

      em.getTransaction().commit();

      //remove existing trending report cache as API changes will have invalidated its format
      trendingReportCache.purgeCache();
    }
    finally {
      ApplicationDAO.close(em);
    }

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();

    log.info("Migrated policy evaluation data for {} applications and a total of {} policy evaluations in {} ms.",
        ownerCount, evaluationCount, System.currentTimeMillis() - start);
  }

  private void saveNewestPolicyViolations(EntityManager em, List<PolicyEvaluation> policyEvaluations,
      Map<String, List<PolicyViolation>> policyViolationsByEvaluation)
  {
    // Sort policy evaluations by time
    Collections.sort(policyEvaluations, new Comparator<PolicyEvaluation>()
    {
      @Override
      public int compare(PolicyEvaluation e1, PolicyEvaluation e2) {
        return e1.getTime().compareTo(e2.getTime());
      }
    });

    // Calculate "newest" policy violations
    Map<String, String> lastScanIdsByStageId = new LinkedHashMap<>();
    Map<String, List<PolicyViolation>> newestPolicyViolationsByStageId = new LinkedHashMap<>();
    Map<String, PolicyEvaluation> policyEvaluationsById = new LinkedHashMap<>();
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      if (policyEvaluation.isReevaluation()) {
        if (!policyEvaluation.getScanId().equals(lastScanIdsByStageId.get(policyEvaluation.getStageTypeId()))) {
          // Policy re-evaluation for a scan older then the last scan. It should not change "newest" policy violations.
          continue;
        }
      }
      else {
        lastScanIdsByStageId.put(policyEvaluation.getStageTypeId(), policyEvaluation.getScanId());
      }

      List<PolicyViolation> policyViolations = policyViolationsByEvaluation.get(policyEvaluation.getId());
      List<PolicyViolation> newestPolicyViolations = newestPolicyViolationsByStageId.get(policyEvaluation
          .getStageTypeId());
      if (newestPolicyViolations == null) {
        newestPolicyViolationsByStageId.put(policyEvaluation.getStageTypeId(), policyViolations);
      }
      else {
        PolicyViolationDiff diff = PolicyViolationDigester.digestPolicyViolations(policyViolations,
            newestPolicyViolations);
        newestPolicyViolations.addAll(diff.getAppeared());
        newestPolicyViolations.removeAll(diff.getCleared());
      }
      policyEvaluationsById.put(policyEvaluation.getId(), policyEvaluation);
    }

    // Persist "newest" policy violations
    for (Entry<String, List<PolicyViolation>> newestPolicyViolationsEntry : newestPolicyViolationsByStageId.entrySet()) {
      String stageTypeId = newestPolicyViolationsEntry.getKey();
      for (PolicyViolation policyViolation : newestPolicyViolationsEntry.getValue()) {
        PolicyEvaluation policyEvaluation = policyEvaluationsById.get(policyViolation.getPolicyEvaluationId());
        NewestPolicyViolation newestPolicyViolation = new NewestPolicyViolation(policyViolation.getId(),
            policyEvaluation.getApplicationId(), stageTypeId);
        newestPolicyViolationDAO.insert(em, newestPolicyViolation);
      }
    }
  }

  private void savePolicyEvaluations(final EntityManager em, final Application app, final String stageId,
      final Collection<PolicyEvaluation> policyEvaluations, final Set<String> monitoringScans,
      final List<PolicyEvaluation> policyEvaluationsCache,
      final Map<String, List<PolicyViolation>> policyViolationsByEvaluationCache,
      final Map<String, List<Component>> componentsByScanCache) throws IOException
  {
    String applicationId = app.getId();
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      policyEvaluation.setStageTypeId(stageId);
      policyEvaluation.setApplicationId(applicationId);
      policyEvaluationDAO.insert(em, policyEvaluation);
      policyEvaluationsCache.add(policyEvaluation);
      log.trace("Migrated: {}", policyEvaluation);

      String scanId = policyEvaluation.getScanId();
      List<PolicyAlert> policyAlerts = findPolicyAlerts(applicationId, scanId,
          determinePolicyAlertsFileName(policyEvaluation));
      List<Component> components = componentsByScanCache.get(scanId);
      if (components == null) {
        components = loadComponents(app, scanId);
        componentsByScanCache.put(scanId, components);
      }
      Map<String, List<String>> hashToPathnames = loadPathnames(components);
      savePolicyAlerts(em, policyEvaluation, policyAlerts, policyViolationsByEvaluationCache, hashToPathnames);

      /* check for existence of monitoring results */

      if (!monitoringScans.contains(policyEvaluation.getScanId())) {
        //could only have one of these per scan on the file system
        List<PolicyAlert> monitoringAlerts = findPolicyAlerts(applicationId, scanId, MONITOR_POLICY_ALERTS_FILE);
        if (!monitoringAlerts.isEmpty()) {
          long time = findReportEntry(applicationId, scanId, MONITOR_POLICY_ALERTS_FILE).time;
          PolicyEvaluation monitoringEvaluation = new PolicyEvaluation(applicationId, stageId, scanId, true, true);
          monitoringEvaluation.setTime(new Date(time));
          policyEvaluationDAO.insert(em, monitoringEvaluation);
          policyEvaluationsCache.add(monitoringEvaluation);
          log.trace("Migrated policy monitoring evaluation: {}", monitoringEvaluation);
          savePolicyAlerts(em, monitoringEvaluation, monitoringAlerts, policyViolationsByEvaluationCache,
              hashToPathnames);
          monitoringScans.add(monitoringEvaluation.getScanId());
        }
      }
    }
  }

  private void savePolicyAlerts(final EntityManager em, final PolicyEvaluation evaluation,
      final List<PolicyAlert> policyAlerts, final Map<String, List<PolicyViolation>> policyViolationsByEvaluationCache,
      final Map<String, List<String>> hashToPathnames)
  {
    List<PolicyViolation> policyViolations = new ArrayList<>();
    policyViolationsByEvaluationCache.put(evaluation.getId(), policyViolations);
    for (PolicyAlert policyAlert : policyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      Policy policy = policyDAO.getById(em, policyFact.getPolicyId());

      for (ComponentFact componentFact : policyFact.getComponentFacts()) {
        // threat category is decided by policy if it is available, otherwise by the subset of stored policy data
        PolicyThreatCategory threatCategory = policy != null ? policy.getThreatCategory()
            : determinePolicyThreatCategory(componentFact.getConstraintFacts());

        List<String> pathnames = hashToPathnames.get(componentFact.getHash());

        PolicyViolation policyViolation = new PolicyViolation(evaluation, policyFact.getPolicyId(),
            policyFact.getPolicyName(), policyFact.getThreatLevel(), threatCategory, componentFact.getHash(),
            componentFact.getGroupId(), componentFact.getArtifactId(), componentFact.getVersion(),
            componentFact.getConstraintFacts(), pathnames);
        List<String> notifications = new ArrayList<>();
        for (Action action : policyAlert.getActions()) {
          if (Action.ID_NOTIFY.equals(action.getActionTypeId())) {
            notifications.add(action.getTarget());
          }
          else {
            policyViolation.setActionTypeId(action.getActionTypeId());
          }
        }
        policyViolation.setNotifications(notifications);
        policyViolationDAO.insert(em, policyViolation);
        policyViolations.add(policyViolation);
      }
    }
  }

  private void saveApplicationComponents(EntityManager em, String appId, String stageTypeId,
      Map<String, List<Component>> componentsByScanCache)
  {
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(em, appId, stageTypeId);
    if (policyEvaluation == null) {
      return;
    }

    List<Component> components = componentsByScanCache.get(policyEvaluation.getScanId());
    if (components == null || components.isEmpty()) {
      return;
    }

    for (Component component : components) {
      if (component.getHash() == null) {
        continue;
      }
      ApplicationComponent applicationComponent = new ApplicationComponent(appId, stageTypeId,
          policyEvaluation.getTime(), component.getHash(), component.getGroupId(), component.getArtifactId(),
          component.getVersion(), component.getMatchState().getId(), component.getIdentificationSource().getId(),
          component.isProprietary(), component.getPathnames());
      applicationComponentDAO.insert(em, applicationComponent);
    }
  }

  /**
   * Results can be present in different files depending on what kind of policyEvaluation it is.
   */
  private String determinePolicyAlertsFileName(final PolicyEvaluation policyEvaluation) {
    if (policyEvaluation.isReevaluation()) {
      return "policyalerts.json";
    }
    return "primarypolicyalerts.json";
  }

  private List<PolicyAlert> findPolicyAlerts(final String appId, final String scanId, final String fileName) throws IOException {
    final ReportEntry reportEntry = findReportEntry(appId, scanId, fileName);
    if (reportEntry != null) {
      return Arrays.asList(JsonUtils.parse(reportEntry.buf, PolicyAlert[].class));
    }
    return Collections.emptyList();
  }

  private ReportEntry findReportEntry(final String appId, final String scanId, final String fileName)
      throws IOException {
    final File reportFile = ReportResource.getReport(insightWork, appId, scanId, true);
    if (reportFile != null) {
      return Report.getEntry(reportFile, fileName);
    }
    return null;
  }

  /**
   * Returns all evaluations for the given stage. Returns empty list if no such evaluations.
   */
  private List<PolicyEvaluation> allByStage(final String stageId, final JsonStore auditStore) throws IOException {
    final ContainerNode<?> auditContainer = auditStore.history(null, determineStageEvaluationFilename(stageId));
    if (auditContainer != null) {
      JsonNode auditData = auditContainer.get("aaData");
      ArrayList<PolicyEvaluation> result = new ArrayList<>();
      for (JsonNode audit : auditData) {
        result.add(JsonUtils.asPojo(audit, PolicyEvaluation.class));
      }
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * Figure out the policy threat category based on the stored constraint facts.
   */
  private PolicyThreatCategory determinePolicyThreatCategory(List<ConstraintFact> constraintFacts) {
    SortedSet<PolicyThreatCategory> policyThreatCategories = new TreeSet<>();
    for (ConstraintFact constraintFact : constraintFacts) {
      policyThreatCategories.addAll(Lists.transform(constraintFact.getConditionFacts(), CONDITION_FACT_TRANSFORMER));
    }
    return PolicyThreatCategory.getCategory(policyThreatCategories);
  }

  private String determineStageEvaluationFilename(String stageId) {
    return "policy-evaluations-" + stageId + ".json";
  }

  private List<Component> loadComponents(Application app, String scanId) {
    try {
      File reportFile = insightWork.getReportFile(app.getId(), scanId);
      if (reportFile == null || !reportFile.exists()) {
        throw new RuntimeException("Report does not exist.");
      }

      ReportEntry bomReportEntry = Report.getEntry(reportFile, "bom.json");
      if (bomReportEntry == null) {
        throw new RuntimeException("bom.json does not exist.");
      }

      return new ComponentDAO().getAll(app, null, null, bomReportEntry.buf);
    }
    catch (Exception e) {
      log.warn(
          "An error occured while attempting to load component data for application {} and scan {}. Migrated evaluations will miss component details. Details: {}",
          app.getName(), scanId, e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  private Map<String, List<String>> loadPathnames(List<Component> components) {
    Map<String, List<String>> mappedComponents = new HashMap<String, List<String>>();

    if (components == null || components.isEmpty()) {
      return mappedComponents;
    }

    for (Component component : components) {
      if (!StringUtils.isBlank(component.getHash())) {
        mappedComponents.put(component.getHash(), component.getPathnames());
      }
    }

    return mappedComponents;
  }
}
