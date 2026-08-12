/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType;
import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public abstract class AbstractClusterLockManagerTest
{
  @Rule
  public DatabaseRule databaseRule = DatabaseRule.getInstance(AbstractClusterLockManagerTest.class);

  protected ClusterLockManager clusterLockManager;

  @Before
  public void before() {
    clusterLockManager = createClusterLockManager();
  }

  protected abstract ClusterLockManager createClusterLockManager();

  protected abstract ClusterLock createClusterLock(ClusterLockId clusterLockId);

  protected Pair<CountDownLatch, Thread> startConcurrentLockThread(ClusterLockId clusterLockId) throws Exception {
    CountDownLatch lockLatch = new CountDownLatch(1);
    CountDownLatch unlockLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
        lockLatch.countDown();
        clusterLock.lock();
        clusterLock.unlock();
        unlockLatch.countDown();
      }
    });
    other.start();
    assertThat(lockLatch.await(10, TimeUnit.SECONDS)).isTrue();
    return Pair.of(unlockLatch, other);
  }

  @Test
  public void testBlockingOnSameLock() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    CountDownLatch latch;
    Thread other;
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      clusterLock.lock();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(clusterLockId);
      latch = countDownLatchThreadPair.getLeft();
      other = countDownLatchThreadPair.getRight();
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();
      clusterLock.unlock();
    }
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    other.join(10000);
  }

  @Test
  public void testNonBlockingOnDifferentLock() throws Exception {
    ClusterLockId clusterLockId1 = ClusterLockId.forDataMigration();
    ClusterLockId clusterLockId2 = ClusterLockId.forNewInstancePopulation();

    CountDownLatch latch;
    Thread other;
    try (ClusterLock clusterLock = createClusterLock(clusterLockId1)) {
      clusterLock.lock();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(clusterLockId2);
      latch = countDownLatchThreadPair.getLeft();
      other = countDownLatchThreadPair.getRight();
      assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
      clusterLock.unlock();
    }
    other.join(10000);
  }

  @Test
  public void testCreateForPolicyViolations() {
    Application application = new Application();
    application.setId("app-id");
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolations(application)) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forPolicyViolations("app-id"));
    }
  }

  @Test
  public void testCreateForPolicyViolationAggregations() {
    String appId = "app-id";
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolationAggregations(appId)) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forPolicyViolationAggregations("app-id"));
    }
  }

  @Test
  public void testCreateLockForRepositoryComponent() {
    String repositoryId = "repositoryId";
    String componentPathname = "componentPathname";

    try (ClusterLock clusterLock = clusterLockManager.createForRepositoryComponent(repositoryId,
        componentPathname))
    {
      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forRepositoryComponent(repositoryId,
          componentPathname));
    }
  }

  @Test
  public void testCreateLockForRepositoryReevaluation() {
    Repository repository = new Repository();
    repository.setId("repositoryId");
    try (ClusterLock clusterLock = clusterLockManager.createForRepositoryReevaluation(repository)) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forRepositoryReevaluation("repositoryId"));
    }
  }

  @Test
  public void testClose_ReleasesLock() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      clusterLock.lock();
    }
    Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(clusterLockId);
    CountDownLatch latch = countDownLatchThreadPair.getLeft();
    Thread other = countDownLatchThreadPair.getRight();
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    other.join(10000);
  }

  @Test
  public void testCloseLockInOtherThread() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    AtomicReference<ClusterLock> clusterLockReference = new AtomicReference<>();
    CountDownLatch lockInitializerAndTakerEnd = new CountDownLatch(1);
    Thread lockInitializerAndTaker = new Thread(() -> {
      ClusterLock clusterLock = createClusterLock(clusterLockId);
      clusterLockReference.set(clusterLock);
      clusterLock.lock();
      lockInitializerAndTakerEnd.countDown();
    });
    CountDownLatch lockWaiterEnd = new CountDownLatch(1);
    Thread lockWaiter = new Thread(() -> {
      try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
        clusterLock.lock();
        lockWaiterEnd.countDown();
      }
    });
    CountDownLatch lockCloserEnd = new CountDownLatch(1);
    Thread lockCloser = new Thread(() -> {
      clusterLockReference.get().close();
      lockCloserEnd.countDown();
    });

    lockInitializerAndTaker.start();
    assertThat(lockInitializerAndTakerEnd.await(3, TimeUnit.SECONDS)).isTrue();
    lockWaiter.start();
    assertThat(lockWaiterEnd.await(3, TimeUnit.SECONDS)).isFalse();
    lockCloser.start();
    assertThat(lockCloserEnd.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(lockWaiterEnd.await(3, TimeUnit.SECONDS)).isTrue();

    lockInitializerAndTaker.join(10000);
    lockWaiter.join(10000);
    lockCloser.join(10000);
  }

  @Test
  public void testLockWithNoWait() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    Thread thread1;
    Thread thread2;
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      // Take the lock in the main thread
      clusterLock.lock();
      AtomicBoolean failedToLock = new AtomicBoolean();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair1 =
          startConcurrentLockWithNoWait(failedToLock, clusterLockId);
      thread1 = countDownLatchThreadPair1.getRight();
      assertThat(countDownLatchThreadPair1.getLeft().await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToLock.get()).isTrue();
      failedToLock = new AtomicBoolean();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair2 =
          startConcurrentLockWithNoWait(failedToLock, clusterLockId);
      thread2 = countDownLatchThreadPair2.getRight();
      assertThat(countDownLatchThreadPair2.getLeft().await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToLock.get()).isTrue();
    }
    thread1.join(10000);
    thread2.join(10000);
  }

  private Pair<CountDownLatch, Thread> startConcurrentLockWithNoWait(
      AtomicBoolean failedToLock,
      ClusterLockId clusterLockId)
  {
    CountDownLatch concurrentThreadEnd = new CountDownLatch(1);
    Thread concurrentThread = new Thread(() -> {
      try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
        // Try to take the lock in the concurrent thread without waiting
        failedToLock.set(!clusterLock.tryLock());
      }
      finally {
        concurrentThreadEnd.countDown();
      }
    });
    concurrentThread.start();
    return Pair.of(concurrentThreadEnd, concurrentThread);
  }

  @Test
  public void testLockTwice() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      clusterLock.lock();
      clusterLock.lock();
    }
  }

  @Test
  public void testTryLockTwice() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      assertThat(clusterLock.tryLock()).isTrue();
      assertThat(clusterLock.tryLock()).isTrue();
    }
  }

  @Test
  public void testCreateForPolicyEvaluation() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";

    try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(application, scanId)) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forPolicyEvaluation("appId", "scanId"));
    }
  }

  @Test
  public void testCreateForAuditJsonFileStore() {
    String ownerId = "ownerId";
    try (ClusterLock clusterLock = clusterLockManager.createForAuditJsonFileStore(ownerId)) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forAuditJsonFileStore("ownerId"));
    }
  }

  @Test
  public void testCreateForSchemaMigration() {
    try (ClusterLock clusterLock = clusterLockManager.createForSchemaMigration()) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forSchemaMigration());
    }
  }

  @Test
  public void testCreateForDataMigration() {
    try (ClusterLock clusterLock = clusterLockManager.createForDataMigration()) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forDataMigration());
    }
  }

  @Test
  public void testCreateForNewInstancePopulation() {
    try (ClusterLock clusterLock = clusterLockManager.createForNewInstancePopulation()) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forNewInstancePopulation());
    }
  }

  @Test
  public void testCreateForPdfGeneration() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";
    try (ClusterLock clusterLock = clusterLockManager.createForPdfGeneration(application, scanId)) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forPdfGeneration("appId", "scanId"));
    }
  }

  @Test
  public void testCreateForInactiveRepositoryViolationCleaner() {
    try (ClusterLock clusterLock = clusterLockManager.createForInactiveRepositoryViolationCleaner()) {
      clusterLock.lock();

      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forInactiveRepositoryViolationCleaner());
    }
  }

  @Test
  public void testLock_AllowsConcurrentShared() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread shared1 = new ClusterLockThread(clusterLockId, LockType.SHARED, true);
    ClusterLockThread shared2 = new ClusterLockThread(clusterLockId, LockType.SHARED, true);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);

    shared1.allowClose();
    shared2.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
  }

  @Test
  public void testLock_SharedDoesNotAllowExclusive() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread shared = new ClusterLockThread(clusterLockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(clusterLockId, LockType.EXCLUSIVE, true);

    shared.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }

  @Test
  public void testLock_ExclusiveDoesNotAllowShared() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread exclusive = new ClusterLockThread(clusterLockId, LockType.EXCLUSIVE, true);
    ClusterLockThread shared = new ClusterLockThread(clusterLockId, LockType.SHARED, true);

    exclusive.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
    shared.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared.acquired);
    exclusive.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared.acquired);

    shared.allowClose();
    assertThat(exclusive.exception).isNull();
    assertThat(shared.exception).isNull();
  }

  @Test
  public void testLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread shared1 = new ClusterLockThread(clusterLockId, LockType.SHARED, true);
    ClusterLockThread shared2 = new ClusterLockThread(clusterLockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(clusterLockId, LockType.EXCLUSIVE, true);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared1.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared2.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }

  @Test
  public void testLock_FIFO() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread shared1 = new ClusterLockThread(clusterLockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(clusterLockId, LockType.EXCLUSIVE, true);
    ClusterLockThread shared2 = new ClusterLockThread(clusterLockId, LockType.SHARED, true);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared2.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared2.acquired);
    shared1.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared2.acquired);
    exclusive.allowClose();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
    shared2.allowClose();

    assertThat(shared1.exception).isNull();
    assertThat(exclusive.exception).isNull();
    assertThat(shared2.exception).isNull();
  }

  @Test
  public void testTryLock_AllowsConcurrentShared() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread shared1 = new ClusterLockThread(clusterLockId, LockType.SHARED, false);
    ClusterLockThread shared2 = new ClusterLockThread(clusterLockId, LockType.SHARED, false);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);

    shared1.allowClose();
    shared2.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
  }

  @Test
  public void testTryLock_SharedDoesNotAllowExclusive() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread shared = new ClusterLockThread(clusterLockId, LockType.SHARED, false);
    ClusterLockThread exclusive = new ClusterLockThread(clusterLockId, LockType.EXCLUSIVE, false);

    shared.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }

  @Test
  public void testTryLock_ExclusiveDoesNotAllowShared() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread exclusive = new ClusterLockThread(clusterLockId, LockType.EXCLUSIVE, false);
    ClusterLockThread shared = new ClusterLockThread(clusterLockId, LockType.SHARED, false);

    exclusive.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
    shared.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared.acquired);
    exclusive.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !shared.acquired);

    shared.allowClose();
    assertThat(exclusive.exception).isNull();
    assertThat(shared.exception).isNull();
  }

  @Test
  public void testTryLock_AllowsConcurrentSharedWhilstExclusiveIsNotAllowed() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    ClusterLockThread shared1 = new ClusterLockThread(clusterLockId, LockType.SHARED, false);
    ClusterLockThread shared2 = new ClusterLockThread(clusterLockId, LockType.SHARED, false);
    ClusterLockThread exclusive = new ClusterLockThread(clusterLockId, LockType.EXCLUSIVE, false);

    shared1.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
    shared2.start();
    await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
    exclusive.start();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared1.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
    shared2.allowClose();
    await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);

    exclusive.allowClose();
    assertThat(shared1.exception).isNull();
    assertThat(shared2.exception).isNull();
    assertThat(exclusive.exception).isNull();
  }

  private class ClusterLockThread
      extends Thread
  {
    private final ClusterLockId clusterLockId;

    private final LockType lockType;

    private final boolean waitForLock;

    private final CountDownLatch endLatch = new CountDownLatch(1);

    private volatile boolean acquired;

    private volatile Exception exception;

    public ClusterLockThread(ClusterLockId clusterLockId, LockType lockType, boolean waitForLock) {
      this.clusterLockId = clusterLockId;
      this.lockType = lockType;
      this.waitForLock = waitForLock;
    }

    @Override
    public void run() {
      try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
        if (waitForLock) {
          clusterLock.lock(lockType);
          acquired = true;
        }
        else {
          acquired = clusterLock.tryLock(lockType);
        }
        endLatch.await(10, TimeUnit.SECONDS);
      }
      catch (Exception e) {
        exception = e;
      }
    }

    public void allowClose() throws Exception {
      endLatch.countDown();
      join(10000);
    }
  }
}
