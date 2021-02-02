/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class OwnerServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private OwnerService ownerService;

  @Inject
  private OwnerDAO ownerDAO;

  @Test(expected = UnauthenticatedException.class)
  public void testGetHierarchy_RootOrganization_Unauthenticated() {
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    ownerService.getHierarchy(rootOrg.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetHierarchy_RootOrganization_Unauthorized() {
    login();
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    ownerService.getHierarchy(rootOrg.getPublicId());
  }

  @Test
  public void testGetHierarchy_RootOrganization_Authorized() {
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    grantPermission(rootOrg.getId(), Permission.READ);
    ownerService.getHierarchy(rootOrg.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetHierarchy_Organization_Unauthenticated() {
    ownerService.getHierarchy(org.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetHierarchy_Organization_Unauthorized() {
    login();
    ownerService.getHierarchy(org.getPublicId());
  }

  @Test
  public void testGetHierarchy_Organization_Authorized() {
    grantPermission(org.getId(), Permission.READ);
    ownerService.getHierarchy(org.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetHierarchy_Application_Unauthenticated() {
    ownerService.getHierarchy(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetHierarchy_Application_Unauthorized() {
    login();
    ownerService.getHierarchy(app.getPublicId());
  }

  @Test
  public void testGetHierarchy_Application_Authorized() {
    grantPermission(app.getId(), Permission.READ);
    ownerService.getHierarchy(app.getPublicId());
  }
}
