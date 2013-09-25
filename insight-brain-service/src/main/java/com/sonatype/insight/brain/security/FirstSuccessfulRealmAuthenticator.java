/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.pam.FirstSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.realm.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends {@link ModularRealmAuthenticator} to short-cut authentication on the first successful attempt.
 * This is different to {@link FirstSuccessfulStrategy} which will still go ahead and query every single
 * realm, but then only return the first successful result from the aggregated authentication info.
 * 
 * @since 1.7
 */
@Singleton
class FirstSuccessfulRealmAuthenticator
    extends ModularRealmAuthenticator
{
  private static final Logger log = LoggerFactory.getLogger(FirstSuccessfulRealmAuthenticator.class);

  @Inject
  public FirstSuccessfulRealmAuthenticator(Collection<Realm> realms) {
    setRealms(realms);
  }

  /**
   * Modified version of {@link ModularRealmAuthenticator#doMultiRealmAuthentication()}.
   */
  @Override
  protected AuthenticationInfo doMultiRealmAuthentication(Collection<Realm> realms, AuthenticationToken token) {

    if (log.isTraceEnabled()) {
      log.trace("Iterating through {} realms for PAM authentication", realms.size());
    }

    for (Realm realm : realms) {
      if (realm.supports(token)) {
        log.trace("Attempting to authenticate token [{}] using realm [{}]", token, realm);
        try {
          AuthenticationInfo info = realm.getAuthenticationInfo(token);
          if (info != null) {
            return info; // success; stop here and do not bother checking the other realms
          }
          log.trace("Realm [{}] returned null during a multi-realm authentication attempt.", realm);
        }
        catch (Throwable t) {
          log.trace("Realm [{}] threw an exception during a multi-realm authentication attempt:", realm, t);
        }
      }
      else {
        log.trace("Realm [{}] does not support token {}.  Skipping realm.", realm, token);
      }
    }

    throw new AuthenticationException("Authentication token of type [" + token.getClass() + "] "
        + "could not be authenticated by any configured realms.  Please ensure that at least one realm can "
        + "authenticate these tokens.");
  }
}