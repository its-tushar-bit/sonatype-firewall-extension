/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class UserTokenServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private UserTokenService userTokenService;

  @Test
  public void testPurgeUserTokens_Authorized() throws Exception {
    grantConfigureSystemPermission();
    userTokenService.purgeUserTokens();
  }

  @Test(expected = UnauthorizedException.class)
  public void testPurgeUserTokens_Unauthorized() throws Exception {
    login();
    userTokenService.purgeUserTokens();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPurgeUserTokens_Unauthenticated() throws Exception {
    userTokenService.purgeUserTokens();
  }
}
