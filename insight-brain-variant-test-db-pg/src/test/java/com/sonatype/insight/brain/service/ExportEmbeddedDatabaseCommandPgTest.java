/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.fixture.postgres.PostgresDatabaseFixture;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link ExportEmbeddedDatabaseCommandTest} (CLM-45228).
 */
@PostgresTest
public class ExportEmbeddedDatabaseCommandPgTest
    extends AbstractDatabaseTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private InsightConfig newServiceConfig() {
    Path databaseFile = Paths.get(getDatabasePath().getAbsolutePath(), "ods.h2.db");
    File workDir = databaseFile.getParent().getParent().toFile();
    InsightConfig config = new InsightConfig();
    config.setSonatypeWork(workDir.getAbsolutePath());
    return config;
  }

  private void initData() {
    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      statement.execute("INSERT INTO insight_brain_ods.saml_configuration " +
          "VALUES ('\0a74878d8bfe44d2086ca8387e340692f', '{}', '', '');");
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Ignore("CLM-39891: Fix embedded-postgres schema validation after testcontainers removal")
  @Test
  @H2DiskTest
  public void testRun_DumpImportableIntoPostgres() throws Exception {
    // This test is unique in that it uses both an H2 database and Postgres. We use @H2DiskTest with the database rule
    // and manually instantiate a PostgresDatabaseFixture for the Postgres side. Annotations cannot be instantiated
    // so the easiest to consume it from a dummy class.
    PostgresTest postgresTest = DummyForAnnotation.class.getAnnotation(PostgresTest.class);
    try (PostgresDatabaseFixture postgresDatabaseFixture = new PostgresDatabaseFixture(
        "testRun_DumpImportableIntoPostgres", postgresTest))
    {
      File dumpFile = new File(tempDir.getRoot(), "dump.sql");
      initData();

      InsightConfig config = newServiceConfig();
      new ExportEmbeddedDatabaseCommand(config).run(config, dumpFile.getPath());

      assertThat(dumpFile).isFile();

      postgresDatabaseFixture.loadSqlDump(dumpFile.toPath());

      Map<String, Map<String, List<TableRow>>> expectedTablesBySchema = new HashMap<>();
      try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection()) {
        loadTableRows(expectedTablesBySchema, connection);
      }
      try (Connection connection = databaseRule.getAggregationDataStore().getDataSource().getConnection()) {
        loadTableRows(expectedTablesBySchema, connection);
      }
      try (Connection connection = databaseRule.getThirdPartyScansDataStore().getDataSource().getConnection()) {
        loadTableRows(expectedTablesBySchema, connection);
      }

      Map<String, Map<String, List<TableRow>>> actualTablesBySchema = new HashMap<>();
      DatabaseConfig pgDatabaseConfig =
          postgresDatabaseFixture.getDatabaseConfig(DatabaseName.ods.name());
      try (Connection connection = DriverManager.getConnection(pgDatabaseConfig.getUrl(),
          pgDatabaseConfig.getUsername(),
          pgDatabaseConfig.getPassword()))
      {
        loadTableRows(actualTablesBySchema, connection);
      }

      assertThat(actualTablesBySchema.keySet()).isEqualTo(expectedTablesBySchema.keySet());
      for (String schemaName : actualTablesBySchema.keySet()) {
        Map<String, List<TableRow>> actualRowsByTable = actualTablesBySchema.get(schemaName);
        Map<String, List<TableRow>> expectedRowsByTable = expectedTablesBySchema.get(schemaName);
        assertThat(actualRowsByTable).as(schemaName).containsOnlyKeys(expectedRowsByTable.keySet());
        for (String tableName : actualRowsByTable.keySet()) {
          List<TableRow> actualRows = actualRowsByTable.get(tableName);
          List<TableRow> expectedRows = expectedRowsByTable.get(tableName);
          assertThat(actualRows).as(schemaName + "." + tableName).hasSameSizeAs(expectedRows);
          actualRows.sort(null);
          expectedRows.sort(null);
          for (int i = 0; i < actualRows.size(); i++) {
            assertThat(actualRows.get(i)).as(schemaName + "." + tableName + "." + i)
                .usingRecursiveComparison()
                .withComparatorForType((String o1, String o2) -> o2.replace("\0", "").compareTo(o1), String.class)
                .isEqualTo(expectedRows.get(i));
          }
        }
      }
    }
  }

  @PostgresTest(suppressMigrations = true)
  private static class DummyForAnnotation
  {
    // for usage in testRun_DumpImportableIntoPostgres
  }

  private static class TableRow
      implements Comparable<TableRow>
  {
    String primaryKey = "";

    List<Object> otherColumns = new ArrayList<>();

    @Override
    public int compareTo(TableRow that) {
      return primaryKey.compareTo(that.primaryKey);
    }

    @Override
    public String toString() {
      return primaryKey + " > " + otherColumns;
    }
  }

  private void loadTableRows(
      Map<String, Map<String, List<TableRow>>> tablesBySchema,
      Connection connection) throws Exception
  {
    DatabaseMetaData metadata = connection.getMetaData();
    try (ResultSet schemas = metadata.getSchemas(null, "insight_%")) {
      while (schemas.next()) {
        String schemaName = schemas.getString(1);
        tablesBySchema.put(schemaName, new HashMap<>());
        try (ResultSet tables = metadata.getTables(null, schemaName, null, new String[]{"TABLE", "VIEW"})) {
          while (tables.next()) {
            String tableName = tables.getString(3);
            List<TableRow> tableRows = new ArrayList<>();
            // for postgresql unquoted names are always folded to lower case e.g. QRTZ_TRIGGERS > qrtz_triggers
            tablesBySchema.get(schemaName).put(tableName.toLowerCase(Locale.ROOT), tableRows);
            Set<String> primaryKeys = getPrimaryKeys(metadata, schemaName, tableName);
            try (Statement query = connection.createStatement();
                ResultSet rows = query.executeQuery("SELECT * FROM " + schemaName + "." + tableName))
            {
              int columnCount = rows.getMetaData().getColumnCount();
              while (rows.next()) {
                TableRow tableRow = new TableRow();
                tableRows.add(tableRow);
                for (int i = 1; i <= columnCount; i++) {
                  Object columnValue;
                  switch (rows.getMetaData().getColumnType(i)) {
                    case Types.CLOB:
                      columnValue = rows.getString(i);
                      break;
                    default:
                      columnValue = rows.getObject(i);
                      if (columnValue instanceof Number) {
                        columnValue = ((Number) columnValue).doubleValue();
                      }
                  }
                  if (primaryKeys.contains(rows.getMetaData().getColumnName(i))) {
                    tableRow.primaryKey += columnValue + "\t";
                  }
                  else {
                    tableRow.otherColumns.add(columnValue);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private Set<String> getPrimaryKeys(DatabaseMetaData metadata, String schemaName, String tableName) throws Exception {
    Set<String> columnNames = new HashSet<>();
    try (ResultSet primaryKeys = metadata.getPrimaryKeys(null, schemaName, tableName)) {
      while (primaryKeys.next()) {
        columnNames.add(primaryKeys.getString("COLUMN_NAME"));
      }
    }
    return columnNames;
  }
}
