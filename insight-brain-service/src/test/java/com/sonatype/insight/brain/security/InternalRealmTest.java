/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Iterator;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

/**
 * @since 1.7
 */
@ExtendWith(MockitoExtension.class)
public class InternalRealmTest
{
  private static final String USER_PASSWORD_HASH =
      "$shiro1$SHA-256$10$Gsv3gW95oRKzzxp37k/wJA==$T2VDhMzPuXN7VTobkLUcwDsxxJJXj5pInbW7YUn8muY=";

  private static final String USER_PASSWORD_CLEAR = "secret";

  private static final String ADMIN_PASSWORD_HASH =
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=";

  @Mock
  private UserDAO userDAO;

  private InternalRealm realm;

  private User testUser;

  @BeforeEach
  public void setUp() {
    PasswordService passwordService = new PasswordService();
    realm = new InternalRealm(passwordService, userDAO);

    // Set up test user — password field stores the hash (as returned by the DAO)
    testUser = new User("JohnDoe", USER_PASSWORD_HASH, "John", "Doe", "john.doe@example.com");
  }

  private void stubAdminUser() {
    User adminUser = new User(User.ADMIN_USERNAME, ADMIN_PASSWORD_HASH, "Admin", "BuiltIn",
        "admin@example.com");
    when(userDAO.getByUsername(User.ADMIN_USERNAME)).thenReturn(adminUser);
  }

  /**
   * testing internals; that we've implemented the abstract doGetAuthenticationInfo correctly
   * auth info indicates account found
   */
  @Test
  public void testDoGetAuthenticationInfo_ValidUser() {
    when(userDAO.getByUsername("jOhndoE")).thenReturn(testUser);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("jOhndoE",
        USER_PASSWORD_CLEAR.toCharArray());
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertThat(principalCollection).isNotEmpty();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal)
        .isEqualTo(new UserPrincipal(testUser.getUsername(), testUser.calculateDisplayName(), InternalRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(USER_PASSWORD_HASH);
  }

  /**
   * testing internals
   * auth info indicates no account found; null value
   */
  @Test
  public void testDoGetAuthenticationInfo_UnknownUser() {
    when(userDAO.getByUsername("Yeti")).thenReturn(null);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("Yeti", "admin123".toCharArray());
    AuthenticationInfo authenticationInfo = realm.doGetAuthenticationInfo(usernamePasswordToken);
    assertThat(authenticationInfo).isNull();
  }

  /**
   * testing internals - auth info indicates account found but wrong password
   */
  @Test
  public void testDoGetAuthenticationInfo_WrongPassword() {
    stubAdminUser();
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
    assertThat(authenticationInfo.getCredentials()).isEqualTo(ADMIN_PASSWORD_HASH);
  }

  /**
   * testing internals - null user name input is not tolerated
   */
  @Test
  public void testDoGetAuthenticationInfo_NullUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(null /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> realm.doGetAuthenticationInfo(usernamePasswordToken))
        .withMessage("The username is required");
  }

  /**
   * testing internals - empty user name input is not tolerated
   */
  @Test
  public void testDoGetAuthenticationInfo_EmptyUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(" " /* username */, (char[]) null);
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(() -> realm.doGetAuthenticationInfo(usernamePasswordToken))
        .withMessage("The username is required");
  }

  /**
   * testing public api - getAuthenticationInfo validates credentials via PasswordMatcher
   */
  @Test
  public void testGetAuthenticationInfo_ValidUser() {
    when(userDAO.getByUsername("jOhndoE")).thenReturn(testUser);

    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("jOhndoE",
        USER_PASSWORD_CLEAR.toCharArray());
    AuthenticationInfo authenticationInfo = realm.getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertThat(principalCollection).isNotEmpty();
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertThat(principal)
        .isEqualTo(new UserPrincipal(testUser.getUsername(), testUser.calculateDisplayName(), InternalRealm.ID));
    assertThat(principalIterator.hasNext()).isFalse();
    assertThat(principalCollection.getRealmNames()).containsExactlyInAnyOrder(realm.getName());
    assertThat(authenticationInfo.getCredentials()).isEqualTo(USER_PASSWORD_HASH);
  }

  /**
   * testing public api - found credentials are checked, bad credentials indicated with exception
   */
  @Test
  public void testGetAuthenticationInfo_WrongPassword() {
    stubAdminUser();
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "Oops! Wrong password!".toCharArray());
    assertThatExceptionOfType(IncorrectCredentialsException.class)
        .isThrownBy(() -> realm.getAuthenticationInfo(usernamePasswordToken));
  }
}
