/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
      ReentrantLock reentrantLock = LockedTransactionContext.LOCKS_BY_ID.get(tx.lockId);
      assertThat(reentrantLock).isNotNull();
      assertThat(tx.reentrantLock).isEqualTo(reentrantLock);
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
        ReentrantLock reentrantLock = LockedTransactionContext.LOCKS_BY_ID.get(tx.lockId);
        assertThat(reentrantLock).isNull();
        assertThat(tx.reentrantLock).isNull();
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
  public void testReentrant_H2() {
    String lockId = "test-lock";
    try (TransactionContext tx1 = new LockedTransactionContext(lockId)) {
      tx1.begin();
      try (TransactionContext tx2 = new LockedTransactionContext(lockId)) {
        tx2.begin();
      }
    }
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

  private void testDeleteLock() throws Exception {
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
  public void testDeleteLock_H2() throws Exception {
    testDeleteLock();
  }

  @Test
  public void testDeleteLock_Postgres() throws Exception {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testDeleteLock();
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
}
