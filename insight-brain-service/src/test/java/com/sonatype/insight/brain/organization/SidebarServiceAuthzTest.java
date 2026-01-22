/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryContainerDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryManagerDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class SidebarServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SidebarService sidebarService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetOwnerDetails_Unauthenticated() {
    sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetOwnerDetails_Unauthorized() {
    login();
    sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetOwnerDetails_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetOwnerList() {
    Organization organization1 = tempEntity.newOrganization("org1");
    Application application1 = tempEntity.newApplication(organization1.getId());
    Organization organizationChildOrg = tempEntity.newOrganization("org1 child1", organization1);
    Application organizationChildOrgApplication = tempEntity.newApplication(organizationChildOrg.getId());
    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    Repository repository1 = tempEntity.newRepository(repositoryManager1, "testPublicId1");

    // no permissions
    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).isEmpty();

    // Limited permissions. Returns a synthetic common ancestor organization
    grantReadPermission(organizationChildOrgApplication.getId());
    grantReadPermission(application1.getId());
    grantReadPermission(repositoryManager1.getId());
    grantReadPermission(repository1.getId());

    ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap.keySet()).containsExactlyInAnyOrder(
        organization1.getId(), organizationChildOrg.getId(), REPOSITORY_CONTAINER_ID,
        organizationChildOrgApplication.getPublicId(), application1.getPublicId(), repositoryManager1.getId(),
        repository1.getId());

    OwnerHierarchyOrganizationDTO rootOrganizationDTO = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        ownerHierarchyDTO.topParentOrganizationId
    );
    assertThat(rootOrganizationDTO.id).isEqualTo(organization1.getId());
    assertThat(rootOrganizationDTO.organizationIds).containsExactly(organizationChildOrg.getId());
    assertThat(rootOrganizationDTO.synthetic).isTrue();
    assertThat(rootOrganizationDTO.applicationIds).containsExactly(application1.getPublicId());

    OwnerHierarchyApplicationDTO applicationDTO = (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(
        rootOrganizationDTO.applicationIds.get(0)
    );
    assertThat(applicationDTO.id).isEqualTo(application1.getId());

    OwnerHierarchyRepositoryContainerDTO repositoryContainerDTO =
        (OwnerHierarchyRepositoryContainerDTO) ownerHierarchyDTO.ownersMap.get(REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainerDTO.id).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainerDTO.repositoryManagerIds).containsExactly(repositoryManager1.getId());

    OwnerHierarchyRepositoryManagerDTO repositoryManagerDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManager1.getId());
    assertThat(repositoryManagerDTO.id).isEqualTo(repositoryManager1.getId());
    assertThat(repositoryManagerDTO.repositoryIds).containsExactly(repository1.getId());

    OwnerHierarchyRepositoryDTO repositoryDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repository1.getId());
    assertThat(repositoryDTO.id).isEqualTo(repository1.getId());

    // full permission. Returns root organization
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    ownerHierarchyDTO = sidebarService.getOwnerList();

    rootOrganizationDTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(ownerHierarchyDTO.topParentOrganizationId);
    assertThat(rootOrganizationDTO.organizationIds).containsExactlyInAnyOrder(org.getId(), organization1.getId());
    assertThat(rootOrganizationDTO.synthetic).isFalse();

    OwnerHierarchyOrganizationDTO organizationDTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(org.getId());
    assertThat(organizationDTO.id).isEqualTo(org.getId());
    assertThat(organizationDTO.synthetic).isFalse();
    assertThat(organizationDTO.applicationIds).containsExactly(app.getPublicId());
    OwnerHierarchyOrganizationDTO organizationDTO1 =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(organization1.getId());
    assertThat(organizationDTO1.id).isEqualTo(organization1.getId());
    assertThat(organizationDTO1.synthetic).isFalse();
    assertThat(organizationDTO1.applicationIds).containsExactly(application1.getPublicId());

    applicationDTO =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(organizationDTO.applicationIds.get(0));
    assertThat(applicationDTO.id).isEqualTo(app.getId());
    OwnerHierarchyApplicationDTO applicationDTO1 =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(organizationDTO1.applicationIds.get(0));
    assertThat(applicationDTO1.id).isEqualTo(application1.getId());

    repositoryContainerDTO =
        (OwnerHierarchyRepositoryContainerDTO) ownerHierarchyDTO.ownersMap.get(REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainerDTO.id).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainerDTO.repositoryManagerIds).containsExactlyInAnyOrder(repositoryManager.getId(),
        repositoryManager1.getId());

    repositoryManagerDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManager.getId());
    assertThat(repositoryManagerDTO.id).isEqualTo(repositoryManager.getId());
    assertThat(repositoryManagerDTO.repositoryIds).containsExactly(repository.getId());
    OwnerHierarchyRepositoryManagerDTO repositoryManagerDTO1 =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManager1.getId());
    assertThat(repositoryManagerDTO1.id).isEqualTo(repositoryManager1.getId());
    assertThat(repositoryManagerDTO1.repositoryIds).containsExactly(repository1.getId());

    repositoryDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repository.getId());
    assertThat(repositoryDTO.id).isEqualTo(repository.getId());
    OwnerHierarchyRepositoryDTO repositoryDTO1 =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repository1.getId());
    assertThat(repositoryDTO1.id).isEqualTo(repository1.getId());
  }

  /**
   * Test the following structure with only permissions at app level No permissions in any org or at the root org
   *
   * <pre>
   * * ROOT (ORG)
   *     * Org1 (ORG)
   *         * Org1_App1 (APP)
   *     * Org2 (ORG)
   *         * Org2_App1 (APP)
   *         * Org2_Org1 (ORG)
   *             * Org2_Org1_App1 (APP)
   * </pre>
   */
  @Test
  public void testGetOwnerList_multiOrgWithOnlyViewAtAppLevel() {
    Organization organization1 = tempEntity.newOrganization("Org1");
    Application applicationInsideOrg1 = tempEntity.newApplication("Org1_App1", organization1.getId());

    Organization organization2 = tempEntity.newOrganization("Org2");
    Application applicationInsideOrg2 = tempEntity.newApplication("Org2_App1", organization2.getId());
    Organization organizationInsideOrg2 = tempEntity.newOrganization("Org2_Org1", organization2);
    Application applicationInsideChildOfOrg2 =
        tempEntity.newApplication("Org2_Org1_App1", organizationInsideOrg2.getId());

    grantReadPermission(applicationInsideOrg1.getId());
    grantReadPermission(applicationInsideOrg2.getId());
    grantReadPermission(applicationInsideChildOfOrg2.getId());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).isNotEmpty();

    // All apps should appear and all orgs, including the root org, should be synthetic
    OwnerHierarchyOrganizationDTO rootOrganizationDTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(Organization.ROOT_ORGANIZATION_ID);
    assertThat(rootOrganizationDTO.synthetic).isTrue();
    assertThat(rootOrganizationDTO.organizationIds).containsOnlyOnce(organization1.getId(), organization2.getId());

    OwnerHierarchyOrganizationDTO org1DTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(organization1.getId());
    assertThat(org1DTO.synthetic).isTrue();
    assertThat(org1DTO.organizationIds).isEmpty();
    assertThat(org1DTO.applicationIds).containsOnlyOnce(applicationInsideOrg1.getPublicId());

    OwnerHierarchyApplicationDTO app1DTO =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(applicationInsideOrg1.getPublicId());
    assertThat(app1DTO).isNotNull();

    OwnerHierarchyOrganizationDTO org2DTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(organization2.getId());
    assertThat(org2DTO.synthetic).isTrue();
    assertThat(org2DTO.organizationIds).containsOnlyOnce(organizationInsideOrg2.getId());
    assertThat(org2DTO.applicationIds).containsOnlyOnce(applicationInsideOrg2.getPublicId());

    OwnerHierarchyApplicationDTO app2DTO =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(applicationInsideOrg2.getPublicId());
    assertThat(app2DTO).isNotNull();

    OwnerHierarchyOrganizationDTO orgInsideOrg2DTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(organizationInsideOrg2.getId());
    assertThat(orgInsideOrg2DTO.synthetic).isTrue();
    assertThat(orgInsideOrg2DTO.organizationIds).isEmpty();
    assertThat(orgInsideOrg2DTO.applicationIds).containsOnlyOnce(applicationInsideChildOfOrg2.getPublicId());

    OwnerHierarchyApplicationDTO deepestApp =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(applicationInsideChildOfOrg2.getPublicId());
    assertThat(deepestApp).isNotNull();
  }

  @Test
  public void testGetOwnersList_onlyPermissionsForRepositories() {
    // Other hierarchy the user has no access to, but exists
    Organization organization1 = tempEntity.newOrganization("Org1");
    tempEntity.newApplication("Org1_App1", organization1.getId());

    RepositoryManager repositoryManager1 = tempEntity.newRepositoryManager();
    Repository repository1 = tempEntity.newRepository(repositoryManager1, "testPublicId1");
    grantReadPermission(repositoryManager1.getId());
    grantReadPermission(repository1.getId());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).isNotEmpty();
    assertThat(ownerHierarchyDTO.topParentOrganizationId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);

    assertThat(ownerHierarchyDTO.ownersMap.keySet()).containsExactlyInAnyOrder(
        repositoryManager1.getId(), REPOSITORY_CONTAINER_ID, repository1.getId());

    OwnerHierarchyRepositoryContainerDTO repositoryContainerDTO =
        (OwnerHierarchyRepositoryContainerDTO) ownerHierarchyDTO.ownersMap.get(REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainerDTO).isNotNull();
    assertThat(repositoryContainerDTO.repositoryManagerIds).containsExactly(repositoryManager1.getId());

    OwnerHierarchyRepositoryManagerDTO repositoryManagerDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManager1.getId());
    assertThat(repositoryManagerDTO).isNotNull();
    assertThat(repositoryManagerDTO.repositoryIds).containsExactly(repository1.getId());

    OwnerHierarchyRepositoryDTO repositoryDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repository1.getId());
    assertThat(repositoryDTO).isNotNull();
  }

  @Test
  public void testGetOwnersList_noPermissions_returnsEmptyMapButRootOrganizationAsTop() {
    // Other hierarchy the user has no access to, but exists
    Organization organization1 = tempEntity.newOrganization("Org1");
    tempEntity.newApplication("Org1_App1", organization1.getId());
    Organization organization2 = tempEntity.newOrganization("Org2");
    tempEntity.newApplication("Org2_App1", organization2.getId());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).isEmpty();
    assertThat(ownerHierarchyDTO.topParentOrganizationId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }
}
