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
import java.sql.Statement;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

public class DatabaseUtil
{
  public static boolean schemaVersionTableExists(DataSource dataSource, String databaseSchema) {
    return tableExists(dataSource, databaseSchema, "schema_version");
  }

  public static boolean quartzSchedulerStateTableExists(DataSource dataSource) {
    return tableExists(dataSource, OperationalDataStore.ID, "qrtz_scheduler_state");
  }

  public static boolean systemConfigurationPropertyTableExists(DataSource dataSource) {
    return tableExists(dataSource, OperationalDataStore.ID, "system_configuration_property");
  }

  public static boolean tableExists(DataSource dataSource, String databaseSchema, String tableName) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES " +
             "WHERE TABLE_SCHEMA = ? AND LOWER(TABLE_NAME) = ?")) {
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

  public static boolean schemaExists(DataSource dataSource, String databaseSchema) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES " +
             "WHERE TABLE_SCHEMA = ?")) {
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

  private static boolean tableExistsWithColumn(
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

  public static int getDatabaseSchemaVersion(DataSource dataSource, String dataStoreId, String databaseSchema) {
    String sql = "SELECT * FROM " + databaseSchema + ".schema_version";
    if (tableExistsWithColumn(dataSource, databaseSchema, "schema_version", "data_store_id")) {
      // as of migration 271 the schema_version has two columns: data_store_id, and schema_version
      sql += " WHERE data_store_id = ?";
    }

    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      if (preparedStatement.getParameterMetaData().getParameterCount() == 1) {
        preparedStatement.setString(1, dataStoreId);
      }
      ResultSet result = preparedStatement.executeQuery();
      if (result.next() && result.isLast()) {
        return result.getInt("schema_version");
      }
      else {
        throw new IllegalStateException(
            databaseSchema + " schema_version table should have 1 entry but has " + result.getRow() + ".");
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to read " + databaseSchema + " schema_version table.", e);
    }
  }

  public static void updateDatabaseSchemaVersion(
      DataSource dataSource,
      String dataStoreId,
      String databaseSchema,
      int schemaVersion)
  {
    String sql = "UPDATE " + databaseSchema + ".schema_version SET schema_version = ?";
    if (tableExistsWithColumn(dataSource, databaseSchema, "schema_version", "data_store_id")) {
      // as of migration 271 the schema_version has two columns: data_store_id, and schema_version
      sql += " WHERE data_store_id = ?";
    }

    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      connection.setAutoCommit(true);
      preparedStatement.setInt(1, schemaVersion);
      if (preparedStatement.getParameterMetaData().getParameterCount() == 2) {
        preparedStatement.setString(2, dataStoreId);
      }
      int updated = preparedStatement.executeUpdate();
      if (updated != 1) {
        throw new IllegalStateException(
            databaseSchema + " schema_version table should have 1 entry but has " + updated + ".");
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(
          "Failed attempt to write " + schemaVersion + " to " + databaseSchema + " schema_version table.", e);
    }
  }

  public static Long getLastCheckinTime(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(
             "SELECT MAX(last_checkin_time) FROM " + OperationalDataStoreProvider.getDatabaseSchema() +
                 ".QRTZ_SCHEDULER_STATE")) {
      if (resultSet.next()) {
        return resultSet.getLong(1);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return null;
  }

  public static String getSchemaMigrationEnabledFromDatabase(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(
             "SELECT value FROM " + OperationalDataStoreProvider.getDatabaseSchema() +
                 ".system_configuration_property WHERE name = '" + DatabaseMigrator.SCHEMA_MIGRATION_ENABLED + "'")) {
      if (resultSet.next()) {
        return resultSet.getString(1);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return null;
  }
}
