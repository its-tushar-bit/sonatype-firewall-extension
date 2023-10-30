/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryContainerDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryManagerDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SidebarServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SidebarService sidebarService;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
  }

  @Test
  public void testGetOwnerDetails_Organization() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);
    Label label = tempEntity.newLabel(organization.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(organization.getId());

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, organization.getId());
    assertThat(ownerDetailsDTO.tags).hasSize(1);
    assertThat(ownerDetailsDTO.tags.get(0).getId()).isEqualTo(tag.getId());

    assertThat(ownerDetailsDTO.policies).hasSize(1);
    assertThat(ownerDetailsDTO.policies.get(0).getId()).isEqualTo(policy.getId());

    assertThat(ownerDetailsDTO.labels).hasSize(1);
    assertThat(ownerDetailsDTO.labels.get(0).getId()).isEqualTo(label.getId());

    assertThat(ownerDetailsDTO.licenseThreatGroups).hasSize(1);
    assertThat(ownerDetailsDTO.licenseThreatGroups.get(0).getId()).isEqualTo(licenseThreatGroup.getId());

    assertThat(ownerDetailsDTO.roles.membersByRole).hasSameSizeAs(new RoleDAO().getApplicationRoles());
  }

  @Test
  public void testGetOwnerDetails_Application() {
    Application application = tempEntity.newApplicationWithParent("OwnerManagerServiceTestApplication");

    Policy policy = tempEntity.newPolicy(application);
    Label label = tempEntity.newLabel(application.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(application.getId());

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.APPLICATION, application.getId());
    assertThat(ownerDetailsDTO.tags).isEmpty();

    assertThat(ownerDetailsDTO.policies).hasSize(1);
    assertThat(ownerDetailsDTO.policies.get(0).getId()).isEqualTo(policy.getId());

    assertThat(ownerDetailsDTO.labels).hasSize(1);
    assertThat(ownerDetailsDTO.labels.get(0).getId()).isEqualTo(label.getId());

    assertThat(ownerDetailsDTO.licenseThreatGroups).hasSize(1);
    assertThat(ownerDetailsDTO.licenseThreatGroups.get(0).getId()).isEqualTo(licenseThreatGroup.getId());

    assertThat(ownerDetailsDTO.roles.membersByRole).hasSameSizeAs(new RoleDAO().getApplicationRoles());
  }

  @Test
  public void testGetOwnerDetails_RepositoryContainer() {
    Policy policy = tempEntity.newPolicy(RepositoryContainer.SINGLETON);

    OwnerDetailsDTO ownerDetailsDTO =
        sidebarService.getOwnerDetails(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(ownerDetailsDTO.tags).isEmpty();

    assertThat(ownerDetailsDTO.policies).hasSize(1);
    assertThat(ownerDetailsDTO.policies.get(0).getId()).isEqualTo(policy.getId());

    assertThat(ownerDetailsDTO.labels).isEmpty();

    assertThat(ownerDetailsDTO.licenseThreatGroups).isEmpty();

    assertThat(ownerDetailsDTO.roles.membersByRole).hasSameSizeAs(new RoleDAO().getApplicationRoles());
  }

  @Test
  public void testGetOwnerDetails_RepositoryManager() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();

    Policy policy = tempEntity.newPolicy(repoManager);

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.REPOSITORY_MANAGER, repoManager.getId());
    assertThat(ownerDetailsDTO.tags).isEmpty();

    assertThat(ownerDetailsDTO.policies).hasSize(1);
    assertThat(ownerDetailsDTO.policies.get(0).getId()).isEqualTo(policy.getId());

    assertThat(ownerDetailsDTO.labels).isEmpty();

    assertThat(ownerDetailsDTO.licenseThreatGroups).isEmpty();

    assertThat(ownerDetailsDTO.roles.membersByRole).hasSameSizeAs(new RoleDAO().getApplicationRoles());
  }

  @Test
  public void testGetOwnerDetails_Repository() {
    Repository repo = tempEntity.newRepository();

    Policy policy = tempEntity.newPolicy(repo);

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.REPOSITORY, repo.getId());
    assertThat(ownerDetailsDTO.tags).isEmpty();

    assertThat(ownerDetailsDTO.policies).hasSize(1);
    assertThat(ownerDetailsDTO.policies.get(0).getId()).isEqualTo(policy.getId());

    assertThat(ownerDetailsDTO.labels).isEmpty();

    assertThat(ownerDetailsDTO.licenseThreatGroups).isEmpty();

    assertThat(ownerDetailsDTO.roles.membersByRole).hasSameSizeAs(new RoleDAO().getApplicationRoles());
  }

  @Test
  public void testGetOwnerList() {
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization(orgOne);
    Application appThree = tempEntity.newApplication(orgTwo.getId());
    RepositoryManager repositoryManagerOne = tempEntity.newRepositoryManager();
    Repository repositoryOne = tempEntity.newRepository(repositoryManagerOne, "repository-one");
    RepositoryManager repositoryManagerTwo = tempEntity.newRepositoryManager();
    Repository repositoryTwo = tempEntity.newRepository(repositoryManagerTwo, "repository-two");
    Repository repositoryThree = tempEntity.newRepository(repositoryManagerTwo, "repository-three");

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).hasSize(12);
    OwnerHierarchyOrganizationDTO rootOrg = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        ownerHierarchyDTO.topParentOrganizationId
    );

    // Root org
    assertThat(rootOrg.id).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(rootOrg.applicationIds).isNull();
    assertThat(rootOrg.parentOrganizationId).isNull();
    assertThat(rootOrg.organizationIds).hasSize(1);
    assertThat(rootOrg.subOrgs).isEqualTo(2);
    assertThat(rootOrg.totalApps).isEqualTo(3);

    // first level organization
    OwnerHierarchyOrganizationDTO firstLevelOrg = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        rootOrg.organizationIds.get(0)
    );
    assertThat(firstLevelOrg.parentOrganizationId).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(firstLevelOrg.name).isEqualTo(orgOne.getName());
    assertThat(firstLevelOrg.applicationIds).hasSize(2);
    assertThat(firstLevelOrg.organizationIds).hasSize(1);
    assertThat(firstLevelOrg.applicationIds.stream().anyMatch(id -> id.equals(appOne.getPublicId()))).isTrue();
    assertThat(firstLevelOrg.applicationIds.stream().anyMatch(id -> id.equals(appTwo.getPublicId()))).isTrue();
    assertThat(firstLevelOrg.subOrgs).isEqualTo(1);
    assertThat(firstLevelOrg.totalApps).isEqualTo(3);

    // second level organization
    OwnerHierarchyOrganizationDTO secondLevelOrg = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        firstLevelOrg.organizationIds.get(0)
    );
    assertThat(secondLevelOrg.parentOrganizationId).isEqualTo(firstLevelOrg.id);
    assertThat(secondLevelOrg.name).isEqualTo(orgTwo.getName());
    assertThat(secondLevelOrg.applicationIds).hasSize(1);
    assertThat(secondLevelOrg.organizationIds).isEmpty();
    OwnerHierarchyApplicationDTO secondLevelOrgApplication =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(
            secondLevelOrg.applicationIds.get(0)
        );
    assertThat(secondLevelOrgApplication.id).isEqualTo(appThree.getId());
    assertThat(secondLevelOrg.subOrgs).isEqualTo(0);
    assertThat(secondLevelOrg.totalApps).isEqualTo(1);

    // repository container
    OwnerHierarchyRepositoryContainerDTO repositoryContainer =
        (OwnerHierarchyRepositoryContainerDTO) ownerHierarchyDTO.ownersMap.get(
            RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainer.repositoryManagerIds).hasSize(2);
    assertThat(repositoryContainer.repositoryManagerIds).containsExactlyInAnyOrder(
        repositoryManagerOne.getId(),
        repositoryManagerTwo.getId()
    );

    // repository manager one
    OwnerHierarchyRepositoryManagerDTO repositoryManagerOneDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManagerOne.getId());
    assertThat(repositoryManagerOneDTO.name).isEqualTo(repositoryManagerOne.getName());
    assertThat(repositoryManagerOneDTO.repositoryIds).hasSize(1);
    assertThat(repositoryManagerOneDTO.repositoryIds).containsExactlyInAnyOrder(repositoryOne.getId());
    assertThat(repositoryManagerOneDTO.id).isEqualTo(repositoryManagerOne.getId());
    assertThat(repositoryManagerOneDTO.instanceId).isEqualTo(repositoryManagerOne.getInstanceId());

    // repository manager two
    OwnerHierarchyRepositoryManagerDTO repositoryManagerTwoDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repositoryManagerTwo.getId());
    assertThat(repositoryManagerTwoDTO.name).isEqualTo(repositoryManagerTwo.getName());
    assertThat(repositoryManagerTwoDTO.repositoryIds).hasSize(2);
    assertThat(repositoryManagerTwoDTO.repositoryIds).containsExactlyInAnyOrder(
        repositoryTwo.getId(),
        repositoryThree.getId()
    );
    assertThat(repositoryManagerTwoDTO.id).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryManagerTwoDTO.instanceId).isEqualTo(repositoryManagerTwo.getInstanceId());

    // repository one
    OwnerHierarchyRepositoryDTO repositoryOneDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryOne.getId());
    assertThat(repositoryOneDTO.id).isEqualTo(repositoryOne.getId());
    assertThat(repositoryOneDTO.name).isEqualTo(repositoryOne.getName());
    assertThat(repositoryOneDTO.repositoryManagerId).isEqualTo(repositoryManagerOne.getId());

    // repository two
    OwnerHierarchyRepositoryDTO repositoryTwoDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryTwo.getId());
    assertThat(repositoryTwoDTO.id).isEqualTo(repositoryTwo.getId());
    assertThat(repositoryTwoDTO.name).isEqualTo(repositoryTwo.getName());
    assertThat(repositoryTwoDTO.repositoryManagerId).isEqualTo(repositoryManagerTwo.getId());

    // repository three
    OwnerHierarchyRepositoryDTO repositoryThreeDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryThree.getId());
    assertThat(repositoryThreeDTO.id).isEqualTo(repositoryThree.getId());
    assertThat(repositoryThreeDTO.name).isEqualTo(repositoryThree.getName());
    assertThat(repositoryThreeDTO.repositoryManagerId).isEqualTo(repositoryManagerTwo.getId());
  }
}
