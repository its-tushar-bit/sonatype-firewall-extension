/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringHostedRepoItem;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContinuousMonitoringQueueItemDAOTest
    extends AbstractDbDAOTest
{
  private ContinuousMonitoringQueueItemDAO dao;

  private ContinuousMonitoringHostedRepoItemDAO hostedRepoItemDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createContinuousMonitoringQueueItemDAO();
    hostedRepoItemDAO = daoFactory.createContinuousMonitoringHostedRepoItemDAO();
  }

  @Test
  public void testEnqueue_insertsParentAndSatellite() {
    ContinuousMonitoringQueueItem parent =
        tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L);

    ContinuousMonitoringQueueItem result = dao.getById(parent.getId());
    assertThat(result).isNotNull();
    assertThat(result.getFlowType()).isEqualTo(ContinuousMonitoringFlowType.HOSTED_REPO);
    assertThat(result.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_PENDING);
    assertThat(result.getRetryCount()).isZero();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      List<ContinuousMonitoringHostedRepoItem> sats = hostedRepoItemDAO.getByQueueIds(tx, List.of(parent.getId()));
      tx.commit();
      assertThat(sats).hasSize(1);
      assertThat(sats.get(0).getRepositoryId()).isEqualTo("repo-1");
      assertThat(sats.get(0).getComponentHash()).isEqualTo("hash-A");
    }
  }

  @Test
  public void testAcquirePending_returnsPendingRowsForFlowAndMarksThemInProgress() {
    String id1 = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    String id2 = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 0L).getId();

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactlyInAnyOrder(id1, id2);
    assertThat(acquired).allSatisfy(item -> {
      assertThat(item.getStatus()).isEqualTo("IN_PROGRESS");
      assertThat(item.getWorkerId()).isEqualTo("worker-1");
      assertThat(item.getAcquiredAt()).isNotNull();
    });
    assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isZero();
  }

  @Test
  public void testAcquirePending_respectsLimit() {
    tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L);
    tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 0L);
    tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-C", 0L);

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 2);

    assertThat(acquired).hasSize(2);
    assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(1L);
  }

  @Test
  public void testAcquirePending_doesNotReturnRowsFromOtherFlows() {
    String hostedId =
        tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    tempEntity.newContinuousMonitoringParentOnlyQueueItem(ContinuousMonitoringFlowType.SBOM, 0L);

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(hostedId);
    assertThat(dao.countPending(ContinuousMonitoringFlowType.SBOM)).isEqualTo(1L);
  }

  /**
   * AT-025 — empty queue: acquirePending returns an empty list and is a safe no-op. Exercises
   * the steady-state path between producer cycles where the consumer polls a clean table.
   */
  @Test
  public void testAcquirePending_returnsEmptyWhenNoPendingRowsExist() {
    assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isZero();

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    assertThat(acquired).isEmpty();
    assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isZero();
  }

  @Test
  public void testAcquirePending_ordersByCreateTimeAscFifo() {
    // Strict FIFO: rows are returned in the order they entered the queue (create_time ASC),
    // regardless of any per-row priority value the producer might have written. The priority
    // column is vestigial (NOT NULL for schema stability) and no longer participates in ordering.
    // Pinned create_time values keep the assertion deterministic on low-resolution clocks.
    long base = 1_700_000_000_000L;
    String first = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 50L, new java.util.Date(base))
        .getId();
    String second = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 50L, new java.util.Date(base + 100))
        .getId();
    String third = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-C", 50L, new java.util.Date(base + 200))
        .getId();

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(first, second, third);
  }

  @Test
  public void testAcquirePending_higherPriorityDoesNotJumpAheadOfOlderEntry() {
    // Regression guard against the previous behavior where ORDER BY priority DESC could let a
    // newer high-priority row leapfrog an older low-priority row already waiting. With strict
    // FIFO ordering the older entry must always come first regardless of priority value.
    long base = 1_700_000_000_000L;
    String olderLowPriority = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 1L, new java.util.Date(base))
        .getId();
    String newerHighPriority = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 100L, new java.util.Date(base + 100))
        .getId();

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId)
        .containsExactly(olderLowPriority, newerHighPriority);
  }

  @Test
  public void testAcquirePending_breaksCreateTimeTiesByIdAscDeterministically() {
    // Within one producer cycle every row shares the cycle's start instant as its create_time, so
    // without a stable tiebreaker the DB would pick an arbitrary order for the page (Postgres
    // uses CTID; H2 is unspecified). The acquire ORDER BY falls through to id ASC. Insert several
    // rows with the SAME create_time and assert the returned IDs are sorted ascending.
    java.util.Date tiedTime = new java.util.Date(1_700_000_000_000L);
    for (int i = 0; i < 5; i++) {
      tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-" + i, 0L, tiedTime);
    }

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    assertThat(acquired).hasSize(5);
    List<String> ids = acquired.stream().map(ContinuousMonitoringQueueItem::getId).toList();
    // Lexicographic UUID sort — that is what ORDER BY id ASC on a varchar PK produces. The
    // tiebreaker is intentionally id-order, not insertion-order; within-cycle ordering is
    // documented as deterministic-but-unspecified on RepositoryEvaluationQueueProducerJob.
    assertThat(ids).isSortedAccordingTo(java.util.Comparator.naturalOrder());
  }

  @Test
  public void testAcquirePending_carryOverFromPreviousCycleIsDrainedBeforeNewlyEnqueuedRows() {
    // Multi-cycle scenario from CLM-41691: a previous cycle enqueued rows that are still PENDING
    // when the next cycle fires and adds more. The leftover rows from the older cycle must be
    // drained first — newly arriving rows must never starve them.
    long base = 1_700_000_000_000L;
    String previousCycleA = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-prev-A", 0L, new java.util.Date(base))
        .getId();
    String previousCycleB = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-prev-B", 0L, new java.util.Date(base + 10))
        .getId();
    // Newer cycle (simulated by a later create_time) drops two more rows in front.
    String newCycleC = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-new-C", 0L,
            new java.util.Date(base + 1_000_000L))
        .getId();
    String newCycleD = tempEntity
        .newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-new-D", 0L,
            new java.util.Date(base + 1_000_010L))
        .getId();

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId)
        .containsExactly(previousCycleA, previousCycleB, newCycleC, newCycleD);
  }

  @Test
  public void testAcquirePending_doesNotReturnInProgressRows() {
    tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L);
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    List<ContinuousMonitoringQueueItem> second =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-2", 10);

    assertThat(second).isEmpty();
  }

  @Test
  public void testDeleteById_removesParentAndCascadesSatellite() {
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    // The (workerId, status=IN_PROGRESS) guard added by CLM-40971 M7 means deleteById only
    // succeeds for rows the caller currently owns; acquire the row first to satisfy the guard.
    String workerId = "worker-1";
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, workerId, 10);

    int deleted = dao.deleteById(id, workerId);

    assertThat(deleted).isEqualTo(1);
    assertThat(dao.getById(id)).isNull();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(hostedRepoItemDAO.getByQueueIds(tx, List.of(id))).isEmpty();
      tx.commit();
    }
  }

  @Test
  public void testDeleteById_isNoOpForUnknownId() {
    assertThat(dao.deleteById(UUID.randomUUID().toString(), "any-worker")).isZero();
  }

  /**
   * CLM-40971 M7: deleteById must not silently dispose of a row another worker now owns. After
   * worker-A acquires a row, only worker-A can delete it; a stale worker-B attempting deletion
   * gets 0 rows and a clean signal that ownership has moved.
   */
  @Test
  public void testDeleteById_doesNothingWhenWorkerIdDoesNotMatch() {
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-A", 10);

    int deleted = dao.deleteById(id, "worker-B");

    assertThat(deleted).isZero();
    assertThat(dao.getById(id)).isNotNull();
  }

  /**
   * CLM-40971 M7: deleteById must not delete PENDING rows even when the workerId matches an
   * earlier acquire+release cycle. Only IN_PROGRESS rows owned by the calling worker are valid
   * deletion targets — anything else means the row has been re-queued for another worker.
   */
  @Test
  public void testDeleteById_doesNothingWhenRowNotInProgress() {
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    // Row stays in PENDING (no acquire). Even passing a workerId, the status guard rejects it.
    int deleted = dao.deleteById(id, "worker-A");

    assertThat(deleted).isZero();
    assertThat(dao.getById(id)).isNotNull();
  }

  @Test
  public void testDeleteById_preventsRollingRestartDoubleExecution() {
    // Scenario the M7 ownership guard exists to prevent: worker-A acquires the row, then the
    // node restarts (resetInProgressToPending moves the row back to PENDING with no worker),
    // worker-B acquires the now-PENDING row, and the stale worker-A wakes up and tries to
    // dispose of "its" row. The guard must return 0 rows deleted and leave worker-B's row alone.
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();

    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-A", 10);
    // Simulate a rolling restart of worker-A's node — the row returns to PENDING.
    dao.resetInProgressToPending("worker-A");
    // Worker-B picks it up on the next poll.
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-B", 10);

    // Stale worker-A tries to delete: rejected.
    int deletedByA = dao.deleteById(id, "worker-A");
    assertThat(deletedByA).isZero();
    // Row still owned by worker-B.
    ContinuousMonitoringQueueItem stillOwned = dao.getById(id);
    assertThat(stillOwned).isNotNull();
    assertThat(stillOwned.getWorkerId()).isEqualTo("worker-B");
    assertThat(stillOwned.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS);

    // Worker-B can still delete its own row.
    assertThat(dao.deleteById(id, "worker-B")).isEqualTo(1);
    assertThat(dao.getById(id)).isNull();
  }

  @Test
  public void testUnacquire_transitionsToPendingWithoutIncrementingRetryCount() {
    // unacquire() is the worker-shutdown signal path: the job did not fail, the worker just
    // got interrupted. The row must go back to PENDING with worker_id cleared, but retry_count
    // MUST NOT be incremented (bumping it for an interrupt would let 3 rolling restarts
    // permanently delete a healthy job — see the Javadoc on unacquire).
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-A", 10);
    int retryBefore = dao.getById(id).getRetryCount();

    int updated = dao.unacquire(id);

    assertThat(updated).isEqualTo(1);
    ContinuousMonitoringQueueItem reread = dao.getById(id);
    assertThat(reread.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_PENDING);
    assertThat(reread.getWorkerId()).isNull();
    assertThat(reread.getAcquiredAt()).isNull();
    assertThat(reread.getRetryCount()).isEqualTo(retryBefore);
  }

  @Test
  public void testUnacquire_returnsZeroForNonExistentRow() {
    // Defensive contract: unacquire of a missing id is a no-op, not an exception. The caller
    // logs and moves on (the row may have been deleted by another worker between the worker's
    // acquire and its interrupt-handling).
    int updated = dao.unacquire(UUID.randomUUID().toString());
    assertThat(updated).isZero();
  }

  @Test
  public void testUnacquire_preservesExistingRetryCount() {
    // Even after a prior failure has incremented retry_count, an interrupt-driven unacquire
    // leaves the count alone. Distinguishes shutdown signals from real failures.
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-A", 10);
    // Simulate one failed attempt — retry_count bumps to 1, row goes back to PENDING.
    dao.markRetry(id, "transient");
    // Now reacquire and unacquire (the interrupt path).
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-A", 10);

    int updated = dao.unacquire(id);

    assertThat(updated).isEqualTo(1);
    ContinuousMonitoringQueueItem reread = dao.getById(id);
    assertThat(reread.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_PENDING);
    assertThat(reread.getWorkerId()).isNull();
    assertThat(reread.getRetryCount()).isEqualTo(1);
  }

  @Test
  public void testProducerOrchestration_dedupsOnNaturalKeyAndRemovesOrphanParent() {
    // tempEntity drives the same producer-side orchestration the production producer uses:
    // queueItemDAO.insertBatch + hostedRepoItemDAO.insertIgnoreDuplicateKey +
    // queueItemDAO.deleteOrphanParentsForSatelliteTable. Inserting twice with the same
    // (repository_id, component_hash) must dedup the satellite on the natural-key UNIQUE and
    // delete the second insert's orphan parent so countPending stays at 1.
    String firstId =
        tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    String secondId =
        tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();

    assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(1L);
    assertThat(dao.getById(firstId)).isNotNull();
    assertThat(dao.getById(secondId)).isNull(); // orphan parent was deleted

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(hostedRepoItemDAO.getByQueueIds(tx, List.of(firstId))).hasSize(1);
      assertThat(hostedRepoItemDAO.getByQueueIds(tx, List.of(secondId))).isEmpty();
      tx.commit();
    }
  }

  @Test
  public void testMarkRetry_incrementsCountAndRequeues() {
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    int firstRetry = dao.markRetry(id, "transient failure");
    int secondRetry = dao.markRetry(id, "still failing");

    assertThat(firstRetry).isEqualTo(1);
    assertThat(secondRetry).isEqualTo(2);

    ContinuousMonitoringQueueItem reread = dao.getById(id);
    assertThat(reread.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_PENDING);
    assertThat(reread.getWorkerId()).isNull();
    assertThat(reread.getAcquiredAt()).isNull();
    assertThat(reread.getErrorMessage()).isEqualTo("still failing");
  }

  @Test
  public void testMarkRetry_truncatesLongErrorMessages() {
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    String tooLong = "x".repeat(800);
    dao.markRetry(id, tooLong);

    ContinuousMonitoringQueueItem reread = dao.getById(id);
    assertThat(reread.getErrorMessage()).hasSize(ContinuousMonitoringQueueItem.ERROR_MESSAGE_MAX_LENGTH);
  }

  @Test
  public void testResetInProgressToPending_clearsWorkerAssignment() {
    String id = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L).getId();
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    int reset = dao.resetInProgressToPending("worker-1");

    assertThat(reset).isEqualTo(1);
    ContinuousMonitoringQueueItem reread = dao.getById(id);
    assertThat(reread.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_PENDING);
    assertThat(reread.getWorkerId()).isNull();
    assertThat(reread.getAcquiredAt()).isNull();
  }

  @Test
  public void testResetInProgressToPending_isScopedByWorkerId() {
    // Two workers each acquire one row; reset under worker-1 must not touch worker-2's row.
    // Use distinct create_time values so the consumer's FIFO order is deterministic — without
    // this, the random UUID tiebreaker decides which row goes to which worker and the assertion
    // below cannot pin "idA" to worker-1.
    long base = 1_700_000_000_000L;
    String idA = tempEntity.newContinuousMonitoringHostedRepoQueueItem(
        "repo-1", "hash-A", 100L, new java.util.Date(base)).getId();
    String idB = tempEntity.newContinuousMonitoringHostedRepoQueueItem(
        "repo-1", "hash-B", 50L, new java.util.Date(base + 1000)).getId();

    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 1);
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-2", 1);

    int reset = dao.resetInProgressToPending("worker-1");

    assertThat(reset).isEqualTo(1);
    // Worker-1 picked up the older idA (FIFO); reset returns it to PENDING.
    ContinuousMonitoringQueueItem rereadA = dao.getById(idA);
    assertThat(rereadA.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_PENDING);
    assertThat(rereadA.getWorkerId()).isNull();
    // Worker-2 picked up the newer idB — must NOT be touched by worker-1's reset.
    ContinuousMonitoringQueueItem rereadB = dao.getById(idB);
    assertThat(rereadB.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS);
    assertThat(rereadB.getWorkerId()).isEqualTo("worker-2");
  }

  @Test
  public void testMarkRetry_returnsZeroAndLogsWhenRowMissing() {
    // Row id that does not exist — markRetry should return 0 (caller logs WARN internally).
    int newCount = dao.markRetry("missing-id-" + UUID.randomUUID(), "concurrent delete race");
    assertThat(newCount).isZero();
  }

  @Test
  public void testCountPending_scopedByFlowType() {
    tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 0L);
    tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 0L);
    tempEntity.newContinuousMonitoringParentOnlyQueueItem(ContinuousMonitoringFlowType.SBOM, 0L);

    assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(2L);
    assertThat(dao.countPending(ContinuousMonitoringFlowType.SBOM)).isEqualTo(1L);
    assertThat(dao.countPending(ContinuousMonitoringFlowType.LIFECYCLE)).isZero();
  }

  /**
   * H2 1.4.196 takes a table-level lock on FOR UPDATE (it lacks SKIP LOCKED until 2.2.220), so
   * a second worker calling {@code acquirePending} while another connection holds the lock must
   * wait for the lock to be released and then complete with the right rows. Mirrors the shape of
   * {@code EvaluationQueueDAOTest.testAcquireRows_Contention}.
   */
  @Test
  public void testAcquirePending_contention_h2() throws Exception {
    // Insert three rows in strict create_time order. Priority values vary but should not affect
    // the order — acquirePending must return them in pure create_time ASC (FIFO).
    String first = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 50L,
        new java.util.Date(1_700_000_000_000L)).getId();
    String second = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 100L,
        new java.util.Date(1_700_000_000_000L + 100)).getId();
    String third = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-C", 1L,
        new java.util.Date(1_700_000_000_000L + 200)).getId();

    CountDownLatch tableLockAcquired = new CountDownLatch(1);
    CountDownLatch workerStarted = new CountDownLatch(1);

    // Thread 1: hold a table-level lock on continuous_monitoring_queue via raw JDBC FOR UPDATE.
    Thread lockingThread = new Thread(() -> {
      try (Connection conn = databaseRule.getOperationalDataStore().getDataSource().getConnection()) {
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM " + OperationalDataStore.ID + ".continuous_monitoring_queue FOR UPDATE"))
        {
          ps.executeQuery();
        }
        tableLockAcquired.countDown();
        workerStarted.await(2, TimeUnit.SECONDS);
        Thread.sleep(500); // Hold the lock briefly so the worker definitely contends for it.
        conn.rollback();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    // Thread 2: try to acquirePending while the lock is held — must block, then succeed.
    @SuppressWarnings("unchecked")
    List<ContinuousMonitoringQueueItem>[] acquiredHolder = new List[1];
    Thread workerThread = new Thread(() -> {
      try {
        workerStarted.countDown();
        acquiredHolder[0] = dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    lockingThread.start();
    tableLockAcquired.await();
    workerThread.start();

    lockingThread.join(5000);
    workerThread.join(5000);

    // Worker should have acquired all three rows in strict create_time ASC (FIFO) order,
    // regardless of the varying priority values used at insert time.
    assertThat(acquiredHolder[0]).hasSize(3)
        .extracting(ContinuousMonitoringQueueItem::getId)
        .containsExactly(first, second, third);
    assertThat(acquiredHolder[0]).allSatisfy(item -> {
      assertThat(item.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_IN_PROGRESS);
      assertThat(item.getWorkerId()).isEqualTo("worker-1");
      assertThat(item.getAcquiredAt()).isNotNull();
    });
  }
}
