/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import com.sonatype.insight.db.AbstractDataSourceFactory;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.DatabaseException;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceFactory
    extends AbstractDataSourceFactory
{
  private static final Logger log = LoggerFactory.getLogger(DataSourceFactory.class);

  private static final Map<String, DataSource> dataSources = new LinkedHashMap<>();

  private static final Map<DataSource, Boolean> newDataSources = new LinkedHashMap<>();

  public DataSourceFactory() {
    super(null, null);
  }

  @Override
  protected Map<String, DataSource> getDataSources() {
    return dataSources;
  }

  @Override
  protected DataSource loadDataSource(DatabaseConfig databaseConfig, String databaseName) {
    DataSource dataSource = super.loadDataSource(databaseConfig, databaseName);
    DatabaseEngine databaseEngine = getDatabaseEngine(dataSource);
    boolean isNew = populateDatabaseSchema(dataSource, databaseEngine, databaseName);
    if (H2DatabaseEngine.INSTANCE.equals(databaseEngine)) {
      logDatabaseSettings(dataSource);
    }
    newDataSources.put(dataSource, isNew);

    return dataSource;
  }

  public static DatabaseEngine getDatabaseEngine(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      return getDatabaseEngine(conn.getMetaData().getDatabaseProductName());
    }
    catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  static DatabaseEngine getDatabaseEngine(String databaseProductName) {
    if ("h2".equalsIgnoreCase(databaseProductName)) {
      return H2DatabaseEngine.INSTANCE;
    }
    if ("postgresql".equalsIgnoreCase(databaseProductName)) {
      return PostgresDatabaseEngine.INSTANCE;
    }
    throw new DatabaseException("Unsupported database engine: " + databaseProductName);
  }

  boolean isNewDataSource(DataSource dataSource) {
    return newDataSources.get(dataSource);
  }

  public static void clear_ForTestsOnly() {
    synchronized (dataSources) {
      dataSources.clear();
      DatamartProvider.clear_ForTestsOnly();
      OperationalDataStoreProvider.clear_ForTestsOnly();
      AggregationDataStoreProvider.clear_ForTestsOnly();
    }
  }

  private void logDatabaseSettings(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      try (Statement statement = connection.createStatement()) {
        try (ResultSet result = statement
            .executeQuery("SELECT name, value FROM INFORMATION_SCHEMA.SETTINGS ORDER BY name")) {
          log.debug("Database settings:");
          while (result.next()) {
            String name = result.getString(1);
            String value = result.getString(2);
            log.debug("\t{}={}", name, value);
          }
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to load database settings: " + e.getMessage(), e);
    }
  }
}
