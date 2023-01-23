/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.cache.LoadingCache;

/**
 * The on-prem version of this class (RepositoryIdentifiedComponentCache) caches components in the database but then
 * uses an in-memory cache to improve lookup performance.
 * <p>
 * Having an ever-growing in-memory cache for a growing number of tenants could create memory pressure. For MTIQ this
 * implementation disables the in-memory caching and relies on the database. If we need to tweak performance later we
 * can investigate per-tenant caching strategies or even an external cache.
 */
@Named
@Singleton
public class MultiTenantRepositoryIdentifiedComponentCache
    extends RepositoryIdentifiedComponentCache
{
  @Inject
  public MultiTenantRepositoryIdentifiedComponentCache(
      RepositoryIdentifiedComponentCacheLoader repositoryIdentifiedComponentCacheLoader,
      RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO)
  {
    super(repositoryIdentifiedComponentCacheLoader, repositoryIdentifiedComponentDAO);
  }

  @Override
  LoadingCache<String, ComponentIdentifier> createLoadingCache() {
    //no-op
    return null;
  }

  @Override
  public ComponentIdentifier get(String hash) {
    try {
      return repositoryIdentifiedComponentDAO.getByHashNotNullAndUpdateLastAccessTime(hash).getComponentIdentifier();
    }
    catch (Exception e) {
      if (e.getCause() instanceof NotFoundException) {
        return null;
      }
      throw e;
    }
  }

  @Override
  protected void addToCache(String hash, ComponentIdentifier componentIdentifier) {
    //no-op
  }

  @Override
  public ComponentIdentifier removeByHash(String hash) {
    //no-op
    return null;
  }

  @Override
  public int removeByComponentIdentifier(ComponentIdentifier componentIdentifier) {
    //no-op
    return -1;
  }
}
