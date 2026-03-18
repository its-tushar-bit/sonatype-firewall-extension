/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.RisksFilterDTOBuilder;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class DashboardPolicyWaiverRequestServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private RisksFilterDTOBuilder risksFilterDTOBuilder;

  @Inject
  private DashboardPolicyWaiverRequestService dashboardPolicyWaiverRequestService;

  private Organization parentOrg;

  @Before
  public void before() {
    parentOrg = tempEntity.newOrganization();
    org = tempEntity.newOrganization(parentOrg);
    app = tempEntity.newApplication(org.getId());
    Policy policy = tempEntity.newPolicy(org);
    createPolicyWaiverRequest(policy.getId(), app.getId());
    createPolicyWaiverRequest(policy.getId(), org.getId());
    createPolicyWaiverRequest(policy.getId(), parentOrg.getId());
    createPolicyWaiverRequest(policy.getId(), REPOSITORY_CONTAINER_ID);
    createPolicyWaiverRequest(policy.getId(), repository.getId());
    createPolicyWaiverRequest(policy.getId(), repositoryManager.getId());
    createPolicyWaiverRequest(policy.getId(), ROOT_ORGANIZATION_ID);

    risksFilterDTOBuilder = new RisksFilterDTOBuilder().withApplicationIds(Collections.emptySet())
        .withOrganizationIds(Collections.emptySet())
        .withPageSize(10);
  }

  private PolicyWaiverRequest createPolicyWaiverRequest(String policyId, String ownerId) {
    PolicyWaiverRequest policyWaiverRequest =
        new PolicyWaiverRequest().setOwnerId(ownerId).setPolicyId(policyId);
    tempEntity.newPolicyWaiverRequest(policyWaiverRequest);
    return policyWaiverRequest;
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitApplicationFilter_Unauthenticated() {
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitApplicationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitApplicationFilter_Authorized() {
    grantReadPermission(app.getId());
    risksFilterDTOBuilder.withApplicationIds(Collections.singleton(app.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(app.getId(), org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ImplicitAllWaiversFilter_Unauthenticated() {
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ImplicitAllWaiversFilter_Unauthorized() {
    login();
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ImplicitAllWaiversFilter_Authorized() {
    grantReadPermission(app.getId());
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(app.getId(), org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);

    grantReadPermission(ROOT_ORGANIZATION_ID);
    dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(
            app.getId(), org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID, repository.getId(),
            repositoryManager.getId(), REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitOrganizationFilter_Unauthenticated() {
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitOrganizationFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitOrganizationFilter_Authorized() {
    grantReadPermission(org.getId());
    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitParentOrganizationFilter_Authorized() {
    grantReadPermission(parentOrg.getId());

    risksFilterDTOBuilder.withOrganizationIds(Collections.singleton(org.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(org.getId(), parentOrg.getId(), ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitRepositoryFilter_Unauthenticated() {
    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitRepositoryFilter_Unauthorized() {
    login();
    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).isEmpty();
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitRepositoryFilter_Authorized() {
    grantReadPermission(repository.getId());
    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repository.getId()));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(
            ROOT_ORGANIZATION_ID, repository.getId(), repositoryManager.getId(), REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetDashboardPolicyWaiverRequests_ExplicitRepositoryContainerFilter_Authorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(RepositoryContainer.REPOSITORY_CONTAINER_ID));
    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> dashboardPolicyWaiverRequests =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTOBuilder.build());
    assertThat(dashboardPolicyWaiverRequests.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(ROOT_ORGANIZATION_ID, REPOSITORY_CONTAINER_ID);
  }
}
