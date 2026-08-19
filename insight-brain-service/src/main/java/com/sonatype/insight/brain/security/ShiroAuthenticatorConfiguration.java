/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.realm.Realm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for Shiro authenticator.
 *
 * <p>
 * This class is placed in the security package to access the package-private
 * {@link FirstSuccessfulRealmAuthenticator} class.
 *
 * <p>
 * Exposes the Shiro {@link Authenticator} bean used by the security configuration.
 */
@Configuration
public class ShiroAuthenticatorConfiguration
{
  /**
   * Creates the FirstSuccessfulRealmAuthenticator.
   *
   * <p>
   * Injected realms via Set&lt;Realm&gt; will include:
   * <ul>
   * <li>InternalRealm - internal database authentication</li>
   * <li>UserTokenRealm - user token authentication</li>
   * <li>LdapRealm - LDAP authentication</li>
   * <li>CrowdRealm - Atlassian Crowd authentication</li>
   * <li>ReverseProxyRealm - reverse proxy authentication</li>
   * <li>SamlRealm - SAML authentication</li>
   * <li>OAuth2Realm - OAuth2/OIDC authentication</li>
   * </ul>
   */
  @Bean
  @Singleton
  @Primary
  public Authenticator authenticator(
      Set<Realm> realms,
      Set<AuthenticationListener> authenticationListeners)
  {
    return new FirstSuccessfulRealmAuthenticator(new LinkedHashSet<>(orderRealms(realms)), authenticationListeners);
  }

  public static List<Realm> orderRealms(Collection<Realm> realms) {
    return ShiroRealmOrdering.orderRealms(realms);
  }
}
