/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

/**
 * Creates the policy_violation partial index using CONCURRENTLY outside of the migration lock.
 * <p>
 * This runs as an AsyncDbMigration (after startup) rather than in a regular incremental script
 * because CREATE INDEX CONCURRENTLY deadlocks with ClusterLockManager.createForSchemaMigration:
 * CONCURRENTLY waits for all open transactions to complete, but the advisory lock transaction
 * remains open until migrations finish.
 */
@Named
@Singleton
public class PolicyViolationIndexAsyncDbMigration
    extends AbstractAsyncDbMigration
{
  private final OperationalDataStore operationalDataStore;

  @Inject
  public PolicyViolationIndexAsyncDbMigration(
      final MigrationTrackerDAO migrationTrackerDAO,
      final OperationalDataStore operationalDataStore)
  {
    super(migrationTrackerDAO, "policy violation index");
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  protected boolean executeMigration() {
    if (operationalDataStore.isDatabaseEmbedded()) {
      log.debug("Skipping policy_violation_app_stage_open_unfixed_idx creation on H2");
      return true;
    }

    String schema = operationalDataStore.getDatabaseSchema();

    try (Connection conn = operationalDataStore.getDataSource().getConnection()) {
      conn.setAutoCommit(true);
      dropInvalidIndexConcurrentlyIfPresent(conn, schema, "policy_violation_app_stage_open_unfixed_idx");

      try (Statement stmt = conn.createStatement()) {
        log.info("Creating policy_violation_app_stage_open_unfixed_idx CONCURRENTLY for schema: {}", schema);
        stmt.execute(
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS policy_violation_app_stage_open_unfixed_idx "
                + "ON " + schema + ".policy_violation "
                + "(application_id, waive_time, stage_type_id, open_time DESC, threat_level DESC, policy_violation_id) "
                + "WHERE fix_time IS NULL");
      }
    }
    catch (SQLException e) {
      log.error("Failed to create policy violation index for schema: {}", schema, e);
      return false;
    }

    return true;
  }
}
