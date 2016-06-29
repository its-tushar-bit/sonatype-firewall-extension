/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import javax.inject.Inject;

import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ProprietaryConfigServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ProprietaryConfigService service;

  @Test
  public void testGetConfig_EvaluateApplication_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    service.getConfig(Goal.EVALUATE_APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfig_EvaluateApplication_Unauthorized() throws Exception {
    login();
    service.getConfig(Goal.EVALUATE_APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfig_EvaluateApplication_Unauthenticated() throws Exception {
    service.getConfig(Goal.EVALUATE_APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetConfig_EvaluateComponent_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_COMPONENT);
    service.getConfig(Goal.EVALUATE_COMPONENT, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfig_EvaluateComponent_Unauthorized() throws Exception {
    login();
    service.getConfig(Goal.EVALUATE_COMPONENT, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfig_EvaluateComponent_Unauthenticated() throws Exception {
    service.getConfig(Goal.EVALUATE_COMPONENT, app.getPublicId());
  }
}
