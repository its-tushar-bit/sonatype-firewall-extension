/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ActivePolicyViolationsWithActionFailServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ActivePolicyViolationsWithActionFailService activePolicyViolationsWithActionFailService;

  @Test
  public void getActiveViolationsWithActionFail_Anon() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
            app.getPublicId(), "irrelevant"));
  }

  @Test
  public void getActiveViolationsWithActionFail_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
            app.getPublicId(), "irrelevant"));
  }

  @Test
  public void getActiveViolationsWithActionFail_Authorized() throws Exception {
    grantReadPermission(app.getId());
    activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
        app.getPublicId(), "proxy");
  }
}
