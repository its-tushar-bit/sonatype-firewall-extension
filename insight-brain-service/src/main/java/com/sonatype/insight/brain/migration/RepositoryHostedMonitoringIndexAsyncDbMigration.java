/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

/**
 * Creates a partial index on {@code repository (repository_id) WHERE repository_type = 'hosted'
 * AND monitoring_enabled = TRUE} using {@code CREATE INDEX CONCURRENTLY} outside the migration
 * lock (CLM-40971).
 * <p>
 * The {@code HostedRepoEligibilitySelector} eligibility query joins {@code repository} on
 * {@code (repository_type='hosted' AND monitoring_enabled=TRUE)} every page of every cycle.
 * Without this partial index the planner falls back to a sequential scan of {@code repository}
 * at scale (large tenants with thousands of repos but only a small fraction monitored), driving
 * the per-page cost from O(monitored) to O(all repos) across {@code DEFAULT_MAX_CYCLE_PAGES}.
 * <p>
 * Runs as an AsyncDbMigration rather than in a regular incremental script because
 * {@code CREATE INDEX CONCURRENTLY} deadlocks with {@code ClusterLockManager.createForSchemaMigration}:
 * CONCURRENTLY waits for all open transactions to complete, but the advisory lock transaction
 * remains open until migrations finish. See {@code db/CLAUDE.md} for the full rationale.
 * <p>
 * Skipped on H2 — embedded dev/test fixtures are too small for the planner to care, and H2 does
 * not honor partial-index {@code WHERE} clauses anyway.
 */
@Named
@Singleton
public class RepositoryHostedMonitoringIndexAsyncDbMigration
    extends AbstractAsyncDbMigration
{
  private static final String INDEX_NAME = "repository_hosted_monitoring_enabled_idx";

  private final OperationalDataStore operationalDataStore;

  @Inject
  public RepositoryHostedMonitoringIndexAsyncDbMigration(
      final MigrationTrackerDAO migrationTrackerDAO,
      final OperationalDataStore operationalDataStore)
  {
    super(migrationTrackerDAO, "repository hosted-monitoring partial index");
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  protected boolean executeMigration() {
    if (operationalDataStore.isDatabaseEmbedded()) {
      log.debug("Skipping {} creation on H2", INDEX_NAME);
      return true;
    }

    String schema = operationalDataStore.getDatabaseSchema();

    try (Connection conn = operationalDataStore.getDataSource().getConnection()) {
      conn.setAutoCommit(true);
      dropInvalidIndex(conn, schema);

      try (Statement stmt = conn.createStatement()) {
        log.info("Creating {} CONCURRENTLY for schema: {}", INDEX_NAME, schema);
        stmt.execute(
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS " + INDEX_NAME + " "
                + "ON " + schema + ".repository (repository_id) "
                + "WHERE repository_type = 'hosted' AND monitoring_enabled = TRUE");
      }
    }
    catch (SQLException e) {
      log.error("Failed to create {} for schema: {}", INDEX_NAME, schema, e);
      return false;
    }

    return true;
  }

  private void dropInvalidIndex(Connection conn, String schema) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "SELECT 1 FROM pg_index i "
            + "JOIN pg_class c ON c.oid = i.indexrelid "
            + "JOIN pg_namespace n ON n.oid = c.relnamespace "
            + "WHERE c.relname = ? "
            + "AND n.nspname = ? "
            + "AND NOT i.indisvalid"))
    {
      ps.setString(1, INDEX_NAME);
      ps.setString(2, schema);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          log.info("Dropping invalid {} in schema: {}", INDEX_NAME, schema);
          try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP INDEX CONCURRENTLY " + schema + "." + INDEX_NAME);
          }
        }
      }
    }
  }
}
