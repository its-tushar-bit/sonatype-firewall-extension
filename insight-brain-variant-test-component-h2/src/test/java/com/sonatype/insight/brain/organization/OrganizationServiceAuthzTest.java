/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class OrganizationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private OrganizationService organizationService;

  @Test
  public void testGetAll_Authorized() {
    grantReadPermission(org.getId());

    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).hasSize(1);
    final Organization organization = organizations.get(0);
    assertThat(organization.getId()).isEqualTo(org.getId());
    assertThat(organization.getName()).isEqualTo(org.getName());
  }

  @Test
  public void testGetAll_Unauthorized() {
    login();
    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testGetAll_Unauthenticated() {
    final List<Organization> organizations = organizationService.getAll();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testGetAllWithoutRelatedRepositories_Authorized() {
    grantReadPermission(org.getId());

    final List<Organization> organizations = organizationService.getAllWithoutRelatedRepositories();
    assertThat(organizations).hasSize(1);
    final Organization organization = organizations.get(0);
    assertThat(organization.getId()).isEqualTo(org.getId());
    assertThat(organization.getName()).isEqualTo(org.getName());
  }

  @Test
  public void testGetAllWithoutRelatedRepositories_Unauthorized() {
    login();
    final List<Organization> organizations = organizationService.getAllWithoutRelatedRepositories();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testGetAllWithoutRelatedRepositories_Unauthenticated() {
    final List<Organization> organizations = organizationService.getAllWithoutRelatedRepositories();
    assertThat(organizations).isEmpty();
  }

  @Test
  public void testAddOrganization_Authorized() {
    grantWritePermission(Organization.ROOT_ORGANIZATION_ID);

    Organization orgToAdd = new Organization("MyOrg");
    organizationService.addOrganization(orgToAdd);
  }

  @Test
  public void testAddOrganization_Unauthenticated() {
    Organization orgToAdd = new Organization("MyOrg");

    assertThrows(UnauthenticatedException.class, () -> organizationService.addOrganization(orgToAdd));
  }

  @Test
  public void testAddOrganization_Unauthorized() {
    login();
    Organization orgToAdd = new Organization("MyOrg");

    assertThrows(UnauthorizedException.class, () -> organizationService.addOrganization(orgToAdd));
  }

  @Test
  public void testUpdateOrganization_Authorized() {
    grantWritePermission(org.getId());

    organizationService.updateOrganization(org);
  }

  @Test
  public void testUpdateOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> organizationService.updateOrganization(org));
  }

  @Test
  public void testUpdateOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> organizationService.updateOrganization(org));
  }

  @Test
  public void testDeleteOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());

    organizationService.deleteOrganization(org.getId());
  }

  @Test
  public void testDeleteOrganization_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class, () -> organizationService.deleteOrganization(org.getId()));
  }

  @Test
  public void testDeleteOrganization_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class, () -> organizationService.deleteOrganization(org.getId()));
  }

  @Test
  public void testGetOrganization_Authorized() {
    grantReadPermission(org.getId());

    Organization organization = organizationService.getOrganization(org.getId());
    assertThat(organization).isNotNull();
    assertThat(organization.getId()).isEqualTo(org.getId());
    assertThat(organization.getName()).isEqualTo(org.getName());
  }

  @Test
  public void testGetOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> organizationService.getOrganization(org.getId()));
  }

  @Test
  public void testGetOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> organizationService.getOrganization(org.getId()));
  }
}
