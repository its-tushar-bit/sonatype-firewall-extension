/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.tools.dbmodifier.DbModifier.TableAndColumns;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

public class DbModifierTest
{
  private static final String TEST_SCHEMA = "TEST_SCHEMA";

  private static final LocalDate MAX_DATE = LocalDate.of(2018, 8, 1);

  private static final LocalDate MIN_DATE = LocalDate.of(2017, 2, 1);

  private static final LocalDate MIN_DATE_TABLE1 = LocalDate.of(2018, 1, 1);

  private static final LocalDate MAX_DATE_TABLE2 = LocalDate.of(2017, 3, 8);

  private static final String TEST_DB_CONNECTION_STRING =
      "jdbc:h2:mem:test;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000";

  private static DbModifier dbModifier;

  private static final String TABLE_NAME1 = "table1";

  private static final String TABLE_NAME2 = "table2";

  private static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private Connection dbConnection;

  private static final String USER_NAME = "";

  private static final String PASSWORD = "";

  @Before
  public void init() throws Exception {
    dbConnection = createTestDbConnection();
    createTestDb(dbConnection);
    dbModifier = new DbModifier(TEST_DB_CONNECTION_STRING, USER_NAME, PASSWORD, TEST_SCHEMA);
  }

  @After
  public void cleanup() throws Exception {
    dropTestDb(dbConnection);
    dbConnection.close();
  }

  @Test
  public void testGetAllTables() {
    List<String> allTables = dbModifier.getAllTables();
    assertThat(allTables.size(), is(2));
    assertThat(allTables, hasItems(TABLE_NAME1, TABLE_NAME2));
  }

  @Test
  public void testGetMinTimestampForTable() {
    List<TableAndColumns> allTimestampColumns = dbModifier.getAllTimestampColumns(dbModifier.getAllTables());
    allTimestampColumns.sort(Comparator.comparing(item -> item.table));

    Timestamp minTimestampInTable1 = dbModifier.getMinTimestampInTable(allTimestampColumns.get(0));
    assertThat(minTimestampInTable1, is(getTimestamp(MIN_DATE_TABLE1)));

    Timestamp minTimestampInTable2 = dbModifier.getMinTimestampInTable(allTimestampColumns.get(1));
    assertThat(minTimestampInTable2, is(getTimestamp(MIN_DATE)));
  }

  @Test
  public void testGetMaxTimestampForTable() {
    List<TableAndColumns> allTimestampColumns = dbModifier.getAllTimestampColumns(dbModifier.getAllTables());
    allTimestampColumns.sort(Comparator.comparing(item -> item.table));

    Timestamp maxTimestampInTable1 = dbModifier.getMaxTimestampInTable(allTimestampColumns.get(0));
    assertThat(maxTimestampInTable1, is(getTimestamp(MAX_DATE)));

    Timestamp maxTimestampInTable2 = dbModifier.getMaxTimestampInTable(allTimestampColumns.get(1));
    assertThat(maxTimestampInTable2, is(getTimestamp(MAX_DATE_TABLE2)));
  }

  @Test
  public void testGetMaxTimestamp() {
    Timestamp maxTimestamp = dbModifier.getMaxTimestamp();
    assertThat(maxTimestamp, is(getTimestamp(MAX_DATE)));
  }

  @Test
  public void testGetMinTimestamp() {
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp, is(getTimestamp(MIN_DATE)));
  }

  @Test
  public void testGetMaxTimestampWithAllNulls() throws Exception {
    dropTestDb(dbConnection);
    createEmptyTimestampDb(dbConnection);
    Timestamp maxTimestamp = dbModifier.getMaxTimestamp();
    assertThat(maxTimestamp, nullValue());
  }

  @Test
  public void testGetMinTimestampWithAllNulls() throws Exception {
    dropTestDb(dbConnection);
    createEmptyTimestampDb(dbConnection);
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp, nullValue());
  }

  private static Connection createTestDbConnection() throws Exception {
    return DriverManager.getConnection(TEST_DB_CONNECTION_STRING, USER_NAME, PASSWORD);
  }

  private static void dropTestDb(Connection conn) throws Exception {
    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate("DROP TABLE IF EXISTS " + TABLE_NAME1);
      statement.executeUpdate("DROP TABLE IF EXISTS " + TABLE_NAME2);
      statement.executeUpdate("SET SCHEMA PUBLIC");
      statement.executeUpdate("DROP SCHEMA IF EXISTS " + TEST_SCHEMA);
    }
  }

  private static void createTestDb(Connection conn) throws Exception {
    String tableSql1 = "CREATE TABLE " + TABLE_NAME1 + " (id varchar(5) NOT NULL, ts1 datetime NOT NULL)";
    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate("CREATE SCHEMA " + TEST_SCHEMA);
      statement.executeUpdate("SET SCHEMA " + TEST_SCHEMA);
      statement.executeUpdate(tableSql1);
      statement.executeUpdate("ALTER TABLE " + TABLE_NAME1 + " ADD PRIMARY KEY (id)");
      int index = 1;
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, MIN_DATE_TABLE1));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2018, 2, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2018, 3, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2018, 4, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2018, 5, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2018, 6, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2018, 7, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, MAX_DATE));
    }

    String tableSql2 = "CREATE TABLE " + TABLE_NAME2 + " (id varchar(5) NOT NULL, ts1 datetime NOT NULL, ts2 datetime)";
    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate(tableSql2);
      statement.executeUpdate("ALTER TABLE " + TABLE_NAME2 + " ADD PRIMARY KEY (id)");
      int index = 1;
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2017, 3, 1), MIN_DATE));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2017, 3, 2), LocalDate.of(2017, 2, 2)));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2017, 3, 3), LocalDate.of(2017, 2, 3)));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2017, 3, 4), LocalDate.of(2017, 2, 4)));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2017, 3, 5), null));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2017, 3, 6), LocalDate.of(2017, 2, 6)));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2017, 3, 7), LocalDate.of(2017, 2, 7)));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, MAX_DATE_TABLE2, LocalDate.of(2017, 2, 8)));
    }
  }

  private static void createEmptyTimestampDb(Connection conn) throws Exception {
    String tableSql1 = "CREATE TABLE " + TABLE_NAME1 + " (id varchar(5) NOT NULL, ts1 datetime)";
    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate("CREATE SCHEMA " + TEST_SCHEMA);
      statement.executeUpdate("SET SCHEMA " + TEST_SCHEMA);
      statement.executeUpdate(tableSql1);
      statement.executeUpdate("ALTER TABLE " + TABLE_NAME1 + " ADD PRIMARY KEY (id)");
      int index = 1;
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, null));
    }

    String tableSql2 = "CREATE TABLE " + TABLE_NAME2 + " (id varchar(5) NOT NULL, ts1 datetime, ts2 datetime)";
    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate(tableSql2);
      statement.executeUpdate("ALTER TABLE " + TABLE_NAME2 + " ADD PRIMARY KEY (id)");
      int index = 1;
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, null, null));
    }
  }

  private static String getInsertString(String tableName, int id, LocalDate... dates) {
    String insertString = "INSERT into " + tableName + " values ('id" + id + "'";
    if (dates == null) {
      return insertString + ", null)";
    }
    for (LocalDate date : dates) {
      if (date == null) {
        insertString += ", null";
      }
      else {
        String timestampString = date.format(timestampFormatter) + " 00:00:00";
        insertString += ", '" + timestampString + "'";
      }
    }
    insertString += ")";
    return insertString;
  }

  private static Timestamp getTimestamp(final LocalDate localDate) {
    return Timestamp.valueOf(localDate.atStartOfDay());
  }
}
