/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.sonatype.insight.db.AbstractDataSourceFactory;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.DatabaseException;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceFactory
    extends AbstractDataSourceFactory
{
  private static final Logger log = LoggerFactory.getLogger(DataSourceFactory.class);

  private static final Map<String, DataSource> dataSources = new LinkedHashMap<>();

  private static final Map<DataSource, Boolean> newDataSources = new LinkedHashMap<>();

  public DataSourceFactory() {
    super(new H2DataSourceFactory());
  }

  @Override
  protected Map<String, DataSource> getDataSources() {
    return dataSources;
  }

  @Override
  protected DataSource loadDataSource(DatabaseConfig databaseConfig, String databaseName) {
    DataSource dataSource = super.loadDataSource(databaseConfig, databaseName);
    DatabaseEngine databaseEngine = getDatabaseEngine(dataSource);
    boolean isNew = !DatabaseUtil.schemaExists(dataSource, databaseName);
    logDatabaseSettings(dataSource, databaseEngine);
    newDataSources.put(dataSource, isNew);

    return dataSource;
  }

  public static DatabaseEngine getDatabaseEngine(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      return getDatabaseEngineFromName(conn.getMetaData().getDatabaseProductName());
    }
    catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  static DatabaseEngine getDatabaseEngineFromName(String databaseProductName) {
    if ("h2".equalsIgnoreCase(databaseProductName)) {
      return H2DatabaseEngine.INSTANCE;
    }
    if ("postgresql".equalsIgnoreCase(databaseProductName)) {
      return PostgresDatabaseEngine.INSTANCE;
    }
    throw new DatabaseException("Unsupported database engine: " + databaseProductName);
  }

  public static boolean populateDatabaseSchema(DataSource dataSource, String databaseName) {
    return new DataSourceFactory().populateDatabaseSchema(dataSource, getDatabaseEngine(dataSource), databaseName);
  }

  public static boolean hasNewDataSource() {
    return newDataSources.containsValue(true);
  }

  public static void clear_ForTestsOnly() {
    synchronized (newDataSources) {
      newDataSources.clear();
    }
    synchronized (dataSources) {
      dataSources.clear();
      DatamartProvider.clear_ForTestsOnly();
      OperationalDataStoreProvider.clear_ForTestsOnly();
      AggregationDataStoreProvider.clear_ForTestsOnly();
      ThirdPartyScansProvider.clear_ForTestsOnly();
    }
  }

  private void logDatabaseSettings(DataSource dataSource, DatabaseEngine databaseEngine) {
    try (Connection connection = dataSource.getConnection()) {
      log.debug("Database settings:");
      databaseEngine.getDatabaseSettings(connection).forEach((key, value) -> log.debug("\t{}={}", key, value));
    }
    catch (Exception e) {
      log.error("Failed to load database settings: " + e.getMessage(), e);
    }
  }
}
