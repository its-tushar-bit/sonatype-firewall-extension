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
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
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

  @Test
  public void testRun_GzippedDump() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try {
      File dumpFile = new File(tempDir.getRoot(), "dump.sql.gz");

      new TestInsightBrainService().run("export-embedded-db", "target/test-classes/config-test.yml", "--dump-file",
          dumpFile.getPath());

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

      new TestInsightBrainService().run("export-embedded-db", "target/test-classes/config-test.yml", "--dump-file",
          dumpFile.getPath());

      assertThat(dumpFile).isFile();

      postgres.loadSqlDump(dumpFile.toPath());

      Map<String, Map<String, Integer>> expectedTablesBySchema = new HashMap<>();
      try (Connection connection = OperationalDataStoreProvider.getDataSource().getConnection()) {
        countTableRows(expectedTablesBySchema, connection);
      }
      try (Connection connection = AggregationDataStoreProvider.getDataSource().getConnection()) {
        countTableRows(expectedTablesBySchema, connection);
      }

      Map<String, Map<String, Integer>> actualTablesBySchema = new HashMap<>();
      try (Connection connection =
          DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        countTableRows(actualTablesBySchema, connection);
      }

      assertThat(actualTablesBySchema).isEqualTo(expectedTablesBySchema);
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void countTableRows(Map<String, Map<String, Integer>> tablesBySchema, Connection connection)
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
            try (Statement query = connection.createStatement();
                ResultSet count = query.executeQuery("SELECT COUNT(*) FROM " + schemaName + "." + tableName)) {
              count.next();
              tablesBySchema.get(schemaName).put(tableName, count.getInt(1));
            }
          }
        }
      }
    }
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
  public void testTransformInsertValues_MultipleColumns() {
    assertThat(ExportEmbeddedDatabaseCommand
        .transformInsertValues("-1, NULL, TRUE, 2.0, 'abc', STRINGDECODE('xyz'), TIMESTAMP '2019-06-14 19:25:51.334'"))
            .isEqualTo("-1\t\\N\tTRUE\t2.0\tabc\txyz\t2019-06-14 19:25:51.334");
  }
}
