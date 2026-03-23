/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.RepositoryClientConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Record;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryClientConfiguration.REPOSITORY_CLIENT_CONFIGURATION;

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
  public RepositoryClientConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  protected RepositoryClientConfiguration toEntity(final Record record) {
    if (record == null) {
      return null;
    }
    RepositoryClientConfiguration entity = super.toEntity(record);
    Short connectionTimeout = record.get(REPOSITORY_CLIENT_CONFIGURATION.CONNECTION_TIMEOUT);
    entity.setConnectionTimeout(connectionTimeout != null ? connectionTimeout.intValue() : 30);
    Short socketTimeout = record.get(REPOSITORY_CLIENT_CONFIGURATION.SOCKET_TIMEOUT);
    entity.setSocketTimeout(socketTimeout != null ? socketTimeout.intValue() : 120);
    return entity;
  }

  public RepositoryClientConfiguration get() {
    return getById(SINGLETON_ENTITY_ID);
  }

  public void set(final RepositoryClientConfiguration configuration) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      if (getById(tx, SINGLETON_ENTITY_ID) == null) {
        insert(tx, configuration);
      }
      else {
        update(tx, configuration);
      }
      tx.commit();
    }
  }

  @Override
  public void insert(final TransactionContext tx, final RepositoryClientConfiguration configuration) {
    configuration.setId(SINGLETON_ENTITY_ID);
    super.insert(tx, configuration);
  }

  @Override
  public void update(final TransactionContext tx, final RepositoryClientConfiguration configuration) {
    configuration.setId(SINGLETON_ENTITY_ID);
    super.update(tx, configuration);
  }

  public void delete() {
    RepositoryClientConfiguration clientConfiguration = get();
    if (clientConfiguration != null) {
      delete(clientConfiguration);
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_CLIENT_CONFIGURATION;
  }

  @Override
  public Class<RepositoryClientConfiguration> getEntityClass() {
    return RepositoryClientConfiguration.class;
  }
}
