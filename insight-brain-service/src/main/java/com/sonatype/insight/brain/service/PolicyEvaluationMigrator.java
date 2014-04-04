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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.json.store.JsonStore;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
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

  private static final Function<ConditionFact, String> CONDITION_TYPE_ID_TRANSFORMER = new Function<ConditionFact, String>()
  {
    @Override
    public String apply(final ConditionFact input) {
      return input.getConditionTypeId().toLowerCase(Locale.ENGLISH);
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

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  @Inject
  public PolicyEvaluationMigrator(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  public void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.debug("Migrating policy evaluation data...");

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
        ownerCount++;
        File auditDir = insightWork.getAuditDir(application.getId());
        JsonStore auditStore = JsonUtils.fileStore(auditDir);

        for (StageType stageType : StageTypes.getAll()) {
          String stageId = stageType.getId();
          List<PolicyEvaluation> policyEvaluations = allByStage(stageId, auditStore);

          if (!policyEvaluations.isEmpty()) {
            log.debug("Migrating {} policy evaluations for Application named: {} and Stage: {}",
                policyEvaluations.size(), application.getName(), stageId);
          }

          //all original policy evaluations
          Collection<PolicyEvaluation> primaryEvaluations = Collections2.filter(policyEvaluations, IS_PRIMARY_EVALUATION);
          savePolicyEvaluations(em, application.getId(), stageId, primaryEvaluations);
          evaluationCount += primaryEvaluations.size();

          //and the rest
          policyEvaluations.removeAll(primaryEvaluations);

          //only the most recent re-evaluation for each scan
          ImmutableListMultimap<String, PolicyEvaluation> groupedByScanId = Multimaps
              .index(policyEvaluations, GROUP_BY_SCAN);
          for (Entry<String, Collection<PolicyEvaluation>> scanIdToPolicyEvaluation : groupedByScanId.asMap().entrySet()) {
            List<PolicyEvaluation> reevaluations = Lists.newArrayList(Iterables.limit(scanIdToPolicyEvaluation.getValue(), 1));
            savePolicyEvaluations(em, application.getId(), stageId, reevaluations);
            evaluationCount += reevaluations.size();
          }
        }
      }

      em.getTransaction().commit();
    }
    finally {
      ApplicationDAO.close(em);
    }

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();

    log.info("Migrated policy evaluation data for {} applications and a total of {} policy evaluations in {} ms.",
        ownerCount, evaluationCount, System.currentTimeMillis() - start);
  }

  private void savePolicyEvaluations(final EntityManager em, final String applicationId, final String stageId,
      final Collection<PolicyEvaluation> policyEvaluations) throws IOException {
    for (PolicyEvaluation policyEvaluation : policyEvaluations) {
      policyEvaluation.setStageTypeId(stageId);
      policyEvaluation.setApplicationId(applicationId);
      policyEvaluationDAO.insert(em, policyEvaluation);
      log.debug("Migrated: {}", policyEvaluation);

      String scanId = policyEvaluation.getScanId();
      List<PolicyAlert> policyAlerts = findPolicyAlerts(applicationId, scanId,
          determinePolicyAlertsFileName(policyEvaluation));
      savePolicyAlerts(em, policyEvaluation.getId(), policyAlerts);

      /* check for existence of monitoring results */
      if (policyEvaluationDAO.getLastMonitoringByApplicationIdAndScanId(em, applicationId, scanId) == null) {
        //could only have one of these per scan on the file system
        List<PolicyAlert> monitoringAlerts = findPolicyAlerts(applicationId, scanId, MONITOR_POLICY_ALERTS_FILE);
        if (!monitoringAlerts.isEmpty()) {
          long time = findReportEntry(applicationId, scanId, MONITOR_POLICY_ALERTS_FILE).time;
          PolicyEvaluation monitoringEvaluation = new PolicyEvaluation(applicationId, stageId, scanId, true, true);
          monitoringEvaluation.setTime(new Date(time));
          policyEvaluationDAO.insert(em, monitoringEvaluation);
          log.debug("Migrated policy monitoring evaluation: {}", monitoringEvaluation);
          savePolicyAlerts(em, monitoringEvaluation.getId(), monitoringAlerts);
        }
      }
    }
  }

  private void savePolicyAlerts(final EntityManager em, final String policyEvaluationId, final List<PolicyAlert> policyAlerts) {
    for (PolicyAlert policyAlert : policyAlerts) {
      PolicyFact policyFact = policyAlert.getTrigger();
      Policy policy = policyDAO.getById(em, policyFact.getPolicyId());

      for (ComponentFact componentFact : policyFact.getComponentFacts()) {
        // threat category is decided by policy if it is available, otherwise by the subset of stored policy data
        PolicyThreatCategory threatCategory = policy != null ? policy.getThreatCategory() :
            determineCategory(componentFact.getConstraintFacts());

        PolicyViolation policyViolation = new PolicyViolation(policyEvaluationId, policyFact.getPolicyId(),
            policyFact.getPolicyName(), policyFact.getThreatLevel(), threatCategory, componentFact.getHash(),
            componentFact.getGroupId(), componentFact.getArtifactId(), componentFact.getVersion(),
            componentFact.getConstraintFacts());

        policyViolationDAO.insert(em, policyViolation);
      }
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
   * Figure out the threatCategory based on the stored data
   */
  private PolicyThreatCategory determineCategory(List<ConstraintFact> constraintFacts) {
    Set<String> conditionTypeIds = Sets.newHashSet();
    for (ConstraintFact constraintFact : constraintFacts) {
      conditionTypeIds.addAll(Lists.transform(constraintFact.getConditionFacts(), CONDITION_TYPE_ID_TRANSFORMER));
    }
    return Policy.determineCategory(conditionTypeIds);
  }

  private String determineStageEvaluationFilename(String stageId) {
    return "policy-evaluations-" + stageId + ".json";
  }

  private String stageId(JsonNode stampedLogEntry) {
    return stampedLogEntry.get("data").get("stage").get("stageTypeId").asText();
  }
}
