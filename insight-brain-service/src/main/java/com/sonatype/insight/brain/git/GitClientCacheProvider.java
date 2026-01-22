/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.nexus.scm.api.base.TokenUserCache;
import com.sonatype.nexus.scm.api.base.TokenUserCacheProvider;

import io.dropwizard.lifecycle.Managed;

@Named
@Singleton
public class GitClientCacheProvider
    implements TokenUserCache, Managed
{
  public static final TenantReference<Map<String, String>> tokenCacheMap =
      new TenantReference<>(HashMap::new);

  /**
   * Initialise the nexus-scm-client library user token caching system
   **/
  @Override
  public void start() throws Exception {
    TokenUserCacheProvider.initialiseTokenProvider(this);
  }

  @Override
  public String getTokenUser(final String token) {
    return tokenCacheMap.get().get(token);
  }

  @Override
  public void putTokenUser(final String token, final String userId) {
    tokenCacheMap.get().put(token, userId);
  }
}
