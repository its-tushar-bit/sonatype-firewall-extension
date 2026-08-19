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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.brain.testsupport.TempFolder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ExportEmbeddedDatabaseCommandTest
    extends AbstractDatabaseTest
{
  @RegisterExtension
  public TempFolder tempDir = new TempFolder();

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
