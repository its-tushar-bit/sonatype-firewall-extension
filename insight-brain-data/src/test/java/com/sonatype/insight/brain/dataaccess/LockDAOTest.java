/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LockDAOTest
{
  private LockDAO dao;

  private CountDownLatch startConcurrentLockThread(String lockId) throws Exception {
    CountDownLatch beginLatch = new CountDownLatch(1);
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        beginLatch.countDown();
        dao.acquireLock(tx, lockId);
        tx.commit();
        commitLatch.countDown();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    });
    other.start();
    assertThat(beginLatch.await(10, TimeUnit.SECONDS)).isTrue();
    return commitLatch;
  }

  private void init(DatabaseConfig dbConfig) {
    if (dbConfig == null) {
      dbConfig = new DatabaseConfig();
      dbConfig.setUrl("jdbc:h2:mem:ods;DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE");
      dbConfig.setUsername("sa");
      dbConfig.setPassword("");
    }
    OperationalDataStoreProvider.init(dbConfig, true);
    dao = new LockDAO();
  }

  @Before
  @After
  public void clearDataSource() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testCreateLock_Idempotent() {
    init(null);
    String lockId = "test-lock";
    dao.createLock(lockId);
    dao.createLock(lockId);
  }

  @Test
  public void testAcquireLock_Reentrant() {
    init(null);
    String lockId = "test-lock";
    dao.createLock(lockId);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.acquireLock(tx, lockId);
      dao.acquireLock(tx, lockId);
      tx.commit();
    }
  }

  @Test
  public void testAcquireLock_BlockingOnSameLock_H2() throws Exception {
    testAcquireLock_BlockingOnSameLock(null);
  }

  @Test
  public void testAcquireLock_BlockingOnSameLock_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      testAcquireLock_BlockingOnSameLock(postgres.getDatabaseConfig());
    }
  }

  private void testAcquireLock_BlockingOnSameLock(DatabaseConfig dbConfig) throws Exception {
    init(dbConfig);
    String lockId = "test-lock";
    dao.createLock(lockId);
    CountDownLatch latch;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.acquireLock(tx, lockId);
      latch = startConcurrentLockThread(lockId);
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();
      tx.commit();
    }
    assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testAcquireLock_NonBlockingOnDifferentLock_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      init(postgres.getDatabaseConfig());
      String lockId1 = "test-lock-1";
      String lockId2 = "test-lock-2";
      dao.createLock(lockId1);
      dao.createLock(lockId2);
      CountDownLatch latch;
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.acquireLock(tx, lockId1);
        latch = startConcurrentLockThread(lockId2);
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        tx.commit();
      }
    }
  }

  @Test
  public void testDeleteLock_Exists() {
    dao = new LockDAO();
    String lockId = "test-lock";
    String otherLockId = "other";
    dao.createLock(lockId);
    dao.createLock(otherLockId);
    assertThat(dao.getById(lockId)).isNotNull();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteLock(tx, lockId);
      tx.commit();
    }

    assertThat(dao.getById(lockId)).isNull();
    assertThat(dao.getById(otherLockId)).isNotNull();
  }

  @Test
  public void testDeleteLock_DoesNotExist() {
    dao = new LockDAO();
    String lockId = "test-lock";
    String otherLockId = "other";
    dao.createLock(otherLockId);
    assertThat(dao.getById(lockId)).isNull();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteLock(tx, lockId);
      tx.commit();
    }

    assertThat(dao.getById(lockId)).isNull();
    assertThat(dao.getById(otherLockId)).isNotNull();
  }
}
