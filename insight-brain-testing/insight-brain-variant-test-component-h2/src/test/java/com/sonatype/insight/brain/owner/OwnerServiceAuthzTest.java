/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class OwnerServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private OwnerService ownerService;

  @Inject
  private OwnerDAO ownerDAO;

  @Test
  public void testGetHierarchyForRead_RootOrganization_Unauthenticated() {
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthenticatedException.class,
        () -> ownerService.getHierarchyForRead(rootOrg.getType(), rootOrg.getPublicId()));
  }

  @Test
  public void testGetHierarchyForRead_RootOrganization_Unauthorized() {
    login();
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthorizedException.class,
        () -> ownerService.getHierarchyForRead(rootOrg.getType(), rootOrg.getPublicId()));
  }

  @Test
  public void testGetHierarchyForRead_RootOrganization_Authorized() {
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    grantPermission(rootOrg.getId(), Permission.READ);
    ownerService.getHierarchyForRead(rootOrg.getType(), rootOrg.getPublicId());
  }

  @Test
  public void testGetHierarchyForRead_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ownerService.getHierarchyForRead(org.getType(), org.getPublicId()));
  }

  @Test
  public void testGetHierarchyForRead_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ownerService.getHierarchyForRead(org.getType(), org.getPublicId()));
  }

  @Test
  public void testGetHierarchyForRead_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    ownerService.getHierarchyForRead(org.getType(), org.getPublicId());
  }

  @Test
  public void testGetHierarchyForRead_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ownerService.getHierarchyForRead(app.getType(), app.getPublicId()));
  }

  @Test
  public void testGetHierarchyForRead_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ownerService.getHierarchyForRead(app.getType(), app.getPublicId()));
  }

  @Test
  public void testGetHierarchyForRead_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    ownerService.getHierarchyForRead(app.getType(), app.getPublicId());
  }

  @Test
  public void testGetHierarchyForLegalReviewer_RootOrganization_Unauthenticated() {
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthenticatedException.class,
        () -> ownerService.getHierarchyForLegalReviewer(rootOrg.getType(), rootOrg.getPublicId()));
  }

  @Test
  public void testGetHierarchyForLegalReviewer_RootOrganization_Unauthorized() {
    login();
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    assertThrows(UnauthorizedException.class,
        () -> ownerService.getHierarchyForLegalReviewer(rootOrg.getType(), rootOrg.getPublicId()));
  }

  @Test
  public void testGetHierarchyForLegalReviewer_RootOrganization_Authorized() {
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    grantPermission(rootOrg.getId(), Permission.LEGAL_REVIEWER);
    ownerService.getHierarchyForLegalReviewer(rootOrg.getType(), rootOrg.getPublicId());
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Organization_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ownerService.getHierarchyForLegalReviewer(org.getType(), org.getPublicId()));
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Organization_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ownerService.getHierarchyForLegalReviewer(org.getType(), org.getPublicId()));
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Organization_Authorized() {
    grantPermission(org.getId(), Permission.LEGAL_REVIEWER);
    ownerService.getHierarchyForLegalReviewer(org.getType(), org.getPublicId());
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Application_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ownerService.getHierarchyForLegalReviewer(app.getType(), app.getPublicId()));
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Application_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ownerService.getHierarchyForLegalReviewer(app.getType(), app.getPublicId()));
  }

  @Test
  public void testGetHierarchyForLegalReviewer_Application_Authorized() {
    grantPermission(app.getId(), Permission.LEGAL_REVIEWER);
    ownerService.getHierarchyForLegalReviewer(app.getType(), app.getPublicId());
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_Unauthenticated() {
    Map<String, Owner> ownersWithReadPermissionsById = ownerService.getOwnersWithReadPermissionsById();
    assertThat(ownersWithReadPermissionsById).isEmpty();
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_Unauthorized() {
    login();
    Map<String, Owner> ownersWithReadPermissionsById = ownerService.getOwnersWithReadPermissionsById();
    assertThat(ownersWithReadPermissionsById).isEmpty();
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_Authorized() {
    login();
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    grantPermission(rootOrg.getId(), Permission.READ);
    Map<String, Owner> ownersWithReadPermissionsById = ownerService.getOwnersWithReadPermissionsById();
    assertThat(ownersWithReadPermissionsById).isNotEmpty();
    assertThat(ownersWithReadPermissionsById).hasSize(5);
  }

  @Test
  public void testGetOwnerByTypeAndInternalId_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> ownerService.getOwnerByTypeAndInternalId(org.getType(), org.getId()));
  }

  @Test
  public void testGetOwnerByTypeAndInternalId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> ownerService.getOwnerByTypeAndInternalId(app.getType(), app.getId()));
  }

  @Test
  public void testGetOwnerByTypeAndInternalId_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    OwnerDTO result = ownerService.getOwnerByTypeAndInternalId(org.getType(), org.getId());
    assertThat(result).isNotNull();
  }
}
