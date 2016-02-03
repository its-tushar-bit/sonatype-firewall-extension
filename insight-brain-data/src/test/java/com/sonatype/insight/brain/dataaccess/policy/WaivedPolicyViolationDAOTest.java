/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.WaivedPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class WaivedPolicyViolationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    Policy policy = tempEntity.newPolicy(applicationId, "testCRUD");
    PolicyWaiver policyWaiver = tempEntity.newWaiver("ababababab", policy.getId(), applicationId, "Some comments here");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "WaivedPolicyViolationDAOTestScanId");
    WaivedPolicyViolationDAO dao = new WaivedPolicyViolationDAO();

    // Create
    WaivedPolicyViolation waivedPolicyViolation = tempEntity.newWaivedPolicyViolation(policyEvaluation, policy,
        policyWaiver);
    waivedPolicyViolation = dao.getById(waivedPolicyViolation.getId());
    assertThat(waivedPolicyViolation, is(notNullValue()));

    // Read
    waivedPolicyViolation = dao.getById(waivedPolicyViolation.getId());
    assertThat(waivedPolicyViolation, is(notNullValue()));
    assertWaivedPolicyViolation(waivedPolicyViolation.getId(), policyWaiver.getId(), "Some comments here",
        waivedPolicyViolation);

    // Update is not allowed
    try {
      dao.update(waivedPolicyViolation);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      assertThat(expected.getMessage(), is("The WaivedPolicyViolation table does not support update operations."));
    }

    // Delete
    dao.delete(waivedPolicyViolation);

    waivedPolicyViolation = dao.getById(waivedPolicyViolation.getId());
    assertThat(waivedPolicyViolation, is(nullValue()));
  }

  private void assertWaivedPolicyViolation(String id,
                                           String policyWaiverId,
                                           String comment,
                                           WaivedPolicyViolation actual)
  {
    assertThat(actual.getId(), is(id));
    assertThat(actual.getPolicyWaiverId(), is(policyWaiverId));
    assertThat(actual.getComment(), is(comment));
  }
}
