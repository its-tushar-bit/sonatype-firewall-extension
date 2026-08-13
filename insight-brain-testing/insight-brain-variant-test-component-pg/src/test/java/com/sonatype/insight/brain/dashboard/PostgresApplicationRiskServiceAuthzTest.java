/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.variant.AbstractComponentPgAuthzTest;
import com.sonatype.insight.brain.variant.ComponentPgTest;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentPgTest
public class PostgresApplicationRiskServiceAuthzTest
    extends AbstractComponentPgAuthzTest
{
  @Inject
  private PostgresApplicationRiskService applicationRiskService;

  protected ApplicationRiskService getApplicationRiskService() {
    return applicationRiskService;
  }

  @BeforeEach
  public void setup() {
    tempEntity.newPolicyViolation(tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId"),
        tempEntity.newPolicy(org));
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthenticated() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        getApplicationRiskService().getApplicationRisks(null, Collections.singleton(app.getId()), null, null, null,
            null,
            null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = getApplicationRiskService().getApplicationRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        getApplicationRiskService().getApplicationRisks(null, Collections.singleton(app.getId()), null, null, null,
            null,
            null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Unauthenticated() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        getApplicationRiskService().getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null,
            null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        getApplicationRiskService().getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null,
            null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.hasNextPage).isEqualTo(false);
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(app.getParentOwnerId());
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        getApplicationRiskService().getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null,
            null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.hasNextPage).isEqualTo(false);
  }
}
