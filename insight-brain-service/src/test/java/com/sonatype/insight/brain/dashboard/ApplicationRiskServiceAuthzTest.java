/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Date;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.CIApplicationFilter;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationRiskServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationRiskService applicationRiskService;

  @Before
  public void setup() {
    tempEntity.newPolicyViolation(tempEntity
        .newPolicyEvaluation(app.getId(), BuildStageType.ID, "scanId"),
        tempEntity.newPolicy(org));
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "hash",
        ComponentIdentifier.createMavenCoordinates("g", "a", "1"));
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthenticated() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        applicationRiskService.getApplicationRisks(null, Collections.singleton(app.getId()), null, null, null, null,
            null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ApplicationRiskScoreDTO> result = applicationRiskService.getApplicationRisks(null,
        Collections.singleton(app.getId()), null, null, null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
  }

  @Test
  public void testGetApplicationRisks_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        applicationRiskService.getApplicationRisks(null, Collections.singleton(app.getId()), null, null, null, null,
            null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Unauthenticated() {
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        applicationRiskService.getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null,
            null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Unauthorized() {
    login();
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        applicationRiskService.getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null,
            null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isEqualTo(0);
  }

  @Test
  public void testGetApplicationRisks_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(app.getParentOwnerId());
    DashboardResultsDTO<ApplicationRiskScoreDTO> result =
        applicationRiskService.getApplicationRisks(Collections.singleton(app.getParentOwnerId()), null, null, null,
            null, null, null, "-TOTAL_RISK", 0, 100);
    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetCIApplicationRisks_Unauthenticated() {
    final CIApplicationFilter filter = new CIApplicationFilter(0, 100, new Date(1569553200000L));
    final DashboardResultsDTO<?> result = applicationRiskService.getCIApplicationRisk(filter);

    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isZero();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCIApplicationRisks_Unauthorized() {
    login();
    final CIApplicationFilter filter = new CIApplicationFilter(0, 100, new Date(1569553200000L));
    final DashboardResultsDTO<?> result = applicationRiskService.getCIApplicationRisk(filter);

    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isZero();
  }

  @Test
  public void testGetCIApplicationRisks_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    final CIApplicationFilter filter = new CIApplicationFilter(0, 100, new Date(1569553200000L));
    final DashboardResultsDTO<?> result = applicationRiskService.getCIApplicationRisk(filter);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_Unauthenticated() {
    final DashboardResultsDTO<?> result =
        applicationRiskService.getApplicationsWithAutomatedSourceControlFeedbackDisabledRisk(0, 100);

    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isZero();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_Unauthorized() {
    login();
    final DashboardResultsDTO<?> result =
        applicationRiskService.getApplicationsWithAutomatedSourceControlFeedbackDisabledRisk(0, 100);

    assertThat(result.dashboardResults).isEmpty();
    assertThat(result.numResults).isZero();
  }

  @Test
  public void testGetApplicationsWithAutomatedSourceControlFeedbackDisabledRisk_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    final DashboardResultsDTO<?> result =
        applicationRiskService.getApplicationsWithAutomatedSourceControlFeedbackDisabledRisk(0, 100);

    assertThat(result.dashboardResults).hasSize(1);
    assertThat(result.numResults).isEqualTo(1);
  }
}
