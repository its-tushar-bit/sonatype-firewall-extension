/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.FirstOccurrencePolicyViolation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Test;

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

  private void assertFirstOccurrencePolicyViolation(String id, String applicationId, String stageTypeId,
      FirstOccurrencePolicyViolation actual)
  {
    assertThat(actual.getId(), is(id));
    assertThat(actual.getApplicationId(), is(applicationId));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
  }
}
