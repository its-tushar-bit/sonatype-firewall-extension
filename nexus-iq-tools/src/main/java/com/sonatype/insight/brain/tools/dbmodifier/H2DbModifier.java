/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.FileUtils;

public class H2DbModifier extends DbModifier
{
  private final File databaseFile;

  private final String dbConnectionString;

  private final String schemaName;

  public H2DbModifier(
      final String username,
      final String password,
      final File file,
      final String schemaName)
  {
    super(username, password);
    this.databaseFile = file;
    this.dbConnectionString = "jdbc:h2:" + file.getAbsolutePath() +
        ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE;SCHEMA=" + schemaName;
    this.schemaName = schemaName;
  }

  // visible for testing
  public H2DbModifier(
      final String username,
      final String password,
      final String dbConnectionString,
      final String schemaName)
  {
    super(username, password);
    this.databaseFile = null;
    this.dbConnectionString = dbConnectionString + ";SCHEMA=" + schemaName;
    this.schemaName = schemaName;
  }

  @Override
  protected String getConnectionString() {
    return dbConnectionString;
  }

  @Override
  protected String getRunUpdateQuerySQL(final TableAndColumns tableAndColumns, final int numDays) {
    final String table = tableAndColumns.table;
    String sql = "UPDATE " + table + " SET ";
    sql += tableAndColumns.columns.stream()
        .map(column -> "\"" + column + "\" = DATEADD('day', " + numDays + ", \"" + column + "\")")
        .collect(Collectors.joining(", "));
    sql += ";";
    return sql;
  }

  @Override
  protected String getMaxOrMinSql(final TableAndColumns tableAndColumns, final boolean isMax) {
    String sql = "SELECT ";
    Function<String, String> toColumnExpression = columnName -> (isMax ? "MAX(" : "MIN(") + columnName + ")";
    sql += tableAndColumns.columns.stream().map(toColumnExpression).collect(Collectors.joining(", "));
    sql += " FROM " + tableAndColumns.table;
    return sql;
  }

  @Override
  protected String getSqlForTimestampColumns(final String tableName) {
    return "SELECT COLUMN_NAME FROM \"INFORMATION_SCHEMA\".\"COLUMNS\" WHERE TABLE_SCHEMA = '" + schemaName +
        "' AND TABLE_NAME = '" + tableName + "' AND TYPE_NAME = 'TIMESTAMP'";
  }

  @Override
  public void compact() {
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("SHUTDOWN COMPACT");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected String getAllTablesSQL() {
    return "SELECT TABLE_NAME from \"INFORMATION_SCHEMA\".\"TABLES\" where TABLE_SCHEMA = '" + schemaName + "'";
  }

  @Override
  public String dbVersion() {
    String dbVersion = dbVersionFromDatabase();
    if (dbVersion == null) {
      dbVersion = dbVersionFromFile();
    }
    if (dbVersion == null) {
      dbVersion = "-1";
    }
    return dbVersion;
  }

  private String dbVersionFromDatabase() {
    try (Connection connection = getConnection()) {
      try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
          "SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + schemaName +
              "' AND TABLE_NAME = 'schema_version'")) {
        if (!result.next()) {
          return null;
        }
      }
      try (Statement statement = connection.createStatement(); ResultSet result = statement
          .executeQuery("SELECT * FROM " + schemaName + ".schema_version")) {
        if (result.next()) {
          return String.valueOf(result.getInt("schema_version"));
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private String dbVersionFromFile() {
    File databaseVersionFile = new File(databaseFile.getAbsolutePath() + ".ver");
    if (databaseVersionFile.exists()) {
      try {
        return FileUtils.fileRead(databaseVersionFile, "UTF-8").trim();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }
}
