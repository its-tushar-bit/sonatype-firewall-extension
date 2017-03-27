/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDeleteToPolicyViolations");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTestScanId");

    PolicyViolationDAO dao = new PolicyViolationDAO();

    // Create
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1",
        "Version1");
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policy.getId(), policy.getName(), 5,
        PolicyThreatCategory.LICENSE, "acacacacacac", componentIdentifier, "constraint data", "pathnames string");
    assertThat(policyViolation.getId(), is(nullValue()));
    dao.insert(policyViolation);
    assertThat(policyViolation.getId(), is(notNullValue()));

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(notNullValue()));
    assertPolicyViolation(policyEvaluation.getId(), policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE,
        "acacacacacac", componentIdentifier, Lists.newArrayList("pathnames string"), policyEvaluation.getTime(),
        null /* actionTypeId */, policyViolation);

    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(notNullValue()));
    assertPolicyViolation(policyEvaluation.getId(), policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE,
        "acacacacacac", componentIdentifier, Lists.newArrayList("pathnames string"), policyEvaluation.getTime(),
        Action.ID_FAIL, policyViolation);

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(nullValue()));
  }

  private void assertPolicyViolation(String policyEvaluationId,
                                     String policyId,
                                     String policyName,
                                     int threatLevel,
                                     PolicyThreatCategory threatCategory,
                                     String hash,
                                     ComponentIdentifier componentIdentifier,
                                     List<String> pathnames,
                                     Date time,
                                     String actionTypeId,
                                     PolicyViolation actual)
  {
    assertThat(actual.getPolicyEvaluationId(), is(policyEvaluationId));
    assertThat(actual.getPolicyId(), is(policyId));
    assertThat(actual.getPolicyName(), is(policyName));
    assertThat(actual.getThreatLevel(), is(threatLevel));
    assertThat(actual.getThreatCategory(), is(threatCategory));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getPathnames(), is(pathnames));
    assertThat(actual.getTime(), is(time));
    assertThat(actual.getActionTypeId(), is(actionTypeId));
  }

  @Test
  public void testCascadeDeleteToFirstOccurrencePolicyViolations() {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDeleteToFirstOccurrencePolicyViolations");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTest");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = tempEntity.newFirstOccurrencePolicyViolation(
        policyViolation.getId(), applicationId, ReleaseStageType.ID);

    new PolicyViolationDAO().delete(policyViolation);
    assertThat(new FirstOccurrencePolicyViolationDAO().getById(firstOccurrencePolicyViolation.getId()), is(nullValue()));
  }

  @Test
  public void testCascadeDeleteToWaivedPolicyViolations() {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDeleteToWaivedPolicyViolations");
    PolicyWaiver policyWaiver = tempEntity.newWaiver("ababababab", policy.getId(), applicationId);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTest");
    WaivedPolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        policyWaiver);
    WaivedPolicyViolationDAO waivedPolicyViolationDAO = new WaivedPolicyViolationDAO();
    assertThat(waivedPolicyViolationDAO.getById(waivedPolicyViolation.getId()), is(notNullValue()));

    PolicyViolationDAO dao = new PolicyViolationDAO();
    PolicyViolation policyViolation = dao.getById(waivedPolicyViolation.getId());
    dao.delete(policyViolation);
    assertThat(waivedPolicyViolationDAO.getById(waivedPolicyViolation.getId()), is(nullValue()));
  }

  @Test
  public void testGetFirstOccurrenceByApplicationIdAndStageTypeId() {
    Policy policy = tempEntity.newPolicy(applicationId, "testGetFirstOccurrenceByApplicationIdAndStageTypeId");

    // Add policy violations for Release stage
    PolicyEvaluation policyEvaluationRelease = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "ScanReleaseId");
    // Add a policy violation that is not first occurrence
    tempEntity.newPolicyViolation(policyEvaluationRelease, policy);
    // Add a policy violation that is first occurrence
    PolicyViolation firstOccurrencePolicyViolationRelease = tempEntity.newPolicyViolation(policyEvaluationRelease,
        policy);
    tempEntity.newFirstOccurrencePolicyViolation(firstOccurrencePolicyViolationRelease.getId(), applicationId,
        ReleaseStageType.ID);

    // Add policy violations for Build stage
    PolicyEvaluation policyEvaluationBuild = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID,
        "ScanBuildId");
    // Add a policy violation that is not first occurrence
    tempEntity.newPolicyViolation(policyEvaluationBuild, policy);
    // Add a policy violation that is first occurrence
    PolicyViolation firstOccurrencePolicyViolationBuild = tempEntity.newPolicyViolation(policyEvaluationBuild, policy);
    tempEntity.newFirstOccurrencePolicyViolation(firstOccurrencePolicyViolationBuild.getId(), applicationId,
        BuildStageType.ID);

    List<PolicyViolation> firstOccurrencePolicyViolationsRelease = new PolicyViolationDAO()
        .getFirstOccurrenceByApplicationIdAndStageTypeId(applicationId, ReleaseStageType.ID);
    assertThat(firstOccurrencePolicyViolationsRelease, hasSize(1));
    assertThat(firstOccurrencePolicyViolationsRelease.get(0).getId(), is(firstOccurrencePolicyViolationRelease.getId()));
  }

  @Test
  public void testGetFirstOccurrenceByApplicationIdAndStageTypeIdAndHash() {
    Policy policy = tempEntity.newPolicy(applicationId, "testGetFirstOccurrenceByApplicationIdAndStageTypeId");

    // Add policy violations for Release stage
    PolicyEvaluation policyEvaluationRelease = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "ScanReleaseId");
    // Add a policy violation that is not first occurrence
    tempEntity.newPolicyViolation(policyEvaluationRelease, policy);
    // Add a policy violation that is first occurrence
    PolicyViolation firstOccurrencePolicyViolationRelease = tempEntity.newPolicyViolation(policyEvaluationRelease,
        policy);
    tempEntity.newFirstOccurrencePolicyViolation(firstOccurrencePolicyViolationRelease.getId(), applicationId,
        ReleaseStageType.ID);
    // Add another policy violation that is first occurrence for another hash
    PolicyViolation firstOccurrencePolicyViolationRelease2 = tempEntity.newPolicyViolation(policyEvaluationRelease,
        policy, ComponentIdentifier.createNugetCoordinates("n", "1"), "another-hash", "reason");
    tempEntity.newFirstOccurrencePolicyViolation(firstOccurrencePolicyViolationRelease2.getId(), applicationId,
        ReleaseStageType.ID);

    // Add policy violations for Build stage
    PolicyEvaluation policyEvaluationBuild = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID,
        "ScanBuildId");
    // Add a policy violation that is not first occurrence
    tempEntity.newPolicyViolation(policyEvaluationBuild, policy);
    // Add a policy violation that is first occurrence
    PolicyViolation firstOccurrencePolicyViolationBuild = tempEntity.newPolicyViolation(policyEvaluationBuild, policy);
    tempEntity.newFirstOccurrencePolicyViolation(firstOccurrencePolicyViolationBuild.getId(), applicationId,
        BuildStageType.ID);

    List<PolicyViolation> firstOccurrencePolicyViolationsRelease = new PolicyViolationDAO()
        .getFirstOccurrenceByApplicationIdAndStageTypeIdAndHash(applicationId, ReleaseStageType.ID,
            firstOccurrencePolicyViolationRelease.getHash());
    assertThat(firstOccurrencePolicyViolationsRelease, hasSize(1));
    assertThat(firstOccurrencePolicyViolationsRelease.get(0).getId(), is(firstOccurrencePolicyViolationRelease.getId()));
  }

  @Test
  public void testGetFirstOccurrenceByApplicationIdAndStageTypeIdAndHash_ComponentWithoutHash() {
    PolicyViolationDAO dao = new PolicyViolationDAO();
    assertThat(dao.getFirstOccurrenceByApplicationIdAndStageTypeIdAndHash("appId", "stageId", null), hasSize(0));
  }

  @Test
  public void testGetByEvaluationId() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation activeViolation = tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "1", "hash-1",
        null);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), applicationId);
    WaivedPolicyViolation waivedPolicyViolation = tempEntity
        .newWaivedPolicyViolation(evaluation1, policy, policyWaiver);

    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(applicationId, evaluation1.getStageTypeId(), "scan-2");
    tempEntity.newPolicyViolation(evaluation2, policy, "gid", "aid", "2", "hash-2", null);

    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getByEvaluationId(evaluation1.getId());
    assertThat(policyViolations, hasSize(2));
    List<String> foundViolationIds = Arrays.asList(policyViolations.get(0).getId(), policyViolations.get(1).getId());
    assertThat(foundViolationIds, containsInAnyOrder(activeViolation.getId(), waivedPolicyViolation.getId()));
  }

  @Test
  public void testGetActiveByEvaluationId() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation activeViolation = tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "1", "hash-1",
        null);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), applicationId);
    tempEntity.newWaivedPolicyViolation(evaluation1, policy, policyWaiver);

    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(applicationId, evaluation1.getStageTypeId(), "scan-2");
    tempEntity.newPolicyViolation(evaluation2, policy, "gid", "aid", "2", "hash-2", null);

    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getActiveByEvaluationId(evaluation1.getId());
    assertThat(policyViolations, hasSize(1));
    assertThat(policyViolations.get(0).getId(), is(activeViolation.getId()));
  }

  @Test
  public void testGetActiveByEvaluationIds() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), applicationId);

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation activeViolation1 = tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "1", "hash-1",
        null);
    tempEntity.newWaivedPolicyViolation(evaluation1, policy, policyWaiver);

    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(applicationId, evaluation1.getStageTypeId(), "scan-2");
    PolicyViolation activeViolation2 = tempEntity.newPolicyViolation(evaluation2, policy, "gid", "aid", "2", "hash-2",
        null);

    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getActiveByEvaluationIds(Sets.newHashSet(
        evaluation1.getId(), evaluation2.getId()));
    assertThat(policyViolations, hasSize(2));
    List<String> foundViolationIds = Arrays.asList(policyViolations.get(0).getId(), policyViolations.get(1).getId());
    assertThat(foundViolationIds, containsInAnyOrder(activeViolation1.getId(), activeViolation2.getId()));
  }

  @Test
  public void testGetByEvaluationIds() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), applicationId);

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation activeViolation1 = tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "1", "hash-1",
        null);
    WaivedPolicyViolation waivedPolicyViolation = tempEntity
        .newWaivedPolicyViolation(evaluation1, policy, policyWaiver);

    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(applicationId, evaluation1.getStageTypeId(), "scan-2");
    PolicyViolation activeViolation2 = tempEntity
        .newPolicyViolation(evaluation2, policy, "gid", "aid", "2", "hash-2", null);

    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getByEvaluationIds(Sets.newHashSet(
        evaluation1.getId(), evaluation2.getId()));
    assertThat(policyViolations, hasSize(3));
    List<String> foundViolationIds = Arrays
        .asList(policyViolations.get(0).getId(), policyViolations.get(1).getId(), policyViolations.get(2).getId());
    assertThat(foundViolationIds,
        containsInAnyOrder(activeViolation1.getId(), activeViolation2.getId(), waivedPolicyViolation.getId()));
  }

  @Test
  public void testGetActiveByEvaluationIds_InOperatorOptimizationForH2() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), applicationId);

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation activeViolation1 = tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "1", "hash-1",
        null);
    tempEntity.newWaivedPolicyViolation(evaluation1, policy, policyWaiver);

    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(applicationId, evaluation1.getStageTypeId(), "scan-2");
    tempEntity.newPolicyViolation(evaluation2, policy, "gid", "aid", "2", "hash-2", null);

    Set<String> evaluationIds = new LinkedHashSet<>();
    while (evaluationIds.size() < PolicyViolationDAO.IN_OPERATOR_THRESHOLD) {
      evaluationIds.add(tempEntity.uuid());
    }
    evaluationIds.add(evaluation1.getId());
    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getActiveByEvaluationIds(evaluationIds);
    assertThat(policyViolations, hasSize(1));
    assertThat(policyViolations.get(0).getId(), is(activeViolation1.getId()));
  }

  @Test
  public void testGetActiveByByEvaluationIdAndHash() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyViolationDAO dao = new PolicyViolationDAO();
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), applicationId);
    WaivedPolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(evaluation, policy, policyWaiver);
    String hash = dao.getById(waivedPolicyViolation.getId()).getHash();
    PolicyViolation activeViolation = tempEntity.newPolicyViolation(evaluation, policy, "gid", "aid", "1", hash, null);

    List<PolicyViolation> policyViolations = dao.getActiveByEvaluationIdAndHash(evaluation.getId(), hash);
    assertThat(policyViolations, hasSize(1));
    assertThat(policyViolations.get(0).getId(), is(activeViolation.getId()));
  }

  @Test
  public void testReplacePolicyId() {
    Policy fromPolicy = tempEntity.newPolicy(applicationId, "From Policy");
    Policy toPolicy = tempEntity.newPolicy(applicationId, "To Policy");
    Policy otherPolicy = tempEntity.newPolicy(applicationId, "Other Policy");
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation toPolicyViolation = tempEntity.newPolicyViolation(evaluation, toPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);

    PolicyViolationDAO dao = new PolicyViolationDAO();
    dao.replacePolicyId(fromPolicy.getId(), toPolicy.getId());

    fromPolicyViolation = dao.getById(fromPolicyViolation.getId());
    assertThat(fromPolicyViolation.getPolicyId(), is(toPolicy.getId()));
    toPolicyViolation = dao.getById(toPolicyViolation.getId());
    assertThat(toPolicyViolation.getPolicyId(), is(toPolicy.getId()));
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId(), is(otherPolicy.getId()));
  }

  @Test
  public void testReplacePolicyIdForApplication() {
    Policy fromPolicy = tempEntity.newPolicy(organization.getId(), "From Policy");
    Policy toPolicy = tempEntity.newPolicy(applicationId, "To Policy");
    Policy otherPolicy = tempEntity.newPolicy(applicationId, "Other Policy");
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scanId");
    PolicyViolation fromPolicyViolation = tempEntity.newPolicyViolation(evaluation, fromPolicy);
    PolicyViolation otherPolicyViolation = tempEntity.newPolicyViolation(evaluation, otherPolicy);
    PolicyEvaluation otherAppEvaluation = tempEntity.newPolicyEvaluation(tempEntity
        .newApplication(organization.getId()).getId(), BuildStageType.ID, "scanId");
    PolicyViolation otherAppPolicyViolation = tempEntity.newPolicyViolation(otherAppEvaluation, fromPolicy);

    PolicyViolationDAO dao = new PolicyViolationDAO();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.replacePolicyId(tx, applicationId, fromPolicy.getId(), toPolicy.getId());
      tx.commit();
    }

    fromPolicyViolation = dao.getById(fromPolicyViolation.getId());
    assertThat(fromPolicyViolation.getPolicyId(), is(toPolicy.getId()));
    otherPolicyViolation = dao.getById(otherPolicyViolation.getId());
    assertThat(otherPolicyViolation.getPolicyId(), is(otherPolicy.getId()));
    otherAppPolicyViolation = dao.getById(otherAppPolicyViolation.getId());
    assertThat(otherAppPolicyViolation.getPolicyId(), is(fromPolicy.getId()));
  }
}
