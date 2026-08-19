/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ApplicationCloneServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApplicationCloneService appCloneService;

  @Test
  public void testCloneApplication_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class,
        () -> appCloneService.cloneApplication(app.getId(), "clonedAppName", "clonedAppPublicId"));
  }

  @Test
  public void testCloneApplication_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class,
        () -> appCloneService.cloneApplication(app.getId(), "clonedAppName", "clonedAppPublicId"));
  }

  @Test
  public void testCloneApplication_Authorized() {
    grantAddApplicationPermission(app.getOrganizationId());
    appCloneService.cloneApplication(app.getId(), "clonedAppName", "clonedAppPublicId");
  }
}
