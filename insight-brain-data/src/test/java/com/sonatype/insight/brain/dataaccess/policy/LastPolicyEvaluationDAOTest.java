/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.LastPolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LastPolicyEvaluationDAOTest
    extends AbstractDbDAOTest
{

  final LastPolicyEvaluationDAO dao = new LastPolicyEvaluationDAO();

  final PolicyEvaluationDAO peDao = new PolicyEvaluationDAO();

  @Test
  public void testCRUD() throws Exception {

    final String stageTypeId = ReleaseStageType.ID;
    final String scanId = "LastPolicyEvaluationDAOTest";

    // Create (as part of a policy eval)
    final PolicyEvaluation eval = tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId);

    // Read
    final LastPolicyEvaluation policyEvaluation = dao.getByEvaluationId(eval.getId());
    assertThat(policyEvaluation.getId(), is(eval.getId()));
    assertThat(policyEvaluation.getApplicationId(), is(applicationId));
    assertThat(policyEvaluation.getStageTypeId(), is(stageTypeId));


    // Update is not allowed
    try {
      dao.update(policyEvaluation);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      assertThat(expected.getMessage(), is("The LastPolicyEvaluation table does not support update operations"));
    }

    // Delete
    dao.delete(policyEvaluation);

    LastPolicyEvaluation readPolicyEvaluation2 = dao.getByEvaluationId(eval.getId());
    assertThat(readPolicyEvaluation2, is(nullValue()));
  }

  @Test
  public void testAddingAndDeletingWorksProperly() {
    final String stageTypeId = ReleaseStageType.ID;
    final String scanId = "LastPolicyEvaluationDAOTest";
    final Date eval1Date = new Date();

    // put one guy in, he should be first
    final PolicyEvaluation eval1 = tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, eval1Date);
    final LastPolicyEvaluation firstPolicyEvaluation = dao.getByApplicationIdAndStageTypeId(applicationId,
        stageTypeId);
    assertThat(firstPolicyEvaluation.getId(), is(eval1.getId()));

    //put in a newer guy, he should be the newest now
    final Date eval2Date = new Date(eval1Date.getTime() + 10);
    final PolicyEvaluation eval2 = tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, eval2Date);
    final LastPolicyEvaluation secondPolicyEvaluation = dao.getByApplicationIdAndStageTypeId(applicationId,
        stageTypeId);
    assertThat(secondPolicyEvaluation.getId(), is(eval2.getId()));

    //put a guy in the middle (timewise), he should not change who the newest is
    final Date eval3Date = new Date(eval1Date.getTime() + 5);
    final PolicyEvaluation eval3 = tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, eval3Date);
    final LastPolicyEvaluation thirdPolicyEvaluation = dao.getByApplicationIdAndStageTypeId(applicationId,
        stageTypeId);
    assertThat(thirdPolicyEvaluation.getId(), is(eval2.getId()));

    //delete the newest guy, should now be the middle guy
    peDao.delete(eval2);
    final LastPolicyEvaluation fourthPolicyEvaluation = dao.getByApplicationIdAndStageTypeId(applicationId,
        stageTypeId);
    assertThat(fourthPolicyEvaluation.getId(), is(eval3.getId()));

    //delete currently newest guy, should now be the first guy
    peDao.delete(eval3);
    final LastPolicyEvaluation fifthPolicyEvaluation = dao.getByApplicationIdAndStageTypeId(applicationId,
        stageTypeId);
    assertThat(fifthPolicyEvaluation.getId(), is(eval1.getId()));
  }

}
