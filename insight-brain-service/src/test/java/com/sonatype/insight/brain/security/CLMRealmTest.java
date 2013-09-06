/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Iterator;

import com.sonatype.insight.brain.model.security.User;

import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @since 1.7
 */
public class CLMRealmTest
{
  @Test
  public void testDoGetAuthenticationInfo() {
    CLMRealm realm = new CLMRealm();
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken(User.ADMIN_USERNAME, (char[]) null);
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
  }
}
