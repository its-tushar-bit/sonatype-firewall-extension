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
    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection
             .prepareStatement(
                 "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'schema_version'")) {
      preparedStatement.setString(1, databaseName);
      try (ResultSet result = preparedStatement.executeQuery()) {
        return result.next();
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed attempt to check if " + databaseName + " schema_version table exists.",
          e);
    }
  }

  public static int getDatabaseSchemaVersion(DataSource dataSource, String databaseName) {
    try (Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet result = statement.executeQuery("SELECT * FROM " + databaseName + ".schema_version")) {
      if (result.last() && result.getRow() == 1) {
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
      preparedStatement.setInt(1, schemaVersion);
      int updated = preparedStatement.executeUpdate();
      if (updated != 1) {
        throw new IllegalStateException(
            databaseName + " schema_version table should have 1 entry but has " + updated + ".");
      }
      connection.commit();
    }
    catch (Exception e) {
      throw new IllegalStateException(
          "Failed attempt to write " + schemaVersion + " to " + databaseName + " schema_version table.", e);
    }
  }
}
