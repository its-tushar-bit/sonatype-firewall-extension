/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datastore;

import java.util.function.IntConsumer;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * Shared logic that is applicable to all {@link DataStore} implementations.
 */
public abstract class AbstractDataStore
    implements DataStore
{
  protected DataSource dataSource;

  protected DatabaseConfig databaseConfig;

  @Override
  public void initWithMigration(final DatabaseConfig databaseConfig, final Boolean migrateToNewViolationModel) {
    init(databaseConfig, true /* migrateDatabase */, migrateToNewViolationModel);
  }

  @Override
  public void initWithoutMigration(DatabaseConfig databaseConfig) {
    init(databaseConfig, false /* migrateDatabase */, false);
  }

  /**
   * Perform the initialization of the data store. This includes creating all supporting objects (e.g.
   * {@link DataSource}, {@link EntityManagerFactory}), population of new databases, and migration on existing
   * databases.
   */
  protected abstract void init(
      final DatabaseConfig databaseConfig,
      final boolean migrateDatabase,
      final Boolean migrateToNewViolationModel);

  @Override
  public void migrate(final Boolean migrateToNewViolationModel) {
    new DatabaseMigrator().migrate(databaseConfig, getID(), getDatabaseSchema(), dataSource,
        getUpgradeGuard(migrateToNewViolationModel));
  }

  /**
   * The new violation model in IQ 114 requires the database to be at version 85 first.
   */
  protected IntConsumer getUpgradeGuard(final Boolean migrateToNewViolationModel) {
    // as of writing only ODS has an upgrade guard
    return null;
  }

  @Override
  public DatabaseConfig getDatabaseConfig() {
    return databaseConfig;
  }

  @Override
  public DataSource getDataSource() {
    if (!isInitialized()) {
      initWithMigration(null /* databaseConfig */, false);
    }
    return dataSource;
  }

  @Override
  public void clear_ForTestsOnly() {
    dataSource = null;
    databaseConfig = null;
  }

  /**
   * Track if the data store is initialized or not. Data store initialization is only allowed once and can be done
   * lazily (for tests only) which will imply a null {@link DatabaseConfig}.
   */
  protected abstract boolean isInitialized();
}
