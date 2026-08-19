/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.repository.HostedComponentScanQueue;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.HostedComponentScanQueue.HOSTED_COMPONENT_SCAN_QUEUE;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ProxyRepositoryComponent.PROXY_REPOSITORY_COMPONENT;
import static org.jooq.impl.DSL.count;

@Named
@Singleton
public class HostedComponentScanQueueDAO
    extends AbstractOperationalSqlDAO<HostedComponentScanQueue>
{
  public enum Status
  {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
  }

  // Bounds lock duration per DELETE, not the IN-clause parameter limit (which is far higher — see
  // AbstractSqlDAO.getInOperatorThreshold). 1000 keeps each batch well under that limit.
  private static final int DELETE_BATCH_SIZE = 1000;

  @Inject
  public HostedComponentScanQueueDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<HostedComponentScanQueue> getByComponentId(
      final TransactionContext tx,
      final String componentId)
  {
    return tx.dsl()
        .selectFrom(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.COMPONENT_ID.eq(componentId))
        .fetch(super::toEntity);
  }

  public List<HostedComponentScanQueue> getByStatus(
      final TransactionContext tx,
      final Status status)
  {
    return tx.dsl()
        .selectFrom(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(status.name()))
        .fetch(super::toEntity);
  }

  /**
   * Count queue entries grouped by status for all components in a repository.
   * Runs a single SQL aggregate query — safe for large repositories.
   *
   * @param tx transaction context
   * @param repositoryId the repository ID
   * @return map of status name to count
   */
  public Map<String, Integer> countByRepositoryIdGroupedByStatus(
      final TransactionContext tx,
      final String repositoryId)
  {
    Result<Record2<String, Integer>> result = tx.dsl()
        .select(HOSTED_COMPONENT_SCAN_QUEUE.STATUS, count())
        .from(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID.eq(repositoryId))
        .groupBy(HOSTED_COMPONENT_SCAN_QUEUE.STATUS)
        .fetch();

    Map<String, Integer> counts = new HashMap<>();
    for (Record2<String, Integer> row : result) {
      counts.put(row.value1(), row.value2());
    }
    return counts;
  }

  /**
   * Atomically selects and acquires up to {@code limit} PENDING jobs in a single transaction
   * using {@code FOR UPDATE SKIP LOCKED} to avoid contention with concurrent workers.
   *
   * @param tx transaction context (caller must commit)
   * @param limit maximum number of jobs to acquire
   * @param acquiredAt the acquisition timestamp
   * @return list of acquired jobs (status already set to IN_PROGRESS)
   */
  public List<HostedComponentScanQueue> acquireNextPendingJobs(
      final TransactionContext tx,
      final int limit,
      final Instant acquiredAt)
  {
    Objects.requireNonNull(acquiredAt, "acquiredAt must not be null");
    if (limit <= 0) {
      return List.of();
    }

    if (isDatabaseEmbedded()) {
      return acquireNextPendingJobsH2(tx, limit, acquiredAt);
    }
    return acquireNextPendingJobsPostgres(tx, limit, acquiredAt);
  }

  // H2 does not support SKIP LOCKED; use plain FOR UPDATE (acceptable for single-instance dev/test)
  private List<HostedComponentScanQueue> acquireNextPendingJobsH2(
      final TransactionContext tx,
      final int limit,
      final Instant acquiredAt)
  {
    List<HostedComponentScanQueue> candidates = tx.dsl()
        .selectFrom(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.PENDING.name()))
        .orderBy(HOSTED_COMPONENT_SCAN_QUEUE.PRIORITY.asc(), HOSTED_COMPONENT_SCAN_QUEUE.ID.asc())
        .limit(limit)
        .forUpdate()
        .fetch(super::toEntity);

    if (candidates.isEmpty()) {
      return candidates;
    }

    List<String> ids = candidates.stream().map(HostedComponentScanQueue::getId).toList();
    tx.dsl()
        .update(HOSTED_COMPONENT_SCAN_QUEUE)
        .set(HOSTED_COMPONENT_SCAN_QUEUE.STATUS, Status.IN_PROGRESS.name())
        .set(HOSTED_COMPONENT_SCAN_QUEUE.ACQUIRED_AT, Date.from(acquiredAt))
        .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.in(ids))
        .execute();

    candidates.forEach(job -> {
      job.setStatus(Status.IN_PROGRESS.name());
      job.setAcquiredAt(Date.from(acquiredAt));
    });
    return candidates;
  }

  private List<HostedComponentScanQueue> acquireNextPendingJobsPostgres(
      final TransactionContext tx,
      final int limit,
      final Instant acquiredAt)
  {
    // SELECT the candidate IDs using FOR UPDATE SKIP LOCKED to avoid contention
    var candidateIds = tx.dsl()
        .select(HOSTED_COMPONENT_SCAN_QUEUE.ID)
        .from(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.PENDING.name()))
        .orderBy(HOSTED_COMPONENT_SCAN_QUEUE.PRIORITY.asc(), HOSTED_COMPONENT_SCAN_QUEUE.ID.asc())
        .limit(limit)
        .forUpdate()
        .skipLocked();

    // UPDATE...RETURNING returns exactly the rows we updated — no secondary SELECT by timestamp
    var updated = DSL.name("updated")
        .as(tx.dsl()
            .update(HOSTED_COMPONENT_SCAN_QUEUE)
            .set(HOSTED_COMPONENT_SCAN_QUEUE.STATUS, Status.IN_PROGRESS.name())
            .set(HOSTED_COMPONENT_SCAN_QUEUE.ACQUIRED_AT, Date.from(acquiredAt))
            .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.in(candidateIds))
            .returning());

    return tx.dsl()
        .with(updated)
        .selectFrom(updated)
        .orderBy(updated.field(HOSTED_COMPONENT_SCAN_QUEUE.PRIORITY).asc(),
            updated.field(HOSTED_COMPONENT_SCAN_QUEUE.ID).asc())
        .fetchInto(HostedComponentScanQueue.class);
  }

  public int completeJob(final TransactionContext tx, final String jobId) {
    return tx.dsl()
        .update(HOSTED_COMPONENT_SCAN_QUEUE)
        .set(HOSTED_COMPONENT_SCAN_QUEUE.STATUS, Status.COMPLETED.name())
        .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.eq(jobId)
            .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.IN_PROGRESS.name())))
        .execute();
  }

  /**
   * Fail a job with error message (update status to FAILED).
   *
   * @param tx transaction context
   * @param jobId the job ID
   * @param errorMessage the error message
   * @return number of rows updated
   */
  public int failJob(
      final TransactionContext tx,
      final String jobId,
      final String errorMessage)
  {
    String truncatedMessage = errorMessage != null && errorMessage.length() > 2000
        ? errorMessage.substring(0, 2000)
        : errorMessage;
    return tx.dsl()
        .update(HOSTED_COMPONENT_SCAN_QUEUE)
        .set(HOSTED_COMPONENT_SCAN_QUEUE.STATUS, Status.FAILED.name())
        .set(HOSTED_COMPONENT_SCAN_QUEUE.ERROR_MESSAGE, truncatedMessage)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.eq(jobId)
            .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.IN_PROGRESS.name())))
        .execute();
  }

  /**
   * Returns true if any of the given component IDs have an IN_PROGRESS queue entry.
   *
   * @param tx transaction context
   * @param componentIds the component IDs to check
   * @return true if at least one IN_PROGRESS entry exists for any of the given components
   */
  public boolean hasInProgressByComponentIds(
      final TransactionContext tx,
      final List<String> componentIds)
  {
    if (componentIds == null || componentIds.isEmpty()) {
      return false;
    }
    return tx.dsl()
        .fetchExists(
            tx.dsl()
                .selectOne()
                .from(HOSTED_COMPONENT_SCAN_QUEUE)
                .where(HOSTED_COMPONENT_SCAN_QUEUE.COMPONENT_ID.in(componentIds)
                    .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.IN_PROGRESS.name()))));
  }

  /**
   * Delete PENDING queue entries for the given list of components in a single query.
   *
   * @param tx transaction context
   * @param componentIds the component IDs to delete pending entries for
   * @return number of rows deleted
   */
  public int deletePendingByComponentIds(final TransactionContext tx, final List<String> componentIds) {
    if (componentIds == null || componentIds.isEmpty()) {
      return 0;
    }
    return tx.dsl()
        .deleteFrom(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.COMPONENT_ID.in(componentIds)
            .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.PENDING.name())))
        .execute();
  }

  /**
   * Deletes a repository's PENDING backlog, keyed on the queue's own {@code repository_id} rather than joined
   * through {@code proxy_repository_component} so not-yet-evaluated entries (no component row) are also removed
   * (CLM-42122). Batched to bound lock duration; IN_PROGRESS is left to complete.
   */
  public int deletePendingByRepositoryId(final String repositoryId) {
    int deleted = 0;
    while (true) {
      final List<String> ids;
      try (TransactionContext tx = createTransactionContext()) {
        ids = tx.dsl()
            .select(HOSTED_COMPONENT_SCAN_QUEUE.ID)
            .from(HOSTED_COMPONENT_SCAN_QUEUE)
            .where(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID.eq(repositoryId)
                .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.PENDING.name())))
            .limit(DELETE_BATCH_SIZE)
            .fetchInto(String.class);
      }
      if (ids.isEmpty()) {
        return deleted;
      }
      try (TransactionContext tx = createTransactionContext()) {
        tx.begin();
        // Re-check status: a selected row may have been acquired (-> IN_PROGRESS) since the select.
        deleted += tx.dsl()
            .deleteFrom(HOSTED_COMPONENT_SCAN_QUEUE)
            .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.in(ids)
                .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.PENDING.name())))
            .execute();
        tx.commit();
      }
    }
  }

  /**
   * Releases acquired jobs back to PENDING so they can be picked up by another worker.
   * Used during graceful shutdown to avoid stranding in-flight jobs.
   */
  public void unacquireJobs(final Set<String> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return;
    }
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      for (List<String> partition : Lists.partition(new ArrayList<>(ids), getInOperatorThreshold())) {
        tx.dsl()
            .update(HOSTED_COMPONENT_SCAN_QUEUE)
            .set(HOSTED_COMPONENT_SCAN_QUEUE.STATUS, Status.PENDING.name())
            .set(HOSTED_COMPONENT_SCAN_QUEUE.ACQUIRED_AT, (Date) null)
            .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.in(partition)
                .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.IN_PROGRESS.name())))
            .execute();
      }
      tx.commit();
    }
  }

  public int deleteByRepositoryComponentIds(final TransactionContext tx, final String repositoryId) {
    return tx.dsl()
        .deleteFrom(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.COMPONENT_ID.in(
            tx.dsl()
                .select(PROXY_REPOSITORY_COMPONENT.COMPONENT_ID)
                .from(PROXY_REPOSITORY_COMPONENT)
                .where(PROXY_REPOSITORY_COMPONENT.REPOSITORY_ID.eq(repositoryId)
                    .and(PROXY_REPOSITORY_COMPONENT.COMPONENT_ID.isNotNull()))))
        .execute();
  }

  public int deleteCompletedAndFailedJobs(final TransactionContext tx, final Instant olderThan) {
    Objects.requireNonNull(olderThan, "olderThan must not be null");

    return tx.dsl()
        .deleteFrom(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.in(Status.COMPLETED.name(), Status.FAILED.name())
            .and(HOSTED_COMPONENT_SCAN_QUEUE.ACQUIRED_AT.isNull()
                .or(HOSTED_COMPONENT_SCAN_QUEUE.ACQUIRED_AT.lt(Date.from(olderThan)))))
        .execute();
  }

  public int resetInProgressToPending(final TransactionContext tx) {
    return tx.dsl()
        .update(HOSTED_COMPONENT_SCAN_QUEUE)
        .set(HOSTED_COMPONENT_SCAN_QUEUE.STATUS, Status.PENDING.name())
        .set(HOSTED_COMPONENT_SCAN_QUEUE.ACQUIRED_AT, (Date) null)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.eq(Status.IN_PROGRESS.name()))
        .execute();
  }

  public int resetInProgressToPending() {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int count = resetInProgressToPending(tx);
      tx.commit();
      return count;
    }
  }

  public List<HostedComponentScanQueue> acquireNextPendingJobs(final int limit) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      List<HostedComponentScanQueue> jobs = acquireNextPendingJobs(tx, limit, Instant.now());
      tx.commit();
      return jobs;
    }
  }

  /** Returns repository IDs with at least one PENDING or IN_PROGRESS queue entry. */
  public Set<String> getRepositoryIdsWithQueuedScans(
      final TransactionContext tx,
      final Collection<String> repositoryIds)
  {
    if (CollectionUtils.isEmpty(repositoryIds)) {
      return Collections.emptySet();
    }
    return new HashSet<>(tx.dsl()
        .selectDistinct(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID)
        .from(HOSTED_COMPONENT_SCAN_QUEUE)
        .where(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID.in(repositoryIds)
            .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.in(
                Status.PENDING.name(),
                Status.IN_PROGRESS.name())))
        .fetch(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID));
  }

  /** Single-id existence check via {@code fetchExists}. */
  public boolean hasQueuedScans(final String repositoryId) {
    if (repositoryId == null || repositoryId.isEmpty()) {
      return false;
    }
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .fetchExists(
              tx.dsl()
                  .selectOne()
                  .from(HOSTED_COMPONENT_SCAN_QUEUE)
                  .where(HOSTED_COMPONENT_SCAN_QUEUE.REPOSITORY_ID.eq(repositoryId))
                  .and(HOSTED_COMPONENT_SCAN_QUEUE.STATUS.in(
                      Status.PENDING.name(),
                      Status.IN_PROGRESS.name())));
    }
  }

  public void completeJob(final String jobId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      completeJob(tx, jobId);
      tx.commit();
    }
  }

  public void failJob(final String jobId, final String errorMessage) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      failJob(tx, jobId, errorMessage);
      tx.commit();
    }
  }

  /**
   * Atomically increments the retry_count for a job and returns the new count.
   * Uses a single UPDATE...RETURNING to avoid a separate SELECT round-trip.
   * Falls back to a separate UPDATE + SELECT on H2 which does not support RETURNING.
   */
  public int incrementRetryCount(final String jobId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      int newCount;
      if (isDatabaseEmbedded()) {
        tx.dsl()
            .update(HOSTED_COMPONENT_SCAN_QUEUE)
            .set(HOSTED_COMPONENT_SCAN_QUEUE.RETRY_COUNT,
                HOSTED_COMPONENT_SCAN_QUEUE.RETRY_COUNT.add(1))
            .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.eq(jobId))
            .execute();
        Integer result = tx.dsl()
            .select(HOSTED_COMPONENT_SCAN_QUEUE.RETRY_COUNT)
            .from(HOSTED_COMPONENT_SCAN_QUEUE)
            .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.eq(jobId))
            .fetchOneInto(Integer.class);
        newCount = result != null ? result : 0;
      }
      else {
        var record = tx.dsl()
            .update(HOSTED_COMPONENT_SCAN_QUEUE)
            .set(HOSTED_COMPONENT_SCAN_QUEUE.RETRY_COUNT,
                HOSTED_COMPONENT_SCAN_QUEUE.RETRY_COUNT.add(1))
            .where(HOSTED_COMPONENT_SCAN_QUEUE.ID.eq(jobId))
            .returning(HOSTED_COMPONENT_SCAN_QUEUE.RETRY_COUNT)
            .fetchOne();
        newCount = record != null ? record.get(HOSTED_COMPONENT_SCAN_QUEUE.RETRY_COUNT) : 0;
      }
      tx.commit();
      return newCount;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return HOSTED_COMPONENT_SCAN_QUEUE;
  }

  @Override
  public Class<HostedComponentScanQueue> getEntityClass() {
    return HostedComponentScanQueue.class;
  }
}
