/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.variant.AbstractComponentPgAuthzTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentPgTest
public class PostgresComponentRiskServiceAuthzTest
    extends AbstractComponentPgAuthzTest
{
  @Inject
  private PostgresComponentRiskService componentRiskService;

  protected DashboardComponentRiskService getComponentRiskService() {
    return componentRiskService;
  }

  @BeforeEach
  public void init() {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "test scan id");
    tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app));
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthenticated() {
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService().getComponentRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService().getComponentRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService().getComponentRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthenticated() {
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ImplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<ComponentRiskDTO> result = getComponentRiskService()
        .getComponentRisks(null, null, null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitOrganizationFilter_Unauthenticated() {
    DashboardResultsDTO<ComponentRiskDTO> result =
        getComponentRiskService()
            .getComponentRisks(Collections.singleton(org.getId()), null, null, null, null, null, null,
                "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitOrganizationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ComponentRiskDTO> result =
        getComponentRiskService()
            .getComponentRisks(Collections.singleton(org.getId()), null, null, null, null, null, null,
                "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetComponentRisks_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(org.getId());
    DashboardResultsDTO<ComponentRiskDTO> result =
        getComponentRiskService()
            .getComponentRisks(Collections.singleton(org.getId()), null, null, null, null, null, null,
                "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }
}
