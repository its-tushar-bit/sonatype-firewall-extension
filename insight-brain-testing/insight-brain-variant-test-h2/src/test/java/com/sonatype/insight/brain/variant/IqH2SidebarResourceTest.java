/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.organization.OwnerDetailsDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryContainerDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryManagerDTO;
import com.sonatype.insight.brain.organization.SidebarResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2SidebarResourceTest
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private OrganizationDAO organizationDAO;

  @BeforeEach
  void setUp() {
    organizationDAO = ctx.lookup(OrganizationDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(SidebarResource.RESOURCE_PATH);
  }

  @Test
  void testGetOwnerList() throws Exception {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization orgOne = ctx.tempEntity().newOrganization();
    Application appOne = ctx.tempEntity().newApplication(orgOne.getId());
    Application appTwo = ctx.tempEntity().newApplication(orgOne.getId());
    Organization orgTwo = ctx.tempEntity().newOrganization(orgOne);
    Application appThree = ctx.tempEntity().newApplication(orgTwo.getId());
    RepositoryManager repositoryManagerOne = ctx.tempEntity().newRepositoryManager();
    Repository repositoryOne =
        ctx.tempEntity().newRepository(repositoryManagerOne, "repository-one", RepositoryType.proxy, "maven");
    RepositoryManager repositoryManagerTwo = ctx.tempEntity().newRepositoryManager();
    Repository repositoryTwo =
        ctx.tempEntity().newRepository(repositoryManagerTwo, "repository-two", RepositoryType.hosted, "npm");
    Repository repositoryThree =
        ctx.tempEntity().newRepository(repositoryManagerTwo, "repository-three", RepositoryType.proxy, "maven");

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    OwnerHierarchyDTO ownerHierarchyDTO = response.getBody(OwnerHierarchyDTO.class);
    assertThat(ownerHierarchyDTO).isNotNull();
    assertThat(ownerHierarchyDTO.ownersMap).hasSize(12);
    OwnerHierarchyOrganizationDTO rootOrg =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(ownerHierarchyDTO.topParentOrganizationId);

    // Root org
    assertThat(rootOrg.id).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(rootOrg.name).isEqualTo(rootOrganization.getName());
    assertThat(rootOrg.getParentId()).isNull();
    assertThat(rootOrg.applicationIds).isNull();
    assertThat(rootOrg.parentOrganizationId).isNull();
    assertThat(rootOrg.organizationIds).hasSize(1);
    assertThat(rootOrg.subOrgs).isEqualTo(2);
    assertThat(rootOrg.totalApps).isEqualTo(3);

    // first level organization
    OwnerHierarchyOrganizationDTO firstLevelOrg =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(rootOrg.organizationIds.get(0));
    assertThat(firstLevelOrg.parentOrganizationId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(firstLevelOrg.name).isEqualTo(orgOne.getName());
    assertThat(firstLevelOrg.getParentId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(firstLevelOrg.applicationIds).hasSize(2);
    assertThat(firstLevelOrg.organizationIds).hasSize(1);
    assertThat(firstLevelOrg.applicationIds.stream().anyMatch(id -> id.equals(appOne.getPublicId()))).isTrue();
    assertThat(firstLevelOrg.applicationIds.stream().anyMatch(id -> id.equals(appTwo.getPublicId()))).isTrue();
    assertThat(firstLevelOrg.subOrgs).isEqualTo(1);
    assertThat(firstLevelOrg.totalApps).isEqualTo(3);

    // second level organization
    OwnerHierarchyOrganizationDTO secondLevelOrg =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(firstLevelOrg.organizationIds.get(0));
    assertThat(secondLevelOrg.parentOrganizationId).isEqualTo(firstLevelOrg.id);
    assertThat(secondLevelOrg.name).isEqualTo(orgTwo.getName());
    assertThat(secondLevelOrg.getParentId()).isEqualTo(firstLevelOrg.id);
    assertThat(secondLevelOrg.applicationIds).hasSize(1);
    assertThat(secondLevelOrg.organizationIds).isEmpty();
    OwnerHierarchyApplicationDTO secondLevelOrgApplication =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(secondLevelOrg.applicationIds.get(0));
    assertThat(secondLevelOrgApplication.id).isEqualTo(appThree.getId());
    assertThat(secondLevelOrg.subOrgs).isEqualTo(0);
    assertThat(secondLevelOrg.totalApps).isEqualTo(1);

    // repository container
    OwnerHierarchyRepositoryContainerDTO repositoryContainer =
        (OwnerHierarchyRepositoryContainerDTO) ownerHierarchyDTO.ownersMap
            .get(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainer.id).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainer.name).isEqualTo(RepositoryContainer.SINGLETON.getName());
    assertThat(repositoryContainer.getParentId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(repositoryContainer.repositoryManagerIds).hasSize(2);
    assertThat(repositoryContainer.repositoryManagerIds).containsExactlyInAnyOrder(repositoryManagerOne.getId(),
        repositoryManagerTwo.getId());

    // repository manager one
    OwnerHierarchyRepositoryManagerDTO repositoryManagerOneDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManagerOne.getId());
    assertThat(repositoryManagerOneDTO.id).isEqualTo(repositoryManagerOne.getId());
    assertThat(repositoryManagerOneDTO.name).isEqualTo(repositoryManagerOne.getName());
    assertThat(repositoryManagerOneDTO.instanceId).isEqualTo(repositoryManagerOne.getInstanceId());
    assertThat(repositoryManagerOneDTO.getParentId()).isEqualTo(repositoryManagerOne.getParentOwnerId());
    assertThat(repositoryManagerOneDTO.repositoryIds).hasSize(1);
    assertThat(repositoryManagerOneDTO.repositoryIds).containsExactlyInAnyOrder(repositoryOne.getId());

    // repository manager two
    OwnerHierarchyRepositoryManagerDTO repositoryManagerTwoDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManagerTwo.getId());
    assertThat(repositoryManagerTwoDTO.id).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryManagerTwoDTO.name).isEqualTo(repositoryManagerTwo.getName());
    assertThat(repositoryManagerTwoDTO.instanceId).isEqualTo(repositoryManagerTwo.getInstanceId());
    assertThat(repositoryManagerTwoDTO.getParentId()).isEqualTo(repositoryManagerTwo.getParentOwnerId());
    assertThat(repositoryManagerTwoDTO.repositoryIds).hasSize(2);
    assertThat(repositoryManagerTwoDTO.repositoryIds).containsExactlyInAnyOrder(repositoryTwo.getId(),
        repositoryThree.getId());

    // repository one
    OwnerHierarchyRepositoryDTO repositoryOneDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryOne.getId());
    assertThat(repositoryOneDTO.id).isEqualTo(repositoryOne.getId());
    assertThat(repositoryOneDTO.name).isEqualTo(repositoryOne.getName());
    assertThat(repositoryOneDTO.getParentId()).isEqualTo(repositoryOne.getParentOwnerId());
    assertThat(repositoryOneDTO.repositoryManagerId).isEqualTo(repositoryManagerOne.getId());
    assertThat(repositoryOneDTO.repositoryType).isEqualTo(repositoryOne.getRepositoryType().name());
    assertThat(repositoryOneDTO.format).isEqualTo(repositoryOne.getFormat());

    // repository two
    OwnerHierarchyRepositoryDTO repositoryTwoDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryTwo.getId());
    assertThat(repositoryTwoDTO.id).isEqualTo(repositoryTwo.getId());
    assertThat(repositoryTwoDTO.name).isEqualTo(repositoryTwo.getName());
    assertThat(repositoryTwoDTO.getParentId()).isEqualTo(repositoryTwo.getParentOwnerId());
    assertThat(repositoryTwoDTO.repositoryManagerId).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryTwoDTO.repositoryType).isEqualTo(repositoryTwo.getRepositoryType().name());
    assertThat(repositoryTwoDTO.format).isEqualTo(repositoryTwo.getFormat());

    // repository three
    OwnerHierarchyRepositoryDTO repositoryThreeDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryThree.getId());
    assertThat(repositoryThreeDTO.id).isEqualTo(repositoryThree.getId());
    assertThat(repositoryThreeDTO.name).isEqualTo(repositoryThree.getName());
    assertThat(repositoryThreeDTO.getParentId()).isEqualTo(repositoryThree.getParentOwnerId());
    assertThat(repositoryThreeDTO.repositoryManagerId).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryThreeDTO.repositoryType).isEqualTo(repositoryThree.getRepositoryType().name());
    assertThat(repositoryThreeDTO.format).isEqualTo(repositoryThree.getFormat());
  }

  @Test
  void testGetOwnerDetails_Organization() throws Exception {
    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID)
        .get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  void testGetOwnerDetails_Application() throws Exception {
    final String applicationPublicId = "SidebarResourceTest_Application";
    ctx.tempEntity().newApplicationWithParent(applicationPublicId);

    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.APPLICATION, applicationPublicId)
        .get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  void testGetOwnerDetails_RepositoryContaier() throws Exception {
    HttpResponse response = restRequest().path(SidebarResource.GET_GLOBAL_OWNER_DETAILS_PATH)
        .parameter(OwnerType.REPOSITORY_CONTAINER)
        .get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  void testGetOwnerDetails_RepositoryManager() throws Exception {
    RepositoryManager repoManager = ctx.tempEntity().newRepositoryManager();
    HttpResponse response = restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH)
        .parameter(OwnerType.REPOSITORY_MANAGER, repoManager.getId())
        .get();

    assertValidOwnerDetailsDTO(response);
  }

  @Test
  void testGetOwnerDetails_Repository() throws Exception {
    Repository repo = ctx.tempEntity().newRepository();
    HttpResponse response =
        restRequest().path(SidebarResource.GET_OWNER_DETAILS_PATH).parameter(OwnerType.REPOSITORY, repo.getId()).get();

    assertValidOwnerDetailsDTO(response);
  }

  private void assertValidOwnerDetailsDTO(HttpResponse response) {
    ctx.assertResponseStatus(200, response);
    OwnerDetailsDTO ownerDetailsDTO = response.getBody(OwnerDetailsDTO.class);
    assertThat(ownerDetailsDTO).isNotNull();
  }
}
