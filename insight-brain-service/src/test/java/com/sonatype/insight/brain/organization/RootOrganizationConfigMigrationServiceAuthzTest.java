/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class RootOrganizationConfigMigrationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RootOrganizationConfigMigrationService service;

  @Test(expected = UnauthenticatedException.class)
  public void testSetRootOrganizationTemplate_Unauthenticated() throws IOException {
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetRootOrganizationTemplate_Unauthorized() throws IOException {
    login();
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetRootOrganizationTemplate_OrgPerm() throws IOException {
    grantWritePermission(org.getId());
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test
  public void testSetRootOrganizationTemplate() throws IOException {
    grantWritePermission();
    service.setRootOrganizationTemplate(org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetRootOrganizationEmptyTemplate_Unauthenticated() throws IOException {
    service.setRootOrganizationEmptyTemplate();
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetRootOrganizationEmptyTemplate_Unauthorized() throws IOException {
    login();
    service.setRootOrganizationEmptyTemplate();
  }

  @Test
  public void testSetRootOrganizationEmptyTemplate() throws IOException {
    grantWritePermission();
    service.setRootOrganizationEmptyTemplate();
  }
}
