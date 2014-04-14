/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.NewestPolicyViolation;
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

public class NewestPolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(applicationId, "testCRUD");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyViolationDAOTestScanId");
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation.getId(), policy);

    NewestPolicyViolationDAO dao = new NewestPolicyViolationDAO();

    // Create
    NewestPolicyViolation newestPolicyViolation = new NewestPolicyViolation(policyViolation.getId(), applicationId,
        ReleaseStageType.ID);
    dao.insert(newestPolicyViolation);
    assertThat(newestPolicyViolation.getId(), is(policyViolation.getId()));

    // Read
    newestPolicyViolation = dao.getById(newestPolicyViolation.getId());
    assertThat(newestPolicyViolation, is(notNullValue()));
    assertNewestPolicyViolation(policyViolation.getId(), applicationId, ReleaseStageType.ID, newestPolicyViolation);

    // Update is not allowed
    try {
      dao.update(newestPolicyViolation);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      assertThat(expected.getMessage(), is("The NewestPolicyViolation table does not support update operations"));
    }

    // Delete
    dao.delete(newestPolicyViolation);

    newestPolicyViolation = dao.getById(newestPolicyViolation.getId());
    assertThat(newestPolicyViolation, is(nullValue()));
  }

  private void assertNewestPolicyViolation(String id, String applicationId, String stageTypeId,
      NewestPolicyViolation actual)
  {
    assertThat(actual.getId(), is(id));
    assertThat(actual.getApplicationId(), is(applicationId));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
  }
}
