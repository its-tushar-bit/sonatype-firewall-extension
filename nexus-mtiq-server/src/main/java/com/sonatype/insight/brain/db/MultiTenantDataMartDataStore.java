/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.persistence.EntityManagerFactory;

import com.sonatype.insight.brain.db.datastore.AbstractDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * TODO - multi-tenant implementation
 */
public class MultiTenantDataMartDataStore
    extends AbstractDataStore
    implements DataMartDataStore
{
  @Override
  protected void init(
      final DatabaseConfig databaseConfig,
      final boolean migrateDatabase,
      final Boolean migrateToNewViolationModel)
  {
    // TODO
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
}
