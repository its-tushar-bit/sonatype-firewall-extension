/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import javax.sql.DataSource;

import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.db.AbstractDatabaseSchemaPopulator;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.DatabaseSchemaPopulator;
import com.sonatype.insight.db.PostgresDatabaseEngine;

/**
 * <p>
 * MTIQ-specific implementation of the {@link DataSourceFactory}. Extends that class but does NOT use the parent
 * implementation for the DataSource creation. Full control of that resides in this class. The parent implementation of
 * other items such as {@link DatabaseSchemaPopulator} are still used from the parent.
 * </p>
 *
 * <p>
 * Specifically MTIQ uses two custom database config objects from the config.yml: `mainDatabase` and `locksDatabase`.
 * The parameter in {@link #createNewDataSource(DatabaseConfig, String, String)} is the same {@link DatabaseConfig}
 * object as we pull directly out of the config.
 * </p>
 */
public class MultiTenantDataSourceFactory
    extends DataSourceFactory
{
  private DataSource mainDataSource;

  private DataSource locksDataSource;

  private MultiTenantInsightConfig multiTenantInsightConfig;

  public void setInsightConfig(final MultiTenantInsightConfig multiTenantInsightConfig) {
    this.multiTenantInsightConfig = multiTenantInsightConfig;
  }

  /**
   * Fully overrides the parent implementation to create the primary {@link DataSource}. Uses a <B>CUSTOM</B> entry from
   * the config (see {@link MultiTenantInsightConfig}) as the source of configuration, and directly calls
   * {@link #createNewDataSourceFromConfig(DatabaseConfig)} to create the {@link DataSource} object.
   */
  @Override
  public DataSource createNewDataSource(
      final DatabaseConfig databaseConfig,
      final String dataStoreId,
      final String databaseSchema)
  {
    // For MTIQ we only create one data source (i.e. connection pool)
    if (mainDataSource == null) {
      DatabaseConfig mainDbConfig = validateConfig(multiTenantInsightConfig.getMainDatabase(), "mainDatabase");

      // verify the passed in datasource is the same. Throw an error if is it not.
      if (mainDbConfig != databaseConfig) {
        throw new IllegalArgumentException("Unexpected database configuration encountered");
      }

      mainDbConfig.setApplicationName(new DbApplicationNameGenerator().generateApplicationNameWithHost("mtiq"));
      mainDataSource = createNewDataSourceFromConfig(mainDbConfig);
    }
    return mainDataSource;
  }

  /**
   * Custom MTIQ implementation to create a {@link DataSource} for the locks mechanism. A custom config entry
   * 'locksDataSource' is used from {@link MultiTenantInsightConfig}.
   */
  public DataSource createLocksDataSource() {
    if (locksDataSource == null) {
      DatabaseConfig locksDbConfig = validateConfig(multiTenantInsightConfig.getLocksDatabase(), "locksDatabase");
      locksDbConfig.setApplicationName(new DbApplicationNameGenerator().generateApplicationNameWithHost("mtiq-locks"));
      locksDataSource = createNewDataSourceFromConfig(locksDbConfig);
    }
    return locksDataSource;
  }

  @Override
  protected AbstractDatabaseSchemaPopulator createDatabaseSchemaPopulator(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return new MultiTenantDatabaseSchemaPopulator(dataSource, dataStoreId, databaseSchema);
  }

  @Override
  protected DatabaseEngine getDatabaseEngine(final String databaseProductName) {
    return PostgresDatabaseEngine.INSTANCE;
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
}
