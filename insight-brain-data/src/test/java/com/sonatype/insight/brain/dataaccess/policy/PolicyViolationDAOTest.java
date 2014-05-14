/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
    PolicyViolation policyViolation = new PolicyViolation(policyEvaluation.getId(), policy.getId(), policy.getName(),
        5, PolicyThreatCategory.LICENSE, "acacacacacac", "Group1", "Artifact1", "Version1", "constraint data",
        "pathnames string");
    policyViolation.setTime(policyEvaluation.getTime());
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

    // Update is not allowed
    try {
      dao.update(policyViolation);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      assertThat(expected.getMessage(), is("The PolicyViolation table does not support update operations"));
    }

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
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation.getId(), policyEvaluation.getTime(), policy);
    NewestPolicyViolation newestPolicyViolation = tempEntity.newNewestPolicyViolation(policyViolation.getId(),
        applicationId, ReleaseStageType.ID);

    new PolicyViolationDAO().delete(policyViolation);
    assertThat(new NewestPolicyViolationDAO().getById(newestPolicyViolation.getId()), is(nullValue()));
  }

  @Test
  public void testGetNewestByApplicationId() {
    Policy policy = tempEntity.newPolicy(applicationId, "testGetNewestByApplicationId");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTest");
    // Add a policy violation that is not newest
    tempEntity.newPolicyViolation(policyEvaluation.getId(), policyEvaluation.getTime(), policy);
    // Add a policy violation that is newest
    PolicyViolation newestPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation.getId(), policyEvaluation.getTime(), policy);
    tempEntity.newNewestPolicyViolation(newestPolicyViolation.getId(), applicationId, ReleaseStageType.ID);

    List<PolicyViolation> newestPolicyViolations = new PolicyViolationDAO().getNewestByApplicationId(applicationId);
    assertThat(newestPolicyViolations, hasSize(1));
    assertThat(newestPolicyViolations.get(0).getId(), is(newestPolicyViolation.getId()));
  }

  @Test
  public void testGetNewestByApplicationIdAndStageTypeIdAndLastNDays() {
    Policy policy = tempEntity.newPolicy(applicationId, "testGetNewestByApplicationIdAndStageTypeIdAndLastNDays");

    int nDays = 30;
    DateTime now = new DateTime();
    Date beforeNDays = now.minusDays(nDays).minusMillis(1).toDate();
    Date afterNDays = now.minusDays(nDays).plusSeconds(5).toDate();
    PolicyEvaluation releasePolicyEvaluationBefore = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTest-release", beforeNDays);
    PolicyEvaluation releasePolicyEvaluationAfter = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTest-release", afterNDays);
    PolicyEvaluation buildPolicyEvaluationAfter = tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID,
        "PolicyViolationDAOTest-build", afterNDays);

    // Add a policy violation that is before nDays
    PolicyViolation releasePolicyViolation = tempEntity.newPolicyViolation(releasePolicyEvaluationBefore.getId(),
        releasePolicyEvaluationBefore.getTime(), policy);
    tempEntity.newNewestPolicyViolation(releasePolicyViolation.getId(), applicationId, ReleaseStageType.ID);
    // Add a policy violation that is after nDays
    releasePolicyViolation = tempEntity.newPolicyViolation(releasePolicyEvaluationAfter.getId(),
        releasePolicyEvaluationAfter.getTime(), policy);
    tempEntity.newNewestPolicyViolation(releasePolicyViolation.getId(), applicationId, ReleaseStageType.ID);
    // Add another policy violation, with a different stage type id, that is after nDays
    PolicyViolation buildPolicyViolation = tempEntity.newPolicyViolation(buildPolicyEvaluationAfter.getId(),
        buildPolicyEvaluationAfter.getTime(), policy);
    tempEntity.newNewestPolicyViolation(buildPolicyViolation.getId(), applicationId, BuildStageType.ID);

    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

    List<PolicyViolation> newestReleasePolicyViolations = policyViolationDAO
        .getNewestByApplicationIdAndStageTypeIdAndLastNDays(applicationId, ReleaseStageType.ID, nDays);
    assertThat(newestReleasePolicyViolations, hasSize(1));
    assertThat(newestReleasePolicyViolations.get(0).getId(), is(releasePolicyViolation.getId()));

    List<PolicyViolation> newestBuildPolicyViolations = policyViolationDAO
        .getNewestByApplicationIdAndStageTypeIdAndLastNDays(applicationId, BuildStageType.ID, nDays);
    assertThat(newestBuildPolicyViolations, hasSize(1));
    assertThat(newestBuildPolicyViolations.get(0).getId(), is(buildPolicyViolation.getId()));
  }
}
