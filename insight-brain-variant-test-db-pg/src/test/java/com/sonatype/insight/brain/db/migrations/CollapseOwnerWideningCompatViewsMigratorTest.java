/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.datastore.DataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(PostgresTestCategory.class)
public class CollapseOwnerWideningCompatViewsMigratorTest
    extends AbstractDatabaseTest
{
  private static final List<String> PATTERN_B_TABLES = List.of(
      "aggregate_file",
      "policy_evaluation",
      "policy_violation",
      "last_policy_evaluation",
      "reevaluate_cascade_progress",
      "quarantined_component_access");

  private DataStore dataStore;

  private String schema;

  private final CollapseOwnerWideningCompatViewsMigrator migrator = new CollapseOwnerWideningCompatViewsMigrator();

  @Before
  public void setup() {
    dataStore = databaseRule.getOperationalDataStore();
    schema = dataStore.getDatabaseSchema();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(cleanDatabase = true, suppressMigrations = true)
  public void migratedTenant_collapsesViewAndRenamesBaseTable() throws Exception {
    DataSource dataSource = dataStore.getDataSource();
    for (String table : PATTERN_B_TABLES) {
      createMigratedNState(dataSource, table);

      assertThat(relkind(dataSource, table)).isEqualTo("v");
      assertThat(exists(dataSource, table + "_t")).isTrue();
    }

    migrator.migrate(dataSource, schema);

    for (String table : PATTERN_B_TABLES) {
      assertThat(relkind(dataSource, table)).as(table).isEqualTo("r");
      assertThat(exists(dataSource, table + "_t")).as(table).isFalse();
      assertThat(hasInboundForeignKey(dataSource, table)).as(table).isTrue();
    }
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(cleanDatabase = true, suppressMigrations = true)
  public void freshTenant_isNoOp() throws Exception {
    DataSource dataSource = dataStore.getDataSource();
    for (String table : PATTERN_B_TABLES) {
      createFreshNState(dataSource, table);
    }

    migrator.migrate(dataSource, schema);

    for (String table : PATTERN_B_TABLES) {
      assertThat(relkind(dataSource, table)).as(table).isEqualTo("r");
      assertThat(exists(dataSource, table + "_t")).as(table).isFalse();
    }
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest(cleanDatabase = true, suppressMigrations = true)
  public void reRun_isIdempotent() throws Exception {
    DataSource dataSource = dataStore.getDataSource();
    for (String table : PATTERN_B_TABLES) {
      createMigratedNState(dataSource, table);
    }

    migrator.migrate(dataSource, schema);
    migrator.migrate(dataSource, schema);

    for (String table : PATTERN_B_TABLES) {
      assertThat(relkind(dataSource, table)).as(table).isEqualTo("r");
      assertThat(exists(dataSource, table + "_t")).as(table).isFalse();
    }
  }

  private void createMigratedNState(DataSource dataSource, String table) throws SQLException {
    exec(dataSource,
        "CREATE SCHEMA IF NOT EXISTS " + schema + ";",
        "CREATE TABLE " + schema + "." + table + "_t (id varchar(64) PRIMARY KEY, owner_id varchar(64));",
        "CREATE VIEW " + schema + "." + table + " AS SELECT id, owner_id FROM " + schema + "." + table + "_t;",
        "CREATE TABLE " + schema + "." + table + "_child (id varchar(64) PRIMARY KEY, parent_id varchar(64)"
            + " REFERENCES " + schema + "." + table + "_t(id));");
  }

  private void createFreshNState(DataSource dataSource, String table) throws SQLException {
    exec(dataSource,
        "CREATE SCHEMA IF NOT EXISTS " + schema + ";",
        "CREATE TABLE " + schema + "." + table + " (id varchar(64) PRIMARY KEY, owner_id varchar(64));");
  }

  private void exec(DataSource dataSource, String... statements) throws SQLException {
    try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
      for (String sql : statements) {
        stmt.execute(sql);
      }
    }
  }

  private String relkind(DataSource dataSource, String relname) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT c.relkind::text FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE c.relname = '" + relname + "' AND n.nspname = '" + schema + "'"))
    {
      return rs.next() ? rs.getString(1) : null;
    }
  }

  private boolean exists(DataSource dataSource, String relname) throws SQLException {
    return relkind(dataSource, relname) != null;
  }

  private boolean hasInboundForeignKey(DataSource dataSource, String referencedTable) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT count(*) FROM pg_constraint con"
                + " JOIN pg_class c ON c.oid = con.confrelid"
                + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE con.contype = 'f' AND c.relname = '" + referencedTable + "' AND n.nspname = '" + schema
                + "'"))
    {
      return rs.next() && rs.getInt(1) > 0;
    }
  }
}
