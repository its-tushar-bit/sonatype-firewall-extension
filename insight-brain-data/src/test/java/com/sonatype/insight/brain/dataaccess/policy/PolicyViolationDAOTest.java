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
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
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
  public void testCascadeDeleteToNewestPolicyViolations() {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDeleteToNewestPolicyViolations");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTest");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    NewestPolicyViolation newestPolicyViolation = tempEntity.newNewestPolicyViolation(policyViolation.getId(),
        applicationId, ReleaseStageType.ID);

    new PolicyViolationDAO().delete(policyViolation);
    assertThat(new NewestPolicyViolationDAO().getById(newestPolicyViolation.getId()), is(nullValue()));
  }

  @Test
  public void testGetNewestByApplicationIdAndStageTypeId() {
    Policy policy = tempEntity.newPolicy(applicationId, "testGetNewestByApplicationIdAndStageTypeId");

    // Add policy violations for Release stage
    PolicyEvaluation policyEvaluationRelease = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "ScanReleaseId");
    // Add a policy violation that is not newest
    tempEntity.newPolicyViolation(policyEvaluationRelease, policy);
    // Add a policy violation that is newest
    PolicyViolation newestPolicyViolationRelease = tempEntity.newPolicyViolation(policyEvaluationRelease, policy);
    tempEntity.newNewestPolicyViolation(newestPolicyViolationRelease.getId(), applicationId, ReleaseStageType.ID);

    // Add policy violations for Build stage
    PolicyEvaluation policyEvaluationBuild = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID,
        "ScanBuildId");
    // Add a policy violation that is not newest
    tempEntity.newPolicyViolation(policyEvaluationBuild, policy);
    // Add a policy violation that is newest
    PolicyViolation newestPolicyViolationBuild = tempEntity.newPolicyViolation(policyEvaluationBuild, policy);
    tempEntity.newNewestPolicyViolation(newestPolicyViolationBuild.getId(), applicationId, BuildStageType.ID);

    List<PolicyViolation> newestPolicyViolationsRelease = new PolicyViolationDAO()
        .getNewestByApplicationIdAndStageTypeId(applicationId, ReleaseStageType.ID);
    assertThat(newestPolicyViolationsRelease, hasSize(1));
    assertThat(newestPolicyViolationsRelease.get(0).getId(), is(newestPolicyViolationRelease.getId()));
  }
}
