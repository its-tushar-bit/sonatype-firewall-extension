/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class UserTokenServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private UserTokenService userTokenService;

  @Test
  public void testPurgeUserTokens_Authorized() throws Exception {
    grantConfigureSystemPermission();
    userTokenService.purgeUserTokens();
  }

  @Test
  public void testPurgeUserTokens_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class, () -> userTokenService.purgeUserTokens());
  }

  @Test
  public void testPurgeUserTokens_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class, () -> userTokenService.purgeUserTokens());
  }

  @Test
  public void testGetUserTokensCreatedBetweenAndRealmId_Authorized() {
    grantConfigureSystemPermission();
    userTokenService.getUserTokensCreatedBetweenAndRealmId(null, null, null);
  }

  @Test
  public void testGetUserTokensCreatedBetweenAndRealmId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> userTokenService.getUserTokensCreatedBetweenAndRealmId(null, null, null));
  }

  @Test
  public void testGetUserTokensCreatedBetweenAndRealmId_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> userTokenService.getUserTokensCreatedBetweenAndRealmId(null, null, null));
  }

  @Test
  public void testDeleteUserTokenByUserCode_Authorized() {
    UserToken userToken = tempEntity.newUserToken("john.doe", InternalRealm.ID);
    grantConfigureSystemPermission();
    userTokenService.deleteUserTokenByUserCode(userToken.getUserCode());
  }

  @Test
  public void testDeleteUserTokenByUserCode_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> userTokenService.deleteUserTokenByUserCode("a-user-code"));
  }

  @Test
  public void testDeleteUserTokenByUserCode_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> userTokenService.deleteUserTokenByUserCode("a-user-code"));
  }

  @Test
  public void testUserTokenExistsForCurrentUser_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> userTokenService.userTokenExistsForCurrentUser());
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> userTokenService.getUserTokenByUsernameAndRealmId(null, null));
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> userTokenService.getUserTokenByUsernameAndRealmId(null, null));
  }

  @Test
  public void testGetUserTokenByUsernameAndRealmId_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class,
        () -> userTokenService.getUserTokenByUsernameAndRealmId(null, null));
  }
}
