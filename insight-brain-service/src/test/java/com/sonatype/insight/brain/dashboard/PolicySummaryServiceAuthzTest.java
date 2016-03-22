/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PolicySummaryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicySummaryService policySummaryService;

  @Test
  public void testGetPolicySummary_ExplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId());
    assertEmptyResults(
        policySummaryService.getPolicySummary(Collections.singleton(app.getId()), null, null, null, null));
  }

  @Test
  public void testGetPolicySummary_ExplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId());
    login();
    assertEmptyResults(
        policySummaryService.getPolicySummary(Collections.singleton(app.getId()), null, null, null, null));
  }

  @Test
  public void testGetPolicySummary_ExplicitApplicationFilter_Authorized() {
    createPolicyViolation(app.getId());
    grantReadPermission(app.getId());
    assertResults(policySummaryService.getPolicySummary(Collections.singleton(app.getId()), null, null, null, null));
  }

  @Test
  public void testGetPolicySummary_ImplicitApplicationFilter_Unauthenticated() {
    createPolicyViolation(app.getId());
    assertEmptyResults(policySummaryService.getPolicySummary(null, null, null, null, null));
  }

  @Test
  public void testGetPolicySummary_ImplicitApplicationFilter_Unauthorized() {
    createPolicyViolation(app.getId());
    login();
    assertEmptyResults(policySummaryService.getPolicySummary(null, null, null, null, null));
  }

  @Test
  public void testGetPolicySummary_ImplicitApplicationFilter_Authorized() {
    createPolicyViolation(app.getId());
    grantReadPermission(app.getId());
    assertResults(policySummaryService.getPolicySummary(null, null, null, null, null));

  }

  private PolicyViolation createPolicyViolation(String appId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "test scan id");
    return tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app.getId(), "test policy name"));
  }

  private void assertResults(PolicySummaryDTO policySummaryDTO) {
    assertThat(policySummaryDTO.weeklyDeltaNew, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(policySummaryDTO.weeklyDeltaNew.get(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1), is(1));
    assertThat(policySummaryDTO.weeklyDeltaFixed, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(policySummaryDTO.weeklyDeltaFixed.get(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1), is(0));
    assertThat(policySummaryDTO.weeklyDeltaUnresolved, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(policySummaryDTO.weeklyDeltaUnresolved.get(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1), is(1));
  }

  private void assertEmptyResults(PolicySummaryDTO policySummaryDTO) {
    assertThat(policySummaryDTO.weeklyDeltaNew, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(policySummaryDTO.weeklyDeltaNew.get(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1), is(0));
    assertThat(policySummaryDTO.weeklyDeltaFixed, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(policySummaryDTO.weeklyDeltaFixed.get(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1), is(0));
    assertThat(policySummaryDTO.weeklyDeltaUnresolved, hasSize(PolicySummaryService.POLICY_SUMMARY_WEEKS));
    assertThat(policySummaryDTO.weeklyDeltaUnresolved.get(PolicySummaryService.POLICY_SUMMARY_WEEKS - 1), is(0));
  }
}
