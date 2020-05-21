/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PostgresDbModifier
    extends DbModifier
{
  private final String hostname;

  private final int port;

  private final String database;

  private final String schemaName;

  public PostgresDbModifier(
      final String username,
      final String password,
      final String hostname,
      final int port,
      final String database, final String schemaName)
  {
    super(username, password);
    this.hostname = hostname;
    this.port = port;
    this.database = database;
    this.schemaName = schemaName;
  }

  @Override
  protected String getConnectionString() {
    return "jdbc:postgresql://" + hostname + ":" + port + "/" + database;
  }

  @Override
  protected String getRunUpdateQuerySQL(final TableAndColumns tableAndColumns, final int numDays) {
    final String table = tableAndColumns.table;
    String sql = "UPDATE " + schemaName + "." + table + " SET ";
    sql += tableAndColumns.columns.stream()
        .map(column -> "\"" + column + "\" = \"" + column + "\" + interval '" + numDays + " days'")
        .collect(Collectors.joining(", "));
    sql += ";";
    return sql;
  }

  @Override
  protected String getMaxOrMinSql(final TableAndColumns tableAndColumns, final boolean isMax) {
    String sql = "SELECT ";
    Function<String, String> toColumnExpression = columnName -> (isMax ? "MAX(" : "MIN(") + columnName + ")";
    sql += tableAndColumns.columns.stream().map(toColumnExpression).collect(Collectors.joining(", "));
    sql += " FROM " + schemaName + "." + tableAndColumns.table;
    return sql;
  }

  @Override
  protected String getSqlForTimestampColumns(final String tableName) {
    return "SELECT column_name "
        + "FROM information_schema.\"columns\" "
        + "WHERE table_schema = '" + schemaName + "' AND table_name = '" + tableName
        + "' AND data_type LIKE 'timestamp%'";
  }

  @Override
  public void compact() {
    throw new UnsupportedOperationException("The compact operation is not supported for Postgres");
  }

  @Override
  protected String getAllTablesSQL() {
    return "SELECT table_name FROM information_schema.\"tables\" WHERE table_schema = '" + schemaName + "'";
  }

  @Override
  public String dbVersion() {
    try (Connection connection = getConnection()) {
      try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
          "SELECT * FROM information_schema.\"tables\" WHERE table_schema = '" + schemaName +
              "' AND table_name = 'schema_version';")) {
        if (!result.next()) {
          return "-1";
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
    return "-1";
  }
}
