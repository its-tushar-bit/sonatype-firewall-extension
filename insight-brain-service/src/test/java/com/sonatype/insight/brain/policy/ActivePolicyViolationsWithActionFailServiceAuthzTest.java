/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ActivePolicyViolationsWithActionFailServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ActivePolicyViolationsWithActionFailService activePolicyViolationsWithActionFailService;

  @Test(expected = UnauthenticatedException.class)
  public void getActiveViolationsWithActionFail_Anon() throws Exception {
    activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
        app.getPublicId(), "irrelevant");
  }

  @Test(expected = UnauthorizedException.class)
  public void getActiveViolationsWithActionFail_Unauthorized() throws Exception {
    login();
    activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
        app.getPublicId(), "irrelevant");
  }

  @Test
  public void getActiveViolationsWithActionFail_Authorized() throws Exception {
    grantReadPermission(app.getId());
    activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
        app.getPublicId(), "proxy");
  }
}
