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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class H2NewestRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private H2NewestRiskService newestRiskService;

  @Test
  public void testGetNewestRisks_ExplicitApplicationFilter_Unauthenticated() {
    createFirstOccurrencePolicyViolation(app.getId());
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ExplicitApplicationFilter_Unauthorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    login();
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ExplicitApplicationFilter_Authorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    grantReadPermission(app.getId());
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(null, Collections.singleton(app.getId()), null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ExplicitOrganizationFilter_Unauthenticated() {
    createFirstOccurrencePolicyViolation(app.getId());
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(Collections.singleton(org.getId()), null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ExplicitOrganizationFilter_Unauthorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    login();
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(Collections.singleton(org.getId()), null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ExplicitOrganizationFilter_Authorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    grantReadPermission(org.getId());
    DashboardResultsDTO<NewestRiskDTO> result = newestRiskService
        .getNewestRisks(Collections.singleton(org.getId()), null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ImplicitApplicationFilter_Unauthenticated() {
    createFirstOccurrencePolicyViolation(app.getId());
    DashboardResultsDTO<NewestRiskDTO> result =
        newestRiskService.getNewestRisks(null, null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ImplicitApplicationFilter_Unauthorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    login();
    DashboardResultsDTO<NewestRiskDTO> result =
        newestRiskService.getNewestRisks(null, null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetNewestRisks_ImplicitApplicationFilter_Authorized() {
    createFirstOccurrencePolicyViolation(app.getId());
    grantReadPermission(app.getId());
    DashboardResultsDTO<NewestRiskDTO> result =
        newestRiskService.getNewestRisks(null, null, null, null, null, null, null, null,
            DashboardFilterDTO.DEFAULT_MAX_DAYS_OLD, 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  private void createFirstOccurrencePolicyViolation(String appId) {
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(appId, BuildStageType.ID, "test scan id");
    tempEntity.newPolicyViolation(evaluation, tempEntity.newPolicy(app));
  }
}
