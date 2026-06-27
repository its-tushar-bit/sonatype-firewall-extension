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

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Creates a composite index on {@code repository_component (repository_id, hash, time DESC,
 * repository_component_id DESC)} using {@code CREATE INDEX CONCURRENTLY} outside the migration
 * lock (CLM-41005). One index serves two parts of the CM eligibility query:
 * <ol>
 * <li>The outer driver: a {@code Bitmap Index Scan} per qualifying {@code repository} (from the
 * {@code EXISTS} semi-join driver) uses the leading {@code (repository_id, hash, time, id)}
 * columns as Index Cond. Per-repository the rows are naturally ordered by {@code (time DESC, id
 * DESC)} so the outer {@code ORDER BY} typically resolves with a small in-memory Sort (over the
 * bitmap result, not the table). At eligibility scale this is dominated by {@code repository_id}
 * being the leading column — see r3465949378 for the EXPLAIN ANALYZE.</li>
 * <li>The inner {@code NOT EXISTS} anti-join probe: correlates on
 * {@code (repository_id, hash, time, repository_component_id)} — exactly the index column
 * order — so the dedup check is one b-tree probe per candidate row, not a seq scan or a
 * scan of every component sharing the same hash.</li>
 * </ol>
 * Note: semi-join push-down is planner-dependent and not contractually guaranteed. The empirical
 * plan on populated data shows {@code Bitmap Index Scan using repository_component_dedup_keyset_idx}
 * for both the outer driver and the inner probe (see r3465949378). If the planner regresses on a
 * future Postgres upgrade or under skewed statistics, a {@code Sort} node could reappear above the
 * driver scan. The inner probe's index use is robust regardless because all four probe columns are
 * indexed leading keys; the outer driver's path is the one to verify against fresh EXPLAIN.
 * <p>
 * See {@code RepositoryComponentDAO.getMonitoringEligiblePagePostgres} and the PR #16434 review
 * threads (r3465319178, r3465949378) for the EXPLAIN ANALYZE that drove this column order: an
 * earlier {@code (time DESC, repository_component_id DESC)} variant served only the outer driver
 * and forced the inner probe onto an unindexed seq scan; this composite serves both paths.
 * <p>
 * Runs as an AsyncDbMigration rather than in a regular incremental script because
 * {@code repository_component} is a large table on production tenants (per
 * {@code insight-brain-db/.claude/rules/sql-saas-compatibility.md}, &gt;100K rows is the threshold
 * for the rule) and {@code CREATE INDEX CONCURRENTLY} cannot run inside the migration advisory-lock
 * transaction (see {@code db/CLAUDE.md} for the deadlock rationale shared with the sibling
 * {@link RepositoryHostedMonitoringIndexAsyncDbMigration}).
 * <p>
 * Skipped on H2 — the plain (non-concurrent) variant of the index lives in
 * {@code schema_incremental_0472.h2.sql} for embedded dev/test fixtures.
 */
@Named
@Singleton
public class RepositoryComponentDedupKeysetIndexAsyncDbMigration
    extends AbstractAsyncDbMigration
{
  private static final String INDEX_NAME = "repository_component_dedup_keyset_idx";

  private final OperationalDataStore operationalDataStore;

  @Inject
  public RepositoryComponentDedupKeysetIndexAsyncDbMigration(
      final MigrationTrackerDAO migrationTrackerDAO,
      final OperationalDataStore operationalDataStore)
  {
    super(migrationTrackerDAO, "repository_component dedup-keyset composite index");
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  protected boolean executeMigration() {
    if (operationalDataStore.isDatabaseEmbedded()) {
      log.debug("Skipping {} creation on H2 (covered by schema_incremental_0472.h2.sql)", INDEX_NAME);
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
                + "ON " + schema + ".repository_component "
                + "(repository_id, hash, time DESC, repository_component_id DESC)");
      }
    }
    catch (SQLException e) {
      log.error("Failed to create {} for schema: {}", INDEX_NAME, schema, e);
      return false;
    }

    return true;
  }

  private void dropInvalidIndex(final Connection conn, final String schema) throws SQLException {
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
