/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.EntityNotFoundException;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LockedTransactionContextTest
{
  private LockDAO dao;

  @Before
  public void before() {
    dao = new LockDAO();
  }

  @After
  public void after() {
    LockedTransactionContext.LOCKS_BY_ID.clear();
  }

  @Test
  public void testConstructor_H2() {
    String lockId = "test-lock";
    try (LockedTransactionContext tx = new LockedTransactionContext(lockId)) {
      assertThat(tx.lockId).isEqualTo(lockId);
      Semaphore lock = LockedTransactionContext.LOCKS_BY_ID.get(tx.lockId);
      assertThat(lock).isNotNull();
      assertThat(tx.lock).isEqualTo(lock);
      assertThat(dao.getById(lockId)).isNull();
    }
  }

  @Test
  public void testConstructor_Postgres() {
    String lockId = "test-lock";
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      try (LockedTransactionContext tx = new LockedTransactionContext(lockId)) {
        assertThat(tx.lockId).isEqualTo(lockId);
        Semaphore lock = LockedTransactionContext.LOCKS_BY_ID.get(tx.lockId);
        assertThat(lock).isNull();
        assertThat(tx.lock).isNull();
        assertThat(dao.getById(lockId)).isNotNull();
      }
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private CountDownLatch startConcurrentLockThread(String lockId) throws Exception {
    CountDownLatch beginLatch = new CountDownLatch(1);
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (TransactionContext tx = new LockedTransactionContext(lockId)) {
        beginLatch.countDown();
        tx.begin();
        tx.commit();
        commitLatch.countDown();
      }
    });
    other.start();
    assertThat(beginLatch.await(10, TimeUnit.SECONDS)).isTrue();
    return commitLatch;
  }

  @Test
  public void testBlockingOnSameLock_H2() throws Exception {
    testBlockingOnSameLock();
  }

  @Test
  public void testBlockingOnSameLock_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testBlockingOnSameLock();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testBlockingOnSameLock() throws Exception {
    String lockId = "test-lock";
    CountDownLatch latch;
    try (TransactionContext tx = new LockedTransactionContext(lockId)) {
      tx.begin();
      latch = startConcurrentLockThread(lockId);
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();
      tx.commit();
    }
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testNonBlockingOnDifferentLock_H2() throws Exception {
    testNonBlockingOnDifferentLock();
  }

  @Test
  public void testNonBlockingOnDifferentLock_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testNonBlockingOnDifferentLock();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testNonBlockingOnDifferentLock() throws Exception {
    String lockId1 = "test-lock-1";
    String lockId2 = "test-lock-2";
    CountDownLatch latch;
    try (TransactionContext tx = new LockedTransactionContext(lockId1)) {
      tx.begin();
      latch = startConcurrentLockThread(lockId2);
      assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
      tx.commit();
    }
  }

  @Test
  public void testGetLockIdForPolicyViolations() {
    Application application = new Application();
    application.setId("app-id");

    assertThat(LockedTransactionContext.getLockIdForPolicyViolations(application))
        .isEqualTo(LockedTransactionContext.POLICY_VIOLATIONS_LOCK_PREFIX + application.getId());
  }

  @Test
  public void testCreateForPolicyViolations() {
    Application application = new Application();
    application.setId("app-id");
    try (LockedTransactionContext tx = LockedTransactionContext.createForPolicyViolations(application)) {
      tx.begin();

      assertThat(tx.lockId).isEqualTo(LockedTransactionContext.getLockIdForPolicyViolations(application));
    }
  }

  @Test
  public void testCreateForPolicyViolationAggregations() {
    String appId = "app-id";
    try (LockedTransactionContext tx = LockedTransactionContext.createForPolicyViolationAggregations(appId)) {
      tx.begin();

      assertThat(tx.lockId).isEqualTo(LockedTransactionContext.getLockIdForPolicyViolationAggregations(appId));
    }
  }

  @Test
  public void testGetLockIdForRepositoryComponent() {
    String repositoryId = "repositoryId";
    String componentPathname = "componentPathname";

    assertThat(LockedTransactionContext.getLockIdForRepositoryComponent(repositoryId, componentPathname))
        .isEqualTo(LockedTransactionContext.REPOSITORY_COMPONENT_LOCK_PREFIX + repositoryId + "-" + componentPathname);
  }

  @Test
  public void testCreateLockForRepositoryComponent() {
    String repositoryId = "repositoryId";
    String componentPathname = "componentPathname";

    try (LockedTransactionContext tx = LockedTransactionContext
        .createForRepositoryComponent(repositoryId, componentPathname)) {
      assertThat(tx.lockId)
          .isEqualTo(LockedTransactionContext.getLockIdForRepositoryComponent(repositoryId, componentPathname));
    }
  }

  @Test
  public void testGetLockIdForRepositoryReevaluation() {
    Repository repository = new Repository();
    repository.setId("repositoryId");

    assertThat(LockedTransactionContext.getLockIdForRepositoryReevaluation(repository))
        .isEqualTo(LockedTransactionContext.REPOSITORY_REEVALUATION_LOCK_PREFIX + repository.getId());
  }

  @Test
  public void testCreateLockForRepositoryReevaluation() {
    Repository repository = new Repository();
    repository.setId("repositoryId");

    assertThat(LockedTransactionContext.createForRepositoryReevaluation(repository).lockId)
        .isEqualTo(LockedTransactionContext.getLockIdForRepositoryReevaluation(repository));
  }

  private CountDownLatch startConcurrentDeleteLockThread(String lockId) {
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        LockedTransactionContext.deleteLock(tx, lockId);
        tx.commit();
        commitLatch.countDown();
      }
    });
    other.start();
    return commitLatch;
  }

  private void testDeleteLock_TransactionContext() {
    String lockId = "test-lock";
    new LockedTransactionContext(lockId);
    assertThat(LockedTransactionContext.lockExists(lockId)).isTrue();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      LockedTransactionContext.deleteLock(tx, lockId);
      tx.commit();
    }
    assertThat(LockedTransactionContext.lockExists(lockId)).isFalse();
  }

  @Test
  public void testDeleteLock_TransactionContext_H2() {
    testDeleteLock_TransactionContext();
  }

  @Test
  public void testDeleteLock_TransactionContext_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testDeleteLock_TransactionContext();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testDeleteLock_LockedTransactionContext() {
    String lockId = "test-lock";
    try (TransactionContext tx = new LockedTransactionContext(lockId)) {
      assertThat(LockedTransactionContext.lockExists(lockId)).isTrue();
      tx.begin();
      LockedTransactionContext.deleteLock(tx, lockId);
      tx.commit();
    }
    assertThat(LockedTransactionContext.lockExists(lockId)).isFalse();
  }

  @Test
  public void testDeleteLock_LockedTransactionContext_H2() {
    testDeleteLock_LockedTransactionContext();
  }

  @Test
  public void testDeleteLock_LockedTransactionContext_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testDeleteLock_LockedTransactionContext();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testDeleteLock_WaitsForOpenTransactions() throws Exception {
    String lockId = "test-lock";
    CountDownLatch latch;
    try (TransactionContext tx = new LockedTransactionContext(lockId)) {
      tx.begin();
      latch = startConcurrentDeleteLockThread(lockId);
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();
      tx.commit();
    }
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(dao.getById(lockId)).isNull();
  }

  @Test
  public void testDeleteLock_WaitsForOpenTransactions_H2() throws Exception {
    testDeleteLock_WaitsForOpenTransactions();
  }

  @Test
  public void testDeleteLock_WaitsForOpenTransactions_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testDeleteLock_WaitsForOpenTransactions();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testClose_ReleasesLock() throws Exception {
    String lockId = "test-lock";
    try (TransactionContext tx = new LockedTransactionContext(lockId)) {
      tx.begin();
    }
    CountDownLatch latch = startConcurrentLockThread(lockId);
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testClose_ReleasesLock_H2() throws Exception {
    testClose_ReleasesLock();
  }

  @Test
  public void testClose_ReleasesLock_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testClose_ReleasesLock();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testCannotLockIfDeleted_H2() {
    testCannotLockIfDeleted();
  }

  @Test
  public void testCannotLockIfDeleted_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testCannotLockIfDeleted();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testCannotLockIfDeleted() {
    String lockId = "test-lock";
    try (TransactionContext tx1 = new LockedTransactionContext(lockId)) {
      try (TransactionContext tx2 = dao.createTransactionContext()) {
        tx2.begin();
        LockedTransactionContext.deleteLock(tx2, lockId);
        tx2.commit();
      }
      assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(tx1::begin);
    }
  }

  @Test
  public void testCloseLockInOtherThread_H2() throws Exception {
    testCloseLockInOtherThread();
  }

  @Test
  public void testCloseLockInOtherThread_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testCloseLockInOtherThread();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testCloseLockInOtherThread() throws Exception {
    String lockId = "test-lock";
    AtomicReference<LockedTransactionContext> txReference = new AtomicReference<>();
    CountDownLatch lockInitializerAndTakerEnd = new CountDownLatch(1);
    Thread lockInitializerAndTaker = new Thread(() -> {
      LockedTransactionContext tx = new LockedTransactionContext(lockId);
      txReference.set(tx);
      tx.begin();
      lockInitializerAndTakerEnd.countDown();
    });
    CountDownLatch lockWaiterEnd = new CountDownLatch(1);
    Thread lockWaiter = new Thread(() -> {
      try (LockedTransactionContext tx = new LockedTransactionContext(lockId)) {
        tx.begin();
        lockWaiterEnd.countDown();
      }
    });
    CountDownLatch lockCloserEnd = new CountDownLatch(1);
    Thread lockCloser = new Thread(() -> {
      txReference.get().close();
      lockCloserEnd.countDown();
    });

    lockInitializerAndTaker.start();
    assertThat(lockInitializerAndTakerEnd.await(3, TimeUnit.SECONDS)).isTrue();
    lockWaiter.start();
    assertThat(lockWaiterEnd.await(3, TimeUnit.SECONDS)).isFalse();
    lockCloser.start();
    assertThat(lockCloserEnd.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(lockWaiterEnd.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testLockWithNoWait_H2() throws Exception {
    testLockWithNoWait();
  }

  @Test
  public void testLockWithNoWait_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testLockWithNoWait();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testLockWithNoWait() throws Exception {
    String lockId = "test-lock";
    try (TransactionContext tx = new LockedTransactionContext(lockId)) {
      tx.begin(); // Take the lock in the main thread
      AtomicBoolean failedToBegin = new AtomicBoolean();
      assertThat(startConcurrentLockWithNoWait(failedToBegin, lockId).await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToBegin.get()).isTrue();
      failedToBegin = new AtomicBoolean();
      assertThat(startConcurrentLockWithNoWait(failedToBegin, lockId).await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(failedToBegin.get()).isTrue();
    }
  }

  private CountDownLatch startConcurrentLockWithNoWait(AtomicBoolean failedToBegin, String lockId) {
    CountDownLatch concurrentThreadEnd = new CountDownLatch(1);
    Thread concurrentThread = new Thread(() -> {
      try (LockedTransactionContext tx2 = new LockedTransactionContext(lockId)) {
        // Try to take the lock in the concurrent thread without waiting
        failedToBegin.set(!tx2.tryBegin());
      }
      finally {
        concurrentThreadEnd.countDown();
      }
    });
    concurrentThread.start();
    return concurrentThreadEnd;
  }
}
