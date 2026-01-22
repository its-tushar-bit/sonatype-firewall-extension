/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Role;
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
  private RepositoryManagerDAO repositoryManagerDAO;

  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

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

    assertThat(ownerDetailsDTO.roles.membersByRole)
        .extracting(m -> m.roleId)
        .containsExactlyInAnyOrder(
            Role.APPLICATION_EVALUATOR_ROLE_ID, 
            Role.COMPONENT_EVALUATOR_ROLE_ID, 
            Role.DEVELOPER_ROLE_ID, 
            Role.LEGAL_REVIEWER_ROLE_ID, 
            Role.OWNER_ROLE_ID
        );
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

    assertThat(ownerDetailsDTO.roles.membersByRole)
        .extracting(m -> m.roleId)
        .containsExactlyInAnyOrder(
            Role.APPLICATION_EVALUATOR_ROLE_ID, 
            Role.COMPONENT_EVALUATOR_ROLE_ID, 
            Role.DEVELOPER_ROLE_ID, 
            Role.LEGAL_REVIEWER_ROLE_ID, 
            Role.OWNER_ROLE_ID
        );
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

    assertThat(ownerDetailsDTO.roles.membersByRole)
        .extracting(m -> m.roleId)
        .containsExactlyInAnyOrder(
            Role.APPLICATION_EVALUATOR_ROLE_ID, 
            Role.COMPONENT_EVALUATOR_ROLE_ID, 
            Role.DEVELOPER_ROLE_ID, 
            Role.LEGAL_REVIEWER_ROLE_ID, 
            Role.OWNER_ROLE_ID
        );
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

    assertThat(ownerDetailsDTO.roles.membersByRole)
        .extracting(m -> m.roleId)
        .containsExactlyInAnyOrder(
            Role.APPLICATION_EVALUATOR_ROLE_ID, 
            Role.COMPONENT_EVALUATOR_ROLE_ID, 
            Role.DEVELOPER_ROLE_ID, 
            Role.LEGAL_REVIEWER_ROLE_ID, 
            Role.OWNER_ROLE_ID
        );
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

    assertThat(ownerDetailsDTO.roles.membersByRole)
        .extracting(m -> m.roleId)
        .containsExactlyInAnyOrder(
            Role.APPLICATION_EVALUATOR_ROLE_ID, 
            Role.COMPONENT_EVALUATOR_ROLE_ID, 
            Role.DEVELOPER_ROLE_ID, 
            Role.LEGAL_REVIEWER_ROLE_ID, 
            Role.OWNER_ROLE_ID
        );
  }

  @Test
  public void testGetOwnerList() {
    Organization rootOrganization = organizationDAO.getByIdNotNull(Organization.ROOT_ORGANIZATION_ID);
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization(orgOne);
    Application appThree = tempEntity.newApplication(orgTwo.getId());
    RepositoryManager repositoryManagerOne = tempEntity.newRepositoryManager();
    Repository repositoryOne =
        tempEntity.newRepository(repositoryManagerOne, "repository-one", RepositoryType.proxy, "maven");
    RepositoryManager repositoryManagerTwo = tempEntity.newRepositoryManager();
    Repository repositoryTwo =
        tempEntity.newRepository(repositoryManagerTwo, "repository-two", RepositoryType.hosted, "npm");
    Repository repositoryThree =
        tempEntity.newRepository(repositoryManagerTwo, "repository-three", RepositoryType.proxy, "nuget");

    // create org and app with a related repository which should not be included in the result
    Organization orgWithRelatedRepo = tempEntity.newOrganization("org-with-repo");
    orgWithRelatedRepo.setRelatedRepositoryId(repositoryOne.getId());
    organizationDAO.update(orgWithRelatedRepo);
    tempEntity.newApplication("app-for-firewall-for-docker", orgWithRelatedRepo.getId());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).hasSize(12);
    OwnerHierarchyOrganizationDTO rootOrg = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        ownerHierarchyDTO.topParentOrganizationId
    );

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
    OwnerHierarchyOrganizationDTO firstLevelOrg = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        rootOrg.organizationIds.get(0)
    );
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
    OwnerHierarchyOrganizationDTO secondLevelOrg = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        firstLevelOrg.organizationIds.get(0)
    );
    assertThat(secondLevelOrg.parentOrganizationId).isEqualTo(firstLevelOrg.id);
    assertThat(secondLevelOrg.name).isEqualTo(orgTwo.getName());
    assertThat(secondLevelOrg.getParentId()).isEqualTo(firstLevelOrg.id);
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
    assertThat(repositoryContainer.id).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainer.name).isEqualTo(RepositoryContainer.SINGLETON.getName());
    assertThat(repositoryContainer.getParentId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(repositoryContainer.repositoryManagerIds).hasSize(2);
    assertThat(repositoryContainer.repositoryManagerIds).containsExactlyInAnyOrder(
        repositoryManagerOne.getId(),
        repositoryManagerTwo.getId()
    );

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
    assertThat(repositoryManagerTwoDTO.repositoryIds).containsExactlyInAnyOrder(
        repositoryTwo.getId(),
        repositoryThree.getId()
    );

    // repository one
    OwnerHierarchyRepositoryDTO repositoryOneDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryOne.getId());
    assertThat(repositoryOneDTO.id).isEqualTo(repositoryOne.getId());
    assertThat(repositoryOneDTO.name).isEqualTo(repositoryOne.getName());
    assertThat(repositoryOneDTO.getParentId()).isEqualTo(repositoryOne.getParentOwnerId());
    assertThat(repositoryOneDTO.repositoryManagerId).isEqualTo(repositoryManagerOne.getId());
    assertThat(repositoryOneDTO.repositoryType).isEqualTo(repositoryOne.getRepositoryType().name());

    // repository two
    OwnerHierarchyRepositoryDTO repositoryTwoDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryTwo.getId());
    assertThat(repositoryTwoDTO.id).isEqualTo(repositoryTwo.getId());
    assertThat(repositoryTwoDTO.name).isEqualTo(repositoryTwo.getName());
    assertThat(repositoryTwoDTO.getParentId()).isEqualTo(repositoryTwo.getParentOwnerId());
    assertThat(repositoryTwoDTO.repositoryManagerId).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryTwoDTO.repositoryType).isEqualTo(repositoryTwo.getRepositoryType().name());

    // repository three
    OwnerHierarchyRepositoryDTO repositoryThreeDTO =
        (OwnerHierarchyRepositoryDTO) ownerHierarchyDTO.ownersMap.get(repositoryThree.getId());
    assertThat(repositoryThreeDTO.id).isEqualTo(repositoryThree.getId());
    assertThat(repositoryThreeDTO.name).isEqualTo(repositoryThree.getName());
    assertThat(repositoryThreeDTO.getParentId()).isEqualTo(repositoryThree.getParentOwnerId());
    assertThat(repositoryThreeDTO.repositoryManagerId).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryThreeDTO.repositoryType).isEqualTo(repositoryThree.getRepositoryType().name());
  }

  @Test
  public void testGetOwnerList_ChildOrganizationIdsAreOrderedAlphabetically() {
    // Generate 100 orgs with random names. The child org ids must be ordered by org name in the hierarchy.
    for (int iOrg = 0; iOrg < 100; iOrg++) {
      // The new org has a random name
      tempEntity.newOrganization();
    }
    List<Organization> orgs =
        sortOwners(organizationDAO.getByParentOrganizationId(Organization.ROOT_ORGANIZATION_ID));
    List<String> expectedIdsOrderedByName = orgs.stream().map(Organization::getId).collect(Collectors.toList());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    OwnerHierarchyOrganizationDTO rootOrgDTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(ownerHierarchyDTO.topParentOrganizationId);
    assertThat(rootOrgDTO.getChildIds(OwnerType.ORGANIZATION)).isEqualTo(expectedIdsOrderedByName);
  }

  @Test
  public void testGetOwnerList_ChildApplicationIdsAreOrderedAlphabetically() {
    Organization org = tempEntity.newOrganization();
    // Generate 100 apps with random names. The child app ids must be ordered by app name in the hierarchy.
    for (int iEntity = 0; iEntity < 100; iEntity++) {
      // The new app has a random name
      tempEntity.newApplication(org.getId());
    }
    List<Application> apps = sortOwners(applicationDAO.getByOrganizationId(org.getId()));
    List<String> expectedIdsOrderedByName = apps.stream().map(Application::getPublicId).collect(Collectors.toList());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    OwnerHierarchyOrganizationDTO orgDTO = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(org.getId());
    assertThat(orgDTO.getChildIds(OwnerType.APPLICATION)).isEqualTo(expectedIdsOrderedByName);
  }

  @Test
  public void testGetOwnerList_ChildRepositoryManagerIdsAreOrderedAlphabetically() {
    // Generate 100 repo managers with random names. The child repo manager ids must be ordered by repo manager name in
    // the hierarchy.
    for (int iEntity = 0; iEntity < 100; iEntity++) {
      // The new repo manager has a random name
      tempEntity.newRepositoryManager();
    }
    List<RepositoryManager> repoManagers = sortOwners(repositoryManagerDAO.getAll());
    List<String> expectedIdsOrderedByName =
        repoManagers.stream().map(RepositoryManager::getId).collect(Collectors.toList());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    OwnerHierarchyRepositoryContainerDTO repoContainerDTO =
        (OwnerHierarchyRepositoryContainerDTO) ownerHierarchyDTO.ownersMap
            .get(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repoContainerDTO.getChildIds(OwnerType.REPOSITORY_MANAGER)).isEqualTo(expectedIdsOrderedByName);
  }

  @Test
  public void testGetOwnerList_ChildRepositoryIdsAreOrderedAlphabetically() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    // Generate 100 repos with random names. The child repo ids must be ordered by repo name in the hierarchy.
    for (int iEntity = 0; iEntity < 100; iEntity++) {
      // The new repo has a random name
      tempEntity.newRepository(repoManager);
    }
    List<Repository> repos = sortOwners(repositoryDAO.getAll());
    List<String> expectedIdsOrderedByName = repos.stream().map(Repository::getId).collect(Collectors.toList());

    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    OwnerHierarchyRepositoryManagerDTO repoManagerDTO =
        (OwnerHierarchyRepositoryManagerDTO) ownerHierarchyDTO.ownersMap.get(repoManager.getId());
    assertThat(repoManagerDTO.getChildIds(OwnerType.REPOSITORY)).isEqualTo(expectedIdsOrderedByName);
  }

  private <T extends Owner> List<T> sortOwners(List<T> owners) {
    List<T> result = new ArrayList<>(owners);
    result.sort(Comparator.comparing(Owner::getName, String.CASE_INSENSITIVE_ORDER));
    return result;
  }
}
