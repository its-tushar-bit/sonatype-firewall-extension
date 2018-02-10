/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.Realm;
import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FirstSuccessfulRealmAuthenticatorTest
    extends AbstractComponentTest
{
  @Inject
  private FirstSuccessfulRealmAuthenticator firstSuccessfulRealmAuthenticator;

  @Test
  public void testDoMultiRealmAuthenticationIncludesExceptionCause() {
    final AuthenticationToken token = mock(AuthenticationToken.class);

    final Realm realm = mock(Realm.class);
    when(realm.supports(token)).thenReturn(true);

    final RuntimeException cause = new RuntimeException();
    when(realm.getAuthenticationInfo(token)).thenThrow(cause);

    final Collection<Realm> realms = Collections.singletonList(realm);

    try {
      firstSuccessfulRealmAuthenticator.doMultiRealmAuthentication(realms, token);
      fail("Expected exception");
    }
    catch (RuntimeException e) {
      assertThat(e, is(cause));
    }
  }

  @Test
  public void testDoMultiRealmAuthenticationWithUnsupportedRealm() {

    final Realm realm = mock(Realm.class);
    final Collection<Realm> realms = Collections.singletonList(realm);

    try {
      firstSuccessfulRealmAuthenticator.doMultiRealmAuthentication(realms, mock(AuthenticationToken.class));
      fail("Expected exception");
    }
    catch (AuthenticationException e) {
      assertThat(e.getMessage(), startsWith("Authentication token of type ["));
    }
  }
}
