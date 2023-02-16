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
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerHierarchyDTO.OwnerHierarchyOrganizationDTO;
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
    Organization organization = tempEntity.newOrganization("org1");
    Application application = tempEntity.newApplication(organization.getId());
    Organization organizationChildOrg = tempEntity.newOrganization("org1 child1", organization);
    Application organizationChildOrgApplication = tempEntity.newApplication(organizationChildOrg.getId());

    // no permissions
    OwnerHierarchyDTO ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).isEmpty();

    // limited permission. Returns a synthetic common ancestor organization
    grantReadPermission(organizationChildOrgApplication.getId());
    grantReadPermission(application.getId());

    ownerHierarchyDTO = sidebarService.getOwnerList();
    assertThat(ownerHierarchyDTO.ownersMap).hasSize(4);

    OwnerHierarchyOrganizationDTO rootOrganizationDTO = (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(
        ownerHierarchyDTO.topParentOrganizationId
    );
    assertThat(rootOrganizationDTO.id).isEqualTo(organization.getId());
    assertThat(rootOrganizationDTO.organizationIds).hasSize(1);
    assertThat(rootOrganizationDTO.synthetic).isTrue();
    assertThat(rootOrganizationDTO.applicationIds).hasSize(1);

    OwnerHierarchyApplicationDTO applicationDTO = (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(
        rootOrganizationDTO.applicationIds.get(0)
    );
    assertThat(applicationDTO.id).isEqualTo(application.getId());

    // full permission. Returns root organization
    grantReadPermission(Organization.ROOT_ORGANIZATION_ID);
    ownerHierarchyDTO = sidebarService.getOwnerList();
    // There are 2 additional entities created by default before running this test.
    // assertThat(ownerListDTO.ownersMap).hasSize(5);

    rootOrganizationDTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(ownerHierarchyDTO.topParentOrganizationId);
    assertThat(rootOrganizationDTO.organizationIds).hasSize(2);
    assertThat(rootOrganizationDTO.synthetic).isFalse();

    OwnerHierarchyOrganizationDTO organizationDTO =
        (OwnerHierarchyOrganizationDTO) ownerHierarchyDTO.ownersMap.get(organization.getId());
    assertThat(organizationDTO.id).isEqualTo(organization.getId());
    assertThat(organizationDTO.synthetic).isFalse();
    assertThat(organizationDTO.applicationIds).hasSize(1);

    applicationDTO =
        (OwnerHierarchyApplicationDTO) ownerHierarchyDTO.ownersMap.get(organizationDTO.applicationIds.get(0));
    assertThat(applicationDTO.id).isEqualTo(application.getId());
  }
}
