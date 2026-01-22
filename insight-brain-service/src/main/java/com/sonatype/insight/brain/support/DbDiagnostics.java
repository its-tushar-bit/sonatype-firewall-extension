/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.27
 */
public class DbDiagnostics
{
  private static final Logger log = LoggerFactory.getLogger(DbDiagnostics.class);

  private final OperationalDataStore operationalDataStore;

  @Inject
  public DbDiagnostics(final OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
  }

  public String getDBFileInfo() throws IOException {
    log.trace("getting db file info");
    final StringBuilder result = new StringBuilder();

    DataSource dataSource = operationalDataStore.getDataSource();
    String databaseProductName = getDatabaseProductName(dataSource);
    result.append("-- Database Diagnostics --\n");
    result.append("Database product name: ").append(databaseProductName).append("\n");
    result.append("Database product version: ").append(getDatabaseProductVersion(dataSource)).append("\n");

    if ("h2".equalsIgnoreCase(databaseProductName)) {
      final DatabaseConfig databaseConfig = operationalDataStore.getDatabaseConfig();

      final File ods = H2DatabaseUtil.getDatabasePath(databaseConfig);
      final File h2 = new File(ods.getPath() + ".h2.db");
      if (!h2.isFile()) {
        result.append("Found no database file at ").append(h2.getCanonicalPath()).append("\n");
      }
      else {
        result.append("Database path: ").append(ods.getCanonicalPath()).append("\n");
        result.append("Total database size: ").append(h2.length()).append(" bytes\n");
      }
    }

    final int version = DatabaseUtil.getLegacyDatabaseSchemaVersion(operationalDataStore);
    result.append("Schema version: ").append(version).append("\n");
    addLatencyInformation(result, dataSource);
    result.append("-- Database Settings --\n");
    getDatabaseSettings(dataSource)
        .forEach((name, value) -> result.append(name).append(": ").append(value).append("\n"));
    return result.toString();
  }

  private static String getDatabaseProductName(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      return connection.getMetaData().getDatabaseProductName();
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to get database product name.", e);
    }
  }

  private static String getDatabaseProductVersion(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      return connection.getMetaData().getDatabaseProductVersion();
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to get database product version.", e);
    }
  }

  private static Map<String, String> getDatabaseSettings(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      return DatabaseUtil.getDatabaseEngine(dataSource).getDatabaseSettings(connection);
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to get database settings.", e);
    }
  }

  private static void addLatencyInformation(StringBuilder sb, DataSource ds) {
    try (Connection connection = ds.getConnection()) {
      long latencyMinimum = Long.MAX_VALUE;
      long latencyMaximum = Long.MIN_VALUE;
      long latencyCumulative = 0;

      // Ping database 5 times, find out the minimum, maximum and average latency
      int tryCount = 5;
      for (int i = 0; i < tryCount; i++) {
        long start = System.nanoTime();
        connection.isValid(/* timeout in seconds */ 3);
        long latency = (System.nanoTime() - start) / 1000;

        latencyMinimum = Math.min(latency, latencyMinimum);
        latencyMaximum = Math.max(latency, latencyMaximum);
        latencyCumulative = latencyCumulative + latency;
      }
      long averageLatency = latencyCumulative / tryCount;

      // Add the information
      sb.append("-- Latency Information --\n");
      sb.append("Minimum: ").append(latencyMinimum).append(" microseconds\n");
      sb.append("Average: ").append(averageLatency).append(" microseconds\n");
      sb.append("Maximum: ").append(latencyMaximum).append(" microseconds\n");
    }
    catch (SQLException e) {
      throw new IllegalStateException("Failed attempt to get database latency.", e);
    }
  }
}
