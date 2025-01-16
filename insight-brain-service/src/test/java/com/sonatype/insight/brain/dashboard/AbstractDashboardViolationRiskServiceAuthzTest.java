/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractDashboardViolationRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  protected abstract DashboardViolationRiskService getDashboardViolationRiskService();

  @Test
  public void testGet_ExplicitApplicationFilter_Unauthenticated() {
    createFirstOccurrencePolicyViolation(app.getId());
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ExplicitApplicationFilter_Unauthorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    login();
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ExplicitApplicationFilter_Authorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    grantReadPermission(app.getId());
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(null, Collections.singleton(app.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ExplicitOrganizationFilter_Unauthenticated() {
    createFirstOccurrencePolicyViolation(app.getId());
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(Collections.singleton(org.getId()), null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ExplicitOrganizationFilter_Unauthorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    login();
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(Collections.singleton(org.getId()), null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ExplicitOrganizationFilter_Authorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    grantReadPermission(org.getId());
    DashboardResultsDTO<DashboardViolationRiskDTO> result = getDashboardViolationRiskService()
        .get(Collections.singleton(org.getId()), null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ImplicitApplicationFilter_Unauthenticated() {
    createFirstOccurrencePolicyViolation(app.getId());
    DashboardResultsDTO<DashboardViolationRiskDTO> result =
        getDashboardViolationRiskService().get(null, null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ImplicitApplicationFilter_Unauthorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    login();
    DashboardResultsDTO<DashboardViolationRiskDTO> result =
        getDashboardViolationRiskService().get(null, null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGet_ImplicitApplicationFilter_Authorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    grantReadPermission(app.getId());
    DashboardResultsDTO<DashboardViolationRiskDTO> result =
        getDashboardViolationRiskService().get(null, null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  private void createFirstOccurrencePolicyViolation(String appId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "test scan id");
    tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app));
  }
}
