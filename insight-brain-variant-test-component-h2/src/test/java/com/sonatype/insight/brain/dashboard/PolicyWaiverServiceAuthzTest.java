/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.RisksFilterDTOBuilder;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.dashboard.ExpirationDate.NEVER;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class PolicyWaiverServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private RisksFilterDTOBuilder risksFilterDTOBuilder;

  @Inject
  private PolicyWaiverService dashboardPolicyWaiverService;

  private Organization parentOrg;

  @BeforeEach
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
        .withOrganizationIds(Collections.emptySet())
        .withPageSize(10);
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
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(
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
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .containsExactlyInAnyOrder(
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

  /**
   * When filtering by specific repository, user sees parent RM waivers regardless of RM permission. This verifies that
   * if a user has read permission on a repository, they automatically see waivers from the parent repository manager,
   * even without explicit RM permission. This is by design: repository permission implies access to parent RM waivers.
   */
  @Test
  public void testGetDashboardPolicyWaivers_RepositoryFilterIncludesParentRepositoryManagerWaivers() {
    // Create a repository manager with a repository
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repoWithManager = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(org);

    // Create waiver at Repository level
    tempEntity.newWaiver(policy.getId(), repoWithManager.getId());

    // Create waiver at RepositoryManager level
    tempEntity.newWaiver(policy.getId(), repositoryManager.getId());

    // Grant read permission ONLY on the repository (NOT on repository manager)
    grantReadPermission(repoWithManager.getId());

    // Filter by repository
    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repoWithManager.getId()));

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should return repository waiver AND repository manager waiver
    // Because repository permission implies access to parent RM waivers
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .contains(repoWithManager.getId(), repositoryManager.getId(),
            REPOSITORY_CONTAINER_ID, ROOT_ORGANIZATION_ID);
  }

  /**
   * Scoped user with READ on a repository only (no RM permission, no explicit filter) sees RM-scoped waivers.
   * NEXUS-52341: when the user has repo-level READ but not RM-level READ, waivers at the parent RM should still
   * be visible in the unfiltered dashboard view.
   */
  @Test
  public void testGetDashboardPolicyWaivers_ScopedUserWithRepoPermissionSeesParentRMWaivers() {
    RepositoryManager rm = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(rm);
    Policy policy = tempEntity.newPolicy(org);

    // Waiver is at RM scope (e.g. applied at Repository Manager level)
    tempEntity.newWaiver(policy.getId(), rm.getId());

    // Grant READ only on the child repository — not on the RM
    grantReadPermission(repo.getId());

    // No explicit filter — this is the "show all" default dashboard view
    DashboardResultsDTO<DashboardPolicyWaiverDTO> result =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Scoped user should see the RM waiver because they have READ on a child repo
    assertThat(result.dashboardResults).extracting(dto -> dto.ownerId)
        .contains(rm.getId());
  }

  /**
   * User with partial Repository Manager permissions sees only authorized RM waivers. This verifies the
   *
   * @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY_MANAGER) annotation correctly filters
   *                         repository manager waivers based on specific RM permissions.
   */
  @Test
  public void testGetDashboardPolicyWaivers_PartialRepositoryManagerPermissions() {
    // Create two repository managers with repositories
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();

    Policy policy = tempEntity.newPolicy(org);

    // Create waivers at RepositoryManager level
    tempEntity.newWaiver(policy.getId(), repositoryManager1.getId());
    tempEntity.newWaiver(policy.getId(), repositoryManager2.getId());

    // Grant READ permission ONLY on repositoryManager1 (not on repositoryManager2)
    grantReadPermission(repositoryManager1.getId());

    // Use empty filter (no specific repository/app/org filter) - queries all waivers
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should include RM1 waiver (user has permission)
    // Should NOT include RM2 waiver (user lacks permission)
    // @AuthzFilter should filter out RM2
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .contains(repositoryManager1.getId())
        .doesNotContain(repositoryManager2.getId());
  }

  /**
   * User with full Repository Manager permissions sees all RM waivers. This verifies that when a user has appropriate
   * RM permissions, they can see RM waivers.
   */
  @Test
  public void testGetDashboardPolicyWaivers_WithRepositoryManagerPermissions() {
    // Create two repository managers with repositories
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    RepositoryManager repositoryManager2 = tempEntity.newRepositoryManager();

    Policy policy = tempEntity.newPolicy(org);

    // Create waivers at RepositoryManager level
    tempEntity.newWaiver(policy.getId(), repositoryManager1.getId());
    tempEntity.newWaiver(policy.getId(), repositoryManager2.getId());

    // Grant READ permission on BOTH repository managers
    grantReadPermission(repositoryManager1.getId());
    grantReadPermission(repositoryManager2.getId());

    // Use empty filter (no specific repository/app/org filter) - queries all waivers
    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should include BOTH RM waivers since user has permission on both
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .contains(repositoryManager1.getId(), repositoryManager2.getId());
  }

  /**
   * Multiple repositories under same RM - filtering by one repo includes parent RM waiver. This verifies that when a
   * user has permission on ANY repository under an RM, they see the parent RM waivers when filtering by that
   * repository.
   */
  @Test
  public void testGetDashboardPolicyWaivers_MultipleReposUnderSameRM() {
    // Create one repository manager with multiple repositories
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(repositoryManager);
    Repository repo2 = tempEntity.newRepository(repositoryManager);
    Repository repo3 = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(org);

    // Create waivers at Repository level
    tempEntity.newWaiver(policy.getId(), repo1.getId());
    tempEntity.newWaiver(policy.getId(), repo2.getId());
    tempEntity.newWaiver(policy.getId(), repo3.getId());

    // Create waiver at RepositoryManager level
    tempEntity.newWaiver(policy.getId(), repositoryManager.getId());

    // Grant READ permission ONLY on repo1 (not on repo2, repo3, or RM)
    grantReadPermission(repo1.getId());

    // Filter by repo1
    risksFilterDTOBuilder.withRepositoryIds(Collections.singleton(repo1.getId()));

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should include repo1 waiver AND parent RM waiver
    // Should NOT include repo2 or repo3 waivers (no permission)
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .contains(repo1.getId(), repositoryManager.getId(), REPOSITORY_CONTAINER_ID, ROOT_ORGANIZATION_ID)
        .doesNotContain(repo2.getId(), repo3.getId());
  }

  /**
   * User with permission on multiple repos under same RM sees RM waiver only once. This verifies deduplication of RM
   * waivers when filtering by multiple repositories under the same repository manager.
   */
  @Test
  public void testGetDashboardPolicyWaivers_RMWaiverAppearsOnceWithMultipleRepos() {
    // Create one repository manager with multiple repositories
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repo1 = tempEntity.newRepository(repositoryManager);
    Repository repo2 = tempEntity.newRepository(repositoryManager);

    Policy policy = tempEntity.newPolicy(org);

    // Create waivers at Repository level
    tempEntity.newWaiver(policy.getId(), repo1.getId());
    tempEntity.newWaiver(policy.getId(), repo2.getId());

    // Create waiver at RepositoryManager level
    tempEntity.newWaiver(policy.getId(), repositoryManager.getId());

    // Grant READ permission on BOTH repositories
    grantReadPermission(repo1.getId());
    grantReadPermission(repo2.getId());

    // Filter by BOTH repositories
    risksFilterDTOBuilder.withRepositoryIds(Set.of(repo1.getId(), repo2.getId()));

    DashboardResultsDTO<DashboardPolicyWaiverDTO> dashboardPolicyWaivers =
        dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTOBuilder.build());

    // Should include both repo waivers and RM waiver
    assertThat(dashboardPolicyWaivers.dashboardResults).extracting(dto -> dto.ownerId)
        .contains(repo1.getId(), repo2.getId(), repositoryManager.getId());

    // Verify RM waiver appears exactly ONCE (not duplicated for each repository)
    long rmWaiverCount = dashboardPolicyWaivers.dashboardResults.stream()
        .filter(dto -> repositoryManager.getId().equals(dto.ownerId))
        .count();
    assertThat(rmWaiverCount).isEqualTo(1);
  }
}
