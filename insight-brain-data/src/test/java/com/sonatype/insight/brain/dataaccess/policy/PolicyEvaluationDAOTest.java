/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
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

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, "scanId2", time2);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(applicationId, stageTypeId);
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

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(applicationId, stageTypeId);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time2));
  }

  @Test
  public void testGetLastByApplicationIdAndStageId_ReevaluationOfOldScan() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, "scanId1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, "scanId2", time2);
    // A re-eval of an older scan should not be returned as the last eval
    Date time3 = new Date(time2.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, "scanId1", true /* isReevaluation */,
        false /* forMonitoring */, time3);

    PolicyEvaluation policyEvaluation = dao.getLastByApplicationIdAndStageId(applicationId, stageTypeId);
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

  @Test
  public void testCascadeDeleteToPolicyViolations() {
    Policy policy = tempEntity.newPolicy(applicationId, "testCascadeDeleteToPolicyViolations");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID,
        "PolicyEvaluationDAOTest");
    tempEntity.newPolicyViolation(policyEvaluation, policy);
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();
    assertThat(policyViolationDAO.getByEvaluationId(policyEvaluation.getId()), hasSize(1));

    new PolicyEvaluationDAO().delete(policyEvaluation);
    assertThat(policyViolationDAO.getByEvaluationId(policyEvaluation.getId()), hasSize(0));
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

  @Test
  public void testGetMostRecentByApplicationIdsAndStageIdsGetsNewest() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    String stageTypeId = ReleaseStageType.ID;
    String scanId = "PolicyEvaluationDAOTest";

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, stageTypeId, scanId, true /* isReevaluation */,
        false /* forMonitoring */, time2);

    List<PolicyEvaluation> policyEvaluations = dao
        .getLastByApplicationIdsAndStageIds(Sets.newHashSet(applicationId), Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations, hasSize(1));
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time2));
  }

  @Test
  public void testGetMostRecentByApplicationIdsAndStageIdsFiltersInvalidAppIdsAndStages() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    Application application2 = tempEntity.newApplication("AbstractDbDAOTest-AppName2", "AbstractDbDAOTest_AppPublicId2",
        organization.getId());

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan1", time1);

    //wrong stage
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan2", time2);

    //wrong appId
    Date time3 = new Date(time1.getTime() + 2000);
    tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID, "scan3", time3);

    //wrong both
    Date time4 = new Date(time1.getTime() + 3000);
    tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID, "scan4", time4);

    List<PolicyEvaluation> policyEvaluations = dao
        .getLastByApplicationIdsAndStageIds(Sets.newHashSet(applicationId), Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations, hasSize(1));
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time1));
  }

  @Test
  public void testGetMostRecentByApplicationIdsAndStageIdsDealsWithDuplicateTimes() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan1", time1);
    tempEntity.newPolicyEvaluation(applicationId, BuildStageType.ID, "scan2", time1);

    List<PolicyEvaluation> policyEvaluations = dao
        .getLastByApplicationIdsAndStageIds(Sets.newHashSet(applicationId), Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations, hasSize(1));
    PolicyEvaluation policyEvaluation = policyEvaluations.get(0);
    assertThat(policyEvaluation, is(notNullValue()));
    assertThat(policyEvaluation.getTime(), is(time1));
  }

  @Test
  public void testGetMostRecentByApplicationIdsAndStageIdsDealsWithTwoApps() {
    PolicyEvaluationDAO dao = new PolicyEvaluationDAO();

    Date time1 = new Date();
    tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan1", time1);
    Date time2 = new Date(time1.getTime() + 1000);
    tempEntity.newPolicyEvaluation(applicationId, ReleaseStageType.ID, "scan2", time2);

    //second app
    Application application2 = tempEntity.newApplication("AbstractDbDAOTest-AppName2", "AbstractDbDAOTest_AppPublicId2",
        organization.getId());
    tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID, "scan3", time1);
    tempEntity.newPolicyEvaluation(application2.getId(), ReleaseStageType.ID, "scan4", time2);

    List<PolicyEvaluation> policyEvaluations = dao
        .getLastByApplicationIdsAndStageIds(Sets.newHashSet(applicationId, application2.getId()),
            Sets.newHashSet(ReleaseStageType.ID));
    assertThat(policyEvaluations, hasSize(2));
    for(PolicyEvaluation pe : policyEvaluations){
      assertThat(pe, is(notNullValue()));
      assertThat(pe.getTime(), is(time2));
    }
  }
}
