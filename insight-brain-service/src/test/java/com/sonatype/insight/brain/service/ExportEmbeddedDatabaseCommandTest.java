/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ExportEmbeddedDatabaseCommandTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void testRun_SupportsOnlyEmbeddedDatabase() throws Exception {
    InsightConfig config = new InsightConfig();
    config.setDatabase(new DatabaseConfig());
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      new ExportEmbeddedDatabaseCommand().run(null, null, config);
    }).withMessageContaining("can only be used when no external database is specified");
  }

  private TestInsightBrainService newService() {
    return new TestInsightBrainService().setWorkDir(tempDir.getRoot());
  }

  private void initData(InsightConfig config) {
    DatabaseConfigProvider databaseConfigProvider = new DatabaseConfigProvider(config);
    OperationalDataStoreProvider.init(databaseConfigProvider.getDatabaseConfig(DatabaseName.ods), true);
  }

  @Test
  public void testRun_UninitializedDatabase() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try {
      File dumpFile = new File(tempDir.getRoot(), "dump.sql");

      assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
        newService().run("export-embedded-db", "target/test-classes/config-test.yml", "--dump-file",
            dumpFile.getPath());
      }).withMessageMatching(".* The database from .* is empty.*");
      assertThat(dumpFile).doesNotExist();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testRun_GzippedDump() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try {
      File dumpFile = new File(tempDir.getRoot(), "dump.sql.gz");

      TestInsightBrainService service = newService();
      service.setConfigurator(config -> {
        initData(config);
      });

      service.run("export-embedded-db", "target/test-classes/config-test.yml", "--dump-file", dumpFile.getPath());

      assertThat(dumpFile).isFile();

      try (InputStream is = new GZIPInputStream(new FileInputStream(dumpFile))) {
        assertThat(is.read()).isPositive();
      }
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testRun_DumpImportableIntoPostgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      File dumpFile = new File(tempDir.getRoot(), "dump.sql");

      TestInsightBrainService service = newService();
      service.setConfigurator(config -> {
        initData(config);
      });

      service.run("export-embedded-db", "target/test-classes/config-test.yml", "--dump-file", dumpFile.getPath());

      assertThat(dumpFile).isFile();

      postgres.loadSqlDump(dumpFile.toPath());

      Map<String, Map<String, List<TableRow>>> expectedTablesBySchema = new HashMap<>();
      try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection()) {
        loadTableRows(expectedTablesBySchema, connection);
      }
      try (Connection connection = AggregationDataStoreProvider.getDataSource().getConnection()) {
        loadTableRows(expectedTablesBySchema, connection);
      }
      try (Connection connection = ThirdPartyScansProvider.getDataSource().getConnection()) {
        loadTableRows(expectedTablesBySchema, connection);
      }

      Map<String, Map<String, List<TableRow>>> actualTablesBySchema = new HashMap<>();
      try (Connection connection =
          DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        loadTableRows(actualTablesBySchema, connection);
      }

      assertThat(actualTablesBySchema.keySet()).isEqualTo(expectedTablesBySchema.keySet());
      for (String schemaName : actualTablesBySchema.keySet()) {
        Map<String, List<TableRow>> actualRowsByTable = actualTablesBySchema.get(schemaName);
        Map<String, List<TableRow>> expectedRowsByTable = expectedTablesBySchema.get(schemaName);
        assertThat(actualRowsByTable.keySet()).isEqualTo(actualRowsByTable.keySet());
        for (String tableName : actualRowsByTable.keySet()) {
          List<TableRow> actualRows = actualRowsByTable.get(tableName);
          List<TableRow> expectedRows = expectedRowsByTable.get(tableName);
          assertThat(actualRows).as(schemaName + "." + tableName).hasSameSizeAs(expectedRows);
          actualRows.sort(null);
          expectedRows.sort(null);
          for (int i = 0; i < actualRows.size(); i++) {
            assertThat(actualRows.get(i)).as(schemaName + "." + tableName + "." + i).usingRecursiveComparison()
                .isEqualTo(expectedRows.get(i));
          }
        }
      }
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
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

  private void loadTableRows(Map<String, Map<String, List<TableRow>>> tablesBySchema, Connection connection)
      throws Exception
  {
    DatabaseMetaData metadata = connection.getMetaData();
    try (ResultSet schemas = metadata.getSchemas(null, "insight_%")) {
      while (schemas.next()) {
        String schemaName = schemas.getString(1);
        tablesBySchema.put(schemaName, new HashMap<>());
        try (ResultSet tables = metadata.getTables(null, schemaName, null, new String[]{"TABLE"})) {
          while (tables.next()) {
            String tableName = tables.getString(3);
            List<TableRow> tableRows = new ArrayList<>();
            // for postgresql unquoted names are always folded to lower case e.g. QRTZ_TRIGGERS > qrtz_triggers
            tablesBySchema.get(schemaName).put(tableName.toLowerCase(Locale.ROOT), tableRows);
            Set<String> primaryKeys = getPrimaryKeys(metadata, schemaName, tableName);
            try (Statement query = connection.createStatement();
                ResultSet rows = query.executeQuery("SELECT * FROM " + schemaName + "." + tableName)) {
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

  @Test
  public void testTransformInsertValues_ColumnSeparator() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues(",1")).isEqualTo("\t1");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues(",    1")).isEqualTo("\t1");
  }

  @Test
  public void testTransformInsertValues_Null() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("NULL")).isEqualTo("\\N");
  }

  @Test
  public void testTransformInsertValues_Number() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("1234567890")).isEqualTo("1234567890");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("-1.234567890")).isEqualTo("-1.234567890");
  }

  @Test
  public void testTransformInsertValues_Boolean() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("TRUE")).isEqualTo("TRUE");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("FALSE")).isEqualTo("FALSE");
  }

  @Test
  public void testTransformInsertValues_Timestamp() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("TIMESTAMP '2019-06-14 19:25:51.334'"))
        .isEqualTo("2019-06-14 19:25:51.334");
  }

  @Test
  public void testTransformInsertValues_String_Quoted() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("''")).isEqualTo("");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("'abc \'\' \\N'")).isEqualTo("abc \' \\\\N");
  }

  @Test
  public void testTransformInsertValues_String_Encoded() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("STRINGDECODE('abc \'\' \\n\\t\\\\ \\u20AC')"))
        .isEqualTo("abc \' \\n\\t\\\\ \u20AC");
  }

  @Test
  public void testTransformInsertValues_Binary() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("X'0010abCDeF'")).isEqualTo("\\\\x0010abCDeF");
  }

  @Test
  public void testTransformInsertValues_MultipleColumns() {
    assertThat(ExportEmbeddedDatabaseCommand
        .transformInsertValues("-1, NULL, TRUE, 2.0, 'abc', STRINGDECODE('xyz'), TIMESTAMP '2019-06-14 19:25:51.334'"))
            .isEqualTo("-1\t\\N\tTRUE\t2.0\tabc\txyz\t2019-06-14 19:25:51.334");
  }
}
