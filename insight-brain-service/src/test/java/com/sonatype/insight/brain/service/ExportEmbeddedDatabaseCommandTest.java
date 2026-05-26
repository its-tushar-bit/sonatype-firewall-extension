/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
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
import java.util.zip.GZIPInputStream;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.fixture.postgres.PostgresDatabaseFixture;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
public class ExportEmbeddedDatabaseCommandTest
    extends AbstractDatabaseTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  @H2InMemoryTest(cleanDatabase = true)
  public void testRun_SupportsOnlyEmbeddedDatabase() {
    InsightConfig config = new InsightConfig();
    config.setDatabase(new DatabaseConfig());
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> new ExportEmbeddedDatabaseCommand(config).run(config, null))
        .withMessageContaining("can only be used when no external database is specified");
  }

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

  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testRun_MissingDatabase() {
    File dumpFile = new File(tempDir.getRoot(), "dump.sql");

    // The test fixture automatically creates H2 db files. Manually delete them for this test
    File file = new File(databaseRule.getMetadata().get(H2DiskTest.DATABASE_PATH) + "/ods.h2.db");
    file.delete();

    InsightConfig config = newServiceConfig();
    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> new ExportEmbeddedDatabaseCommand(config).run(config, dumpFile.getPath()))
        .withMessageContaining("Cannot find the embedded database");
    assertThat(dumpFile).doesNotExist();
  }

  @Test
  @H2DiskTest(suppressMigrations = true, copyExistingDatabase = "ExportEmbeddedDatabaseCommandTest/EmptyDatabase")
  public void testRun_UninitializedDatabase() {
    File dumpFile = new File(tempDir.getRoot(), "dump.sql");
    InsightConfig config = newServiceConfig();

    assertThatExceptionOfType(RuntimeException.class)
        .isThrownBy(() -> new ExportEmbeddedDatabaseCommand(config).run(config, dumpFile.getPath()))
        .withMessageContaining("The server needs to have been started normally once before" +
            " in order to complete the required upgrade steps.");
    assertThat(dumpFile).doesNotExist();
  }

  @Test
  @H2DiskTest
  public void testRun_GzippedDump() throws Exception {
    File outputDumpFile = new File(tempDir.getRoot(), "dump.sql.gz");
    initData();

    InsightConfig config = newServiceConfig();
    new ExportEmbeddedDatabaseCommand(config).run(config, outputDumpFile.getPath());

    assertThat(outputDumpFile).isFile();

    try (InputStream is = new GZIPInputStream(Files.newInputStream(outputDumpFile.toPath()))) {
      assertThat(is.read()).isPositive();
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
      com.sonatype.insight.db.DatabaseConfig pgDatabaseConfig =
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
  @Category(PostgresTestCategory.class)
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

  @Test
  @H2DiskTest
  public void testRun_SqlStatementsInCorrectOrder() throws Exception {
    File dumpFile = new File(tempDir.getRoot(), "dump.sql");

    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      statement.execute("CREATE SCHEMA test_schema;");
      statement.execute("CREATE TABLE test_schema.test_table (id VARCHAR(36) PRIMARY KEY);");
      statement.execute("CREATE VIEW test_schema.test_view AS SELECT * FROM test_schema.test_table;");
      statement.execute("INSERT INTO test_schema.test_table VALUES ('test-id');");
    }

    InsightConfig config = newServiceConfig();
    new ExportEmbeddedDatabaseCommand(config).run(config, dumpFile.getPath());

    assertThat(dumpFile).isFile();
    List<String> lines = Files.readAllLines(dumpFile.toPath());

    int schemaIndex = findStatementIndex(lines, "CREATE SCHEMA");
    int tableIndex = findStatementIndex(lines, "CREATE TABLE");
    int viewIndex = findStatementIndex(lines, "CREATE VIEW");
    int insertIndex = findStatementIndex(lines, "COPY ");

    assertThat(schemaIndex).as("Schema statements should come first").isLessThan(tableIndex);
    assertThat(tableIndex).as("Table statements should come before view statements").isLessThan(viewIndex);
    assertThat(viewIndex).as("View statements should come before insert statements").isLessThan(insertIndex);
  }

  @Test
  @H2DiskTest
  public void testRun_TransactionAndConstraintDeferral() throws Exception {
    File dumpFile = new File(tempDir.getRoot(), "dump.sql");

    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      statement.execute("CREATE SCHEMA test_schema;");
      statement.execute("CREATE TABLE test_schema.parent (id VARCHAR(36) PRIMARY KEY);");
      statement.execute("CREATE TABLE test_schema.child (id VARCHAR(36) PRIMARY KEY, parent_id VARCHAR(36));");
      statement.execute("ALTER TABLE test_schema.child ADD CONSTRAINT child_parent_fk " +
          "FOREIGN KEY (parent_id) REFERENCES test_schema.parent(id);");
      statement.execute("INSERT INTO test_schema.parent VALUES ('parent1');");
      statement.execute("INSERT INTO test_schema.child VALUES ('child1', 'parent1');");
    }

    InsightConfig config = newServiceConfig();
    new ExportEmbeddedDatabaseCommand(config).run(config, dumpFile.getPath());

    assertThat(dumpFile).isFile();
    List<String> lines = Files.readAllLines(dumpFile.toPath());

    int beginIndex = -1;
    int alterTableIndex = -1;
    int setConstraintsIndex = -1;
    int copyIndex = -1;
    int commitIndex = -1;

    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.equals("BEGIN;")) {
        beginIndex = i;
      }
      if (line.contains("ALTER TABLE") && line.contains("FOREIGN KEY")) {
        alterTableIndex = i;
      }
      if (line.equals("SET CONSTRAINTS ALL DEFERRED;")) {
        setConstraintsIndex = i;
      }
      if (line.startsWith("COPY ")) {
        copyIndex = i;
      }
      if (line.equals("COMMIT;")) {
        commitIndex = i;
      }
    }

    assertThat(beginIndex).as("Should have BEGIN transaction").isGreaterThanOrEqualTo(0);
    assertThat(alterTableIndex).as("Should have ALTER TABLE with FK").isGreaterThanOrEqualTo(0);
    assertThat(setConstraintsIndex).as("Should have SET CONSTRAINTS ALL DEFERRED").isGreaterThanOrEqualTo(0);
    assertThat(copyIndex).as("Should have COPY statement").isGreaterThanOrEqualTo(0);
    assertThat(commitIndex).as("Should have COMMIT transaction").isGreaterThanOrEqualTo(0);

    assertThat(alterTableIndex).as("FK constraints must be created before SET CONSTRAINTS")
        .isLessThan(setConstraintsIndex);
    assertThat(setConstraintsIndex).as("SET CONSTRAINTS must come before data insertion").isLessThan(copyIndex);
    assertThat(copyIndex).as("Data insertion must come before COMMIT").isLessThan(commitIndex);
  }

  @Test
  @H2DiskTest
  public void testRun_StatementClassificationAndComments() throws Exception {
    File dumpFile = new File(tempDir.getRoot(), "dump.sql");
    initData();

    InsightConfig config = newServiceConfig();
    new ExportEmbeddedDatabaseCommand(config).run(config, dumpFile.getPath());

    assertThat(dumpFile).isFile();
    List<String> lines = Files.readAllLines(dumpFile.toPath());

    boolean foundDeferrableForeignKey = false;

    for (String line : lines) {
      if (line.contains("DEFERRABLE INITIALLY IMMEDIATE")) {
        foundDeferrableForeignKey = true;
        break;
      }
    }

    assertThat(lines).as("Should include transaction begin statement").contains("BEGIN;");
    assertThat(lines).as("Should defer constraints before inserts").contains("SET CONSTRAINTS ALL DEFERRED;");
    assertThat(foundDeferrableForeignKey).as("Should mark foreign keys as deferrable for PostgreSQL imports")
        .isTrue();
  }

  private int findStatementIndex(List<String> lines, String statementPrefix) {
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).startsWith(statementPrefix)) {
        return i;
      }
    }
    return -1;
  }
}
