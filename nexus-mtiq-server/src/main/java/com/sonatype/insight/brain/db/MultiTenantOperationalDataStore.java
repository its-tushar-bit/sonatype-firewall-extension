/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.AbstractDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * TODO - multi-tenant implementation
 */
public class MultiTenantOperationalDataStore
    extends AbstractDataStore
    implements OperationalDataStore
{
  @Override
  protected void init(
      final DatabaseConfig databaseConfig,
      final boolean migrateDatabase,
      final Boolean migrateToNewViolationModel)
  {

  }

  @Override
  protected boolean isInitialized() {
    return false;
  }

  @Override
  public String getDatabaseSchema() {
    return null;
  }

  @Override
  public EntityManagerFactory getJPAEntityManagerFactory() {
    return null;
  }

  @Override
  public DataSource getDataSourceWithoutInit() {
    return null;
  }

  @Override
  public boolean isDatabaseInMemory() {
    return false;
  }

  @Override
  public EntityManagerFactory getEntityManagerFactoryForLocks() {
    return null;
  }

  @Override
  public boolean isDatabaseEmbedded() {
    return false;
  }
}
