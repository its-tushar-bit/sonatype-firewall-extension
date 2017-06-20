/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.filters.PolicyViolationStateFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationState;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ApplicationRiskServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApplicationRiskService applicationRiskService;

  private Organization org;
  private Application app1;
  private Application app2;
  private Policy orgPolicy;
  private Policy app1Policy;
  private PolicyEvaluation app1PolicyEvaluation;
  private PolicyEvaluation app2PolicyEvaluation;
  private PolicyViolation orgPolicyViolation;
  private PolicyViolation app1PolicyViolation;
  private PolicyViolation app2PolicyViolation;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("app1", "app1", org.getId());
    app2 = tempEntity.newApplication("app2", "app2", org.getId());
    orgPolicy = tempEntity.newPolicy(org.getId(), "org owned policy", 3);
    app1Policy = tempEntity.newPolicy(app1.getId(), "app owned policy", 5);
    long time = System.currentTimeMillis() - 1000;
    app1PolicyEvaluation = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan app1 id",
        new Date(time));
    app2PolicyEvaluation = tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan app2 id",
        new Date(time + 1));
    orgPolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    app1PolicyViolation = tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    app2PolicyViolation = tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
    tempEntity.newFirstOccurrencePolicyViolation(orgPolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app1PolicyViolation.getId(), app1.getId(), BuildStageType.ID);
    tempEntity.newFirstOccurrencePolicyViolation(app2PolicyViolation.getId(), app2.getId(), BuildStageType.ID);
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash-1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-3", MatchState.SIMILAR, false);
    tempEntity.newApplicationComponent(app1.getId(), ReleaseStageType.ID, "hash-4", MatchState.UNKNOWN, false);
    tempEntity.newApplicationComponent(app2.getId(), BuildStageType.ID, "hash-2",
        ComponentIdentifier.createMavenCoordinates("g", "a", "2"));
  }

  @Test
  public void testGetApplicationRisks_FilterByStage_ExcludesDevelop() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), DevelopStageType.ID, "newScanIdApp1");
    tempEntity.newPolicyViolation(evaluation, app1Policy, app1Policy.getThreatLevel(), app1Policy.getThreatCategory(),
        "g", "a", "v", "somehash");

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(null, null, null, null, null, null, null, 100);
    assertThat(result.dashboardResults, hasSize(2));
    assertThat(result.numResults, is(2));
    assertThat(result.dashboardResults.get(0).getStageRiskScore(DevelopStageType.ID), is(nullValue()));
    assertThat(result.dashboardResults.get(1).getStageRiskScore(DevelopStageType.ID), is(nullValue()));

    try {
      applicationRiskService.getApplicationRisks(null, null, Collections.singleton(DevelopStageType.ID), null, null,
          null, null, 100);
      fail("Expected exception");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Invalid stage type: develop."));
    }
  }

  @Test
  public void testGetApplicationRisks_StagesInChronologicalOrder() throws Exception {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app1.getId(), OperateStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);
    evaluation = tempEntity.newPolicyEvaluation(app1.getId(), ReleaseStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);
    evaluation = tempEntity.newPolicyEvaluation(app1.getId(), StageReleaseStageType.ID, "scan app1 id");
    tempEntity.newPolicyViolation(evaluation, app1Policy);

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService.getApplicationRisks(null,
        Collections.singleton(app1.getId()),
        new LinkedHashSet<>(Arrays.asList(ReleaseStageType.ID, OperateStageType.ID, BuildStageType.ID,
            StageReleaseStageType.ID)), null, null, null, null, 100);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks, hasSize(4));
    assertThat(appDTO.stageRisks.get(0).stageTypeId, is(BuildStageType.ID));
    assertThat(appDTO.stageRisks.get(1).stageTypeId, is(StageReleaseStageType.ID));
    assertThat(appDTO.stageRisks.get(2).stageTypeId, is(ReleaseStageType.ID));
    assertThat(appDTO.stageRisks.get(3).stageTypeId, is(OperateStageType.ID));
  }

  @Test
  public void testGetApplicationRisks_ViolationForComponentWithoutHash() throws Exception {
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy, null, null, null, null, "unknown");

    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null, null, 100);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks, hasSize(1));
    assertThat(appDTO.stageRisks.get(0).stageTypeId, is(BuildStageType.ID));
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk,
        is(orgPolicy.getThreatLevel() + app1Policy.getThreatLevel() * 2));
  }

  @Test
  public void testGetApplicationRisks_FilterByPolicyViolationState() throws Exception {
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash1", app1Policy.getId(), app1.getId(), "Some comments here");
    tempEntity.newWaivedPolicyViolation(app1PolicyEvaluation, app1Policy, "gid", "aid", "1", "hash1", policyWaiver);
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED), 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    ApplicationRiskScoreDTO appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks, hasSize(1));
    assertThat(appDTO.stageRisks.get(0).stageTypeId, is(BuildStageType.ID));
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk, is(app1Policy.getThreatLevel()));

    result = applicationRiskService
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.OPEN), 1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks, hasSize(1));
    assertThat(appDTO.stageRisks.get(0).stageTypeId, is(BuildStageType.ID));
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk, is(orgPolicy.getThreatLevel() + app1Policy.getThreatLevel()));

    result = applicationRiskService
        .getApplicationRisks(null, Collections.singleton(app1.getId()), null, null, null, null,
            new PolicyViolationStateFilter(PolicyViolationState.WAIVED, PolicyViolationState.OPEN),
            1000);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
    appDTO = result.dashboardResults.get(0);
    assertThat(appDTO.stageRisks, hasSize(1));
    assertThat(appDTO.stageRisks.get(0).stageTypeId, is(BuildStageType.ID));
    assertThat(appDTO.stageRisks.get(0).risk.totalRisk,
        is(orgPolicy.getThreatLevel() + app1Policy.getThreatLevel() * 2));
  }

  @Test
  public void testGetApplicationRisks_ResultsCountCanExceedNumberOfReturnedResults() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(null, null, null, null, null, null, null, 1);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(2));
  }
}
