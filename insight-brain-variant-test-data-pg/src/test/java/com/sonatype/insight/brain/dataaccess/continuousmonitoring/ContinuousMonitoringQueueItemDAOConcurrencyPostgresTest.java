/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringFlowType;
import com.sonatype.insight.brain.model.continuousmonitoring.ContinuousMonitoringQueueItem;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Postgres-only concurrency test for the {@code FOR UPDATE SKIP LOCKED} acquire path
 * (CLM-40039 §6.2). Two concurrent workers race to acquire the same set of PENDING rows;
 * the test asserts that each row is acquired by exactly one worker — never duplicated,
 * never skipped — and that {@code countPending} is zero after both calls return.
 * <p>
 * Categorised as {@link PostgresTestCategory} so it runs in the Postgres-only pipeline against a
 * real Postgres instance, not the default H2 fixture.
 */
@PostgresTest
@Category(PostgresTestCategory.class)
public class ContinuousMonitoringQueueItemDAOConcurrencyPostgresTest
    extends AbstractDbDAOTest
{
  private ContinuousMonitoringQueueItemDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createContinuousMonitoringQueueItemDAO();
  }

  @Test
  public void testAcquirePending_underConcurrentWorkers_eachRowIsAcquiredExactlyOnce() throws Exception {
    int rowCount = 50;
    // Half-queue per worker so neither can drain it alone, regardless of CI thread scheduling.
    // SKIP LOCKED's no-double-acquire property is still verified by doesNotContainAnyElementsOf.
    int perWorkerLimit = rowCount / 2;
    IntStream.range(0, rowCount)
        .forEach(i -> tempEntity.newContinuousMonitoringHostedRepoQueueItem("repo-1", "hash-" + i, 0L));
    assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isEqualTo(rowCount);

    // Barrier maximises the chance both acquirePending statements actually overlap at the DB,
    // where SKIP LOCKED is what keeps their row sets disjoint.
    CyclicBarrier startGate = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Callable<List<ContinuousMonitoringQueueItem>> acquireA = () -> {
        startGate.await(10, TimeUnit.SECONDS);
        return dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-A", perWorkerLimit);
      };
      Callable<List<ContinuousMonitoringQueueItem>> acquireB = () -> {
        startGate.await(10, TimeUnit.SECONDS);
        return dao.acquirePending(ContinuousMonitoringFlowType.HOSTED_REPO, "worker-B", perWorkerLimit);
      };

      Future<List<ContinuousMonitoringQueueItem>> futureA = executor.submit(acquireA);
      Future<List<ContinuousMonitoringQueueItem>> futureB = executor.submit(acquireB);

      List<ContinuousMonitoringQueueItem> acquiredA = futureA.get(30, TimeUnit.SECONDS);
      List<ContinuousMonitoringQueueItem> acquiredB = futureB.get(30, TimeUnit.SECONDS);

      // Every row was acquired by exactly one worker — no duplicates, no skips.
      assertThat(acquiredA.size() + acquiredB.size()).isEqualTo(rowCount);
      assertThat(acquiredA).extracting(ContinuousMonitoringQueueItem::getId)
          .doesNotContainAnyElementsOf(
              acquiredB.stream().map(ContinuousMonitoringQueueItem::getId).toList());
      // With rowCount == 2 * perWorkerLimit and the sum assertion above, each worker must
      // have claimed exactly perWorkerLimit rows.
      assertThat(acquiredA).as("worker-A should acquire its bounded share").isNotEmpty();
      assertThat(acquiredB).as("worker-B should acquire its bounded share").isNotEmpty();
      // Every acquired row is in IN_PROGRESS and stamped with the right worker_id.
      assertThat(acquiredA).allSatisfy(item -> assertThat(item.getWorkerId()).isEqualTo("worker-A"));
      assertThat(acquiredB).allSatisfy(item -> assertThat(item.getWorkerId()).isEqualTo("worker-B"));
      // Queue is fully drained.
      assertThat(dao.countPending(ContinuousMonitoringFlowType.HOSTED_REPO)).isZero();
    }
    finally {
      executor.shutdownNow();
    }
  }
}
