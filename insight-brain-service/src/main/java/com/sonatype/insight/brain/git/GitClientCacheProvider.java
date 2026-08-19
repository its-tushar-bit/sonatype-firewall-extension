/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.nexus.scm.api.base.TokenUserCache;
import com.sonatype.nexus.scm.api.base.TokenUserCacheProvider;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
@Singleton
public class GitClientCacheProvider
    implements TokenUserCache
{
  public static final TenantReference<Map<String, String>> tokenCacheMap =
      new TenantReference<>(ConcurrentHashMap::new);

  /**
   * Initialise the nexus-scm-client library user token caching system
   **/
  @PostConstruct
  public void initialise() {
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
