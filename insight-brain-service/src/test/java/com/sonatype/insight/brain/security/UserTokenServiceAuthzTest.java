/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

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

  @Test
  public void testGetUserTokensCreatedBetweenAndRealmId_Authorized() {
    grantConfigureSystemPermission();
    userTokenService.getUserTokensCreatedBetweenAndRealmId(null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetUserTokensCreatedBetweenAndRealmId_Unauthorized() {
    login();
    userTokenService.getUserTokensCreatedBetweenAndRealmId(null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetUserTokensCreatedBetweenAndRealmId_Unauthenticated() {
    userTokenService.getUserTokensCreatedBetweenAndRealmId(null, null, null);
  }

  @Test
  public void testDeleteUserTokenByUserCode_Authorized() {
    UserToken userToken = tempEntity.newUserToken("john.doe", InternalRealm.ID);
    grantConfigureSystemPermission();
    userTokenService.deleteUserTokenByUserCode(userToken.getUserCode());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteUserTokenByUserCode_Unauthorized() {
    login();
    userTokenService.deleteUserTokenByUserCode("a-user-code");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteUserTokenByUserCode_Unauthenticated() {
    userTokenService.deleteUserTokenByUserCode("a-user-code");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUserTokenExistsForCurrentUser_Unauthenticated() {
    userTokenService.userTokenExistsForCurrentUser();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetUserTokenByUsernameAndRealmId_Unauthenticated() {
    userTokenService.getUserTokenByUsernameAndRealmId(null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetUserTokenByUsernameAndRealmId_Unauthorized() {
    login();
    userTokenService.getUserTokenByUsernameAndRealmId(null, null);
  }

  @Test(expected = BadRequestException.class)
  public void testGetUserTokenByUsernameAndRealmId_Authorized() {
    grantConfigureSystemPermission();
    userTokenService.getUserTokenByUsernameAndRealmId(null, null);
  }
}
