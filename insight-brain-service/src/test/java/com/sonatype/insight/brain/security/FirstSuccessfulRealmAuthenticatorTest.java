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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> firstSuccessfulRealmAuthenticator.doMultiRealmAuthentication(realms, token)).isEqualTo(cause);
  }

  @Test
  public void testDoMultiRealmAuthenticationWithUnsupportedRealm() {

    final Realm realm = mock(Realm.class);
    final Collection<Realm> realms = Collections.singletonList(realm);

    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(
            () -> firstSuccessfulRealmAuthenticator.doMultiRealmAuthentication(realms, mock(AuthenticationToken.class)))
        .withMessageStartingWith("Authentication token of type [");
  }
}
