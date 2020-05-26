/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerDAOTest
    extends AbstractDbDAOTest
{
  private final OwnerDAO ownerDAO = new OwnerDAO();

  @Test
  public void testWalkHierarchy_Application() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(application)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(application.getId(), organization.getId(),
        organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_ApplicationId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(application.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(application.getId(), organization.getId(),
        organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_Organization() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organization)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(organization.getId(), organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_OrganizationId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organization.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(organization.getId(), organization.getParentOrganizationId());
  }

  @Test
  public void testWalkHierarchy_RepositoryContainer() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(RepositoryContainer.SINGLETON)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_RepositoryContainerId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(RepositoryContainer.REPOSITORY_CONTAINER_ID)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_Repository() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(repository.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWalkHierarchy_RepositoryId() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds).containsExactly(repository.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID,
        Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetChildOwners_Application() {
    assertThat(ownerDAO.getChildOwners(application)).isEmpty();
  }

  @Test
  public void testGetChildOwners_Organization() {
    List<Owner> childOwners = ownerDAO.getChildOwners(organization);
    assertThat(childOwners).extracting(Owner::getId).containsExactly(application.getId());
  }

  @Test
  public void testGetChildOwners_RootOrganization() {
    List<Owner> childOwners = ownerDAO.getChildOwners(ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID));
    assertThat(childOwners).extracting(Owner::getId).containsExactly(organization.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetChildOwners_RepositoryContainer() {
    List<Owner> childOwners = ownerDAO.getChildOwners(RepositoryContainer.SINGLETON);
    assertThat(childOwners).extracting(Owner::getId).containsExactly(repository.getId());
  }

  @Test
  public void testGetChildOwners_Repository() {
    assertThat(ownerDAO.getChildOwners(repository)).isEmpty();
  }
}
