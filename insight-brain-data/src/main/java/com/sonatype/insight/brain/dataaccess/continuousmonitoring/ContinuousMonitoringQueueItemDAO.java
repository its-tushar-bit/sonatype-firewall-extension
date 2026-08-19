/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ContinuousMonitoringQueue.CONTINUOUS_MONITORING_QUEUE;

/**
 * jOOQ-on-relational DAO for the unified continuous monitoring queue parent table (CLM-40039).
 * Owns dialect dispatch for consumer-side acquire ({@code FOR UPDATE SKIP LOCKED} on Postgres,
 * table-level {@code FOR UPDATE} on H2). Consumers above this layer are dialect-agnostic.
 * <p>
 * Per-flow satellite tables (e.g. {@code continuous_monitoring_hosted_repo_item}) have their own
 * DAOs (e.g. {@link ContinuousMonitoringHostedRepoItemDAO}). Producers compose this DAO with the
 * relevant satellite DAO inside a single transaction; this class deliberately knows nothing about
 * any specific satellite.
 */
@Named
@Singleton
public class ContinuousMonitoringQueueItemDAO
    extends AbstractOperationalSqlDAO<ContinuousMonitoringQueueItem>
{
  private static final Logger log = LoggerFactory.getLogger(ContinuousMonitoringQueueItemDAO.class);

  @Inject
  public ContinuousMonitoringQueueItemDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  /**
   * Deletes parent rows in {@code parentIds} that have no matching row in the satellite table
   * referenced by {@code satelliteFkField}. Orphan parents arise when a satellite insert is
   * deduped on its natural-key constraint, leaving the parent without a satellite. Generic
   * over the satellite FK so producers for any flow can call it after their own satellite
   * insert; the satellite table is derived from {@code satelliteFkField.getTable()}.
   * <p>
   * Pre-selects orphans via a single {@code LEFT JOIN ... WHERE satellite.fk IS NULL} per CLAUDE.md
   * §15 (CLM-40971), then issues an {@code IN (...)}-bounded DELETE. The earlier correlated
   * {@code NOT EXISTS} re-executed the subquery once per row in the chunk; the rewrite is one
   * O(parentIds) join + one O(orphans) delete.
   */
  public void deleteOrphanParentsForSatelliteTable(
      final TransactionContext tx,
      final List<String> parentIds,
      final TableField<? extends Record, String> satelliteFkField)
  {
    if (CollectionUtils.isEmpty(parentIds)) {
      return;
    }
    Objects.requireNonNull(satelliteFkField, "satelliteFkField must not be null");

    int deleted = getListWithSqlInClause(parentIds, chunk -> {
      List<String> orphans = tx.dsl()
          .select(CONTINUOUS_MONITORING_QUEUE.ID)
          .from(CONTINUOUS_MONITORING_QUEUE)
          .leftJoin(satelliteFkField.getTable())
          .on(satelliteFkField.eq(CONTINUOUS_MONITORING_QUEUE.ID))
          .where(CONTINUOUS_MONITORING_QUEUE.ID.in(chunk))
          .and(satelliteFkField.isNull())
          .fetch(CONTINUOUS_MONITORING_QUEUE.ID);
      if (orphans.isEmpty()) {
        return List.of(0);
      }
      return List.of(tx.dsl()
          .deleteFrom(CONTINUOUS_MONITORING_QUEUE)
          .where(CONTINUOUS_MONITORING_QUEUE.ID.in(orphans))
          .execute());
    })
        .stream()
        .mapToInt(Integer::intValue)
        .sum();

    if (deleted > 0) {
      log.debug("Deleted {} orphan queue rows (satellite deduped on natural key)", deleted);
    }
  }

  /**
   * Atomically selects up to {@code limit} PENDING rows for the given flow and transitions them
   * to IN_PROGRESS, recording the worker that acquired them. Returns the acquired rows so the
   * consumer can process them; consumers must call {@link #deleteById} on success or
   * {@link #markRetry} on a transient failure.
   */
  public List<ContinuousMonitoringQueueItem> acquirePending(
      final ContinuousMonitoringFlowType flowType,
      final String workerId,
      final int limit)
  {
    Objects.requireNonNull(flowType, "flowType must not be null");
    Objects.requireNonNull(workerId, "workerId must not be null");
    if (limit <= 0) {
      return List.of();
    }
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      List<ContinuousMonitoringQueueItem> acquired = isDatabaseEmbedded()
          ? acquirePendingH2(tx, flowType, workerId, limit)
          : acquirePendingPostgres(tx, flowType, workerId, limit);
      tx.commit();
      return acquired;
    }
  }

  private List<ContinuousMonitoringQueueItem> acquirePendingH2(
      final TransactionContext tx,
      final ContinuousMonitoringFlowType flowType,
      final String workerId,
      final int limit)
  {
    // H2 1.4.x lacks row-level SKIP LOCKED; its forUpdate() takes a table-level lock that
    // serializes acquires on this queue. Acceptable for the single-instance H2 deployment
    // target (dev/test/light-prod). Postgres uses FOR UPDATE SKIP LOCKED in
    // acquirePendingPostgres() for row-level concurrency. Mirrors the SELECT+UPDATE+mutate
    // pattern used by EvaluationQueueDAO.acquireRowsH2.
    List<ContinuousMonitoringQueueItem> results = tx.dsl()
        .selectFrom(CONTINUOUS_MONITORING_QUEUE)
        .where(CONTINUOUS_MONITORING_QUEUE.FLOW_TYPE.eq(flowType.name())
            .and(CONTINUOUS_MONITORING_QUEUE.STATUS.eq(ContinuousMonitoringQueueItem.STATUS_PENDING)))
        // ID as secondary sort: every row in a single producer page shares one cycleStart
        // create_time, so without a stable tiebreaker the DB would pick an arbitrary order for the
        // page. ID is assigned per row at insert and is unique, giving deterministic ordering.
        .orderBy(CONTINUOUS_MONITORING_QUEUE.CREATE_TIME.asc(), CONTINUOUS_MONITORING_QUEUE.ID.asc())
        .limit(limit)
        .forUpdate()
        .fetchInto(ContinuousMonitoringQueueItem.class);

    if (results.isEmpty()) {
      return results;
    }

    Date now = new Date();
    List<String> ids = results.stream().map(ContinuousMonitoringQueueItem::getId).toList();
    // Chunk the UPDATE — protects against an oversized `limit` argument more gracefully than
    // failing on an oversized IN clause, and matches the pattern in
    // EvaluationQueueDAO.acquireRowsH2.
    getListWithSqlInClause(ids, chunk -> List.of(tx.dsl()
        .update(CONTINUOUS_MONITORING_QUEUE)
        .set(CONTINUOUS_MONITORING_QUEUE.STATUS, ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS)
        .set(CONTINUOUS_MONITORING_QUEUE.WORKER_ID, workerId)
        .set(CONTINUOUS_MONITORING_QUEUE.ACQUIRED_AT, now)
        .set(CONTINUOUS_MONITORING_QUEUE.UPDATE_TIME, now)
        .where(CONTINUOUS_MONITORING_QUEUE.ID.in(chunk))
        .execute()));

    // Mutate already-fetched entities in place (no second round-trip) — inside one transaction
    // the in-memory state IS what's about to be committed. Matches EvaluationQueueDAO.acquireRowsH2.
    results.forEach(item -> {
      item.setStatus(ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS);
      item.setWorkerId(workerId);
      item.setAcquiredAt(now);
      item.setUpdateTime(now);
    });
    return results;
  }

  private List<ContinuousMonitoringQueueItem> acquirePendingPostgres(
      final TransactionContext tx,
      final ContinuousMonitoringFlowType flowType,
      final String workerId,
      final int limit)
  {
    // Canonical Postgres queue pattern (mirrors EvaluationQueueDAO.acquireRowsPostgres and
    // HostedComponentScanQueueDAO): single UPDATE whose WHERE clause references an inline
    // FOR UPDATE SKIP LOCKED subquery. The row locks taken during the inner SELECT (not any
    // CTE materialization) are what guarantee disjoint acquisition between concurrent workers.
    // The outer `updated` CTE exists only because UPDATE … RETURNING does not honor ORDER BY,
    // so we wrap it to re-emit the rows in strict FIFO order.
    Date now = new Date();

    var candidateSelect = tx.dsl()
        .select(CONTINUOUS_MONITORING_QUEUE.ID)
        .from(CONTINUOUS_MONITORING_QUEUE)
        .where(CONTINUOUS_MONITORING_QUEUE.FLOW_TYPE.eq(flowType.name())
            .and(CONTINUOUS_MONITORING_QUEUE.STATUS.eq(ContinuousMonitoringQueueItem.STATUS_PENDING)))
        // ID as secondary sort: every row in a single producer page shares one cycleStart
        // create_time, so without a stable tiebreaker the DB would pick an arbitrary order for
        // the page (CTID on Postgres). ID is assigned per row at insert and is unique.
        .orderBy(CONTINUOUS_MONITORING_QUEUE.CREATE_TIME.asc(), CONTINUOUS_MONITORING_QUEUE.ID.asc())
        .limit(limit)
        .forUpdate()
        .skipLocked();

    var updated = DSL.name("updated")
        .as(tx.dsl()
            .update(CONTINUOUS_MONITORING_QUEUE)
            .set(CONTINUOUS_MONITORING_QUEUE.STATUS, ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS)
            .set(CONTINUOUS_MONITORING_QUEUE.WORKER_ID, workerId)
            .set(CONTINUOUS_MONITORING_QUEUE.ACQUIRED_AT, now)
            .set(CONTINUOUS_MONITORING_QUEUE.UPDATE_TIME, now)
            .where(CONTINUOUS_MONITORING_QUEUE.ID.in(candidateSelect))
            .returning());

    return tx.dsl()
        .with(updated)
        .selectFrom(updated)
        .orderBy(
            updated.field(CONTINUOUS_MONITORING_QUEUE.CREATE_TIME).asc(),
            updated.field(CONTINUOUS_MONITORING_QUEUE.ID).asc())
        .fetchInto(ContinuousMonitoringQueueItem.class);
  }

  /**
   * Deletes the parent queue row for {@code id} only if it is currently IN_PROGRESS under the
   * specified {@code workerId} (CLM-40971 M7). Per-flow satellite rows are removed by
   * {@code ON DELETE CASCADE}.
   * <p>
   * The (workerId, status=IN_PROGRESS) guard prevents a stale consumer from disposing of a job
   * that {@code recoverStaleJobs} has already reset to PENDING and another worker has since
   * claimed — a rolling-restart double-execution risk. Callers must surface a 0-row result as a
   * non-fatal "ownership lost" warning rather than silent success.
   *
   * @return number of rows deleted (0 if ownership has been transferred elsewhere, 1 normally)
   */
  public int deleteById(final String id, final String workerId) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(workerId, "workerId must not be null");
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int count = tx.dsl()
          .deleteFrom(CONTINUOUS_MONITORING_QUEUE)
          .where(CONTINUOUS_MONITORING_QUEUE.ID.eq(id))
          .and(CONTINUOUS_MONITORING_QUEUE.WORKER_ID.eq(workerId))
          .and(CONTINUOUS_MONITORING_QUEUE.STATUS.eq(ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS))
          .execute();
      tx.commit();
      return count;
    }
  }

  /**
   * Bulk delete used by tests/cleanup paths. Per-flow satellite rows cascade via FK.
   * <p>
   * Self-managed transaction overload — opens its own {@link TransactionContext}. Callers that
   * need to share a transaction with other operations should use
   * {@link #deleteByIds(TransactionContext, List)}.
   */
  public int deleteByIds(final List<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return 0;
    }
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int total = deleteByIds(tx, ids);
      tx.commit();
      return total;
    }
  }

  /**
   * Bulk delete that participates in the caller's transaction so callers can compose multi-step
   * operations atomically.
   */
  public int deleteByIds(final TransactionContext tx, final List<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return 0;
    }
    return getListWithSqlInClause(ids, chunk -> List.of(tx.dsl()
        .deleteFrom(CONTINUOUS_MONITORING_QUEUE)
        .where(CONTINUOUS_MONITORING_QUEUE.ID.in(chunk))
        .execute()))
            .stream()
            .mapToInt(Integer::intValue)
            .sum();
  }

  /**
   * Atomically transitions an IN_PROGRESS row back to PENDING (clearing {@code worker_id} and
   * {@code acquired_at}) <strong>without</strong> incrementing {@code retry_count} (CLM-40971
   * C1). Use this for worker-shutdown signals (InterruptedException) where the job did not
   * actually fail — bumping retry_count for an interrupt would let 3 rolling restarts permanently
   * delete a healthy job. Returns the number of rows updated (0 if the row was deleted between
   * acquire and unacquire, 1 normally).
   * <p>
   * <strong>Why no {@code workerId} guard:</strong> unlike {@link #deleteById(String, String)},
   * this method intentionally does not enforce ownership. The destructive operation
   * ({@code deleteById}) needs a guard because a stale worker disposing of a row reclaimed by
   * another worker after {@code recoverStaleJobs} would silently lose the new worker's processing
   * result. A stale {@code unacquire} call is non-destructive: the row simply transitions to
   * PENDING and is reacquired (correctly, via {@code FOR UPDATE SKIP LOCKED}) on the next poll.
   * The pathological race — Worker-A interrupted → row recovered → Worker-B acquires → Worker-A's
   * late unacquire wipes Worker-B's worker_id — is benign because Worker-B's eventual
   * {@code deleteById(id, workerB)} returns 0 rows under the M7 ownership guard, the
   * ownership-lost log fires, and the row is reprocessed by another worker. No data loss, no
   * double-execution.
   */
  public int unacquire(final String id) {
    Objects.requireNonNull(id, "id must not be null");
    Date now = new Date();
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int updated = tx.dsl()
          .update(CONTINUOUS_MONITORING_QUEUE)
          .set(CONTINUOUS_MONITORING_QUEUE.STATUS, ContinuousMonitoringQueueItem.STATUS_PENDING)
          .set(CONTINUOUS_MONITORING_QUEUE.WORKER_ID, (String) null)
          .set(CONTINUOUS_MONITORING_QUEUE.ACQUIRED_AT, (Date) null)
          .set(CONTINUOUS_MONITORING_QUEUE.UPDATE_TIME, now)
          .where(CONTINUOUS_MONITORING_QUEUE.ID.eq(id))
          .execute();
      tx.commit();
      return updated;
    }
  }

  /**
   * Atomically increments {@code retry_count}, transitions back to PENDING (clearing
   * {@code worker_id} and {@code acquired_at}), and stores a truncated error message.
   * Returns the new retry count, or 0 if the row no longer exists (logged at WARN — typically
   * indicates a concurrent delete or a producer race that the caller should be aware of).
   */
  public int markRetry(final String id, final String errorMessage) {
    Objects.requireNonNull(id, "id must not be null");
    Date now = new Date();
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      // Postgres path uses UPDATE ... RETURNING so the post-increment retry_count comes back in
      // a single round-trip (CLM-40971 C4). H2 1.4.x's jOOQ binding doesn't support RETURNING on
      // UPDATE, so the embedded path keeps the 2-statement read-after-write pattern.
      Integer newCount = isDatabaseEmbedded()
          ? markRetryEmbedded(tx, id, now, errorMessage)
          : markRetryNative(tx, id, now, errorMessage);
      tx.commit();
      return newCount != null ? newCount : 0;
    }
  }

  private Integer markRetryNative(
      final TransactionContext tx,
      final String id,
      final Date now,
      final String errorMessage)
  {
    Record1<Integer> result = tx.dsl()
        .update(CONTINUOUS_MONITORING_QUEUE)
        .set(CONTINUOUS_MONITORING_QUEUE.STATUS, ContinuousMonitoringQueueItem.STATUS_PENDING)
        .set(CONTINUOUS_MONITORING_QUEUE.WORKER_ID, (String) null)
        .set(CONTINUOUS_MONITORING_QUEUE.ACQUIRED_AT, (Date) null)
        .set(CONTINUOUS_MONITORING_QUEUE.RETRY_COUNT, CONTINUOUS_MONITORING_QUEUE.RETRY_COUNT.add(1))
        .set(CONTINUOUS_MONITORING_QUEUE.ERROR_MESSAGE, truncate(id, errorMessage))
        .set(CONTINUOUS_MONITORING_QUEUE.UPDATE_TIME, now)
        .where(CONTINUOUS_MONITORING_QUEUE.ID.eq(id))
        .returningResult(CONTINUOUS_MONITORING_QUEUE.RETRY_COUNT)
        .fetchOne();
    if (result == null) {
      // Row no longer exists — typically a concurrent delete race. Log at WARN with tenant
      // context (CLM-40971) so MTIQ deployments with 1000+ tenants can triage which tenant is
      // actually exhibiting the race.
      log.warn("markRetry called for queue id {} (tenant={}) but no row was updated; concurrent delete?",
          id, TenantThreadLocal.getTenant().tenantSlug);
      return 0;
    }
    return result.value1();
  }

  private Integer markRetryEmbedded(
      final TransactionContext tx,
      final String id,
      final Date now,
      final String errorMessage)
  {
    int updated = tx.dsl()
        .update(CONTINUOUS_MONITORING_QUEUE)
        .set(CONTINUOUS_MONITORING_QUEUE.STATUS, ContinuousMonitoringQueueItem.STATUS_PENDING)
        .set(CONTINUOUS_MONITORING_QUEUE.WORKER_ID, (String) null)
        .set(CONTINUOUS_MONITORING_QUEUE.ACQUIRED_AT, (Date) null)
        .set(CONTINUOUS_MONITORING_QUEUE.RETRY_COUNT, CONTINUOUS_MONITORING_QUEUE.RETRY_COUNT.add(1))
        .set(CONTINUOUS_MONITORING_QUEUE.ERROR_MESSAGE, truncate(id, errorMessage))
        .set(CONTINUOUS_MONITORING_QUEUE.UPDATE_TIME, now)
        .where(CONTINUOUS_MONITORING_QUEUE.ID.eq(id))
        .execute();
    if (updated == 0) {
      log.warn("markRetry called for queue id {} (tenant={}) but no row was updated; concurrent delete?",
          id, TenantThreadLocal.getTenant().tenantSlug);
      return 0;
    }
    return tx.dsl()
        .select(CONTINUOUS_MONITORING_QUEUE.RETRY_COUNT)
        .from(CONTINUOUS_MONITORING_QUEUE)
        .where(CONTINUOUS_MONITORING_QUEUE.ID.eq(id))
        .fetchOneInto(Integer.class);
  }

  /**
   * Resets IN_PROGRESS rows owned by {@code workerId} back to PENDING (clearing the worker
   * assignment). Called on application start so rows stranded by a crashed worker are picked
   * up again on the next acquire cycle.
   * <p>
   * <strong>Worker-scoped on purpose:</strong> a global reset would steal in-flight rows from
   * healthy peer nodes during a rolling restart. Each consumer starts with a fresh
   * {@code workerId} (UUID) so this only resets what the previous incarnation of THIS node
   * left behind.
   */
  public int resetInProgressToPending(final String workerId) {
    Objects.requireNonNull(workerId, "workerId must not be null");
    Date now = new Date();
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int count = tx.dsl()
          .update(CONTINUOUS_MONITORING_QUEUE)
          .set(CONTINUOUS_MONITORING_QUEUE.STATUS, ContinuousMonitoringQueueItem.STATUS_PENDING)
          .set(CONTINUOUS_MONITORING_QUEUE.WORKER_ID, (String) null)
          .set(CONTINUOUS_MONITORING_QUEUE.ACQUIRED_AT, (Date) null)
          .set(CONTINUOUS_MONITORING_QUEUE.UPDATE_TIME, now)
          .where(CONTINUOUS_MONITORING_QUEUE.STATUS.eq(ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS)
              .and(CONTINUOUS_MONITORING_QUEUE.WORKER_ID.eq(workerId)))
          .execute();
      tx.commit();
      return count;
    }
  }

  public long countPending(final ContinuousMonitoringFlowType flowType) {
    Objects.requireNonNull(flowType, "flowType must not be null");
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      Integer count = tx.dsl()
          .selectCount()
          .from(CONTINUOUS_MONITORING_QUEUE)
          .where(CONTINUOUS_MONITORING_QUEUE.FLOW_TYPE.eq(flowType.name())
              .and(CONTINUOUS_MONITORING_QUEUE.STATUS.eq(ContinuousMonitoringQueueItem.STATUS_PENDING)))
          .fetchOneInto(Integer.class);
      tx.commit();
      return count != null ? count.longValue() : 0L;
    }
  }

  private static String truncate(final String id, final String message) {
    if (message == null) {
      return null;
    }
    if (message.length() > ContinuousMonitoringQueueItem.ERROR_MESSAGE_MAX_LENGTH) {
      // Log the full message before truncating so the original survives in logs even though the
      // database column is bounded at ERROR_MESSAGE_MAX_LENGTH.
      log.warn("Error message for continuous monitoring queue item {} exceeds {} chars and will be truncated"
          + " in the database; full message follows: {}",
          id, ContinuousMonitoringQueueItem.ERROR_MESSAGE_MAX_LENGTH, message);
      return message.substring(0, ContinuousMonitoringQueueItem.ERROR_MESSAGE_MAX_LENGTH);
    }
    return message;
  }

  @Override
  public Table<?> getJooqTable() {
    return CONTINUOUS_MONITORING_QUEUE;
  }

  @Override
  public Class<ContinuousMonitoringQueueItem> getEntityClass() {
    return ContinuousMonitoringQueueItem.class;
  }
}
