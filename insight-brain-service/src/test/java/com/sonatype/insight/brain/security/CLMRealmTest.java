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

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
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

  @Test
  public void testDoGetAuthenticationInfo_ValidUser() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "admin123".toCharArray());
    SimpleAuthenticationInfo authenticationInfo = (SimpleAuthenticationInfo) realm
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

  @Test
  public void testDoGetAuthenticationInfo_UnknownUser() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("Yeti", "admin123".toCharArray());
    SimpleAuthenticationInfo authenticationInfo = (SimpleAuthenticationInfo) realm
        .doGetAuthenticationInfo(usernamePasswordToken);
    assertThat(authenticationInfo, nullValue());
  }

  @Test
  public void testDoGetAuthenticationInfo_WrongPassword() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "Oops! Wrong password!".toCharArray());
    SimpleAuthenticationInfo authenticationInfo = (SimpleAuthenticationInfo) realm
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

  @Test
  public void testGetAuthenticationInfo_ValidUser() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME,
        "admin123".toCharArray());
    SimpleAuthenticationInfo authenticationInfo = (SimpleAuthenticationInfo) realm
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
