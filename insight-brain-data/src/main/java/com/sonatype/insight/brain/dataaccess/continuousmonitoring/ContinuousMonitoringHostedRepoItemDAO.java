/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.sql.Savepoint;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.InsertSetMoreStep;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.exception.DataAccessException;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ContinuousMonitoringHostedRepoItem.CONTINUOUS_MONITORING_HOSTED_REPO_ITEM;
import static org.jooq.exception.SQLStateClass.C23_INTEGRITY_CONSTRAINT_VIOLATION;

/**
 * jOOQ-on-relational DAO for the Hosted-Repo flow's satellite table (CLM-40039 §3.13). Owns
 * dialect dispatch for ignore-duplicate-key insert ({@code ON CONFLICT DO NOTHING} on Postgres,
 * savepoint-based recovery on H2) and the consumer-side satellite read.
 * <p>
 * Producers compose this DAO with {@link ContinuousMonitoringQueueItemDAO} inside a single
 * transaction: insert parents, insert satellites, then call
 * {@link ContinuousMonitoringQueueItemDAO#deleteOrphanParentsForSatelliteTable} so any parent
 * whose satellite was deduped on the natural-key constraint
 * {@code (repository_id, component_hash)} is cleaned up before the transaction commits.
 */
@Named
@Singleton
public class ContinuousMonitoringHostedRepoItemDAO
    extends AbstractOperationalSqlDAO<ContinuousMonitoringHostedRepoItem>
{
  @Inject
  public ContinuousMonitoringHostedRepoItemDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Inserts hosted-repo satellite rows with dialect-aware ignore-duplicate-key semantics.
   * Mirrors the proven {@link com.sonatype.insight.dataaccess.AbstractDAO#insertBatch} dialect
   * split: Postgres uses jOOQ's {@code onDuplicateKeyIgnore()} (translated to
   * {@code ON CONFLICT DO NOTHING}); H2 uses per-row inserts with savepoint rollback on
   * integrity-constraint violations, because H2 does not support {@code onDuplicateKeyIgnore}
   * reliably.
   * <p>
   * Note: satellite {@code queue_id} is the FK to the parent's id and must equal the parent's
   * generated UUID, so we cannot use {@code AbstractSqlDAO.insertBatch} (which would
   * auto-assign a fresh UUID via {@code generateIdIfNeeded}).
   */
  public void insertIgnoreDuplicateKey(
      final TransactionContext tx,
      final List<ContinuousMonitoringHostedRepoItem> satellites)
  {
    if (satellites == null || satellites.isEmpty()) {
      return;
    }
    SQLDialect dialect = tx.dsl().dialect();
    if (dialect == SQLDialect.H2) {
      for (ContinuousMonitoringHostedRepoItem sat : satellites) {
        InsertSetMoreStep<?> step = buildInsert(tx, sat);
        // Savepoint covers step.execute() because tx.dsl() and this conn share the same JDBC
        // connection — keep that invariant if buildInsert is ever refactored. The caller's
        // TransactionContext must have an active transaction (autoCommit=false), otherwise
        // conn.setSavepoint() throws SQLException("Savepoint cannot be set when in autocommit
        // mode"); the guard below converts that opaque JDBC failure into a clear API-misuse
        // exception at the boundary.
        tx.dsl().connection(conn -> {
          if (conn.getAutoCommit()) {
            throw new IllegalArgumentException(
                "insertIgnoreDuplicateKey requires the caller's tx to be active (autoCommit=false)");
          }
          Savepoint savepoint = conn.setSavepoint();
          try {
            step.execute();
            conn.releaseSavepoint(savepoint);
          }
          catch (DataAccessException e) {
            conn.rollback(savepoint);
            String sqlState = e.sqlState();
            if (sqlState == null || !sqlState.startsWith(C23_INTEGRITY_CONSTRAINT_VIOLATION.className())) {
              throw e;
            }
          }
        });
      }
    }
    else {
      var steps = satellites.stream()
          .map(sat -> buildInsert(tx, sat).onDuplicateKeyIgnore())
          .toList();
      tx.dsl().batch(steps).execute();
    }
  }

  /**
   * Reads satellite rows for the given queue ids. Used by the Hosted Repo consumer to enrich
   * acquired parent rows with per-flow identity columns.
   */
  public List<ContinuousMonitoringHostedRepoItem> getByQueueIds(
      final TransactionContext tx,
      final List<String> queueIds)
  {
    return getListWithSqlInClause(queueIds, chunk -> tx.dsl()
        .selectFrom(CONTINUOUS_MONITORING_HOSTED_REPO_ITEM)
        .where(CONTINUOUS_MONITORING_HOSTED_REPO_ITEM.QUEUE_ID.in(chunk))
        .fetchInto(ContinuousMonitoringHostedRepoItem.class));
  }

  private InsertSetMoreStep<?> buildInsert(
      final TransactionContext tx,
      final ContinuousMonitoringHostedRepoItem sat)
  {
    UpdatableRecord<?> record = (UpdatableRecord<?>) tx.dsl().newRecord(CONTINUOUS_MONITORING_HOSTED_REPO_ITEM, sat);
    return tx.dsl().insertInto(CONTINUOUS_MONITORING_HOSTED_REPO_ITEM).set(record);
  }

  @Override
  public Table<?> getJooqTable() {
    return CONTINUOUS_MONITORING_HOSTED_REPO_ITEM;
  }

  @Override
  public Class<ContinuousMonitoringHostedRepoItem> getEntityClass() {
    return ContinuousMonitoringHostedRepoItem.class;
  }
}
