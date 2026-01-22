/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.RepositoryIdentifiedComponentDAO;

import com.google.common.cache.CacheLoader;

@Named
@Singleton
public class RepositoryIdentifiedComponentCacheLoader
    extends CacheLoader<String, ComponentIdentifier>
{
  private final RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO;

  @Inject
  public RepositoryIdentifiedComponentCacheLoader(RepositoryIdentifiedComponentDAO repositoryIdentifiedComponentDAO) {
    this.repositoryIdentifiedComponentDAO = repositoryIdentifiedComponentDAO;
  }

  @Override
  public ComponentIdentifier load(@Nonnull String hash) throws Exception {
    return repositoryIdentifiedComponentDAO.getByHashNotNullAndUpdateLastAccessTime(hash).getComponentIdentifier();
  }
}
