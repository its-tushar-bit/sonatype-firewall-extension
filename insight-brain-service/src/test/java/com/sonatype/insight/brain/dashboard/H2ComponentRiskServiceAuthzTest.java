/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class H2ComponentRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private H2ComponentRiskService componentRiskService;

  @Before
  public void init() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan id");
    tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app));
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthenticated() {
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService.getComponentRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService.getComponentRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService.getComponentRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthenticated() {
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = componentRiskService
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitOrganizationFilter_Unauthenticated() {
    DashboardResultsDTO<ComponentRiskDTO> result =
        componentRiskService.getComponentRisks(Collections.singleton(org.getId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitOrganizationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ComponentRiskDTO> result =
        componentRiskService.getComponentRisks(Collections.singleton(org.getId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(org.getId());
    DashboardResultsDTO<ComponentRiskDTO> result =
        componentRiskService.getComponentRisks(Collections.singleton(org.getId()), null, null, null, null, null, null,
            "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }
}
