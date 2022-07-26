/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

public class DatabaseUtil
{
  public static boolean schemaVersionTableExists(DataSource dataSource, String databaseName) {
    return tableExists(dataSource, databaseName, "schema_version");
  }

  public static boolean quartzSchedulerStateTableExists(DataSource dataSource) {
    return tableExists(dataSource, OperationalDataStoreProvider.ID, "qrtz_scheduler_state");
  }

  public static boolean systemConfigurationPropertyTableExists(DataSource dataSource) {
    return tableExists(dataSource, OperationalDataStoreProvider.ID, "system_configuration_property");
  }

  public static boolean tableExists(DataSource dataSource, String databaseName, String tableName) {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES " +
             "WHERE TABLE_SCHEMA = ? AND LOWER(TABLE_NAME) = ?")) {
      preparedStatement.setString(1, databaseName);
      preparedStatement.setString(2, tableName);
      try (ResultSet result = preparedStatement.executeQuery()) {
        return result.next();
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(
          String.format("Failed attempt to check if %s %s table exists.", databaseName, tableName), e);
    }
  }

  public static int getDatabaseSchemaVersion(DataSource dataSource, String databaseName) {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT * FROM " + databaseName + ".schema_version")) {
      if (result.next() && result.isLast()) {
        return result.getInt("schema_version");
      }
      else {
        throw new IllegalStateException(
            databaseName + " schema_version table should have 1 entry but has " + result.getRow() + ".");
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to read " + databaseName + " schema_version table.", e);
    }
  }

  public static void updateDatabaseSchemaVersion(DataSource dataSource, String databaseName, int schemaVersion) {
    try (Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection
        .prepareStatement("UPDATE " + databaseName + ".schema_version SET schema_version = ?")) {
      connection.setAutoCommit(true);
      preparedStatement.setInt(1, schemaVersion);
      int updated = preparedStatement.executeUpdate();
      if (updated != 1) {
        throw new IllegalStateException(
            databaseName + " schema_version table should have 1 entry but has " + updated + ".");
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(
          "Failed attempt to write " + schemaVersion + " to " + databaseName + " schema_version table.", e);
    }
  }

  public static Long getLastCheckinTime(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(
             "SELECT MAX(last_checkin_time) FROM insight_brain_ods.QRTZ_SCHEDULER_STATE")) {
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
             "SELECT value FROM insight_brain_ods.system_configuration_property WHERE name = '" +
                 DatabaseMigrator.SCHEMA_MIGRATION_ENABLED + "'")) {
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
