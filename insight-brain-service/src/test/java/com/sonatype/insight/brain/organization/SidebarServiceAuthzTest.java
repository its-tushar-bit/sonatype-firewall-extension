/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

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
}
