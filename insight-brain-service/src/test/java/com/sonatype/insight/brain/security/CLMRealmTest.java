/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Iterator;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.User;

import org.sonatype.guice.bean.containers.InjectedTest;

import org.apache.shiro.authc.AuthenticationInfo;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @since 1.7
 */
public class CLMRealmTest
    extends InjectedTest
{
  @Inject
  private CLMRealm realm;

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
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "admin123".toCharArray());
    AuthenticationInfo authenticationInfo = realm
        .doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertNotNull(principalCollection);
    assertFalse(principalCollection.isEmpty());
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertEquals(principal, User.ADMIN_USERNAME);
    assertFalse(principalIterator.hasNext());
    assertThat(principalCollection.getRealmNames(), hasSize(1));
    assertEquals(realm.getName(), principalCollection.getRealmNames().iterator().next());
    assertThat(
        (char[]) authenticationInfo.getCredentials(),
        is("$shiro1$SHA-256$500000$MQE0sE4AN/+RmveFR2MruQ==$AnBUsybg4CT8HjK7zofGD9A+3xdDZTpUVDpp/K7wX9M=".toCharArray()));
  }

  /**
   * testing internals
   * auth info indicates no account found; null value
   */
  @Test
  public void testDoGetAuthenticationInfo_UnknownUser() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("Yeti", "admin123".toCharArray());
    AuthenticationInfo authenticationInfo = realm
        .doGetAuthenticationInfo(usernamePasswordToken);
    assertThat(authenticationInfo, nullValue());
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
    AuthenticationInfo authenticationInfo = realm
        .doGetAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertNotNull(principalCollection);
    assertFalse(principalCollection.isEmpty());
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertEquals(principal, User.ADMIN_USERNAME);
    assertFalse(principalIterator.hasNext());
    assertThat(principalCollection.getRealmNames(), hasSize(1));
    assertEquals(realm.getName(), principalCollection.getRealmNames().iterator().next());
    // The credentials must be the hashed password from the db, not the (wrong) password or its hash passed in as param.
    assertThat(
        (char[]) authenticationInfo.getCredentials(),
        is("$shiro1$SHA-256$500000$MQE0sE4AN/+RmveFR2MruQ==$AnBUsybg4CT8HjK7zofGD9A+3xdDZTpUVDpp/K7wX9M=".toCharArray()));
  }

  /**
   * testing internals
   * null user name input is not tolerated
   */
  @Test
  public void testDoGetAuthenticationInfo_NullUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(null /* username */, (char[]) null);
    try {
      realm.doGetAuthenticationInfo(usernamePasswordToken);
      fail("Expected AuthenticationException");
    }
    catch (AuthenticationException expected) {
      assertThat(expected.getMessage(), is("The username is required"));
    }
  }

  /**
   * testing internals
   * empty user name input is not tolerated
   */
  @Test
  public void testDoGetAuthenticationInfo_EmptyUserName() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(" " /* username */, (char[]) null);
    try {
      realm.doGetAuthenticationInfo(usernamePasswordToken);
      fail("Expected AuthenticationException");
    }
    catch (AuthenticationException expected) {
      assertThat(expected.getMessage(), is("The username is required"));
    }
  }

  /**
   * testing public api
   * principal is populated with the username
   * expect that public access preserves the information populated privately; note we expect this, but don't really 
   * care since we don't use the extra info
   */
  @Test
  public void testGetAuthenticationInfo_ValidUser() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "admin123".toCharArray());
    AuthenticationInfo authenticationInfo = realm
        .getAuthenticationInfo(usernamePasswordToken);
    PrincipalCollection principalCollection = authenticationInfo.getPrincipals();
    assertNotNull(principalCollection);
    assertFalse(principalCollection.isEmpty());
    Iterator<?> principalIterator = principalCollection.iterator();
    Object principal = principalIterator.next();
    assertEquals(principal, User.ADMIN_USERNAME);
    assertFalse(principalIterator.hasNext());
    assertThat(principalCollection.getRealmNames(), hasSize(1));
    assertEquals(realm.getName(), principalCollection.getRealmNames().iterator().next());
    assertThat(
        (char[]) authenticationInfo.getCredentials(),
        is("$shiro1$SHA-256$500000$MQE0sE4AN/+RmveFR2MruQ==$AnBUsybg4CT8HjK7zofGD9A+3xdDZTpUVDpp/K7wX9M=".toCharArray()));
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
    try {
      realm.getAuthenticationInfo(usernamePasswordToken);
      fail("Expected IncorrectCredentialsException");
    }
    catch (IncorrectCredentialsException expected) {
    }
  }

  @Test
  public void testEncryptPassword() {
    char[] password = "admin123".toCharArray();
    String encrypted = realm.encryptPassword(password);
    assertThat(encrypted, notNullValue());
    assertThat(encrypted, startsWith("$shiro1$"));
    assertThat(encrypted.length(), greaterThan(8));
    for (char c : password) {
      assertThat(c, is((char) 0));
    }
  }

  @Test
  public void testEncryptPassword_Null() {
    String encrypted = realm.encryptPassword(null);
    assertThat(encrypted, nullValue());
  }
}
