/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.127
 */
@Named
@Singleton
public class RepositoryClientConfigurationDAO
    extends AbstractOperationalSqlDAO<RepositoryClientConfiguration>
{
  public static final String SINGLETON_ENTITY_ID = "repository-client-configuration";

  @Inject
  public RepositoryClientConfigurationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * @return The repository client configuration or {@code null} if none.
   */
  public RepositoryClientConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  @Override
  public RepositoryClientConfiguration getById(TransactionContext tx, String id) {
    return super.getById(tx, SINGLETON_ENTITY_ID);
  }

  public void set(RepositoryClientConfiguration configuration) {
    update(configuration);
  }

  @Override
  public void insert(TransactionContext tx, RepositoryClientConfiguration configuration) {
    configuration.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, configuration);
  }

  @Override
  public void update(TransactionContext tx, RepositoryClientConfiguration configuration) {
    configuration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, configuration);
  }

  public void delete() {
    RepositoryClientConfiguration clientConfiguration = get();
    if (clientConfiguration != null) {
      delete(clientConfiguration);
    }
  }
}
