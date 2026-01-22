/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Iterator;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @since 1.7
 */
public class InternalRealmTest
    extends BrainInjectedTest
{
  @Inject
  private InternalRealm realm;

  /**
   * testing internals; that we've implemented the abstract doGetAuthenticationInfo correctly
   * auth info indicates account found
   * auth info comprised of:
   * - 1 principal
   * - principal is from the clm realm
   * - principal value is the username
   * - credentials in expected string format
   */
  @Test
  public void testDoGetAuthenticationInfo_ValidUser() {
    User user = tempEntity.newUser("JohnDoe", "John", "Doe", "john.doe@example.com");

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("jOhndoE", user.getPassword());
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertThat(principalCollection).isNotEmpty();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal)
        .isEqualTo(new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(TemporaryEntity.USER_PASSWORD_HASH);
  }

  /**
   * testing internals
   * auth info indicates no account found; null value
   */
  @Test
  public void testDoGetAuthenticationInfo_UnknownUser() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("Yeti", "admin123".toCharArray());
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    assertThat(authenticationInfo).isNull();
  }

  /**
   * testing internals
   * auth info indicates account found
   * call to auth does not compare the credentials, that's left to Shiro and the public method
   * auth info comprised of:
   * - 1 principal
   * - principal is from the clm realm
   * - principal value is the username
   * - credentials in expected string format
   */
  @Test
  public void testDoGetAuthenticationInfo_WrongPassword() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "Oops! Wrong password!".toCharArray());
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertThat(principalCollection).isNotEmpty();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal).isEqualTo(new UserPrincipal(User.ADMIN_USERNAME, "Admin BuiltIn", InternalRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    // The credentials must be the hashed password from the db, not the (wrong) password or its hash passed in as param.
    assertThat(authenticationInfo.getCredentials())
        .isEqualTo("$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=");
  }

  /**
   * testing internals
   * null user name input is not tolerated
   */
  @Test
  public void testDoGetAuthenticationInfo_NullUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(null /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> realm.doGetAuthenticationInfo(usernamePasswordToken)).withMessage("The username is required");
  }

  /**
   * testing internals
   * empty user name input is not tolerated
   */
  @Test
  public void testDoGetAuthenticationInfo_EmptyUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(" " /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> realm.doGetAuthenticationInfo(usernamePasswordToken)).withMessage("The username is required");
  }

  /**
   * testing public api
   * principal is populated with the username
   * expect that public access preserves the information populated privately; note we expect this, but don't really
   * care since we don't use the extra info
   */
  @Test
  public void testGetAuthenticationInfo_ValidUser() {
    User user = tempEntity.newUser("JohnDoe", "John", "Doe", "john.doe@example.com");

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("jOhndoE", user.getPassword());
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertThat(principalCollection).isNotEmpty();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal)
        .isEqualTo(new UserPrincipal(user.getUsername(), user.calculateDisplayName(), InternalRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(TemporaryEntity.USER_PASSWORD_HASH);
  }

  /**
   * testing public api
   * found credentials are checked, bad credentials indicated with exception
   * our class should not throw a different exception
   */
  @Test
  public void testGetAuthenticationInfo_WrongPassword() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "Oops! Wrong password!".toCharArray());
    assertThatExceptionOfType(IncorrectCredentialsException.class)
        .isThrownBy(() -> realm.getAuthenticationInfo(usernamePasswordToken));
  }
}
