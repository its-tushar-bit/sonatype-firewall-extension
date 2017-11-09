/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.WaivedPolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.policy.evaluator.PolicyWaiversMap;
import com.sonatype.insight.brain.policy.evaluator.ScanPolicyEvaluator;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates the waived policy violations from the file system to the ODS database.
 * 
 * @since 1.12
 */
@Named
public class WaivedPolicyViolationMigrator
{
  private static final Logger log = LoggerFactory.getLogger(WaivedPolicyViolationMigrator.class);

  private static final String MARKER_FILE_NAME = "waivedpolicyviolations-migrated";

  private final InsightWork insightWork;

  private final ApplicationDAO appDAO = new ApplicationDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final PolicyEvaluationDAO policyEvaluationDAO = new PolicyEvaluationDAO();

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private final WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();

  private final PolicyWaiverDAO policyWaiverDAO = new PolicyWaiverDAO();

  @Inject
  public WaivedPolicyViolationMigrator(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  public void migrate() throws IOException {
    long start = System.currentTimeMillis();
    log.info("Migrating waived policy violation data...");

    File markerFile = new File(insightWork.getWorkDir(), MARKER_FILE_NAME);
    if (markerFile.exists()) {
      log.debug("Waived policy violations already migrated.");
      return;
    }

    int appCount;
    int policyEvalCount = 0;
    try (TransactionContext tx = appDAO.createTransactionContext()) {
      tx.begin();

      List<Application> applications = appDAO.getAll(tx);
      appCount = applications.size();
      for (Application application : applications) {
        long appStart = System.currentTimeMillis();

        List<PolicyEvaluation> policyEvaluations = policyEvaluationDAO.getByApplicationId(tx, application.getId());
        policyEvalCount += policyEvaluations.size();
        ListMultimap<String, PolicyEvaluation> policyEvaluationsByScanId = ArrayListMultimap.create();
        for (PolicyEvaluation policyEvaluation : policyEvaluations) {
          policyEvaluationsByScanId.put(policyEvaluation.getScanId(), policyEvaluation);
        }

        List<PolicyWaiver> policyWaivers = policyWaiverDAO.getApplicableByOwnerId(application.getId());
        PolicyWaiversMap policyWaiversMap = new PolicyWaiversMap(policyWaivers);

        for (String scanId : policyEvaluationsByScanId.keySet()) {
          List<PolicyEvaluation> policyEvaluationsForThisScan = policyEvaluationsByScanId.get(scanId);
          // We have a policythreats.json for each scan only for the last evaluation, so we can create waived policy
          // violations only for the last evaluation.
          PolicyEvaluation mostRecentPolicyEvaluationForThisScan = getMostRecentPolicyEvaluation(policyEvaluationsForThisScan);
          migratePolicyEvaluation(tx, mostRecentPolicyEvaluationForThisScan, policyWaiversMap);
        }

        log.debug("Migration of waived policy violations for application named {} completed in {} ms.",
            application.getName(), System.currentTimeMillis() - appStart);
      }

      tx.commit();
    }

    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();

    log.info(
        "Migrated waived policy violation data for {} applications and a total of {} policy evaluations in {} ms.",
        appCount, policyEvalCount, System.currentTimeMillis() - start);
  }

  private void migratePolicyEvaluation(TransactionContext tx,
                                       PolicyEvaluation policyEvaluation,
                                       PolicyWaiversMap policyWaiversMap)
  {
    try {
      PolicyThreats policyThreats = loadPolicyThreats(policyEvaluation);
      if (policyThreats == null) {
        return;
      }

      List<PolicyThreats.Component> componentsWithViolations = policyThreats.aaData;
      if (CollectionUtils.isEmpty(componentsWithViolations)) {
        return;
      }

      for (PolicyThreats.Component componentWithViolations : componentsWithViolations) {
        List<PolicyThreats.PolicyViolation> waivedViolations = componentWithViolations.waivedViolations;
        if (CollectionUtils.isEmpty(waivedViolations)) {
          continue;
        }

        for (PolicyThreats.PolicyViolation waivedViolation : waivedViolations) {
          PolicyWaiver policyWaiver = policyWaiversMap.get(waivedViolation.policyId, componentWithViolations.hash);

          if ((policyWaiver != null && policyWaiver.getCreateTime().compareTo(policyEvaluation.getTime()) > 0)) {
            // The policy waiver is newer than the policy evaluation
            policyWaiver = null;
          }
          createWaivedPolicyViolation(tx, policyEvaluation, componentWithViolations, waivedViolation, policyWaiver);
        }
      }
    }
    catch (IOException | RuntimeException e) {
      log.warn("Cannot migrate waived policy violations for scan id {} for application id {} because: {}",
          policyEvaluation.getScanId(), policyEvaluation.getApplicationId(), e.getMessage(), e);
    }
  }

  private PolicyEvaluation getMostRecentPolicyEvaluation(List<PolicyEvaluation> policyEvaluations) {
    if (policyEvaluations.size() == 1) {
      return policyEvaluations.get(0);
    }

    return Collections.max(policyEvaluations, new Comparator<PolicyEvaluation>()
    {
      @Override
      public int compare(PolicyEvaluation policyEvaluation1, PolicyEvaluation policyEvaluation2) {
        return policyEvaluation1.getTime().compareTo(policyEvaluation2.getTime());
      }
    });
  }

  @SuppressWarnings("deprecation")
  private void createWaivedPolicyViolation(TransactionContext tx,
                                           PolicyEvaluation policyEvaluation,
                                           PolicyThreats.Component componentWithViolations,
                                           PolicyThreats.PolicyViolation waivedViolation,
                                           PolicyWaiver policyWaiver)
  {
    List<ConstraintFact> constraintFacts = new ArrayList<>();
    for (PolicyThreats.PolicyConstraint policyConstraint : waivedViolation.constraints) {
      constraintFacts.add(toConstraintFact(policyConstraint));
    }
    PolicyThreatCategory threatCategory = getPolicyThreatCategory(waivedViolation.policyId, constraintFacts);

    ComponentIdentifier componentIdentifier = null;
    if (componentWithViolations.groupId != null) {
      componentIdentifier = ComponentIdentifier.createMavenCoordinates(componentWithViolations.groupId,
          componentWithViolations.artifactId, componentWithViolations.version);
    }
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, waivedViolation.policyId,
        waivedViolation.policyName, waivedViolation.policyThreatLevel, threatCategory, componentWithViolations.hash,
        componentIdentifier, constraintFacts, null);
    policyViolation.setWaived(true);
    policyViolationDAO.insert(tx, policyViolation);

    if (policyWaiver != null) {
      WaivedPolicyViolation waivedPolicyViolation = new WaivedPolicyViolation(policyViolation.getId(),
          policyWaiver.getId(), policyWaiver.getComment());
      waivedPolicyViolationDAO.insert(tx, waivedPolicyViolation);
    }
  }

  private ConstraintFact toConstraintFact(PolicyThreats.PolicyConstraint policyConstraint) {
    ConstraintFact constraintFact = new ConstraintFact(policyConstraint.constraintId, policyConstraint.constraintName,
        policyConstraint.constraintOperator);
    if (policyConstraint.conditions != null) {
      for (PolicyThreats.PolicyCondition policyCondition : policyConstraint.conditions) {
        ConditionFact conditionFact = new ConditionFact(policyCondition.conditionType,
            policyCondition.conditionSummary, policyCondition.conditionReason);
        constraintFact.addConditionFact(conditionFact);
      }
    }
    return constraintFact;
  }

  private PolicyThreatCategory getPolicyThreatCategory(String policyId, List<ConstraintFact> constraintFacts) {
    Policy policy = policyDAO.getById(policyId);
    if (policy != null) {
      return policy.getThreatCategory();
    }

    return PolicyThreatCategoryUtil.determinePolicyThreatCategory(constraintFacts);
  }

  private PolicyThreats loadPolicyThreats(PolicyEvaluation policyEvaluation) throws IOException {
    File reportFile = insightWork.getReportFile(policyEvaluation.getApplicationId(), policyEvaluation.getScanId());
    if (!reportFile.exists()) {
      return null;
    }

    ReportEntry policyThreatsReportEntry = Report.getEntry(reportFile, ScanPolicyEvaluator.POLICY_THREATS_FILENAME);
    if (policyThreatsReportEntry == null) {
      return null;
    }

    ContainerNode<?> policyThreatsJson = JsonUtils.parse(policyThreatsReportEntry.buf);
    String version = JsonUtils.getNullableString(policyThreatsJson.get("version"));
    if ("1".equals(version)) {
      return JsonUtils.asPojo(policyThreatsJson, PolicyThreats.class);
    }
    return null;
  }
}
