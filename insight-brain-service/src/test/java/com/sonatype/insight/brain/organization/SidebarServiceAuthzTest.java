/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.organization.OwnerListDTO.SidebarApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerListDTO.SidebarOrganizationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SidebarServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SidebarService sidebarService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetOwnerDetails_Unauthenticated() {
    sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetOwnerDetails_Unauthorized() {
    login();
    sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetOwnerDetails_Authorized() {
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);

    sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testGetOwnerList() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());

    OwnerListDTO ownerListDTO = sidebarService.getOwnerList();
    assertThat(ownerListDTO.organizations).isEmpty();

    grantReadPermission(application.getId());
    ownerListDTO = sidebarService.getOwnerList();
    assertOwnerListDTO(ownerListDTO, organization, true, application);

    grantReadPermission(organization.getId());
    ownerListDTO = sidebarService.getOwnerList();
    assertOwnerListDTO(ownerListDTO, organization, false, application);
  }

  private static void assertOwnerListDTO(OwnerListDTO ownerListDTO,
                                         Organization organization,
                                         boolean synthetic,
                                         Application application)
  {
    assertThat(ownerListDTO.organizations).hasSize(1);

    SidebarOrganizationDTO organizationDTO = ownerListDTO.organizations.get(0);
    assertThat(organizationDTO.id).isEqualTo(organization.getId());
    assertThat(organizationDTO.synthetic).isEqualTo(synthetic);
    assertThat(organizationDTO.applications).hasSize(1);

    SidebarApplicationDTO applicationDTO = organizationDTO.applications.get(0);
    assertThat(applicationDTO.id).isEqualTo(application.getId());
  }
}
