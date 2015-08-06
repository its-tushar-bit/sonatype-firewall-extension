/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class LicenseThreatGroupServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private LicenseThreatGroupService licenseThreatGroupService;

  @Test
  public void testGetLicenseThreatGroupsForApplication_Authorized() throws Exception {
    grantReadPermission(app.getId());
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetLicenseThreatGroupsForOrganization_Authorized() throws Exception {
    grantReadPermission(org.getId());
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseThreatGroupsForApplication_Unauthenticated() throws Exception {
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLicenseThreatGroupsForOrganization_Unauthenticated() throws Exception {
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseThreatGroupsForApplication_Unauthorized() throws Exception {
    login();
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLicenseThreatGroupsForOrganization_Unauthorized() throws Exception {
    login();
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForApplication_Authorized() throws Exception {
    grantReadPermission(app.getId());
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForOrganization_Authorized() throws Exception {
    grantReadPermission(org.getId());
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLicenseThreatGroupsForApplication_Unauthenticated() throws Exception {
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableLicenseThreatGroupsForOrganiation_Unauthenticated() throws Exception {
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLicenseThreatGroupsForApplication_Unauthorized() throws Exception {
    login();
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableLicenseThreatGroupsForOrganization_Unauthorized() throws Exception {
    login();
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testAddLicenseThreatGroupForApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    licenseThreatGroupService.addLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(),
        new LicenseThreatGroup(null, "Test LTG", 5));
  }

  @Test
  public void testAddLicenseThreatGroupForOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());
    licenseThreatGroupService.addLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(),
        new LicenseThreatGroup(null, "Test LTG", 5));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseThreatGroupForApplication_Unauthenticated() throws Exception {
    licenseThreatGroupService.addLicenseThreatGroup(OwnerType.APPLICATION,
        app.getPublicId(), new LicenseThreatGroup(null, "Test LTG", 5));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddLicenseThreatGroupForOrganization_Unauthenticated() throws Exception {
    licenseThreatGroupService.addLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(),
        new LicenseThreatGroup(null, "Test LTG", 5));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseThreatGroupForApplication_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    licenseThreatGroupService.addLicenseThreatGroup(OwnerType.APPLICATION,
        app.getPublicId(), new LicenseThreatGroup(null, "Test LTG", 5));
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddLicenseThreatGroupForOrganization_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    licenseThreatGroupService.addLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(),
        new LicenseThreatGroup(null, "Test LTG", 5));
  }

  @Test
  public void testUpdateLicenseThreatGroupForApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), licenseThreatGroup);
  }

  @Test
  public void testUpdateLicenseThreatGroupForOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), licenseThreatGroup);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLicenseThreatGroupForApplication_Unauthenticated() throws Exception {
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(),
        licenseThreatGroup);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateLicenseThreatGroupForOrganization_Unauthenticated() throws Exception {
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), licenseThreatGroup);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLicenseThreatGroupForApplication_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(),
        licenseThreatGroup);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateLicenseThreatGroupForOrganization_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), licenseThreatGroup);
  }

  @Test
  public void testDeleteLicenseThreatGroupForApplication_Authorized() throws Exception {
    grantWritePermission(app.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), ltg.getId());
  }

  @Test
  public void testDeleteLicenseThreatGroupForOrganization_Authorized() throws Exception {
    grantWritePermission(org.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), ltg.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseThreatGroupForApplication_Unauthenticated() throws Exception {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), ltg.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteLicenseThreatGroupForOrganization_Unauthenticated() throws Exception {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), ltg.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseThreatGroupForApplication_Unauthorized() throws Exception {
    grantReadPermission(app.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), ltg.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteLicenseThreatGroupForOrganization_Unauthorized() throws Exception {
    grantReadPermission(org.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), ltg.getId());
  }
}
