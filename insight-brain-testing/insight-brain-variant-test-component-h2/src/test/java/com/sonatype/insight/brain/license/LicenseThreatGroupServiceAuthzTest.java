/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class LicenseThreatGroupServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private LicenseThreatGroupService licenseThreatGroupService;

  @Test
  public void testGetLicenseThreatGroupsForApplication_Authorized() {
    grantReadPermission(app.getId());
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetLicenseThreatGroupsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    licenseThreatGroupService.getLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetLicenseThreatGroupsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
    });
  }

  @Test
  public void testGetLicenseThreatGroupsForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
    });
  }

  @Test
  public void testGetLicenseThreatGroupsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
    });
  }

  @Test
  public void testGetLicenseThreatGroupsForOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
    });
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForApplication_Authorized() {
    grantReadPermission(app.getId());
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
    });
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForOrganiation_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
    });
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.APPLICATION, app.getPublicId());
    });
  }

  @Test
  public void testGetApplicableLicenseThreatGroupsForOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.getApplicableLicenseThreatGroups(OwnerType.ORGANIZATION, org.getId());
    });
  }

  @Test
  public void testAddLicenseThreatGroupForOrganization_Authorized() {
    grantWritePermission(org.getId());
    licenseThreatGroupService.addLicenseThreatGroup(org.getId(), new LicenseThreatGroup(null,
        "Test LTG", 5));
  }

  @Test
  public void testAddLicenseThreatGroupForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.addLicenseThreatGroup(org.getId(), new LicenseThreatGroup(null,
          "Test LTG", 5));
    });
  }

  @Test
  public void testAddLicenseThreatGroupForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.addLicenseThreatGroup(org.getId(), new LicenseThreatGroup(null,
          "Test LTG", 5));
    });
  }

  @Test
  public void testUpdateLicenseThreatGroupForApplication_Authorized() {
    grantWritePermission(app.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), licenseThreatGroup);
  }

  @Test
  public void testUpdateLicenseThreatGroupForOrganization_Authorized() {
    grantWritePermission(org.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), licenseThreatGroup);
  }

  @Test
  public void testUpdateLicenseThreatGroupForApplication_Unauthenticated() {
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId());
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), licenseThreatGroup);
    });
  }

  @Test
  public void testUpdateLicenseThreatGroupForOrganization_Unauthenticated() {
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId());
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), licenseThreatGroup);
    });
  }

  @Test
  public void testUpdateLicenseThreatGroupForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(app.getId());
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), licenseThreatGroup);
    });
  }

  @Test
  public void testUpdateLicenseThreatGroupForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(org.getId());
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.updateLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), licenseThreatGroup);
    });
  }

  @Test
  public void testDeleteLicenseThreatGroupForApplication_Authorized() {
    grantWritePermission(app.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), ltg.getId());
  }

  @Test
  public void testDeleteLicenseThreatGroupForOrganization_Authorized() {
    grantWritePermission(org.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(org.getId());
    licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), ltg.getId());
  }

  @Test
  public void testDeleteLicenseThreatGroupForApplication_Unauthenticated() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), ltg.getId());
    });
  }

  @Test
  public void testDeleteLicenseThreatGroupForOrganization_Unauthenticated() {
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(org.getId());
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), ltg.getId());
    });
  }

  @Test
  public void testDeleteLicenseThreatGroupForApplication_Unauthorized() {
    grantReadPermission(app.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(app.getId());
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.APPLICATION, app.getPublicId(), ltg.getId());
    });
  }

  @Test
  public void testDeleteLicenseThreatGroupForOrganization_Unauthorized() {
    grantReadPermission(org.getId());
    LicenseThreatGroup ltg = tempEntity.newLicenseThreatGroup(org.getId());
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.deleteLicenseThreatGroup(OwnerType.ORGANIZATION, org.getId(), ltg.getId());
    });
  }

  // CLM-39702 — counts endpoint READ permission enforcement

  @Test
  public void testGetLicenseThreatGroupCountsForApplication_Authorized() {
    grantReadPermission(app.getId());
    licenseThreatGroupService.getLicenseThreatGroupCounts(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetLicenseThreatGroupCountsForOrganization_Authorized() {
    grantReadPermission(org.getId());
    licenseThreatGroupService.getLicenseThreatGroupCounts(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetLicenseThreatGroupCountsForApplication_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroupCounts(OwnerType.APPLICATION, app.getPublicId());
    });
  }

  @Test
  public void testGetLicenseThreatGroupCountsForOrganization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroupCounts(OwnerType.ORGANIZATION, org.getId());
    });
  }

  @Test
  public void testGetLicenseThreatGroupCountsForApplication_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroupCounts(OwnerType.APPLICATION, app.getPublicId());
    });
  }

  @Test
  public void testGetLicenseThreatGroupCountsForOrganization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> {
      licenseThreatGroupService.getLicenseThreatGroupCounts(OwnerType.ORGANIZATION, org.getId());
    });
  }
}
