/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.tools.dbmodifier.DbModifier.TableAndColumns;
import com.sonatype.insight.brain.tools.dbmodifier.DbModifier.TableDateMinMax;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class DbModifierTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private static final String TEST_SCHEMA = "TEST_SCHEMA";

  private static final LocalDate MAX_DATE = LocalDate.of(2017, 8, 1);

  private static final LocalDate MIN_DATE = LocalDate.of(2014, 2, 1);

  private static final LocalDate MIN_DATE_TABLE1 = LocalDate.of(2017, 1, 1);

  private static final LocalDate MAX_DATE_TABLE2 = LocalDate.of(2014, 3, 8);

  private static final String TEST_DB_CONNECTION_STRING =
      "jdbc:h2:mem:test;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE";

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
    dbModifier = new DbModifier(null, TEST_DB_CONNECTION_STRING, USER_NAME, PASSWORD, TEST_SCHEMA);
  }

  @After
  public void cleanup() throws Exception {
    dropTestDb(dbConnection);
    dbConnection.close();
  }

  @Test
  public void testShiftDays_ForwardOneDay() {
    dbModifier.shiftDays(1);
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp).isEqualTo(getTimestamp(MIN_DATE.plusDays(1)));
    Timestamp maxTimestamp = dbModifier.getMaxTimestamp();
    assertThat(maxTimestamp).isEqualTo(getTimestamp(MAX_DATE.plusDays(1)));
  }

  @Test
  public void testShiftDays_ForwardThirtyDays() {
    dbModifier.shiftDays(30);
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp).isEqualTo(getTimestamp(MIN_DATE.plusDays(30)));
  }

  @Test
  public void testShiftDays_BackwardOneDay() {
    dbModifier.shiftDays(-1);
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp).isEqualTo(getTimestamp(MIN_DATE.minusDays(1)));
  }

  @Test
  public void testShiftDays_BackwardThirtyDays() {
    dbModifier.shiftDays(-30);
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp).isEqualTo(getTimestamp(MIN_DATE.minusDays(30)));
  }

  @Test
  public void testShiftDays_AllNulls() throws Exception {
    dropTestDb(dbConnection);
    createEmptyTimestampDb(dbConnection);
    dbModifier.shiftDays(1);
    assertThat(dbModifier.getMinTimestamp()).isNull();
    assertThat(dbModifier.getMaxTimestamp()).isNull();
  }

  @Test
  public void testShiftToDate_Backward() {
    LocalDate dateToShiftTo = LocalDate.of(2016, 6, 1);
    testDateShift(dateToShiftTo);
  }

  @Test
  public void testShiftToDate_Forward() {
    LocalDate dateToShiftTo = LocalDate.of(2018, 6, 1);
    testDateShift(dateToShiftTo);
  }

  private void testDateShift(final LocalDate dateToShiftTo) {
    dbModifier.shiftToDate(dateToShiftTo);
    Timestamp maxTimestamp = dbModifier.getMaxTimestamp();
    assertThat(maxTimestamp).isEqualTo(getTimestamp(dateToShiftTo));
    Duration timeDifference = Duration.between(getTimestamp(MAX_DATE).toLocalDateTime(), dateToShiftTo.atStartOfDay());
    long daysDifference = timeDifference.toDays();
    assertThat(dbModifier.getMinTimestamp()).isEqualTo(getTimestamp(MIN_DATE.plusDays(daysDifference)));
  }

  @Test
  public void testShiftToDate_AllNulls() throws Exception {
    dropTestDb(dbConnection);
    createEmptyTimestampDb(dbConnection);
    LocalDate dateToShiftTo = LocalDate.of(2018, 6, 1);
    dbModifier.shiftToDate(dateToShiftTo);
    assertThat(dbModifier.getMinTimestamp()).isNull();
    assertThat(dbModifier.getMaxTimestamp()).isNull();
  }

  @Test
  public void testShiftToDate_CheckTimeIsNormalized() throws Exception {
    modifyDbToAddTimeToMax(dbConnection, "15:23:00");
    LocalDate dateToShiftTo = LocalDate.of(2018, 6, 1);
    dbModifier.shiftToDate(dateToShiftTo);
    Timestamp maxTimestamp = dbModifier.getMaxTimestamp();
    Timestamp expectedTimestamp = Timestamp.valueOf(dateToShiftTo.atTime(15, 23, 0));
    assertThat(maxTimestamp).isEqualTo(expectedTimestamp);
    Duration timeDifference = Duration.between(getTimestamp(MAX_DATE).toLocalDateTime(), dateToShiftTo.atStartOfDay());
    long daysDifference = timeDifference.toDays();
    assertThat(dbModifier.getMinTimestamp()).isEqualTo(getTimestamp(MIN_DATE.plusDays(daysDifference)));
  }

  @Test
  public void testShowDateInfo() {
    List<TableDateMinMax> tableDates = dbModifier.getDateInfo();
    tableDates.forEach(tableDate -> {
      if (tableDate.table.equals(TABLE_NAME1)) {
        assertThat(tableDate.max).isEqualTo(getTimestamp(MAX_DATE));
        assertThat(tableDate.min).isEqualTo(getTimestamp(MIN_DATE_TABLE1));
      }
      else {
        assertThat(tableDate.max).isEqualTo(getTimestamp(MAX_DATE_TABLE2));
        assertThat(tableDate.min).isEqualTo(getTimestamp(MIN_DATE));
      }
    });
  }

  @Test
  public void testGetAllTables() {
    List<String> allTables = dbModifier.getAllTables();
    assertThat(allTables).containsExactlyInAnyOrder(TABLE_NAME1, TABLE_NAME2);
  }

  @Test
  public void testGetMinTimestampForTable() {
    List<TableAndColumns> allTimestampColumns = dbModifier.getAllTimestampColumns(dbModifier.getAllTables());
    allTimestampColumns.sort(Comparator.comparing(item -> item.table));

    Timestamp minTimestampInTable1 = dbModifier.getMinTimestampInTable(allTimestampColumns.get(0));
    assertThat(minTimestampInTable1).isEqualTo(getTimestamp(MIN_DATE_TABLE1));

    Timestamp minTimestampInTable2 = dbModifier.getMinTimestampInTable(allTimestampColumns.get(1));
    assertThat(minTimestampInTable2).isEqualTo(getTimestamp(MIN_DATE));
  }

  @Test
  public void testGetMaxTimestampForTable() {
    List<TableAndColumns> allTimestampColumns = dbModifier.getAllTimestampColumns(dbModifier.getAllTables());
    allTimestampColumns.sort(Comparator.comparing(item -> item.table));

    Timestamp maxTimestampInTable1 = dbModifier.getMaxTimestampInTable(allTimestampColumns.get(0));
    assertThat(maxTimestampInTable1).isEqualTo(getTimestamp(MAX_DATE));

    Timestamp maxTimestampInTable2 = dbModifier.getMaxTimestampInTable(allTimestampColumns.get(1));
    assertThat(maxTimestampInTable2).isEqualTo(getTimestamp(MAX_DATE_TABLE2));
  }

  @Test
  public void testGetMaxTimestamp() {
    Timestamp maxTimestamp = dbModifier.getMaxTimestamp();
    assertThat(maxTimestamp).isEqualTo(getTimestamp(MAX_DATE));
  }

  @Test
  public void testGetMinTimestamp() {
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp).isEqualTo(getTimestamp(MIN_DATE));
  }

  @Test
  public void testGetMaxTimestampWithAllNulls() throws Exception {
    dropTestDb(dbConnection);
    createEmptyTimestampDb(dbConnection);
    Timestamp maxTimestamp = dbModifier.getMaxTimestamp();
    assertThat(maxTimestamp).isNull();
  }

  @Test
  public void testGetMinTimestampWithAllNulls() throws Exception {
    dropTestDb(dbConnection);
    createEmptyTimestampDb(dbConnection);
    Timestamp minTimestamp = dbModifier.getMinTimestamp();
    assertThat(minTimestamp).isNull();
  }

  @Test
  public void testDbVersion_FromDatabase() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyFileToDirectory(new File("target/test-classes/DbModifierTest/testDbVersion_FromDatabase/test.h2.db"),
        databaseDir);
    DbModifier dbModifier = new DbModifier(new File(databaseDir, "test"), "sa", PASSWORD, "test");

    assertThat(dbModifier.dbVersion()).isEqualTo("1");
  }
  
  @Test
  public void testDbVersion_FromFile() throws Exception {
    File databaseDir = tempDir.newFolder("db");
    FileUtils.copyFileToDirectory(new File("target/test-classes/DbModifierTest/testDbVersion_FromFile/test.h2.db"),
        databaseDir);
    DbModifier dbModifier = new DbModifier(new File(databaseDir, "test"), "sa", PASSWORD, "test");
    
    assertThat(dbModifier.dbVersion()).isEqualTo("-1");
    
    FileUtils.copyFileToDirectory(new File("target/test-classes/DbModifierTest/testDbVersion_FromFile/test.ver"),
        databaseDir);
    
    assertThat(dbModifier.dbVersion()).isEqualTo("1");
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
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2017, 2, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2017, 3, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2017, 4, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2017, 5, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2017, 6, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, LocalDate.of(2017, 7, 1)));
      statement.executeUpdate(getInsertString(TABLE_NAME1, index++, MAX_DATE));
    }

    String tableSql2 = "CREATE TABLE " + TABLE_NAME2 + " (id varchar(5) NOT NULL, ts1 datetime NOT NULL, ts2 datetime)";
    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate(tableSql2);
      statement.executeUpdate("ALTER TABLE " + TABLE_NAME2 + " ADD PRIMARY KEY (id)");
      int index = 1;
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2014, 3, 1), MIN_DATE));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2014, 3, 2), LocalDate.of(2014, 2, 2)));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2014, 3, 3), LocalDate.of(2014, 2, 3)));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2014, 3, 4), LocalDate.of(2014, 2, 4)));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2014, 3, 5), null));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2014, 3, 6), LocalDate.of(2014, 2, 6)));
      statement
          .executeUpdate(getInsertString(TABLE_NAME2, index++, LocalDate.of(2014, 3, 7), LocalDate.of(2014, 2, 7)));
      statement.executeUpdate(getInsertString(TABLE_NAME2, index++, MAX_DATE_TABLE2, LocalDate.of(2014, 2, 8)));
    }
  }

  private static void modifyDbToAddTimeToMax(Connection conn, String timeString) throws Exception {
    String oldTimeStampString = MAX_DATE.format(timestampFormatter) + " 00:00:00";
    String newTimestampString = MAX_DATE.format(timestampFormatter) + " " + timeString;
    String tableSql =
        "UPDATE " + TABLE_NAME1 + " SET ts1 = '" + newTimestampString + "' WHERE ts1 = '" + oldTimeStampString + "'";
    try (Statement statement = conn.createStatement()) {
      statement.executeUpdate("SET SCHEMA " + TEST_SCHEMA);
      statement.executeUpdate(tableSql);
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
