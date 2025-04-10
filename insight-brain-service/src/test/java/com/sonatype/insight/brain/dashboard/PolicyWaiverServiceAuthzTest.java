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
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class PolicyWaiverServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private RisksFilterDTOBuilder risksFilterDTOBuilder;

  @Inject
  private PolicyWaiverService dashboardPolicyWaiverService;

  private Organization parentOrg;

  @Before
  public void before() {
    parentOrg = tempEntity.newOrganization();
    org = tempEntity.newOrganization(parentOrg);
    app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(org);
    tempEntity.newWaiver(policy.getId(), app.getId());
    tempEntity.newWaiver(policy.getId(), org.getId());
    tempEntity.newWaiver(policy.getId(), parentOrg.getId());
    tempEntity.newWaiver(policy.getId(), REPOSITORY_CONTAINER_ID);
    tempEntity.newWaiver(policy.getId(), repository.getId());
    tempEntity.newWaiver(policy.getId(), ROOT_ORGANIZATION_ID);

    risksFilterDTOBuilder = new RisksFilterDTOBuilder().withApplicationIds(Collections.emptySet())
        .withOrganizationIds(Collections.emptySet()).withPageSize(10);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitApplicationFilter_Unauthenticated() {
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitApplicationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(app.getId(), org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ImplicitAllWaiversFilter_Unauthenticated() {
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ImplicitAllWaiversFilter_Unauthorized() {
    login();
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ImplicitAllWaiversFilter_Authorized() {
    grantReadPermission(ROOT_ORGANIZATION_ID);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId).containsExactlyInAnyOrder(
        app.getId(), org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID, REPOSITORY_CONTAINER_ID,
        repository.getId());
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitOrganizationFilter_Unauthenticated() {
    risksFilterDTOBuilder
        .withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitOrganizationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder
        .withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(org.getId());
    risksFilterDTOBuilder
        .withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitParentOrganizationFilter_Authorized() {
    grantReadPermission(parentOrg.getId());

    risksFilterDTOBuilder
        .withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isEqualTo(false);
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitRepositoryFilter_Unauthenticated() {
    risksFilterDTOBuilder
        .withRepositoryIds(Collections.singleton(repository.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitRepositoryFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder
        .withRepositoryIds(Collections.singleton(repository.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitRepositoryFilter_Authorized() {
    grantReadPermission(repository.getId());
    risksFilterDTOBuilder
        .withRepositoryIds(Collections.singleton(repository.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId).containsExactlyInAnyOrder(
        repository.getId(), REPOSITORY_CONTAINER_ID, ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ExplicitRepositoryContainerFilter_Authorized() {
    grantReadPermission(REPOSITORY_CONTAINER_ID);
    risksFilterDTOBuilder
        .withRepositoryIds(Collections.singleton(REPOSITORY_CONTAINER_ID));
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(REPOSITORY_CONTAINER_ID, ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaivers_ImplicitExpirationFilter_Unauthenticated() {
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId())).withExpirationDate(NEVER);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ImplicitExpirationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId())).withExpirationDate(NEVER);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaivers_ImplicitExpirationFilter_Authorized() {
    grantReadPermission(app.getId());
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId())).withExpirationDate(NEVER);
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaivers.hasNextPage).isFalse();
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(app.getId(), org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);
  }
}
