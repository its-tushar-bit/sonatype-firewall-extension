/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO.transformToOrganizationDTO;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyEntityDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryContainerDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyRepositoryManagerDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class OwnerHierarchyTest
    extends AbstractComponentH2Test
{
  @Inject
  private OrganizationService organizationService;

  @Inject
  private ApplicationService applicationService;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private RepositoryManagerDAO repositoryManagerDAO;

  @Test
  public void testCreateHierarchy() {
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization(orgOne);
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    List<Organization> orgs = organizationService.getAll();
    List<Application> apps = new ArrayList<>(Arrays.asList(appOne, appTwo, appThree));
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, emptyList(), emptyList(), organizationDAO, repositoryManagerDAO);

    OwnerHierarchyOrganizationDTO root = hierarchy.root();
    assertThat(root).isNotNull();
    assertThat(root.id).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(root.organizationIds).hasSize(1);
    assertThat(root.organizationIds.stream().anyMatch(id -> id.equals(orgOne.getId()))).isTrue();

    OwnerHierarchyOrganizationDTO firstLevelOrg = hierarchy.getOrganizationById(orgOne.getId());
    assertThat(firstLevelOrg).isNotNull();
    assertThat(firstLevelOrg.organizationIds).hasSize(1);
    assertThat(firstLevelOrg.organizationIds.stream().anyMatch(id -> id.equals(orgTwo.getId()))).isTrue();
    assertThat(firstLevelOrg.applicationIds).hasSize(2);
    assertThat(firstLevelOrg.applicationIds.stream().anyMatch(id -> id.equals(appOne.getPublicId()))).isTrue();
    assertThat(firstLevelOrg.applicationIds.stream().anyMatch(id -> id.equals(appTwo.getPublicId()))).isTrue();

    OwnerHierarchyOrganizationDTO secondLevelOrg = hierarchy.getOrganizationById(orgTwo.getId());
    assertThat(secondLevelOrg).isNotNull();
    assertThat(secondLevelOrg.organizationIds).hasSize(0);
    assertThat(secondLevelOrg.applicationIds).hasSize(1);
    assertThat(secondLevelOrg.applicationIds.stream().anyMatch(id -> id.equals(appThree.getPublicId()))).isTrue();
  }

  @Test
  public void testCreateHierarchyWithRepositoriesAndApps() {
    // given an organization with two applications and two repository managers and three repositories
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    RepositoryManager repositoryManagerOne = tempEntity.newRepositoryManager();
    Repository repositoryOne = tempEntity.newRepository(repositoryManagerOne, "repository-one");
    RepositoryManager repositoryManagerTwo = tempEntity.newRepositoryManager();
    Repository repositoryTwo = tempEntity.newRepository(repositoryManagerTwo, "repository-two");
    Repository repositoryThree = tempEntity.newRepository(repositoryManagerTwo, "repository-three");

    // when creating the hierarchy
    List<Organization> orgs = organizationService.getAll();
    List<Application> apps = new ArrayList<>(Arrays.asList(appOne, appTwo));
    List<RepositoryManager> repositoryManagers = Arrays.asList(repositoryManagerOne, repositoryManagerTwo);
    List<Repository> repositories = Arrays.asList(repositoryOne, repositoryTwo, repositoryThree);
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, repositoryManagers, repositories, organizationDAO, repositoryManagerDAO);

    // then the hierarchy contains the organizations
    OwnerHierarchyOrganizationDTO root = hierarchy.root();
    assertThat(root).isNotNull();
    assertThat(root.id).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(root.organizationIds).hasSize(1);
    assertThat(root.repositoryContainerId).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    // and the applications
    List<String> applicationIds = hierarchy.getOrganizationById(orgOne.getId()).applicationIds;
    assertThat(applicationIds).containsExactlyInAnyOrder(appOne.getPublicId(), appTwo.getPublicId());

    // and the hierarchy contains the expected repository entities
    verifyFieldsInOwnerHierarchy(hierarchy, repositoryManagerOne, repositoryManagerTwo, repositoryOne, repositoryTwo,
        repositoryThree);
  }

  @Test
  public void testCreateHierarchyWithRepositoriesOnly() {
    // given an organization with two applications and two repository managers and three repositories
    RepositoryManager repositoryManagerOne = tempEntity.newRepositoryManager();
    Repository repositoryOne = tempEntity.newRepository(repositoryManagerOne, "repository-one");
    RepositoryManager repositoryManagerTwo = tempEntity.newRepositoryManager();
    Repository repositoryTwo = tempEntity.newRepository(repositoryManagerTwo, "repository-two");
    Repository repositoryThree = tempEntity.newRepository(repositoryManagerTwo, "repository-three");
    List<Organization> orgs = organizationService.getAll();

    // when creating the hierarchy
    List<RepositoryManager> repositoryManagers = Arrays.asList(repositoryManagerOne, repositoryManagerTwo);
    List<Repository> repositories = Arrays.asList(repositoryOne, repositoryTwo, repositoryThree);
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, emptyList(), repositoryManagers, repositories, organizationDAO, repositoryManagerDAO);

    // then the hierarchy contains the repository container, but no organization or application
    OwnerHierarchyOrganizationDTO root = hierarchy.root();
    assertThat(root).isNotNull();
    assertThat(root.id).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
    assertThat(root.organizationIds).hasSize(0);
    assertThat(root.repositoryContainerId).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    // and the hierarchy contains the expected repository entities
    verifyFieldsInOwnerHierarchy(hierarchy, repositoryManagerOne, repositoryManagerTwo, repositoryOne, repositoryTwo,
        repositoryThree);
  }

  private static void verifyFieldsInOwnerHierarchy(
      final OwnerHierarchy hierarchy,
      final RepositoryManager repositoryManagerOne,
      final RepositoryManager repositoryManagerTwo,
      final Repository repositoryOne,
      final Repository repositoryTwo,
      final Repository repositoryThree)
  {
    OwnerHierarchyRepositoryContainerDTO repositoryContainer = hierarchy.getRepositoryContainer();
    assertThat(repositoryContainer).isNotNull();
    assertThat(repositoryContainer.id).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryContainer.repositoryManagerIds).containsExactlyInAnyOrder(
        repositoryManagerOne.getId(),
        repositoryManagerTwo.getId());

    OwnerHierarchyRepositoryManagerDTO repositoryManagerOneDTO =
        hierarchy.getRepositoryManagerById(repositoryManagerOne.getId());
    assertThat(repositoryManagerOneDTO).isNotNull();
    assertThat(repositoryManagerOneDTO.id).isEqualTo(repositoryManagerOne.getId());
    assertThat(repositoryManagerOneDTO.getChildIds()).containsExactlyInAnyOrder(repositoryOne.getId());
    assertThat(repositoryManagerOneDTO.getParentId()).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryManagerOneDTO.name).isEqualTo(repositoryManagerOne.getName());
    assertThat(repositoryManagerOneDTO.instanceId).isEqualTo(repositoryManagerOne.getInstanceId());

    OwnerHierarchyRepositoryManagerDTO repositoryManagerTwoDTO =
        hierarchy.getRepositoryManagerById(repositoryManagerTwo.getId());
    assertThat(repositoryManagerTwoDTO).isNotNull();
    assertThat(repositoryManagerTwoDTO.id).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryManagerTwoDTO.getChildIds()).containsExactlyInAnyOrder(
        repositoryTwo.getId(),
        repositoryThree.getId());
    assertThat(repositoryManagerTwoDTO.getParentId()).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    assertThat(repositoryManagerTwoDTO.name).isEqualTo(repositoryManagerTwo.getName());
    assertThat(repositoryManagerTwoDTO.instanceId).isEqualTo(repositoryManagerTwo.getInstanceId());

    OwnerHierarchyRepositoryDTO repositoryOneDTO = hierarchy.getRepositoryById(repositoryOne.getId());
    assertThat(repositoryOneDTO).isNotNull();
    assertThat(repositoryOneDTO.id).isEqualTo(repositoryOne.getId());
    assertThat(repositoryOneDTO.name).isEqualTo(repositoryOne.getName());
    assertThat(repositoryOneDTO.getChildIds()).isEmpty();
    assertThat(repositoryOneDTO.getParentId()).isEqualTo(repositoryManagerOne.getId());
    assertThat(repositoryOneDTO.repositoryManagerId).isEqualTo(repositoryOne.getRepositoryManagerId());

    OwnerHierarchyRepositoryDTO repositoryTwoDTO = hierarchy.getRepositoryById(repositoryTwo.getId());
    assertThat(repositoryTwoDTO).isNotNull();
    assertThat(repositoryTwoDTO.id).isEqualTo(repositoryTwo.getId());
    assertThat(repositoryTwoDTO.name).isEqualTo(repositoryTwo.getName());
    assertThat(repositoryTwoDTO.getChildIds()).isEmpty();
    assertThat(repositoryTwoDTO.getParentId()).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryTwoDTO.repositoryManagerId).isEqualTo(repositoryTwo.getRepositoryManagerId());

    OwnerHierarchyRepositoryDTO repositoryThreeDTO = hierarchy.getRepositoryById(repositoryThree.getId());
    assertThat(repositoryThreeDTO).isNotNull();
    assertThat(repositoryThreeDTO.id).isEqualTo(repositoryThree.getId());
    assertThat(repositoryThreeDTO.name).isEqualTo(repositoryThree.getName());
    assertThat(repositoryThreeDTO.getChildIds()).isEmpty();
    assertThat(repositoryThreeDTO.getParentId()).isEqualTo(repositoryManagerTwo.getId());
    assertThat(repositoryThreeDTO.repositoryManagerId).isEqualTo(repositoryThree.getRepositoryManagerId());
  }

  @Test
  public void testCreateHierarchyWithPartialOrganizationsAccess() {
    Organization orgOne = tempEntity.newOrganization();
    tempEntity.newOrganization();
    tempEntity.newApplication(orgOne.getId());
    tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization(orgOne);
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    List<Organization> orgs = new ArrayList<>(Arrays.asList(orgTwo));
    List<Application> apps = new ArrayList<>(Arrays.asList(appThree));
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, emptyList(), emptyList(), organizationDAO, repositoryManagerDAO);

    OwnerHierarchyOrganizationDTO root = hierarchy.root();
    assertThat(hierarchy.asHashMap()).hasSize(3);
    assertThat(root).isNotNull();
    assertThat(root.id).isEqualTo(orgTwo.getId());
    assertThat(root.organizationIds).hasSize(0);
    assertThat(root.applicationIds).hasSize(1);
    assertThat(root.applicationIds.stream().anyMatch(id -> id.equals(appThree.getPublicId()))).isTrue();
    assertThat(hierarchy.getOrganizationById(orgTwo.getId()).synthetic).isFalse();
  }

  @Test
  public void testCreateHierarchyWithAccessToSingleApp() {
    Organization orgOne = tempEntity.newOrganization();
    tempEntity.newOrganization();
    tempEntity.newApplication(orgOne.getId());
    tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization(orgOne);
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    List<Organization> orgs = new ArrayList<>();
    List<Application> apps = new ArrayList<>(Arrays.asList(appThree));
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, emptyList(), emptyList(), organizationDAO, repositoryManagerDAO);

    OwnerHierarchyOrganizationDTO root = hierarchy.root();
    assertThat(hierarchy.asHashMap()).hasSize(3);
    assertThat(root).isNotNull();
    assertThat(root.id).isEqualTo(orgTwo.getId());
    assertThat(root.organizationIds).hasSize(0);
    assertThat(root.applicationIds).hasSize(1);
    assertThat(root.applicationIds.stream().anyMatch(id -> id.equals(appThree.getPublicId()))).isTrue();
    assertThat(hierarchy.getOrganizationById(orgTwo.getId()).synthetic).isTrue();
  }

  @Test
  public void testAsHashMap() {
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication("Application A1", "application-a1", orgOne.getId());
    Application appTwo = tempEntity.newApplication("Application A2", "application-a2", orgOne.getId());
    Application appThree = tempEntity.newApplication("Application Z", "application-z", orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization("Organization Z", orgOne); // 5
    Organization orgThree = tempEntity.newOrganization("Organization A2", orgOne); // 3
    Organization orgFour = tempEntity.newOrganization("Organization A1", orgOne); // 2
    Organization orgFive = tempEntity.newOrganization("Organization L", orgOne); // 4
    Organization orgSix = tempEntity.newOrganization("123 Organization Z", orgOne); // 1
    Application appFour = tempEntity.newApplication(orgTwo.getId());
    RepositoryManager repositoryManagerOne = tempEntity.newRepositoryManager();
    Repository repositoryOne = tempEntity.newRepository(repositoryManagerOne, "repository-one");
    RepositoryManager repositoryManagerTwo = tempEntity.newRepositoryManager();
    Repository repositoryTwo = tempEntity.newRepository(repositoryManagerTwo, "repository-two");
    Repository repositoryThree = tempEntity.newRepository(repositoryManagerTwo, "repository-three");

    List<Organization> orgs = organizationService.getAll();
    List<Application> apps = applicationService.getApplications();
    List<RepositoryManager> repositoryManagers = Arrays.asList(repositoryManagerOne, repositoryManagerTwo);
    List<Repository> repositories = Arrays.asList(repositoryOne, repositoryTwo, repositoryThree);
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, repositoryManagers, repositories, organizationDAO, repositoryManagerDAO);

    Map<String, OwnerHierarchyEntityDTO> ownersMap = hierarchy.asHashMap();
    assertThat(ownersMap.containsKey(Organization.ROOT_ORGANIZATION_ID)).isTrue();
    assertThat(ownersMap.containsKey(orgOne.getId())).isTrue();
    assertThat(ownersMap.containsKey(orgTwo.getId())).isTrue();
    assertThat(ownersMap.containsKey(orgThree.getId())).isTrue();
    assertThat(ownersMap.containsKey(orgFour.getId())).isTrue();
    assertThat(ownersMap.containsKey(orgFive.getId())).isTrue();
    assertThat(ownersMap.containsKey(orgSix.getId())).isTrue();

    assertThat(ownersMap.containsKey(appOne.getPublicId())).isTrue();
    assertThat(ownersMap.containsKey(appTwo.getPublicId())).isTrue();
    assertThat(ownersMap.containsKey(appThree.getPublicId())).isTrue();
    assertThat(ownersMap.containsKey(appFour.getPublicId())).isTrue();

    assertThat(ownersMap).containsKey(repositoryOne.getId());
    assertThat(ownersMap).containsKey(repositoryTwo.getId());
    assertThat(ownersMap).containsKey(repositoryThree.getId());
    assertThat(ownersMap).containsKey(repositoryManagerOne.getId());
    assertThat(ownersMap).containsKey(repositoryManagerTwo.getId());
    assertThat(ownersMap).containsKey(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    OwnerHierarchyOrganizationDTO orgOneDTO = (OwnerHierarchyOrganizationDTO) ownersMap.get(orgOne.getId());
    assertThat(orgOneDTO.organizationIds).isEqualTo(
        Arrays.asList(orgSix.getId(), orgFour.getId(), orgThree.getId(), orgFive.getId(), orgTwo.getId()));
    assertThat(orgOneDTO.applicationIds).isEqualTo(
        Arrays.asList(appOne.getPublicId(), appTwo.getPublicId(), appThree.getPublicId()));

    OwnerHierarchyEntityDTO repositoryManagerOneDTO = ownersMap.get(repositoryManagerOne.getId());
    assertThat(repositoryManagerOneDTO.getChildIds()).containsExactlyInAnyOrder(repositoryOne.getId());
    assertThat(repositoryManagerOneDTO.getParentId()).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);

    OwnerHierarchyEntityDTO repositoryManagerTwoDTO = ownersMap.get(repositoryManagerTwo.getId());
    assertThat(repositoryManagerTwoDTO.getChildIds()).containsExactlyInAnyOrder(
        repositoryTwo.getId(),
        repositoryThree.getId());
    assertThat(repositoryManagerTwoDTO.getParentId()).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testContains() {
    Organization orgOne = tempEntity.newOrganization();

    List<Organization> orgs = new ArrayList<>(Arrays.asList(orgOne));
    List<Application> apps = new ArrayList<>();
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, emptyList(), emptyList(), organizationDAO, repositoryManagerDAO);

    assertThat(hierarchy.contains(orgOne.getId())).isTrue();
    assertThat(hierarchy.contains("Ramdom ID")).isFalse();
  }

  @Test
  public void testAdd() {
    Organization orgOne = tempEntity.newOrganization();

    List<Organization> orgs = new ArrayList<>();
    List<Application> apps = new ArrayList<>();
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, emptyList(), emptyList(), organizationDAO, repositoryManagerDAO);

    assertThat(hierarchy.contains(orgOne.getId())).isFalse();
    hierarchy.add(transformToOrganizationDTO.apply(orgOne));
    assertThat(hierarchy.contains(orgOne.getId())).isTrue();
  }

  @Test
  public void testRemove() {
    Organization orgOne = tempEntity.newOrganization();

    List<Organization> orgs = new ArrayList<>(Arrays.asList(orgOne));
    List<Application> apps = new ArrayList<>();
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, emptyList(), emptyList(), organizationDAO, repositoryManagerDAO);

    assertThat(hierarchy.contains(orgOne.getId())).isTrue();
    hierarchy.remove(orgOne.getId());
    assertThat(hierarchy.contains(orgOne.getId())).isFalse();
  }

  @Test
  public void testGetOrganizationById() {
    Organization orgOne = tempEntity.newOrganization();
    Organization orgTwo = tempEntity.newOrganization();

    List<Organization> orgs = new ArrayList<>(Arrays.asList(orgOne, orgTwo));
    List<Application> apps = new ArrayList<>();
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, apps, emptyList(), emptyList(), organizationDAO, repositoryManagerDAO);

    assertThat(hierarchy.getOrganizationById(orgOne.getId()).id).isEqualTo(orgOne.getId());
    assertThat(hierarchy.getOrganizationById(orgTwo.getId()).id).isEqualTo(orgTwo.getId());
    assertThat(hierarchy.getOrganizationById("Ramdon ID")).isNull();
  }

  @Test
  public void testVirtualRepositoryManagerRoutedToVirtualList() {
    // given a virtual repository manager and a regular repository manager
    RepositoryManager virtualManager = tempEntity.newRepositoryManager();
    virtualManager.setManagerType(ManagerType.VIRTUAL);
    repositoryManagerDAO.update(virtualManager);

    RepositoryManager regularManager = tempEntity.newRepositoryManager();

    List<Organization> orgs = organizationService.getAll();
    List<RepositoryManager> repositoryManagers = Arrays.asList(virtualManager, regularManager);
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, emptyList(), repositoryManagers, emptyList(), organizationDAO, repositoryManagerDAO);

    // then the virtual manager is in virtualRepositoryManagerIds, not repositoryManagerIds
    OwnerHierarchyRepositoryContainerDTO repositoryContainer = hierarchy.getRepositoryContainer();
    assertThat(repositoryContainer.repositoryManagerIds).containsExactly(regularManager.getId());
    assertThat(repositoryContainer.virtualRepositoryManagerIds).containsExactly(virtualManager.getId());
  }

  @Test
  public void testRegularRepositoryManagerRoutedToRegularList() {
    // given two repository managers with no managerType set
    RepositoryManager managerOne = tempEntity.newRepositoryManager();
    RepositoryManager managerTwo = tempEntity.newRepositoryManager();

    List<Organization> orgs = organizationService.getAll();
    List<RepositoryManager> repositoryManagers = Arrays.asList(managerOne, managerTwo);
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, emptyList(), repositoryManagers, emptyList(), organizationDAO, repositoryManagerDAO);

    // then both managers are in repositoryManagerIds, and virtualRepositoryManagerIds is empty
    OwnerHierarchyRepositoryContainerDTO repositoryContainer = hierarchy.getRepositoryContainer();
    assertThat(repositoryContainer.repositoryManagerIds).containsExactlyInAnyOrder(
        managerOne.getId(),
        managerTwo.getId());
    assertThat(repositoryContainer.virtualRepositoryManagerIds).isEmpty();
  }

  @Test
  public void testManagerTypeIsPropagatedToDTO() {
    // given a virtual repository manager
    RepositoryManager virtualManager = tempEntity.newRepositoryManager();
    virtualManager.setManagerType(ManagerType.VIRTUAL);
    repositoryManagerDAO.update(virtualManager);

    List<Organization> orgs = organizationService.getAll();
    List<RepositoryManager> repositoryManagers = Arrays.asList(virtualManager);
    OwnerHierarchy hierarchy =
        new OwnerHierarchy(orgs, emptyList(), repositoryManagers, emptyList(), organizationDAO, repositoryManagerDAO);

    // then the managerType is set on the DTO
    OwnerHierarchyRepositoryManagerDTO managerDTO = hierarchy.getRepositoryManagerById(virtualManager.getId());
    assertThat(managerDTO.managerType).isEqualTo(ManagerType.VIRTUAL);
  }
}
