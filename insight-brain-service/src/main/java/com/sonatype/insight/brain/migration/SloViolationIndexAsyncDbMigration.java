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
 * Creates the policy_violation SLO feed index using CONCURRENTLY outside of the migration lock. The index is on
 * (owner_id, stage_type_id, GREATEST(COALESCE(open_time,ε), COALESCE(waive_time,ε), COALESCE(fix_time,ε),
 * COALESCE(legacy_violation_time,ε)), policy_violation_id). This supports the SLO violation feed (CLM-42077),
 * whose all-states, per
 * application-and-stage query ordered by update time would otherwise bitmap-scan and sort the whole
 * application partition on every request.
 * <p>
 * This runs as an AsyncDbMigration (after startup) rather than in a regular incremental script because
 * CREATE INDEX CONCURRENTLY deadlocks with ClusterLockManager.createForSchemaMigration: CONCURRENTLY waits
 * for all open transactions to complete, but the advisory lock transaction remains open until migrations finish.
 */
@Named
@Singleton
public class SloViolationIndexAsyncDbMigration
    extends AbstractAsyncDbMigration
{
  private final OperationalDataStore operationalDataStore;

  @Inject
  public SloViolationIndexAsyncDbMigration(
      final MigrationTrackerDAO migrationTrackerDAO,
      final OperationalDataStore operationalDataStore)
  {
    super(migrationTrackerDAO, "slo violation feed index");
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  protected boolean executeMigration() {
    if (operationalDataStore.isDatabaseEmbedded()) {
      log.debug("Skipping policy_violation_app_stage_updated_idx creation on H2");
      return true;
    }

    String schema = operationalDataStore.getDatabaseSchema();

    try (Connection conn = operationalDataStore.getDataSource().getConnection()) {
      conn.setAutoCommit(true);
      dropInvalidIndexConcurrentlyIfPresent(conn, schema, "policy_violation_app_stage_updated_idx");

      String table = resolveBaseTable(conn, schema, "policy_violation");
      try (Statement stmt = conn.createStatement()) {
        log.info("Creating policy_violation_app_stage_updated_idx CONCURRENTLY for schema: {}", schema);
        stmt.execute(
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS policy_violation_app_stage_updated_idx "
                + "ON " + schema + "." + table + " "
                + "(owner_id, stage_type_id, "
                + "GREATEST(COALESCE(open_time, TIMESTAMP '1970-01-01 00:00:00'), "
                + "COALESCE(waive_time, TIMESTAMP '1970-01-01 00:00:00'), "
                + "COALESCE(fix_time, TIMESTAMP '1970-01-01 00:00:00'), "
                + "COALESCE(legacy_violation_time, TIMESTAMP '1970-01-01 00:00:00')), "
                + "policy_violation_id)");
      }
    }
    catch (SQLException e) {
      log.error("Failed to create SLO violation feed index for schema: {}", schema, e);
      return false;
    }

    return true;
  }
}
