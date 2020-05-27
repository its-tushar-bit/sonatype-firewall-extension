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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DbModifier
{
  private static final Logger log = LoggerFactory.getLogger(DbModifier.class);

  protected final String username;

  protected final String password;

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

  protected DbModifier(final String username, final String password) {
    this.username = username;
    this.password = password;
  }

  protected abstract String getConnectionString();

  protected Connection getConnection() throws SQLException {
    return DriverManager.getConnection(getConnectionString(), username, password);
  }

  public void shiftToDate(final LocalDate maxDate) {
    Timestamp maxTimestamp = getMaxTimestamp();
    log.info("Max timestamp in the db: {}", maxTimestamp);
    if (maxTimestamp == null) {
      return;
    }
    Duration dateDifference = Duration
        .between(maxTimestamp.toLocalDateTime().truncatedTo(ChronoUnit.DAYS), maxDate.atStartOfDay());

    int daysBetween = (int) dateDifference.toDays();
    shiftDays(daysBetween);
  }

  public void shiftDays(final int numDays) {
    log.info("Shifting timestamps by {} days", numDays);
    List<TableAndColumns> tablesWithTsColumns = getAllTablesWithTimestampColumns();
    tablesWithTsColumns.forEach(tableAndColumns -> runUpdateQuery(tableAndColumns, numDays));
  }

  public List<TableDateMinMax> getDateInfo() {
    long start = System.currentTimeMillis();
    List<TableDateMinMax> allTableDates = new ArrayList<>();
    List<TableAndColumns> tablesWithTsColumns = getAllTablesWithTimestampColumns();
    tablesWithTsColumns.forEach(tableAndColumns -> {
      TableDateMinMax tableDates = new TableDateMinMax();
      tableDates.table = tableAndColumns.table;
      tableDates.max = getMaxTimestampInTable(tableAndColumns);
      tableDates.min = getMinTimestampInTable(tableAndColumns);
      allTableDates.add(tableDates);
    });
    log.info("Retrieved timestamp info for all tables in {} ms", System.currentTimeMillis() - start);
    return allTableDates;
  }

  private void runUpdateQuery(TableAndColumns tableAndColumns, int numDays) {
    String sql = getRunUpdateQuerySQL(tableAndColumns, numDays);

    log.info("Executing SQL: {}", sql);
    long start = System.currentTimeMillis();
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      int updatedRecordsCount = statement.executeUpdate(sql);
      log.info("Updated {} records in {} ms", updatedRecordsCount, System.currentTimeMillis() - start);
    }
    catch (Exception e) {
      log.info("Failed to execute SQL in {} ms", System.currentTimeMillis() - start);
      throw new RuntimeException(e);
    }
  }

  protected abstract String getRunUpdateQuerySQL(TableAndColumns tableAndColumns, int numDays);

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

  protected abstract String getMaxOrMinSql(final TableAndColumns tableAndColumns, boolean isMax);

  private List<Timestamp> retrieveTimestamps(final String maxOrMinSql) {
    long start = System.currentTimeMillis();
    List<Timestamp> timestamps = new ArrayList<>();
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      log.info("Executing SQL: {}", maxOrMinSql);
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
    log.info("Retrieved timestamp info in {} ms", System.currentTimeMillis() - start);
    return timestamps;
  }

  private List<TableAndColumns> getAllTablesWithTimestampColumns() {
    List<String> allTables = getAllTables();
    return getAllTimestampColumns(allTables);
  }

  // visible for testing
  List<TableAndColumns> getAllTimestampColumns(final List<String> tableNames) {
    long start = System.currentTimeMillis();
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
    log.info("Retrieved all timestamp columns in {} ms", System.currentTimeMillis() - start);
    return tablesList;
  }

  protected abstract String getSqlForTimestampColumns(final String tableName);

  public abstract void compact();

  public void scrub(boolean rebuild, boolean keepFiles) {
    File workDir = new File(".");
    DbScrubber.scrubDb(getConnectionString(), username, password, rebuild, keepFiles, workDir);
  }

  // visible for testing
  List<String> getAllTables() {
    long start = System.currentTimeMillis();
    List<String> tableNames = new ArrayList<>();
    String getTablesSql = getAllTablesSQL();
    try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery(getTablesSql);
      while (resultSet.next()) {
        tableNames.add(resultSet.getString(1));
      }
      log.info("Retrieved all table names in {} ms", System.currentTimeMillis() - start);
      return tableNames;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract String getAllTablesSQL();

  public abstract String dbVersion();
}
