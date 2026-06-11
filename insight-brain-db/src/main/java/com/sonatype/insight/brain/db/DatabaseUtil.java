/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.DatabaseException;
import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import datadog.trace.api.Trace;
import io.opentelemetry.instrumentation.annotations.WithSpan;

public class DatabaseUtil
{
  public static boolean legacySchemaVersionTableExists(final DataStore dataStore) {
    return legacySchemaVersionTableExists(dataStore.getDataSource(), dataStore.getDatabaseSchema());
  }

  public static boolean legacySchemaVersionTableExists(final DataSource dataSource, final String databaseSchema) {
    return tableExists(dataSource, databaseSchema, "schema_version");
  }

  public static boolean quartzSchedulerStateTableExists(final DataSource dataSource, final String databaseSchema) {
    return tableExists(dataSource, databaseSchema, "qrtz_scheduler_state");
  }

  public static boolean systemConfigurationPropertyTableExists(DataSource dataSource, final String databaseSchema) {
    return tableExists(dataSource, databaseSchema, "system_configuration_property");
  }

  @Trace
  @WithSpan
  public static boolean tableExists(DataSource dataSource, String databaseSchema, String tableName) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = ? AND LOWER(TABLE_NAME) = ?"))
    {
      preparedStatement.setString(1, databaseSchema);
      preparedStatement.setString(2, tableName);
      try (ResultSet result = preparedStatement.executeQuery()) {
        return result.next();
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(
          String.format("Failed attempt to check if %s %s table exists.", databaseSchema, tableName), e);
    }
  }

  @Trace
  @WithSpan
  public static boolean schemaExists(DataSource dataSource, String databaseSchema) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES " +
            "WHERE TABLE_SCHEMA = ?"))
    {
      preparedStatement.setString(1, databaseSchema);
      try (ResultSet result = preparedStatement.executeQuery()) {
        return result.next();
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(
          String.format("Failed attempt to check if %s schema exists.", databaseSchema), e);
    }
  }

  @Trace
  @WithSpan
  public static boolean databaseSchemaExists(DataSource dataSource, String databaseSchema) {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT * FROM INFORMATION_SCHEMA.SCHEMATA " +
                "WHERE SCHEMA_NAME = ?"))
    {
      preparedStatement.setString(1, databaseSchema);
      try (ResultSet result = preparedStatement.executeQuery()) {
        return result.next();
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(
          String.format("Failed attempt to check if %s schema exists.", databaseSchema), e);
    }
  }

  @Trace
  @WithSpan
  public static boolean tableExistsWithColumn(
      final DataSource dataSource,
      final String databaseSchema,
      final String tableName,
      final String columnName)
  {
    boolean tableExists = tableExists(dataSource, databaseSchema, tableName);
    if (tableExists) {
      try (Connection connection = dataSource.getConnection()) {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet rs = metaData.getColumns(null, databaseSchema, tableName, columnName);
        return rs.next();
      }
      catch (Exception e) {
        throw new IllegalStateException(
            String.format("Failed attempt to check if %s.%s table exists.", databaseSchema, tableName), e);
      }
    }
    return false;
  }

  public static int getLegacyDatabaseSchemaVersion(final DataStore dataStore) {
    return getLegacyDatabaseSchemaVersion(dataStore.getDataSource(), dataStore.getID(), dataStore.getDatabaseSchema());
  }

  @Trace
  @WithSpan
  public static int getLegacyDatabaseSchemaVersion(
      final DataSource dataSource,
      final String dataStoreId,
      final String databaseSchema)
  {
    if (!legacySchemaVersionTableExists(dataSource, databaseSchema)) {
      return -1;
    }

    String sql = "SELECT * FROM " + databaseSchema + ".schema_version";
    if (tableExistsWithColumn(dataSource, databaseSchema, "schema_version", "data_store_id")) {
      // as of migration 271 the schema_version has two columns: data_store_id, and schema_version
      sql += " WHERE data_store_id = ?";
    }

    try (Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql))
    {
      if (preparedStatement.getParameterMetaData().getParameterCount() == 1) {
        preparedStatement.setString(1, dataStoreId);
      }
      ResultSet result = preparedStatement.executeQuery();
      if (result.next() && result.isLast()) {
        return result.getInt("schema_version");
      }
      else {
        throw new IllegalStateException(
            databaseSchema + ".schema_version table should have 1 result for " + dataStoreId + " but has " +
                result.getRow() + ".");
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to read " + databaseSchema + " schema_version table.", e);
    }
  }

  @Trace
  @WithSpan
  public static Long getLastCheckinTime(final DataSource dataSource, final String databaseSchema) {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(
            "SELECT MAX(last_checkin_time) FROM " + databaseSchema +
                ".QRTZ_SCHEDULER_STATE"))
    {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return null;
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

  /**
   * Return the {@link DatabaseEngine} from the `driverClassName` field in a {@link DatabaseConfig} object.
   */
  public static DatabaseEngine getDatabaseEngine(DatabaseConfig databaseConfig) {
    if ("org.h2.Driver".equalsIgnoreCase(databaseConfig.getDriverClassName())) {
      return H2DatabaseEngine.INSTANCE;
    }
    if ("org.postgresql.Driver".equalsIgnoreCase(databaseConfig.getDriverClassName())) {
      return PostgresDatabaseEngine.INSTANCE;
    }
    throw new DatabaseException("Could not determine DatabaseEngine from the DatabaseConfig");
  }

  public static boolean isDatabaseEmbedded(final DatabaseConfig databaseConfig) {
    DatabaseEngine databaseEngine = getDatabaseEngine(databaseConfig);

    return H2DatabaseEngine.INSTANCE.equals(databaseEngine);
  }

  @Trace
  @WithSpan
  public static Map<String, Integer> getDatabaseSchemaVersions(DataSource dataSource, String databaseSchema) {
    String sql = setSchema("SELECT * FROM %s.schema_version", databaseSchema);
    Map<String, Integer> schemaVersions = new HashMap<>();

    try (Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql))
    {
      ResultSet result = preparedStatement.executeQuery();
      while (result != null && result.next()) {
        schemaVersions.put(result.getString("data_store_id"), result.getInt("schema_version"));
      }
      return schemaVersions;
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to read " + databaseSchema + " schema_version table.", e);
    }
  }

  private static String setSchema(String sql, String databaseSchema) {
    return String.format(sql, databaseSchema.trim().replace(" ", "-"));
  }

  @Trace
  @WithSpan
  public static List<String> getTenantSchemas(DataSource dataSource) {
    final List<String> schemas = new ArrayList<>();
    try (
        Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(
            "select schema_name from information_schema.schemata where schema_name like 't\\_%'"))
    {
      ResultSet result = preparedStatement.executeQuery();
      while (result != null && result.next()) {
        schemas.add(result.getString("schema_name"));
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to get list of existing schemas.", e);
    }
    return schemas;
  }

  public static boolean isInMemoryDatabase(final DatabaseConfig databaseConfig) {
    return databaseConfig.getUrl().contains("jdbc:h2:mem");
  }
}
