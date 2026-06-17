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
  public void testAcquirePending_ordersByPriorityDesc() {
    String low = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 1L).getId();
    String high = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 100L).getId();

    List<ContinuousMonitoringQueueItem> acquired =
        dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 10);

    // higher priority first
    assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(high, low);
  }

  @Test
  public void testAcquirePending_ordersByCreateTimeAscWhenPriorityEqual() {
    // Insert items with same priority but pinned create_time values to make ordering
    // deterministic (avoids Thread.sleep flake under low-resolution clocks / loaded CI).
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

    // Same priority → FIFO order (earlier create_time first)
    assertThat(acquired).extracting(ContinuousMonitoringQueueItem::getId).containsExactly(first, second, third);
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

    int deleted = dao.deleteById(id);

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
    assertThat(dao.deleteById(UUID.randomUUID().toString())).isZero();
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
    String idA = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 100L).getId();
    String idB = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 50L).getId();

    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-1", 1);
    dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-2", 1);

    int reset = dao.resetInProgressToPending("worker-1");

    assertThat(reset).isEqualTo(1);
    // Worker-1's row went back to PENDING with worker_id cleared.
    ContinuousMonitoringQueueItem rereadA = dao.getById(idA);
    assertThat(rereadA.getStatus()).isEqualTo(ContinuousMonitoringQueueItem.STATUS_PENDING);
    assertThat(rereadA.getWorkerId()).isNull();
    // Worker-2's row was NOT touched — still IN_PROGRESS, still owned by worker-2.
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
    String first = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-A", 100L,
        new java.util.Date(1_700_000_000_000L)).getId();
    String second = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-B", 50L,
        new java.util.Date(1_700_000_000_000L + 100)).getId();
    String third = tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-C", 50L,
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

    // Worker should have acquired all three rows, in priority/create-time order
    // (priority DESC = 100 first, then ties broken by create_time ASC).
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
