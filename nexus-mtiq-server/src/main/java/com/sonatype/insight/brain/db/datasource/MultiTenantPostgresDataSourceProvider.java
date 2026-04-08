/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DbApplicationNameGenerator;
import com.sonatype.insight.brain.db.MultiTenantDatabaseSchemaInitializer;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * MTIQ-specific implementation of the {@link DataSourceProvider}. Extends the {@link PostgresDataSourceProvider}
 * implementation but does NOT use the parent implementation for the DataSource creation. Full control of that resides
 * in this class.
 * </p>
 *
 * <p>
 * Specifically MTIQ uses two custom database config objects from the config.yml: `mainDatabase` and `locksDatabase`.
 * The parameter in {@link #createNewDataSource(DatabaseConfig)} is the same {@link DatabaseConfig} object as we pull
 * directly out of the config.
 * </p>
 */
public class MultiTenantPostgresDataSourceProvider
    extends PostgresDataSourceProvider
    implements DataSourceProvider, LegacyDataSourceProvider
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantPostgresDataSourceProvider.class);

  private final DatabaseConfig mainDatabaseConfig;

  private final DatabaseConfig locksDatabaseConfig;

  protected DataSource locksDataSource;

  public MultiTenantPostgresDataSourceProvider(
      final DatabaseConfig mainDatabaseConfig,
      final DatabaseConfig locksDatabaseConfig)
  {
    this.mainDatabaseConfig = mainDatabaseConfig;
    this.locksDatabaseConfig = locksDatabaseConfig;
  }

  /**
   * Fully overrides the parent implementation to create the primary {@link DataSource}. Uses a <B>CUSTOM</B> entry from
   * the config (see {@link MultiTenantInsightConfig}) as the source of configuration, and directly calls
   * {@link #createNewDataSource(DatabaseConfig)} to create the {@link DataSource} object.
   */
  @Override
  public DataSource getDataSource(final DatabaseConfig databaseConfig, final String dataStoreId /* not used */) {
    // For MTIQ we only create one data source (i.e. connection pool)
    if (dataSource == null) {
      DatabaseConfig mainDbConfig = validateConfig(mainDatabaseConfig, "mainDatabase");

      log.info("Creating main data source (connection pool) with maxConnections = {}, maxIdleConnections = {}",
          mainDbConfig.getMaxConnections(), mainDbConfig.getMaxIdleConnections());

      // verify the passed in datasource is the same. Throw an error if is it not.
      if (mainDbConfig != databaseConfig) {
        throw new IllegalArgumentException("Unexpected database configuration encountered");
      }

      mainDbConfig.setApplicationName(new DbApplicationNameGenerator().generateApplicationNameWithHost("mtiq"));
      dataSource = createNewDataSource(mainDbConfig);
    }
    return dataSource;
  }

  /**
   * Custom MTIQ implementation to create a {@link DataSource} for the locks mechanism. A custom config entry
   * 'locksDataSource' is used from {@link MultiTenantInsightConfig}.
   */
  public DataSource getLocksDataSource() {
    if (locksDataSource == null) {
      DatabaseConfig locksDbConfig = validateConfig(locksDatabaseConfig, "locksDatabase");

      log.info("Creating locks data source (connection pool) with maxConnections = {}, maxIdleConnections = {}",
          locksDbConfig.getMaxConnections(), locksDbConfig.getMaxIdleConnections());

      locksDbConfig.setApplicationName(new DbApplicationNameGenerator().generateApplicationNameWithHost("mtiq-locks"));
      locksDataSource = createNewDataSource(locksDbConfig);
    }
    return locksDataSource;
  }

  private DatabaseConfig validateConfig(final DatabaseConfig databaseConfig, final String configName) {
    if (databaseConfig == null) {
      throw new IllegalStateException(
          String.format("MTIQ-specific database config entry '%s' missing from config.yml", configName));
    }

    if (databaseConfig.getMaxConnections() == null) {
      throw new IllegalStateException(
          String.format("Missing required database configuration attribute 'maxConnections' for entry '%s'",
              configName));
    }
    return databaseConfig;
  }

  /**
   * TODO This method will be removed in the move to liquibase
   */
  @Override
  public boolean populateDbSchema(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return new MultiTenantDatabaseSchemaInitializer().populateDbSchema(dataSource, databaseEngine, dataStoreId,
        databaseSchema);
  }
}
