/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Iterator;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class UserTokenRealmTest
    extends AbstractComponentTest
{
  @Inject
  private UserTokenRealm realm;

  @Inject
  private PasswordService passwordService;

  @Test
  public void testDoGetAuthenticationInfo() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    User user = tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal).isEqualTo(new UserPrincipal(username, user.calculateDisplayName(), UserTokenRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testDoGetAuthenticationInfo_NoUserToken() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("Yeti", "TestPassword");
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    assertThat(authenticationInfo).isNull();
  }

  @Test
  public void testDoGetAuthenticationInfo_WrongPassword() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    User user = tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal).isEqualTo(new UserPrincipal(username, user.calculateDisplayName(), UserTokenRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    // The credentials must be the hashed password from the db, not the (wrong) password or its hash passed in as param.
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  @Test
  public void testDoGetAuthenticationInfo_NullUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(null /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> {
      realm.doGetAuthenticationInfo(usernamePasswordToken);
    }).withMessage("The username is required");
  }

  @Test
  public void testDoGetAuthenticationInfo_EmptyUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(" " /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(() -> {
      realm.doGetAuthenticationInfo(usernamePasswordToken);
    }).withMessage("The username is required");
  }

  /**
   * testing public api
   * principal is populated with the username
   * expect that public access preserves the information populated privately; note we expect this, but don't really
   * care since we don't use the extra info
   */
  @Test
  public void testGetAuthenticationInfo() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    User user = tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), userTokenPassword);
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal).isEqualTo(new UserPrincipal(username, user.calculateDisplayName(), UserTokenRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(hashedUserTokenPassword);
  }

  /**
   * testing public api
   * found credentials are checked, bad credentials indicated with exception
   * our class should not throw a different exception
   */
  @Test
  public void testGetAuthenticationInfo_WrongPassword() {
    String username = "JohnDoe";
    String userTokenPassword = "TestPassword";
    String hashedUserTokenPassword = passwordService.encryptPassword(userTokenPassword);
    tempEntity.newUser(username);
    UserToken userToken = tempEntity.newUserToken(username, "TestUserCode", hashedUserTokenPassword, InternalRealm.ID);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(userToken.getUserCode(), "WrongPassword");
    assertThatExceptionOfType(IncorrectCredentialsException.class).isThrownBy(() -> {
      realm.getAuthenticationInfo(usernamePasswordToken);
    });
  }
}
