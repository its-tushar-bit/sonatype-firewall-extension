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
import java.util.Comparator;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAsyncDbMigration
    implements Comparable<AbstractAsyncDbMigration>
{
  private static final Comparator<AbstractAsyncDbMigration> COMPARATOR = Comparator
      .comparingInt(AbstractAsyncDbMigration::migrationPriority)
      .thenComparing(AbstractAsyncDbMigration::getMigrationName);

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final MigrationTrackerDAO migrationTrackerDAO;

  private final String type;

  protected AbstractAsyncDbMigration(
      final MigrationTrackerDAO migrationTrackerDAO,
      final String type)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.type = type;
  }

  protected String getType() {
    return type;
  }

  protected abstract boolean executeMigration();

  /** Called after {@link #executeMigration()}, regardless of whether it succeeded or failed. */
  protected void onCompletion() {
  }

  public String getMigrationName() {
    return getClass().getSimpleName();
  }

  public int migrationPriority() {
    return Integer.MAX_VALUE;
  }

  public void runMigration() {
    if (migrationTrackerDAO.isTrackerPresent(getMigrationName())) {
      log.debug("Migration of {} has already been completed", type);
      return;
    }

    log.info("Starting migration of {}\n" +
        "The server will continue to be operational and fully functional during this optimization", type);
    onStart();

    boolean completed = executeMigration();

    if (completed) {
      log.info("Migration of {} completed. Adding Migration Tracker \"{}\" entry to prevent running again.",
          type, getMigrationName());
      migrationTrackerDAO.insertTracker(getMigrationName());
    }
    else {
      log.error("Migration of {} did not complete. Will retry on next startup.", type);
    }

    onCompletion();
  }

  protected void onStart() {
  }

  /**
   * Drops the named index CONCURRENTLY if a previous CREATE INDEX CONCURRENTLY left it in an invalid state.
   * Postgres marks an index {@code indisvalid = false} when a concurrent build is interrupted; such an index is
   * never used by the planner and blocks a subsequent {@code CREATE INDEX CONCURRENTLY IF NOT EXISTS} from
   * rebuilding it, so it must be dropped first.
   * <p>
   * SECURITY: both {@code schema} and {@code indexName} MUST be trusted, static, non-user-supplied values —
   * {@code schema} comes from {@code operationalDataStore} and {@code indexName} is a hardcoded literal in each
   * migration. They are concatenated into the {@code DROP INDEX} statement because DDL identifiers cannot be bound
   * as {@link PreparedStatement} parameters; there is no user-input path into this method.
   *
   * @param conn a connection with autocommit enabled (CONCURRENTLY cannot run inside a transaction block)
   * @param schema the schema containing the index (trusted, non-user-supplied)
   * @param indexName the unqualified index name to check and drop (trusted, hardcoded literal)
   */
  protected void dropInvalidIndexConcurrentlyIfPresent(
      final Connection conn,
      final String schema,
      final String indexName) throws SQLException
  {
    try (PreparedStatement ps = conn.prepareStatement(
        "SELECT 1 FROM pg_index i "
            + "JOIN pg_class c ON c.oid = i.indexrelid "
            + "JOIN pg_namespace n ON n.oid = c.relnamespace "
            + "WHERE c.relname = ? "
            + "AND n.nspname = ? "
            + "AND NOT i.indisvalid"))
    {
      ps.setString(1, indexName);
      ps.setString(2, schema);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          log.info("Dropping invalid {} in schema: {}", indexName, schema);
          try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP INDEX CONCURRENTLY " + schema + "." + indexName);
          }
        }
      }
    }
  }

  /**
   * Resolves the real base table for {@code policy_violation}. During the compat-view window (release N),
   * {@code policy_violation} is a view over {@code policy_violation_t} and cannot be indexed; the index must
   * target {@code policy_violation_t}. On fresh installs and after the view is collapsed (release N+1),
   * {@code policy_violation_t} does not exist and the real table is {@code policy_violation}.
   *
   * @param tableName the unqualified base table name (trusted, hardcoded literal)
   */
  protected String resolveBaseTable(
      final Connection conn,
      final String schema,
      final String tableName) throws SQLException
  {
    String temporaryTable = tableName + "_t";
    try (PreparedStatement ps = conn.prepareStatement(
        "SELECT 1 FROM pg_class c "
            + "JOIN pg_namespace n ON n.oid = c.relnamespace "
            + "WHERE c.relname = ? AND n.nspname = ? AND c.relkind = 'r'"))
    {
      ps.setString(1, temporaryTable);
      ps.setString(2, schema);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? temporaryTable : tableName;
      }
    }
  }

  @Override
  public int compareTo(final AbstractAsyncDbMigration o) {
    return COMPARATOR.compare(this, o);
  }
}
