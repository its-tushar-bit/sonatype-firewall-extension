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

  private class LockThread
      extends Thread
  {
    final String lockId;

    final CountDownLatch beginLatch = new CountDownLatch(1);

    final CountDownLatch acquireLatch = new CountDownLatch(1);

    final CountDownLatch preCommitLatch = new CountDownLatch(1);

    final CountDownLatch commitLatch = new CountDownLatch(1);

    public LockThread(String lockId, boolean waitBeforeCommit) {
      this.lockId = lockId;
      if (!waitBeforeCommit) {
        preCommitLatch.countDown();
      }
    }

    public LockThread startAndWaitUntilBegin() throws Exception {
      this.start();
      assertThat(beginLatch.await(10, TimeUnit.SECONDS)).isTrue();
      return this;
    }

    @Override
    public void run() {
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        beginLatch.countDown();
        dao.acquireLock(tx, lockId);
        acquireLatch.countDown();
        preCommitLatch.await(10, TimeUnit.SECONDS);
        tx.commit();
        commitLatch.countDown();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
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
    LockThread other;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.acquireLock(tx, lockId);
      other = new LockThread(lockId, false).startAndWaitUntilBegin();
      assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isFalse();
      tx.commit();
    }
    assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testTryAcquireLock_BlockingOnSameLock_H2() throws Exception {
    testTryAcquireLock_BlockingOnSameLock(null);
  }

  @Test
  public void testTryAcquireLock_BlockingOnSameLock_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      testTryAcquireLock_BlockingOnSameLock(postgres.getDatabaseConfig());
    }
  }

  private void testTryAcquireLock_BlockingOnSameLock(DatabaseConfig dbConfig) throws Exception {
    init(dbConfig);
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread other;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.tryAcquireLock(tx, lockId);
      other = new LockThread(lockId, false).startAndWaitUntilBegin();
      assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isFalse();
      tx.commit();
    }
    assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testAcquireLock_NonBlockingOnDifferentLock_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      init(postgres.getDatabaseConfig());
      String lockId1 = "test-lock-1";
      String lockId2 = "test-lock-2";
      dao.createLock(lockId1);
      dao.createLock(lockId2);
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.acquireLock(tx, lockId1);
        LockThread other = new LockThread(lockId2, false).startAndWaitUntilBegin();
        assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
        tx.commit();
      }
    }
  }

  @Test
  public void testTryAcquireLock_NonBlockingOnDifferentLock_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      init(postgres.getDatabaseConfig());
      String lockId1 = "test-lock-1";
      String lockId2 = "test-lock-2";
      dao.createLock(lockId1);
      dao.createLock(lockId2);
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.tryAcquireLock(tx, lockId1);
        LockThread other = new LockThread(lockId2, false).startAndWaitUntilBegin();
        assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
        tx.commit();
      }
    }
  }

  @Test
  public void testTryAcquireLock_NonBlockingOnAcquiredLock_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      init(postgres.getDatabaseConfig());
      String lockId = "test-lock";
      dao.createLock(lockId);
      LockThread other = new LockThread(lockId, true).startAndWaitUntilBegin();
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        assertThat(other.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(dao.tryAcquireLock(tx, lockId)).isFalse();
      }
      other.preCommitLatch.countDown();
      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(dao.tryAcquireLock(tx, lockId)).isTrue();
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

  @Test
  public void testDeleteByPrefix_H2() {
    testDeleteByPrefix();
  }

  @Test
  public void testDeleteByPrefix_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      testDeleteByPrefix();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private void testDeleteByPrefix() {
    dao = new LockDAO();
    String lockId0 = "test0-lock1";
    String lockId1 = "test1-lock1";
    String lockId2 = "test1-lock2";
    String lockId3 = "test2-lock1";
    dao.createLock(lockId0);
    dao.createLock(lockId1);
    dao.createLock(lockId2);
    dao.createLock(lockId3);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByPrefix(tx, "test1-");
      tx.commit();
    }

    assertThat(dao.getById(lockId0)).isNotNull();
    assertThat(dao.getById(lockId1)).isNull();
    assertThat(dao.getById(lockId2)).isNull();
    assertThat(dao.getById(lockId3)).isNotNull();
  }
}
