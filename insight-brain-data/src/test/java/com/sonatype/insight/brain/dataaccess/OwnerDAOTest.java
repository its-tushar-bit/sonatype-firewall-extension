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
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class OwnerDAOTest
    extends AbstractDbDAOTest
{
  private final OwnerDAO ownerDAO = new OwnerDAO();

  @Test
  public void testWalkHierarchy_Application() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(applicationId)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds, contains(applicationId, organization.getId(), organization.getParentOrganizationId()));
  }

  @Test
  public void testWalkHierarchy_Organization() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(organization.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds, contains(organization.getId(), organization.getParentOrganizationId()));
  }

  @Test
  public void testWalkHierarchy_RepositoryContainer() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(RepositoryContainer.REPOSITORY_CONTAINER_ID)) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds, contains(RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID));
  }

  @Test
  public void testWalkHierarchy_Repository() {
    List<String> ownersIds = new ArrayList<>();
    for (Owner owner : ownerDAO.walkHierarchy(repository.getId())) {
      ownersIds.add(owner.getId());
    }
    assertThat(ownersIds,
        contains(repository.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID, Organization.ROOT_ORGANIZATION_ID));
  }

  @Test
  public void testGetChildOwners_Application() {
    assertThat(ownerDAO.getChildOwners(application), hasSize(0));
  }

  @Test
  public void testGetChildOwners_Organization() {
    List<Owner> childOwners = ownerDAO.getChildOwners(organization);
    assertThat(childOwners, hasSize(1));
    assertThat(childOwners.get(0).getId(), is(application.getId()));
  }

  @Test
  public void testGetChildOwners_RootOrganization() {
    List<Owner> childOwners = ownerDAO.getChildOwners(ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID));
    assertThat(childOwners, hasSize(2));
    assertThat(childOwners.get(0).getId(), is(organization.getId()));
    assertThat(childOwners.get(1).getId(), is(RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetChildOwners_RepositoryContainer() {
    List<Owner> childOwners = ownerDAO.getChildOwners(RepositoryContainer.SINGLETON);
    assertThat(childOwners, hasSize(1));
    assertThat(childOwners.get(0).getId(), is(repository.getId()));
  }

  @Test
  public void testGetChildOwners_Repository() {
    assertThat(ownerDAO.getChildOwners(repository), hasSize(0));
  }
}
