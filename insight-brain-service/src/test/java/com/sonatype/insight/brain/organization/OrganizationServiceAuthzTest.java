/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private OrganizationService organizationService;

  @Test
  public void testGetAllWith_Authorized() throws Exception {
    grantReadPermission(org.getId());

    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).hasSize(1);
    final Organization organization = organizations.get(0);
    assertThat(organization.getId()).isEqualTo(org.getId());
    assertThat(organization.getName()).isEqualTo(org.getName());
  }

  @Test
  public void testGetAllWith_Unauthorized() throws Exception {
    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testAddOrganization_Authorized() throws Exception {
    grantWritePermission();

    final Organization orgToAdd = new Organization();
    orgToAdd.setName("MyOrg");
    final Organization addedOrg = organizationService.addOrganization(orgToAdd);
    tempEntity.register(addedOrg);
    assertThat(addedOrg.getId()).isEqualTo(orgToAdd.getId());
    assertThat(addedOrg.getName()).isEqualTo(orgToAdd.getName());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddOrganization_Unauthenticated() throws Exception {
    final Organization orgToAdd = new Organization();
    orgToAdd.setName("MyOrg");

    organizationService.addOrganization(orgToAdd);
  }

  @Test
  public void testUpdateOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    final Organization orgToUpdate = new Organization();
    orgToUpdate.setId(org.getId());
    orgToUpdate.setName("MyOrg");

    final Organization updateOrganization = organizationService.updateOrganization(orgToUpdate);
    assertThat(updateOrganization.getId()).isEqualTo(orgToUpdate.getId());
    assertThat(updateOrganization.getName()).isEqualTo(orgToUpdate.getName());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateOrganization_Unauthenticated() throws Exception {
    final Organization orgToUpdate = new Organization();
    orgToUpdate.setName("MyOrg");
    orgToUpdate.setId(org.getId());

    organizationService.addOrganization(orgToUpdate);
  }

  @Test
  public void testDeleteOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    organizationService.deleteOrganization(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteOrganization_Unauthenticated() throws Exception {
    organizationService.deleteOrganization(org.getId());
  }
}
