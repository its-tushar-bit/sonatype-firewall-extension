/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.configuration.ldap.LdapRealm;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.testing.BrainInjectedTest;

import com.google.inject.Binder;
import org.apache.shiro.realm.CachingRealm;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RealmCachingDisabledTest
    extends BrainInjectedTest
{
  @Inject
  private Set<CachingRealm> cachingRealms;

  @Override
  protected void configure(Binder binder) {
    binder.bind(new com.google.inject.TypeLiteral<Set<CachingRealm>>() {})
        .toProvider(CachingRealmSetProvider.class);
  }

  /**
   * Provider that collects all CachingRealm instances into a Set.
   */
  private static class CachingRealmSetProvider implements com.google.inject.Provider<Set<CachingRealm>>
  {
    private final InternalRealm internalRealm;

    private final UserTokenRealm userTokenRealm;

    private final LdapRealm ldapRealm;

    private final CrowdRealm crowdRealm;

    private final ReverseProxyRealm reverseProxyRealm;

    private final SamlRealm samlRealm;

    private final OAuth2Realm oAuth2Realm;

    @Inject
    CachingRealmSetProvider(
        InternalRealm internalRealm,
        UserTokenRealm userTokenRealm,
        LdapRealm ldapRealm,
        CrowdRealm crowdRealm,
        ReverseProxyRealm reverseProxyRealm,
        SamlRealm samlRealm,
        OAuth2Realm oAuth2Realm)
    {
      this.internalRealm = internalRealm;
      this.userTokenRealm = userTokenRealm;
      this.ldapRealm = ldapRealm;
      this.crowdRealm = crowdRealm;
      this.reverseProxyRealm = reverseProxyRealm;
      this.samlRealm = samlRealm;
      this.oAuth2Realm = oAuth2Realm;
    }

    @Override
    public Set<CachingRealm> get() {
      Set<CachingRealm> realms = new HashSet<>();
      realms.add(internalRealm);
      realms.add(userTokenRealm);
      realms.add(ldapRealm);
      realms.add(crowdRealm);
      realms.add(reverseProxyRealm);
      realms.add(samlRealm);
      realms.add(oAuth2Realm);
      return realms;
    }
  }

  @Test
  public void testRealmCachingDisabled() {
    // Authentication and/or authorization caching may not be cluster-friendly and should be disabled
    // see https://github.com/sonatype/insight-brain/pull/5475 for more information
    assertThat(cachingRealms).extracting(CachingRealm::getCacheManager).containsOnlyNulls();
  }
}
