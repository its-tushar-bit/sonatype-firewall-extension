/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ApplicationRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationRiskService applicationRiskService;

  @Before
  public void setup() {
    tempEntity.newPolicyViolation(tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId"),
        tempEntity.newPolicy(org.getId(), "policy", 3));
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthenticated() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 1);
    assertThat(result.dashboardResults, hasSize(0));
    assertThat(result.numResults, is(0));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService.getApplicationRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 1);
    assertThat(result.dashboardResults, hasSize(0));
    assertThat(result.numResults, is(0));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 1);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Unauthenticated() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 1);
    assertThat(result.dashboardResults, hasSize(0));
    assertThat(result.numResults, is(0));
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 1);
    assertThat(result.dashboardResults, hasSize(0));
    assertThat(result.numResults, is(0));
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(app.getParentOwnerId());
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService
        .getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 1);
    assertThat(result.dashboardResults, hasSize(1));
    assertThat(result.numResults, is(1));
  }
}
