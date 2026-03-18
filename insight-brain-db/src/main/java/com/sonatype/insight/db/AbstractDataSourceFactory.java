/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataSourceFactory
{
  private static final int DEFAULT_MAX_CONNECTIONS = 50;

  private static final Logger log = LoggerFactory.getLogger(AbstractDataSourceFactory.class);

  private final EmbeddedDataSourceFactory embeddedDataSourceFactory;

  protected AbstractDataSourceFactory() {
    this(null);
  }

  protected AbstractDataSourceFactory(EmbeddedDataSourceFactory embeddedDataSourceFactory) {
    this.embeddedDataSourceFactory = embeddedDataSourceFactory;
  }

  /**
   * See {@link #createNewDataSource(DatabaseConfig, String, String, String)}
   */
  public DataSource createNewDataSource(DatabaseConfig databaseConfig, String dataStoreId, String databaseSchema) {
    return createNewDataSource(databaseConfig, dataStoreId, databaseSchema, null /* liquibaseChangelogPath */);
  }

  /**
   * Create a new {@link DataSource} from the given config, data store id, and database schema.
   *
   * @param databaseConfig configuration for the database
   * @param dataStoreId the ID of the Insight data store, e.g. ods, data mart, etc...
   * @param databaseSchema the literal database schema. For most setups the data store ID and schema are the
   *          same value. Multi-tenant IQ is a known implementation where the values will differ.
   * @param liquibaseChangelogPath For liquibase configurations, the path to the change log
   * @return the configured {@link DataSource}
   */
  public DataSource createNewDataSource(
      DatabaseConfig databaseConfig,
      String dataStoreId,
      String databaseSchema,
      String liquibaseChangelogPath)
  {
    Map<String, DataSource> dataSources = getDataSources();
    synchronized (dataSources) {
      DataSource dataSource = dataSources.get(dataStoreId);
      if (dataSource != null) {
        return dataSource;
      }

      if (databaseConfig != null) {
        dataSource = loadDataSource(databaseConfig, databaseSchema);
      }
      else {
        log.warn("Default to embedded in-memory db for data store {}", dataStoreId);
        DataSource inMemoryDataSource = prepareInMemoryDatabase(dataStoreId);
        dataSource = createInMemoryDataSource(dataStoreId, databaseSchema, inMemoryDataSource, liquibaseChangelogPath);
      }

      dataSources.put(dataStoreId, dataSource);

      return dataSource;
    }
  }

  protected abstract Map<String, DataSource> getDataSources();

  protected DataSource prepareInMemoryDatabase(String databaseName) {
    if (embeddedDataSourceFactory == null) {
      throw new IllegalStateException("Embedded database not available");
    }
    return embeddedDataSourceFactory.getDataSource(databaseName);
  }

  protected DataSource createInMemoryDataSource(
      String dataStoreId,
      String databaseSchema,
      DataSource inMemoryDataSource,
      String liquibaseChangelogPath)
  {
    long start = System.currentTimeMillis();

    DatabaseEngine databaseEngine = getDatabaseEngine(inMemoryDataSource);

    populateDbSchema(inMemoryDataSource, databaseEngine, dataStoreId, databaseSchema, liquibaseChangelogPath);

    DataSource dataSource =
        new InMemoryDataSource(inMemoryDataSource, databaseEngine.buildSetSchemaSql(databaseSchema));

    log.debug("Created data source for schema {}/{} in {} ms.", dataStoreId, databaseSchema,
        System.currentTimeMillis() - start);
    return dataSource;
  }

  private DatabaseEngine getDatabaseEngine(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      return getDatabaseEngine(conn.getMetaData().getDatabaseProductName());
    }
    catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  protected DatabaseEngine getDatabaseEngine(String databaseProductName) {
    if ("h2".equalsIgnoreCase(databaseProductName)) {
      return H2DatabaseEngine.INSTANCE;
    }
    if ("postgresql".equalsIgnoreCase(databaseProductName)) {
      return PostgresDatabaseEngine.INSTANCE;
    }
    throw new DatabaseException("Unsupported database engine: " + databaseProductName);
  }

  public boolean populateDbSchema(
      DataSource dataSource,
      DatabaseEngine databaseEngine,
      String dataStoreId,
      String databaseSchema)
  {
    return populateDbSchema(dataSource, databaseEngine, dataStoreId, databaseSchema,
        null /* liquibaseChangelogPath */);
  }

  /**
   * @return Returns true only if the database schema is new and it is populated at this time.
   */
  public boolean populateDbSchema(
      DataSource dataSource,
      DatabaseEngine databaseEngine,
      String dataStoreId,
      String databaseSchema,
      String liquibaseChangelogPath)
  {
    // if (liquibaseChangelogPath != null && !liquibaseChangelogPath.isEmpty()) {
    // databasePopulator =
    // new LiquibaseDatabaseSchemaPopulator(dataSource, databaseEngine, databaseSchema, liquibaseChangelogPath);
    // }
    // else {
    AbstractDatabaseSchemaPopulator databasePopulator =
        createDatabaseSchemaPopulator(dataSource, databaseEngine, dataStoreId, databaseSchema);
    return databasePopulator.populate();
  }

  protected AbstractDatabaseSchemaPopulator createDatabaseSchemaPopulator(
      final DataSource dataSource,
      final DatabaseEngine databaseEngine,
      final String dataStoreId,
      final String databaseSchema)
  {
    return new DatabaseSchemaPopulator(dataSource, databaseEngine, dataStoreId, databaseSchema);
  }

  protected DataSource loadDataSource(DatabaseConfig databaseConfig, String databaseSchema) {
    return createNewDataSourceFromConfig(databaseConfig);
  }

  protected DataSource createNewDataSourceFromConfig(DatabaseConfig databaseConfig) {
    long start = System.currentTimeMillis();

    log.debug("DB URL: '{}'", databaseConfig.getUrl());
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setConnectionFactoryClassName(getConnectionFactoryClassName(databaseConfig));
    dataSource.setDriverClassName(databaseConfig.getDriverClassName());
    dataSource.setUrl(databaseConfig.getUrl());
    dataSource.setUsername(databaseConfig.getUsername());
    dataSource.setPassword(databaseConfig.getPassword());
    int maxConnections = DEFAULT_MAX_CONNECTIONS;
    if (databaseConfig.getMaxConnections() != null) {
      maxConnections = databaseConfig.getMaxConnections();
    }
    dataSource.setMaxConn(Duration.ofSeconds(databaseConfig.getMaxConnectionLifetimeSeconds()));
    dataSource.setLogExpiredConnections(false);
    log.debug("Setting database connection pool max size to {}.", maxConnections);
    dataSource.setMaxTotal(maxConnections);
    int maxIdleConnections = maxConnections;
    if (databaseConfig.getMaxIdleConnections() != null) {
      maxIdleConnections = databaseConfig.getMaxIdleConnections();
    }
    dataSource.setMaxIdle(maxIdleConnections);
    dataSource.setDefaultReadOnly(databaseConfig.isReadOnly());
    dataSource.setAutoCommitOnReturn(databaseConfig.isAutoCommitOnReturnToPool());
    dataSource.setTestOnBorrow(true);
    dataSource.setValidationQueryTimeout(Duration.ofSeconds(databaseConfig.getConnectionValidationTimeoutSeconds()));
    dataSource.setAccessToUnderlyingConnectionAllowed(databaseConfig.isAccessToUnderlyingConnectionAllowed());
    if (databaseConfig.getSessionVariables() != null) {
      dataSource.addConnectionProperty("sessionVariables", databaseConfig.getSessionVariables());
    }
    if (databaseConfig.getOptions() != null) {
      dataSource.addConnectionProperty("options", databaseConfig.getOptions());
    }
    if (databaseConfig.getApplicationName() != null) {
      dataSource.addConnectionProperty("ApplicationName", databaseConfig.getApplicationName());
    }

    log.debug("Created data source for url {} in {} ms.", databaseConfig.getUrl(), System.currentTimeMillis() - start);
    return dataSource;
  }

  protected String getConnectionFactoryClassName(DatabaseConfig databaseConfig) {
    return databaseConfig.getConnectionFactoryClassName();
  }
}
