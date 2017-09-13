/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.successmetrics.PolicyViolationResolutionState;

import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PolicyViolationResolutionStateDAOTest
    extends AbstractDbDAOTest
{
  private PolicyViolationResolutionStateDAO dao = new PolicyViolationResolutionStateDAO();

  @Test
  public void testCRUD() {
    Application app = tempEntity.newApplicationWithParent("test-app");
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1");
    Policy policy = tempEntity.newPolicy(app.getId(), "policy1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy);

    // sanity checks
    assertThat(violation.getTime(), is(notNullValue()));
    assertThat(violation.getHash(), is(notNullValue()));
    assertThat(violation.getPolicyId(), is(notNullValue()));
    assertThat(violation.getPolicyName(), is(notNullValue()));
    assertThat(violation.getThreatLevel(), is(notNullValue()));

    PolicyViolationResolutionState resolutionState = new PolicyViolationResolutionState(app.getId(), violation);
    resolutionState.setStageTypeById(BuildStageType.ID);

    // create
    dao.insert(resolutionState);
    assertThat(resolutionState.getId(), is(notNullValue()));

    // read
    resolutionState = dao.getById(resolutionState.getId());
    assertThat(resolutionState, is(notNullValue()));
    assertThat(resolutionState.getApplicationId(), is(app.getId()));
    assertThat(resolutionState.getFirstOccurrenceTime(), is(violation.getTime()));
    assertThat(resolutionState.getPolicyId(), is(violation.getPolicyId()));
    assertThat(resolutionState.getPolicyName(), is(violation.getPolicyName()));
    assertThat(resolutionState.getThreatLevel(), is(violation.getThreatLevel()));
    assertThat(resolutionState.getHash(), is(violation.getHash()));
    assertThat(resolutionState.getComponentIdentifier(), is(violation.getComponentIdentifier()));
    assertThat(resolutionState.getBuildStageType(), is(true));
    assertThat(resolutionState.getDevelopStageType(), is(false));
    assertThat(resolutionState.getStageReleaseStageType(), is(false));
    assertThat(resolutionState.getReleaseStageType(), is(false));
    assertThat(resolutionState.getOperateStageType(), is(false));
    assertThat(resolutionState.getProxyStageType(), is(false));

    // update
    resolutionState.setStageTypeById(DevelopStageType.ID, true);
    resolutionState.setStageTypeById(BuildStageType.ID, false);
    dao.update(resolutionState);

    resolutionState = dao.getById(resolutionState.getId());

    assertThat(resolutionState, is(notNullValue()));
    assertThat(resolutionState.getBuildStageType(), is(false));
    assertThat(resolutionState.getDevelopStageType(), is(true));

    // delete
    String id = resolutionState.getId();
    dao.delete(resolutionState);

    resolutionState = dao.getById(id);

    assertThat(resolutionState, is(nullValue()));
  }

  @Test
  public void testGetByApplicationId() {
    Application app = tempEntity.newApplicationWithParent("test-app");
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scan1");
    Policy policy = tempEntity.newPolicy(app.getId(), "policy1");
    PolicyViolation violation = tempEntity.newPolicyViolation(evaluation, policy);

    PolicyViolationResolutionState resolutionState = tempEntity.newPolicyViolationResolutionState(app.getId(),
        violation, BuildStageType.ID);

    List<PolicyViolationResolutionState> results = dao.getByApplicationId(app.getId());

    assertThat(results, hasSize(1));
    assertThat(results.get(0).getId(), is(resolutionState.getId()));

    results = dao.getByApplicationId("some-other-id");

    assertThat(results, is(empty()));
  }
}
