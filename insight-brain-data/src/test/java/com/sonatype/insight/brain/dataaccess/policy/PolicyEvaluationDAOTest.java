/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PolicyEvaluationDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    // Create
    PolicyEvaluation policyEvaluation = new PolicyEvaluation(applicationId, stageTypeId, scanId);
    assertThat(policyEvaluation.getId(), is(nullValue()));
    dao.insert(policyEvaluation);
    assertThat(policyEvaluation.getId(), is(notNullValue()));
    assertThat(policyEvaluation.getTime().getTime(), is(lessThan(System.currentTimeMillis() + 1000)));
    assertThat(policyEvaluation.getTime().getTime(), is(greaterThan(System.currentTimeMillis() - 10 * 1000)));

    // Read
    policyEvaluation = dao.getById(policyEvaluation.getId());
    assertThat(policyEvaluation, is(notNullValue()));
    assertPolicyEvaluation(applicationId, stageTypeId, scanId, false, false, policyEvaluation);

    // Update is not allowed
    try {
      dao.update(policyEvaluation);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException expected) {
      assertThat(expected.getMessage(), is("The PolicyEvaluation table does not support update operations"));
    }

    // Delete
    dao.delete(policyEvaluation);

    policyEvaluation = dao.getById(policyEvaluation.getId());
    assertThat(policyEvaluation, is(nullValue()));
  }

  @Test
  public void testGetLastByApplicationIdAndScanId() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, time2);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndScanId(applicationId, scanId);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time2));
  }

  @Test
  public void testGetLastByApplicationIdAndStageId() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, time2);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(applicationId, ReleaseStageType.ID);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time2));
  }

  @Test
  public void testGetLastByApplicationIdAndStageId_Reevaluation() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, true /* isReevaluation */,
        false /* forMonitoring */, time2);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(applicationId, ReleaseStageType.ID);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time2));
  }

  @Test
  public void testGetLastPrimaryByApplicationIdAndStageId() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, true /* isReevaluation */,
        false /* forMonitoring */, time2);

    PolicyEvaluation policyEvaluation = dao.getLastPrimaryByApplicationIdAndStageId(applicationId, ReleaseStageType.ID);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time1));
  }

  private void assertPolicyEvaluation(String applicationId, String stageTypeId, String scanId, boolean reevaluation,
      boolean forMonitoring, PolicyEvaluation actual)
  {
    assertThat(actual.getApplicationId(), is(applicationId));
    assertThat(actual.getStageTypeId(), is(stageTypeId));
    assertThat(actual.getScanId(), is(scanId));
    assertThat(actual.isReevaluation(), is(reevaluation));
    assertThat(actual.isForMonitoring(), is(forMonitoring));
  }
}
