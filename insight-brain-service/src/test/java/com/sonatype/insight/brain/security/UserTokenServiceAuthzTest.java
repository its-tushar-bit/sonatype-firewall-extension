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
  public void testDeleteUserToken_Authorized() {
    String username = "user-a";
    tempEntity.newUserToken(username, true);
    grantConfigureSystemPermission();
    userTokenService.deleteUserToken(username);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteUserToken_Unauthorized() {
    String username = "user-a";
    login();
    userTokenService.deleteUserToken(username);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteUserToken_Unauthenticated() {
    String username = "user-a";
    userTokenService.deleteUserToken(username);
  }
}
