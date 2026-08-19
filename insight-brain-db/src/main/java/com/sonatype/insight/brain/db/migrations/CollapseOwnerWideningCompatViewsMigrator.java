/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.PostIncrementalMigrator;
import com.sonatype.insight.db.DatabaseEngine;

/**
 * CLM-43708 Pattern B collapse. For each owner-widening table whose name was unchanged in release N
 * (only a column renamed), N left the real table under a temporary {@code _t} name and a compat view
 * under the final name exposing both column names. This migrator drops that view and renames the
 * {@code _t} table back to the final name. The relkind guard makes it a no-op on a tenant provisioned
 * fresh during N (final name is already the real table, no view / {@code _t}). Postgres FKs bind by OID,
 * so inbound FKs auto-follow the rename. H2 did plain in-place renames in N, so there is nothing to
 * collapse and this migrator no-ops there.
 */
public class CollapseOwnerWideningCompatViewsMigrator
    implements PostIncrementalMigrator
{
  private static final List<String> PATTERN_B_TABLES = List.of(
      "aggregate_file",
      "policy_evaluation",
      "policy_violation",
      "last_policy_evaluation",
      "reevaluate_cascade_progress",
      "quarantined_component_access");

  @Override
  public void migrate(DataSource dataSource, String databaseSchema) throws SQLException {
    DatabaseEngine engine = DatabaseUtil.getDatabaseEngine(dataSource);
    if (!"postgresql".equals(engine.getId())) {
      return;
    }

    try (Connection conn = dataSource.getConnection()) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute(engine.buildSetSchemaSql(databaseSchema));
      }
      for (String table : PATTERN_B_TABLES) {
        collapse(conn, databaseSchema, table);
      }
    }
  }

  private void collapse(Connection conn, String schema, String table) throws SQLException {
    String sql =
        "DO $$ BEGIN"
            + "  IF EXISTS (SELECT 1 FROM pg_class c"
            + "    JOIN pg_namespace n ON n.oid = c.relnamespace"
            + "    WHERE c.relname = '" + table + "' AND n.nspname = '" + schema + "' AND c.relkind = 'v') THEN"
            + "    DROP VIEW " + schema + "." + table + ";"
            + "    ALTER TABLE " + schema + "." + table + "_t RENAME TO " + table + ";"
            + "  END IF;"
            + " END $$;";
    try (Statement stmt = conn.createStatement()) {
      stmt.execute(sql);
    }
  }
}
