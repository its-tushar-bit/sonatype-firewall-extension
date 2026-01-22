/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApplicationCloneServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationCloneService appCloneService;

  @Test(expected = UnauthenticatedException.class)
  public void testCloneApplication_Unauthenticated() {
    appCloneService.cloneApplication(app.getId(), "clonedAppName", "clonedAppPublicId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testCloneApplication_Unauthorized() {
    login();
    appCloneService.cloneApplication(app.getId(), "clonedAppName", "clonedAppPublicId");
  }

  @Test
  public void testCloneApplication_Authorized() {
    grantAddApplicationPermission(app.getOrganizationId());
    appCloneService.cloneApplication(app.getId(), "clonedAppName", "clonedAppPublicId");
  }
}
