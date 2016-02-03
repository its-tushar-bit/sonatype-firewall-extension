/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FirstOccurrencePolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(applicationId, "testCRUD");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTestScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);

    FirstOccurrencePolicyViolationDAO dao = new FirstOccurrencePolicyViolationDAO();

    // Create
    FirstOccurrencePolicyViolation firstOccurrencePolicyViolation = new FirstOccurrencePolicyViolation(
        policyViolation.getId(), applicationId, ReleaseStageType.ID);
    dao.insert(firstOccurrencePolicyViolation);
    assertThat(firstOccurrencePolicyViolation.getId(), is(policyViolation.getId()));

    // Read
    firstOccurrencePolicyViolation = dao.getById(firstOccurrencePolicyViolation.getId());
    assertThat(firstOccurrencePolicyViolation, is(notNullValue()));
    assertFirstOccurrencePolicyViolation(policyViolation.getId(), applicationId, ReleaseStageType.ID,
        firstOccurrencePolicyViolation);

    // Update is not allowed
    try {
      dao.update(firstOccurrencePolicyViolation);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      assertThat(expected.getMessage(),
          is("The FirstOccurrencePolicyViolation table does not support update operations"));
    }

    // Delete
    dao.delete(firstOccurrencePolicyViolation);

    firstOccurrencePolicyViolation = dao.getById(firstOccurrencePolicyViolation.getId());
    assertThat(firstOccurrencePolicyViolation, is(nullValue()));
  }

  @Test
  public void testGetByApplicationIdAndStageId() {
    FirstOccurrencePolicyViolationDAO dao = new FirstOccurrencePolicyViolationDAO();

    // (app1, build)
    Policy policy1 = tempEntity.newPolicy(applicationId, "policy1");
    PolicyEvaluation policyEvaluation1a = tempEntity.newPolicyEvaluation(policy1.getOwnerId(), BuildStageType.ID,
        "scanId1a");
    PolicyViolation policyViolation1a = tempEntity.newPolicyViolation(policyEvaluation1a, policy1);
    FirstOccurrencePolicyViolation firstOccurrence1a = tempEntity.newFirstOccurrencePolicyViolation(
        policyViolation1a.getId(), policyEvaluation1a.getApplicationId(), policyEvaluation1a.getStageTypeId());

    // (app1, release)
    PolicyEvaluation policyEvaluation1b = tempEntity.newPolicyEvaluation(policy1.getOwnerId(), ReleaseStageType.ID,
        "scanId1b");
    PolicyViolation policyViolation1b = tempEntity.newPolicyViolation(policyEvaluation1b, policy1);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation1b.getId(), policyEvaluation1b.getApplicationId(),
        policyEvaluation1b.getStageTypeId());

    // (app2, build)
    Policy policy2 = tempEntity.newPolicy(tempEntity.newApplication(organization.getId()).getId(), "policy2");
    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(policy2.getOwnerId(), BuildStageType.ID,
        "scanId2");
    PolicyViolation policyViolation2 = tempEntity.newPolicyViolation(policyEvaluation2, policy2);
    tempEntity.newFirstOccurrencePolicyViolation(policyViolation2.getId(), policyEvaluation2.getApplicationId(),
        policyEvaluation2.getStageTypeId());

    List<FirstOccurrencePolicyViolation> firstOccurrences;
    try (TransactionContext tx = dao.createTransactionContext()) {
      firstOccurrences = dao.getByApplicationIdAndStageId(tx, applicationId, BuildStageType.ID);
    }
    assertThat(firstOccurrences, hasSize(1));
    assertFirstOccurrencePolicyViolation(firstOccurrence1a.getId(), firstOccurrence1a.getApplicationId(),
        firstOccurrence1a.getStageTypeId(), firstOccurrences.get(0));
  }

  private void assertFirstOccurrencePolicyViolation(String id,
                                                    String applicationId,
                                                    String stageTypeId,
                                                    FirstOccurrencePolicyViolation actual)
  {
    assertThat(actual.getId(), is(id));
    assertThat(actual.getApplicationId(), is(applicationId));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
  }
}
