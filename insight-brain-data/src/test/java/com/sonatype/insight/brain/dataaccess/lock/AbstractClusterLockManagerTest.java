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

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType;
import com.sonatype.insight.brain.db.rule.DatabaseRule;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Category(SlowTest.class)
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

  protected abstract ClusterLock createClusterLock(final String lockId);

  protected abstract Pair<CountDownLatch, Thread> startConcurrentDeleteLockThread(String lockId);

  protected abstract void deleteForPdfGeneration(final Application application);

  protected boolean lockExists(final String lockId) {
    AbstractClusterLockManager abstractClusterLockManager = (AbstractClusterLockManager) clusterLockManager;
    return abstractClusterLockManager.lockExists(lockId);
  }

  protected Pair<CountDownLatch, Thread> startConcurrentLockThread(String lockId) throws Exception {
    CountDownLatch lockLatch = new CountDownLatch(1);
    CountDownLatch unlockLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (ClusterLock clusterLock = createClusterLock(lockId)) {
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
    String lockId = "test-lock";
    CountDownLatch latch;
    Thread other;
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      clusterLock.lock();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(lockId);
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
    String lockId1 = "test-lock-1";
    String lockId2 = "test-lock-2";
    CountDownLatch latch;
    Thread other;
    try (ClusterLock clusterLock = createClusterLock(lockId1)) {
      clusterLock.lock();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(lockId2);
      latch = countDownLatchThreadPair.getLeft();
      other = countDownLatchThreadPair.getRight();
      assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
      clusterLock.unlock();
    }
    other.join(10000);
  }

  @Test
  public void testGetLockIdForPolicyViolations() {
    Application application = new Application();
    application.setId("app-id");

    assertThat(ClusterLockManager.getLockIdForPolicyViolations(application))
        .isEqualTo(ClusterLockManager.POLICY_VIOLATIONS_LOCK_PREFIX + application.getId());
  }

  @Test
  public void testCreateForPolicyViolations() {
    Application application = new Application();
    application.setId("app-id");
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolations(application)) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForPolicyViolations(application));
    }
  }

  @Test
  public void testCreateForPolicyViolationAggregations() {
    String appId = "app-id";
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyViolationAggregations(appId)) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForPolicyViolationAggregations(appId));
    }
  }

  @Test
  public void testGetLockIdForRepositoryComponent() {
    String repositoryId = "repositoryId";
    String componentPathname = "componentPathname";

    assertThat(ClusterLockManager.getLockIdForRepositoryComponent(repositoryId, componentPathname))
        .isEqualTo(ClusterLockManager.REPOSITORY_COMPONENT_LOCK_PREFIX + repositoryId + "-" + componentPathname);
  }

  @Test
  public void testCreateLockForRepositoryComponent() {
    String repositoryId = "repositoryId";
    String componentPathname = "componentPathname";

    try (ClusterLock clusterLock = clusterLockManager.createForRepositoryComponent(repositoryId,
        componentPathname)) {
      assertThat(clusterLock.getLockId())
          .isEqualTo(ClusterLockManager.getLockIdForRepositoryComponent(repositoryId, componentPathname));
    }
  }

  @Test
  public void testGetLockIdForRepositoryReevaluation() {
    Repository repository = new Repository();
    repository.setId("repositoryId");

    assertThat(ClusterLockManager.getLockIdForRepositoryReevaluation(repository))
        .isEqualTo(ClusterLockManager.REPOSITORY_REEVALUATION_LOCK_PREFIX + repository.getId());
  }

  @Test
  public void testCreateLockForRepositoryReevaluation() {
    Repository repository = new Repository();
    repository.setId("repositoryId");

    assertThat(clusterLockManager.createForRepositoryReevaluation(repository).getLockId())
        .isEqualTo(ClusterLockManager.getLockIdForRepositoryReevaluation(repository));
  }

  @Test
  public void testDeleteLock_WaitsForLocks() throws Exception {
    String lockId = "test-lock";
    CountDownLatch latch;
    Thread other;
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      clusterLock.lock();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentDeleteLockThread(lockId);
      latch = countDownLatchThreadPair.getLeft();
      other = countDownLatchThreadPair.getRight();
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();
      clusterLock.unlock();
    }
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(lockExists(lockId)).isFalse();
    other.join(10000);
  }

  @Test
  public void testClose_ReleasesLock() throws Exception {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      clusterLock.lock();
    }
    Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(lockId);
    CountDownLatch latch = countDownLatchThreadPair.getLeft();
    Thread other = countDownLatchThreadPair.getRight();
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    other.join(10000);
  }

  @Test
  public void testCloseLockInOtherThread() throws Exception {
    String lockId = "test-lock";
    AtomicReference<ClusterLock> clusterLockReference = new AtomicReference<>();
    CountDownLatch lockInitializerAndTakerEnd = new CountDownLatch(1);
    Thread lockInitializerAndTaker = new Thread(() -> {
      ClusterLock clusterLock = createClusterLock(lockId);
      clusterLockReference.set(clusterLock);
      clusterLock.lock();
      lockInitializerAndTakerEnd.countDown();
    });
    CountDownLatch lockWaiterEnd = new CountDownLatch(1);
    Thread lockWaiter = new Thread(() -> {
      try (ClusterLock clusterLock = createClusterLock(lockId)) {
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
    String lockId = "test-lock";
    Thread thread1;
    Thread thread2;
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      // Take the lock in the main thread
      clusterLock.lock();
      AtomicBoolean failedToLock = new AtomicBoolean();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair1 = startConcurrentLockWithNoWait(failedToLock, lockId);
      thread1 = countDownLatchThreadPair1.getRight();
      assertThat(countDownLatchThreadPair1.getLeft().await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToLock.get()).isTrue();
      failedToLock = new AtomicBoolean();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair2 = startConcurrentLockWithNoWait(failedToLock, lockId);
      thread2 = countDownLatchThreadPair2.getRight();
      assertThat(countDownLatchThreadPair2.getLeft().await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToLock.get()).isTrue();
    }
    thread1.join(10000);
    thread2.join(10000);
  }

  private Pair<CountDownLatch, Thread> startConcurrentLockWithNoWait(AtomicBoolean failedToLock, String lockId) {
    CountDownLatch concurrentThreadEnd = new CountDownLatch(1);
    Thread concurrentThread = new Thread(() -> {
      try (ClusterLock clusterLock = createClusterLock(lockId)) {
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
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      clusterLock.lock();
      clusterLock.lock();
    }
  }

  @Test
  public void testTryLockTwice() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      assertThat(clusterLock.tryLock()).isTrue();
      assertThat(clusterLock.tryLock()).isTrue();
    }
  }

  @Test
  public void testGetLockIdForPolicyEvaluation() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";

    assertThat(ClusterLockManager.getLockIdForPolicyEvaluation(application, scanId))
        .isEqualTo(ClusterLockManager.POLICY_EVALUATION_LOCK_PREFIX + application.getId() + "-" + scanId);
  }

  @Test
  public void testCreateForPolicyEvaluation() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";

    assertThat(clusterLockManager.createForPolicyEvaluation(application, scanId).getLockId())
        .isEqualTo(ClusterLockManager.getLockIdForPolicyEvaluation(application, scanId));
  }

  @Test
  public void testGetLockIdForAuditJsonFileStore() {
    String ownerId = "ownerId";
    assertThat(ClusterLockManager.getLockIdForAuditJsonFileStore(ownerId))
        .isEqualTo(ClusterLockManager.AUDIT_JSON_FILE_STORE_LOCK_PREFIX + ownerId);
  }

  @Test
  public void testCreateForAuditJsonFileStore() {
    String ownerId = "ownerId";
    try (ClusterLock clusterLock = clusterLockManager.createForAuditJsonFileStore(ownerId)) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForAuditJsonFileStore(ownerId));
    }
  }

  @Test
  public void testGetLockIdForSchemaMigration() {
    assertThat(ClusterLockManager.getLockIdForSchemaMigration()).isEqualTo(ClusterLockManager.SCHEMA_MIGRATION)
        .isEqualTo("schema-migration");
  }

  @Test
  public void testCreateForSchemaMigration() {
    try (ClusterLock clusterLock = clusterLockManager.createForSchemaMigration()) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForSchemaMigration());
    }
  }

  @Test
  public void testDeleteForSchemaMigration() {
    clusterLockManager.createForSchemaMigration();
    assertThat(lockExists(ClusterLockManager.getLockIdForSchemaMigration())).isTrue();

    clusterLockManager.deleteForSchemaMigration();

    assertThat(lockExists(ClusterLockManager.getLockIdForSchemaMigration())).isFalse();
  }

  @Test
  public void testGetLockIdForSchemaMigrationInProgress() {
    assertThat(ClusterLockManager.getLockIdForSchemaMigrationInProgress()).isEqualTo(
            ClusterLockManager.SCHEMA_MIGRATION_IN_PROGRESS)
        .isEqualTo("schema-migration-in-progress");
  }

  @Test
  public void testCreateForSchemaMigrationInProgress() {
    try (ClusterLock clusterLock = clusterLockManager.createForSchemaMigrationInProgress()) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForSchemaMigrationInProgress());
    }
  }

  @Test
  public void testDeleteForSchemaMigrationInProgress() {
    clusterLockManager.createForSchemaMigrationInProgress();
    assertThat(lockExists(ClusterLockManager.getLockIdForSchemaMigrationInProgress())).isTrue();

    clusterLockManager.deleteForSchemaMigrationInProgress();

    assertThat(lockExists(ClusterLockManager.getLockIdForSchemaMigrationInProgress())).isFalse();
  }

  @Test
  public void testGetLockIdForDataMigration() {
    assertThat(ClusterLockManager.getLockIdForDataMigration()).isEqualTo(ClusterLockManager.DATA_MIGRATION)
        .isEqualTo("data-migration");
  }

  @Test
  public void testCreateForDataMigration() {
    try (ClusterLock clusterLock = clusterLockManager.createForDataMigration()) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForDataMigration());
    }
  }

  @Test
  public void testDeleteForDataMigration() {
    clusterLockManager.createForDataMigration();
    assertThat(lockExists(ClusterLockManager.getLockIdForDataMigration())).isTrue();

    clusterLockManager.deleteForDataMigration();

    assertThat(lockExists(ClusterLockManager.getLockIdForDataMigration())).isFalse();
  }

  @Test
  public void testGetLockIdForAsyncDbMigration() {
    String jobName = "JobName";
    assertThat(ClusterLockManager.getLockIdForAsyncDbMigration(jobName))
        .isEqualTo(ClusterLockManager.ASYNC_DB_MIGRATION + "-" + jobName)
        .isEqualTo("async-db-migration-" + jobName);
  }

  @Test
  public void testCreateForAsyncDbMigration() {
    String name = "JobName";
    try (ClusterLock clusterLock = clusterLockManager.createForAsyncDbMigration(name)) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForAsyncDbMigration(name));
    }
  }

  @Test
  public void testDeleteForAsyncDbMigration() {
    String name = "JobName";
    try (ClusterLock ignored = clusterLockManager.createForAsyncDbMigration(name)) {
      assertThat(lockExists(ClusterLockManager.getLockIdForAsyncDbMigration(name))).isTrue();

      clusterLockManager.deleteForAsyncDbMigration(name);

      assertThat(lockExists(ClusterLockManager.getLockIdForAsyncDbMigration(name))).isFalse();
    }
  }

  @Test
  public void testGetLockIdForNewInstancePopulation() {
    assertThat(ClusterLockManager.getLockIdForNewInstancePopulation()).isEqualTo(
            ClusterLockManager.NEW_INSTANCE_POPULATION)
        .isEqualTo("new-instance-population");
  }

  @Test
  public void testCreateForNewInstancePopulation() {
    try (ClusterLock clusterLock = clusterLockManager.createForNewInstancePopulation()) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForNewInstancePopulation());
    }
  }

  @Test
  public void testDeleteForNewInstancePopulation() {
    clusterLockManager.createForNewInstancePopulation();
    assertThat(lockExists(ClusterLockManager.getLockIdForNewInstancePopulation())).isTrue();

    clusterLockManager.deleteForNewInstancePopulation();

    assertThat(lockExists(ClusterLockManager.getLockIdForNewInstancePopulation())).isFalse();
  }

  @Test
  public void testGetLockIdForPdfGeneration() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";
    assertThat(ClusterLockManager.getLockIdForPdfGeneration(application, scanId))
        .isEqualTo(ClusterLockManager.PDF_GENERATION_LOCK_PREFIX + application.getId() + "-" + scanId);
  }

  @Test
  public void testCreateForPdfGeneration() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";
    try (ClusterLock clusterLock = clusterLockManager.createForPdfGeneration(application, scanId)) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForPdfGeneration(application, scanId));
    }
  }

  @Test
  public void testDeleteForPdfGeneration() {
    Application application = new Application();
    application.setId("appId");
    String scanId = "scanId";
    clusterLockManager.createForPdfGeneration(application, scanId);
    assertThat(lockExists(ClusterLockManager.getLockIdForPdfGeneration(application, scanId))).isTrue();

    deleteForPdfGeneration(application);

    assertThat(lockExists(ClusterLockManager.getLockIdForPdfGeneration(application, scanId))).isFalse();
  }

  @Test
  public void testGetLockIdForInactiveRepositoryViolationCleaner() {
    assertThat(ClusterLockManager.getLockIdForInactiveRepositoryViolationCleaner())
        .isEqualTo(ClusterLockManager.INACTIVE_REPOSITORY_VIOLATION_CLEANER);
  }

  @Test
  public void testCreateForInactiveRepositoryViolationCleaner() {
    try (ClusterLock clusterLock = clusterLockManager.createForInactiveRepositoryViolationCleaner()) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(
          ClusterLockManager.getLockIdForInactiveRepositoryViolationCleaner());
    }
  }

  @Test
  public void testDeleteForInactiveRepositoryViolationCleaner() {
    clusterLockManager.createForInactiveRepositoryViolationCleaner();
    assertThat(lockExists(ClusterLockManager.getLockIdForInactiveRepositoryViolationCleaner())).isTrue();

    clusterLockManager.deleteForInactiveRepositoryViolationCleaner();

    assertThat(lockExists(ClusterLockManager.getLockIdForInactiveRepositoryViolationCleaner())).isFalse();
  }

  @Test
  public void testGetLockIdForFilename() {
    String filename = "test";
    assertThat(ClusterLockManager.getLockIdForFilename(filename)).isEqualTo(
        ClusterLockManager.FILENAME_LOCK_PREFIX + filename);
  }

  @Test
  public void testCreateForFilename() {
    String filename = "test";
    try (ClusterLock clusterLock = clusterLockManager.createForFilename(filename)) {
      clusterLock.lock();

      assertThat(clusterLock.getLockId()).isEqualTo(ClusterLockManager.getLockIdForFilename(filename));
    }
  }

  @Test
  public void testDeleteForFilename() {
    String filename = "test";
    clusterLockManager.createForFilename(filename);
    assertThat(lockExists(ClusterLockManager.getLockIdForFilename(filename))).isTrue();

    clusterLockManager.deleteForFilename(filename);

    assertThat(lockExists(ClusterLockManager.getLockIdForFilename(filename))).isFalse();
  }

  @Test
  public void testLock_AllowsConcurrentShared() throws Exception {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, true);

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
    String lockId = "test-lock";
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);

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
    String lockId = "test-lock";
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, true);

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
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);

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

  protected void testLock_FIFO(boolean expectFIFO) throws Exception {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, true);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, true);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, true);

    if (expectFIFO) {
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
    }
    else {
      shared1.start();
      await().atMost(2, TimeUnit.SECONDS).until(() -> shared1.acquired);
      exclusive.start();
      await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
      shared2.start();
      await().atMost(2, TimeUnit.SECONDS).until(() -> shared2.acquired);
      shared1.allowClose();
      await().pollDelay(2, TimeUnit.SECONDS).until(() -> !exclusive.acquired);
      shared2.allowClose();
      await().atMost(2, TimeUnit.SECONDS).until(() -> exclusive.acquired);
      exclusive.allowClose();
    }

    assertThat(shared1.exception).isNull();
    assertThat(exclusive.exception).isNull();
    assertThat(shared2.exception).isNull();
  }

  @Test
  public void testTryLock_AllowsConcurrentShared() throws Exception {
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, false);

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
    String lockId = "test-lock";
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, false);

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
    String lockId = "test-lock";
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, false);
    ClusterLockThread shared = new ClusterLockThread(lockId, LockType.SHARED, false);

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
    String lockId = "test-lock";
    ClusterLockThread shared1 = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread shared2 = new ClusterLockThread(lockId, LockType.SHARED, false);
    ClusterLockThread exclusive = new ClusterLockThread(lockId, LockType.EXCLUSIVE, false);

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
    private final String lockId;

    private final LockType lockType;

    private final boolean waitForLock;

    private final CountDownLatch endLatch = new CountDownLatch(1);

    private volatile boolean acquired;

    private volatile Exception exception;

    public ClusterLockThread(String lockId, LockType lockType, boolean waitForLock) {
      this.lockId = lockId;
      this.lockType = lockType;
      this.waitForLock = waitForLock;
    }

    @Override
    public void run() {
      try (ClusterLock clusterLock = createClusterLock(lockId)) {
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
