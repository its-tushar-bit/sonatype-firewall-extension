/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
}
