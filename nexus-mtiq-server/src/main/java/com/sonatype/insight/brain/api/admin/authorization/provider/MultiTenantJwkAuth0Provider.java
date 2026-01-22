/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.authorization.provider;

import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class MultiTenantJwkAuth0Provider
    implements MultiTenantJwkProvider
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantJwkAuth0Provider.class.getName());

  private Auth0Config auth0Config;

  private JwkProvider jwkProvider;

  private boolean denyRequest;

  @Inject
  public MultiTenantJwkAuth0Provider(MultiTenantInsightConfig multiTenantInsightConfig) {
    auth0Config = multiTenantInsightConfig.getAuth0Config();

    try {
      //Caching for 2 keys (current, next) with 24h ttl.
      jwkProvider = new JwkProviderBuilder(auth0Config.getDomain()).cached(2, 24, TimeUnit.HOURS).build();
      log.debug("Jwk Auth0 Provider created using domain {}", auth0Config.getDomain());
    }
    catch (IllegalStateException e) {
      log.error("Cannot create an authorization provider! All admin access will be denied.", e);
      denyRequest = true;
    }
  }

  @Override
  public Jwk getJsonWebKey(String keyId) throws JwkException {
    return jwkProvider.get(keyId);
  }

  @Override
  public String[] getIssuers() {
    return new String[]{auth0Config.getDomain(), auth0Config.getCustomDomain()};
  }

  @Override
  public boolean denyRequest() {
    return denyRequest;
  }
}
