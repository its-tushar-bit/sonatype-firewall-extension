/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.FileUtils;

public class DbModifier
{
  private final File databaseFile;

  private final String schemaName;

  private final String dbConnectionString;

  private final String username;

  private final String password;

  // visible for testing
  static class TableAndColumns
  {
    String table;

    final List<String> columns = new ArrayList<>();
  }

  // visible for testing
  static class TableDateMinMax
  {
    String table;

    Timestamp min;

    Timestamp max;
  }

  // visible for testing
  DbModifier(final File file,
             final String dbConnectionString,
             final String username,
             final String password,
             final String schemaName)
  {
    this.databaseFile = file;
    this.dbConnectionString = dbConnectionString + ";SCHEMA=" + schemaName;
    this.username = username;
    this.password = password;
    this.schemaName = schemaName;
  }

  public DbModifier(final File file, final String username, final String password, final String schemaName) {
    this(file, getDbConnectionString(file), username, password, schemaName);
  }

  private Connection getConnection() throws SQLException {
    return DriverManager.getConnection(dbConnectionString, username, password);
  }

  private static String getDbConnectionString(final File file) {
    return "jdbc:h2:" + file.getAbsolutePath() + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";
  }

  public void shiftToDate(final LocalDate maxDate) {
    Timestamp maxTimestamp = getMaxTimestamp();
    if (maxTimestamp == null) {
      return;
    }
    Duration dateDifference = Duration
        .between(maxTimestamp.toLocalDateTime().truncatedTo(ChronoUnit.DAYS), maxDate.atStartOfDay());

    int daysBetween = (int) dateDifference.toDays();
    shiftDays(daysBetween);
  }

  public void shiftDays(final int numDays) {
    List<TableAndColumns> tablesWithTsColumns = getAllTablesWithTimestampColumns();
    tablesWithTsColumns.forEach(tableAndColumns -> runUpdateQuery(tableAndColumns, numDays));
  }

  public List<TableDateMinMax> getDateInfo() {
    List<TableDateMinMax> allTableDates = new ArrayList<>();
    List<TableAndColumns> tablesWithTsColumns = getAllTablesWithTimestampColumns();
    tablesWithTsColumns.forEach(tableAndColumns -> {
      TableDateMinMax tableDates = new TableDateMinMax();
      tableDates.table = tableAndColumns.table;
      tableDates.max = getMaxTimestampInTable(tableAndColumns);
      tableDates.min = getMinTimestampInTable(tableAndColumns);
      allTableDates.add(tableDates);
    });
    return allTableDates;
  }

  private void runUpdateQuery(TableAndColumns tableAndColumns, int numDays) {
    final String table = tableAndColumns.table;
    String sql = "UPDATE " + table + " SET ";
    sql += tableAndColumns.columns.stream()
        .map(column -> "\"" + column + "\" = DATEADD('day', " + numDays + ", \"" + column + "\")")
        .collect(Collectors.joining(", "));
    sql += ";";
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return null if no timestamp values set in db
   */
  public Timestamp getMinTimestamp() {
    List<TableAndColumns> allTablesWithTimestamps = getAllTablesWithTimestampColumns();
    return getTimestamp(allTablesWithTimestamps, this::getMinTimestampInTable, Comparator.reverseOrder());
  }

  /**
   * @return null if no timestamp values set in db
   */
  public Timestamp getMaxTimestamp() {
    List<TableAndColumns> allTablesWithTimestamps = getAllTablesWithTimestampColumns();
    return getTimestamp(allTablesWithTimestamps, this::getMaxTimestampInTable, Comparator.naturalOrder());
  }

  private Timestamp getTimestamp(final List<TableAndColumns> tableAndColumnsList,
                                 final Function<TableAndColumns, Timestamp> timestampEval,
                                 final Comparator<Timestamp> sortOrder)
  {
    return tableAndColumnsList.stream().map(timestampEval).filter(Objects::nonNull).max(sortOrder).orElse(null);
  }

  // visible for testing
  Timestamp getMinTimestampInTable(final TableAndColumns tableAndColumns) {
    return getTimestampInTable(tableAndColumns, false);
  }

  // visible for testing
  Timestamp getMaxTimestampInTable(final TableAndColumns tableAndColumns) {
    return getTimestampInTable(tableAndColumns, true);
  }

  private Timestamp getTimestampInTable(final TableAndColumns tableAndColumns, boolean isMax) {
    String maxOrMinSql = getMaxOrMinSql(tableAndColumns, isMax);
    List<Timestamp> timestamps = retrieveTimestamps(maxOrMinSql);

    Comparator<Timestamp> sortOrder = isMax ? Comparator.naturalOrder() : Comparator.reverseOrder();
    return timestamps.stream().max(sortOrder).orElse(null);
  }

  private String getMaxOrMinSql(final TableAndColumns tableAndColumns, boolean isMax) {
    String sql = "SELECT ";
    Function<String, String> toColumnExpression = columnName -> (isMax ? "MAX(" : "MIN(") + columnName + ")";
    sql += tableAndColumns.columns.stream().map(toColumnExpression).collect(Collectors.joining(", "));
    sql += " FROM " + tableAndColumns.table;
    return sql;
  }

  private List<Timestamp> retrieveTimestamps(final String maxOrMinSql) {
    List<Timestamp> timestamps = new ArrayList<>();
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery(maxOrMinSql);
      int numOfColumns = resultSet.getMetaData().getColumnCount();
      while (resultSet.next()) {
        for (int i = 1; i <= numOfColumns; i++) {
          Timestamp resultValue = resultSet.getTimestamp(i);
          if (resultValue != null) {
            timestamps.add(resultValue);
          }
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    return timestamps;
  }

  private List<TableAndColumns> getAllTablesWithTimestampColumns() {
    List<String> allTables = getAllTables();
    return getAllTimestampColumns(allTables);
  }

  // visible for testing
  List<TableAndColumns> getAllTimestampColumns(final List<String> tableNames) {
    List<TableAndColumns> tablesList = new ArrayList<>();
    for (String tableName : tableNames) {
      TableAndColumns tableAndColumns = new TableAndColumns();
      tableAndColumns.table = tableName;

      try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
        ResultSet resultSet = statement.executeQuery(getSqlForTimestampColumns(tableName));
        while (resultSet.next()) {
          tableAndColumns.columns.add(resultSet.getString(1));
        }
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (!tableAndColumns.columns.isEmpty()) {
        tablesList.add(tableAndColumns);
      }
    }
    return tablesList;
  }

  private String getSqlForTimestampColumns(final String tableName) {
    return "SELECT COLUMN_NAME FROM \"INFORMATION_SCHEMA\".\"COLUMNS\" WHERE TABLE_SCHEMA = '" + schemaName +
        "' AND TABLE_NAME = '" + tableName + "' AND TYPE_NAME = 'TIMESTAMP'";
  }

  public void compact() {
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("SHUTDOWN COMPACT");
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void scrub(boolean rebuild, boolean keepFiles) {
    DbScrubber.scrubDb(dbConnectionString, rebuild, keepFiles);
  }

  // visible for testing
  List<String> getAllTables() {
    List<String> tableNames = new ArrayList<>();
    String getTablesSql =
        "SELECT TABLE_NAME from \"INFORMATION_SCHEMA\".\"TABLES\" where TABLE_SCHEMA = '" + schemaName + "'";
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery(getTablesSql);
      while (resultSet.next()) {
        tableNames.add(resultSet.getString(1));
      }
      return tableNames;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

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
