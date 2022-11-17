/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import javax.inject.Inject;

import com.sonatype.insight.brain.RisksFilterDTOBuilder;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardPolicyWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private RisksFilterDTOBuilder risksFilterDTOBuilder;

  @Inject
  private DashboardPolicyWaiverService dashboardPolicyWaiverService;

  @Before
  public void beforeEach() {
    Policy policy = tempEntity.newPolicy(org);
    tempEntity.newWaiver(policy.getId(), app.getId());
    tempEntity.newWaiver(policy.getId(), org.getId());
    tempEntity.newWaiver(policy.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID);
    tempEntity.newWaiver(policy.getId(), repository.getId());
    tempEntity.newWaiver(policy.getId(), Organization.ROOT_ORGANIZATION_ID);

    risksFilterDTOBuilder = new RisksFilterDTOBuilder().withApplicationIds(Collections.emptySet())
        .withOrganizationIds(Collections.emptySet()).withMaxResults(10);
  }

  @Test
  public void getDashboardPolicyWaivers_ExplicitApplicationFilter_Unauthenticated() {
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ExplicitApplicationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults)
        .as("At app level we should get the app the parent org and the root org")
        .isEqualTo(3);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(3);
  }

  @Test
  public void getDashboardPolicyWaivers_ImplicitAllWaiversFilter_Unauthenticated() {
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ImplicitAllWaiversFilter_Unauthorized() {
    login();
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ImplicitAllWaiversFilter_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults)
        .as("We should get the apps, the parent orgs, the root org and the repositories that the user have permission")
        .isEqualTo(5);
    assertThat(dashboardPolicyWaivers.dashboardResults).hasSize(5);
  }

  @Test
  public void getDashboardPolicyWaivers_ExplicitOrganizationFilter_Unauthenticated() {
    risksFilterDTOBuilder
        .withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ExplicitOrganizationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder
        .withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(org.getId());
    risksFilterDTOBuilder
        .withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults)
        .as("At the org level we should get the org and the root org")
        .isEqualTo(2);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(2);
  }

  @Test
  public void getDashboardPolicyWaivers_ImplicitExpirationFilter_Unauthenticated() {
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId())).withExpirationDate(NEVER);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ImplicitExpirationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId())).withExpirationDate(NEVER);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults).isEqualTo(0);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(0);
  }

  @Test
  public void getDashboardPolicyWaivers_ImplicitExpirationFilter_Authorized() {
    grantReadPermission(app.getId());
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId())).withExpirationDate(NEVER);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.numResults)
        .as("At app level we should get the app the parent org and the root org")
        .isEqualTo(3);
    assertThat(dashboardPolicyWaivers.dashboardResults.size()).isEqualTo(3);
  }
}
