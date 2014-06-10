/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import com.google.common.collect.Lists;
import org.junit.Test;

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
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation, policy.getId(), policy.getName(),
        5, PolicyThreatCategory.LICENSE, "acacacacacac", "Group1", "Artifact1", "Version1", "constraint data",
        "pathnames string");
    assertThat(policyViolation.getId(), is(nullValue()));
    dao.insert(policyViolation);
    assertThat(policyViolation.getId(), is(notNullValue()));
    assertThat(policyViolation.getTime(), is(policyEvaluation.getTime()));

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(notNullValue()));
    assertPolicyViolation(policyEvaluation.getId(), policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE,
        "acacacacacac", "Group1", "Artifact1", "Version1", Lists.newArrayList("pathnames string"),
        policyEvaluation.getTime(), policyViolation);

    policyViolation.setActionTypeId(Action.ID_FAIL);
    dao.update(policyViolation);

    // Read
    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(notNullValue()));
    assertPolicyViolation(policyEvaluation.getId(), policy.getId(), policy.getName(), 5, PolicyThreatCategory.LICENSE,
        "acacacacacac", "Group1", "Artifact1", "Version1", Lists.newArrayList("pathnames string"),
        policyEvaluation.getTime(), policyViolation);
    assertThat(policyViolation.getActionTypeId(), is(Action.ID_FAIL));

    // Delete
    dao.delete(policyViolation);

    policyViolation = dao.getById(policyViolation.getId());
    assertThat(policyViolation, is(nullValue()));
  }

  private void assertPolicyViolation(String policyEvaluationId, String policyId, String policyName, int threatLevel,
      PolicyThreatCategory threatCategory, String hash, String groupId, String artifactId, String version,
      List<String> pathnames, Date time, PolicyViolation actual)
  {
    assertThat(actual.getPolicyEvaluationId(), is(policyEvaluationId));
    assertThat(actual.getPolicyId(), is(policyId));
    assertThat(actual.getPolicyName(), is(policyName));
    assertThat(actual.getThreatLevel(), is(threatLevel));
    assertThat(actual.getThreatCategory(), is(threatCategory));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getGroupId(), is(groupId));
    assertThat(actual.getArtifactId(), is(artifactId));
    assertThat(actual.getVersion(), is(version));
    assertThat(actual.getPathnames(), is(pathnames));
    assertThat(actual.getTime(), is(time));
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
  public void testGetFirstOccurrence_ComponentWithHash() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation violation1 = tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "1", "hash-1", null);
    tempEntity.newFirstOccurrencePolicyViolation(violation1.getId(), applicationId, evaluation1.getStageTypeId());
    PolicyViolation violation2 = tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "2", "hash-2", null);
    tempEntity.newFirstOccurrencePolicyViolation(violation2.getId(), applicationId, evaluation1.getStageTypeId());

    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(applicationId, evaluation1.getStageTypeId(), "scan-2");
    PolicyViolation violation3 = tempEntity.newPolicyViolation(evaluation2, policy, "gid", "aid", "2", "hash-2", null);

    PolicyViolation first = new PolicyViolationDAO().getFirstOccurrence(applicationId, evaluation1.getStageTypeId(),
        violation3);
    assertThat(first, is(notNullValue()));
    assertThat(first.getId(), is(violation2.getId()));
  }

  @Test
  public void testGetFirstOccurrence_ComponentWithoutHash() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy, null /* groupId */,
        null /* artifactId */, null /* version */, null /* hash */, null /* reason */);

    PolicyViolation first = new PolicyViolationDAO().getFirstOccurrence(applicationId, evaluation.getStageTypeId(),
        violation);
    assertThat(first, is(notNullValue()));
    assertThat(first.getId(), is(violation.getId()));
  }

  @Test
  public void testGetFirstOccurrence_MissingFirstOccurrenceViolationDueToIncompleteDataMigration() {
    Policy policy = tempEntity.newPolicy(applicationId, "name");

    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan-1");
    tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "1", "hash-1", null);
    tempEntity.newPolicyViolation(evaluation1, policy, "gid", "aid", "2", "hash-2", null);

    PolicyEvaluation evaluation2 = tempEntity
        .newPolicyEvaluation(applicationId, evaluation1.getStageTypeId(), "scan-2");
    PolicyViolation violation3 = tempEntity.newPolicyViolation(evaluation2, policy, "gid", "aid", "2", "hash-2", null);

    PolicyViolation first = new PolicyViolationDAO().getFirstOccurrence(applicationId, evaluation1.getStageTypeId(),
        violation3);
    assertThat(first, is(notNullValue()));
    assertThat(first.getId(), is(violation3.getId()));
  }
}
