/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresTest
@Category(PostgresTestCategory.class)
public class PostgresPolicyViolationIndexAsyncDbMigrationTest
    extends PolicyViolationIndexAsyncDbMigrationTest
{
  @Inject
  private OperationalDataStore operationalDataStore;

  @Before
  public void setupPostgres() throws Exception {
    dropIndexIfExists();
  }

  @Test
  public void testRunMigration_createsValidIndex() throws Exception {
    underTest.runMigration();

    assertThat(isIndexValid()).isTrue();
  }

  @Test
  public void testRunMigration_replacesInvalidIndex() throws Exception {
    createInvalidIndex();

    underTest.runMigration();

    assertThat(isIndexValid()).isTrue();
  }

  @Test
  public void testRunMigration_idempotent() throws Exception {
    underTest.runMigration();
    assertThat(isIndexValid()).isTrue();

    migrationTrackerDAO.deleteById(underTest.getMigrationName());
    underTest.runMigration();
    assertThat(isIndexValid()).isTrue();
  }

  private boolean isIndexValid() throws Exception {
    String schema = operationalDataStore.getDatabaseSchema();
    try (Connection conn = operationalDataStore.getDataSource().getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT i.indisvalid FROM pg_index i "
                + "JOIN pg_class c ON c.oid = i.indexrelid "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE c.relname = 'policy_violation_app_stage_open_unfixed_idx' "
                + "AND n.nspname = '" + schema + "'"))
    {
      if (rs.next()) {
        return rs.getBoolean("indisvalid");
      }
      return false;
    }
  }

  private void createInvalidIndex() throws Exception {
    String schema = operationalDataStore.getDatabaseSchema();
    try (Connection conn = operationalDataStore.getDataSource().getConnection();
        Statement stmt = conn.createStatement())
    {
      conn.setAutoCommit(true);
      stmt.execute(
          "CREATE INDEX CONCURRENTLY IF NOT EXISTS policy_violation_app_stage_open_unfixed_idx "
              + "ON " + schema + ".policy_violation "
              + "(application_id, waive_time, stage_type_id, open_time DESC, threat_level DESC, policy_violation_id) "
              + "WHERE fix_time IS NULL");
      stmt.execute(
          "UPDATE pg_index SET indisvalid = false WHERE indexrelid = '"
              + schema + ".policy_violation_app_stage_open_unfixed_idx'::regclass");
    }
  }

  private void dropIndexIfExists() throws Exception {
    String schema = operationalDataStore.getDatabaseSchema();
    try (Connection conn = operationalDataStore.getDataSource().getConnection();
        Statement stmt = conn.createStatement())
    {
      stmt.execute("DROP INDEX IF EXISTS " + schema + ".policy_violation_app_stage_open_unfixed_idx");
    }
  }
}
