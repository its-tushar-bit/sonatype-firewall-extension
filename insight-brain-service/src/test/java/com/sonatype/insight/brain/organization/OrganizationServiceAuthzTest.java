/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

public class OrganizationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private OrganizationService organizationService;


  @Test
  public void testGetAllWith_Authorized() throws Exception {
    grantReadPermission(org.getId());

    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations, hasSize(1));
    final Organization organization = organizations.get(0);
    assertThat(organization.getId(), is(org.getId()));
    assertThat(organization.getName(), is(org.getName()));
  }

  @Test
  public void testGetAllWith_Unauthorized() throws Exception {
    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations, hasSize(0));
  }

  @Test
  public void testAddOrganization_Authorized() throws Exception {
    grantWritePermission();

    final Organization orgToAdd = new Organization();
    orgToAdd.setName("MyOrg");
    final Organization addedOrg = organizationService.addOrganization(orgToAdd);
    tempEntity.register(addedOrg);
    assertThat(addedOrg.getId(), is(orgToAdd.getId()));
    assertThat(addedOrg.getName(), is(orgToAdd.getName()));
  }

  @Test
  public void testAddOrganization_Unauthenticated() throws Exception {
    final Organization orgToAdd = new Organization();
    orgToAdd.setName("MyOrg");

    try {
      organizationService.addOrganization(orgToAdd);
      fail("Expected UnauthenticatedException");
    }
    catch (UnauthenticatedException ignore) {
      // Properly thrown exception
    }
  }

  @Test
  public void testUpdateOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    final Organization orgToUpdate = new Organization();
    orgToUpdate.setId(org.getId());
    orgToUpdate.setName("MyOrg");

    final Organization updateOrganization = organizationService.updateOrganization(orgToUpdate);
    assertThat(updateOrganization.getId(), is(orgToUpdate.getId()));
    assertThat(updateOrganization.getName(), is(orgToUpdate.getName()));
  }

  @Test
  public void testUpdateOrganization_Unauthenticated() throws Exception {
    final Organization orgToUpdate = new Organization();
    orgToUpdate.setName("MyOrg");
    orgToUpdate.setId(org.getId());

    try {
      organizationService.addOrganization(orgToUpdate);
      fail("Expected UnauthenticatedException");
    }
    catch (UnauthenticatedException ignore) {
      // Properly thrown exception
    }
  }

  @Test
  public void testDeleteOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    organizationService.deleteOrganization(org.getId());
  }

  @Test
  public void testDeleteOrganization_Unauthenticated() throws Exception {
    try {
      organizationService.deleteOrganization(org.getId());
      fail("Expected UnauthenticatedException");
    }
    catch (UnauthenticatedException ignore) {
      // Properly thrown exception
    }
  }
}
