/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyEntityDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO.transformToOrganizationDTO;

public class OwnerHierarchyTest extends AbstractComponentTest
{
  @Inject
  private OrganizationService organizationService;

  @Inject
  private ApplicationService applicationService;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
  }

  @Test
  public void testCreateHierarchy() {
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization(orgOne);
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    List<Organization> orgs = organizationService.getAll();
    List<Application> apps = new ArrayList<>(Arrays.asList(appOne, appTwo, appThree));
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

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
  public void testCreateHierarchyWithPartialOrganizationsAccess() {
    Organization orgOne = tempEntity.newOrganization();
    tempEntity.newOrganization();
    tempEntity.newApplication(orgOne.getId());
    tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization(orgOne);
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    List<Organization> orgs = new ArrayList<>(Arrays.asList(orgTwo));
    List<Application> apps = new ArrayList<>(Arrays.asList(appThree));
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

    OwnerHierarchyOrganizationDTO root = hierarchy.root();
    assertThat(hierarchy.asHashMap()).hasSize(2);
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
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

    OwnerHierarchyOrganizationDTO root = hierarchy.root();
    assertThat(hierarchy.asHashMap()).hasSize(2);
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

    List<Organization> orgs = organizationService.getAll();
    List<Application> apps = applicationService.getApplications();
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

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

    OwnerHierarchyOrganizationDTO orgOneDTO = (OwnerHierarchyOrganizationDTO) ownersMap.get(orgOne.getId());
    assertThat(orgOneDTO.organizationIds).isEqualTo(
        Arrays.asList(orgSix.getId(), orgFour.getId(), orgThree.getId(), orgFive.getId(), orgTwo.getId())
    );
    assertThat(orgOneDTO.applicationIds).isEqualTo(
        Arrays.asList(appOne.getPublicId(), appTwo.getPublicId(), appThree.getPublicId())
    );
  }

  @Test
  public void testContains() {
    Organization orgOne = tempEntity.newOrganization();

    List<Organization> orgs = new ArrayList<>(Arrays.asList(orgOne));
    List<Application> apps = new ArrayList<>();
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

    assertThat(hierarchy.contains(orgOne.getId())).isTrue();
    assertThat(hierarchy.contains("Ramdom ID")).isFalse();
  }

  @Test
  public void testAdd() {
    Organization orgOne = tempEntity.newOrganization();

    List<Organization> orgs = new ArrayList<>();
    List<Application> apps = new ArrayList<>();
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

    assertThat(hierarchy.contains(orgOne.getId())).isFalse();
    hierarchy.add(transformToOrganizationDTO.apply(orgOne));
    assertThat(hierarchy.contains(orgOne.getId())).isTrue();
  }

  @Test
  public void testRemove() {
    Organization orgOne = tempEntity.newOrganization();

    List<Organization> orgs = new ArrayList<>(Arrays.asList(orgOne));
    List<Application> apps = new ArrayList<>();
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

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
    OwnerHierarchy hierarchy = new OwnerHierarchy(orgs, apps);

    assertThat(hierarchy.getOrganizationById(orgOne.getId()).id).isEqualTo(orgOne.getId());
    assertThat(hierarchy.getOrganizationById(orgTwo.getId()).id).isEqualTo(orgTwo.getId());
    assertThat(hierarchy.getOrganizationById("Ramdon ID")).isNull();
  }
}
