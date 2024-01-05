/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.LockModeType;
import javax.persistence.RollbackException;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LockDAOTest
    extends AbstractDbDAOTest
{
  private LockDAO dao;

  private class LockThread
      extends Thread
  {
    final String lockId;

    final LockModeType lockModeType;

    final boolean waitForLock;

    final AtomicBoolean acquired = new AtomicBoolean();

    final CountDownLatch beginLatch = new CountDownLatch(1);

    final CountDownLatch acquireLatch = new CountDownLatch(1);

    final CountDownLatch preCommitLatch = new CountDownLatch(1);

    final CountDownLatch commitLatch = new CountDownLatch(1);

    final CountDownLatch endLatch = new CountDownLatch(1);

    final AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();

    public LockThread(String lockId, boolean waitBeforeCommit) {
      this(lockId, LockModeType.PESSIMISTIC_WRITE, true, waitBeforeCommit);
    }

    public LockThread(String lockId, LockModeType lockModeType, boolean waitForLock, boolean waitBeforeCommit) {
      this.lockId = lockId;
      this.lockModeType = lockModeType;
      this.waitForLock = waitForLock;
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
        if (waitForLock) {
          dao.acquireLock(tx, lockId, lockModeType);
          acquired.set(true);
        }
        else {
          acquired.set(dao.tryAcquireLock(tx, lockId, lockModeType));
        }
        acquireLatch.countDown();
        preCommitLatch.await(10, TimeUnit.SECONDS);
        tx.commit();
        commitLatch.countDown();
      }
      catch (Exception e) {
        exceptionAtomicReference.set(e);
      }
      finally {
        endLatch.countDown();
      }
    }
  }

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createLockDAO();
  }

  @Test
  public void testCreateLock_Idempotent() {
    String lockId = "test-lock";
    dao.createLock(lockId);
    dao.createLock(lockId);
  }

  @Test
  public void testAcquireLock_Reentrant() {
    String lockId = "test-lock";
    dao.createLock(lockId);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.acquireLock(tx, lockId, LockModeType.PESSIMISTIC_WRITE);
      dao.acquireLock(tx, lockId, LockModeType.PESSIMISTIC_WRITE);
      tx.commit();
    }
  }

  @Test
  @H2InMemoryTest(customSettings = "DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE")
  public void testAcquireLock_BlockingOnSameLock_H2() throws Exception {
    testAcquireLock_BlockingOnSameLock();
  }

  @Test
  @PostgresTest
  public void testAcquireLock_BlockingOnSameLock_Postgres() throws Exception {
    testAcquireLock_BlockingOnSameLock();
  }

  private void testAcquireLock_BlockingOnSameLock() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread other;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.acquireLock(tx, lockId, LockModeType.PESSIMISTIC_WRITE);
      other = new LockThread(lockId, false).startAndWaitUntilBegin();
      assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isFalse();
      tx.commit();
    }
    assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @H2InMemoryTest(customSettings = "DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE")
  public void testTryAcquireLock_BlockingOnSameLock_H2() throws Exception {
    testTryAcquireLock_BlockingOnSameLock();
  }

  @Test
  @PostgresTest
  public void testTryAcquireLock_BlockingOnSameLock_Postgres() throws Exception {
    testTryAcquireLock_BlockingOnSameLock();
  }

  private void testTryAcquireLock_BlockingOnSameLock() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread other;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.tryAcquireLock(tx, lockId, LockModeType.PESSIMISTIC_WRITE);
      other = new LockThread(lockId, false).startAndWaitUntilBegin();
      assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isFalse();
      tx.commit();
    }
    assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @PostgresTest
  public void testAcquireLock_NonBlockingOnDifferentLock_Postgres() throws Exception {
    String lockId1 = "test-lock-1";
    String lockId2 = "test-lock-2";
    dao.createLock(lockId1);
    dao.createLock(lockId2);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.acquireLock(tx, lockId1, LockModeType.PESSIMISTIC_WRITE);
      LockThread other = new LockThread(lockId2, false).startAndWaitUntilBegin();
      assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
      tx.commit();
    }
  }

  @Test
  @PostgresTest
  public void testTryAcquireLock_NonBlockingOnDifferentLock_Postgres() throws Exception {
    String lockId1 = "test-lock-1";
    String lockId2 = "test-lock-2";
    dao.createLock(lockId1);
    dao.createLock(lockId2);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.tryAcquireLock(tx, lockId1, LockModeType.PESSIMISTIC_WRITE);
      LockThread other = new LockThread(lockId2, false).startAndWaitUntilBegin();
      assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
      tx.commit();
    }
  }

  @Test
  @PostgresTest
  public void testTryAcquireLock_NonBlockingOnAcquiredLock_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread other = new LockThread(lockId, true).startAndWaitUntilBegin();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(other.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(dao.tryAcquireLock(tx, lockId, LockModeType.PESSIMISTIC_WRITE)).isFalse();
    }
    other.preCommitLatch.countDown();
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThat(other.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
      assertThat(dao.tryAcquireLock(tx, lockId, LockModeType.PESSIMISTIC_WRITE)).isTrue();
    }
  }

  @Test
  public void testDeleteLock_Exists() {
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
  @PostgresTest
  public void testDeleteByPrefix_Postgres() {
    testDeleteByPrefix();
  }

  @Test
  public void testAcquireLock_Read_H2() {
    String lockId = "test-lock";
    dao.createLock(lockId);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
          () -> dao.acquireLock(tx, lockId, LockModeType.PESSIMISTIC_READ))
          .withMessage("Embedded database only supports acquiring an exclusive lock.");
      tx.commit();
    }
  }

  @Test
  @PostgresTest
  public void testAcquireLock_AllowsConcurrentReads_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread read1 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, true, true);
    LockThread read2 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, true, false);

    read1.startAndWaitUntilBegin();
    assertThat(read1.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    read2.startAndWaitUntilBegin();
    assertThat(read2.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();

    read1.preCommitLatch.countDown();
    assertThat(read1.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @PostgresTest
  public void testAcquireLock_ReadDoesNotAllowWrite_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread read = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, true, true);
    LockThread write = new LockThread(lockId, LockModeType.PESSIMISTIC_WRITE, true, false);

    read.startAndWaitUntilBegin();
    assertThat(read.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    write.startAndWaitUntilBegin();
    assertThat(write.acquireLatch.await(3, TimeUnit.SECONDS)).isFalse();

    read.preCommitLatch.countDown();
    assertThat(read.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(write.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @PostgresTest
  public void testAcquireLock_WriteDoesNotAllowRead_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread write = new LockThread(lockId, LockModeType.PESSIMISTIC_WRITE, true, true);
    LockThread read = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, true, false);

    write.startAndWaitUntilBegin();
    assertThat(write.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    read.startAndWaitUntilBegin();
    assertThat(read.acquireLatch.await(3, TimeUnit.SECONDS)).isFalse();

    write.preCommitLatch.countDown();
    assertThat(write.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(read.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @PostgresTest
  public void testAcquireLock_AllowsConcurrentReadsWhilstWriteIsNotAllowed_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread read1 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, true, true);
    LockThread read2 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, true, false);
    LockThread write = new LockThread(lockId, LockModeType.PESSIMISTIC_WRITE, true, false);

    read1.startAndWaitUntilBegin();
    assertThat(read1.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    write.startAndWaitUntilBegin();
    assertThat(write.acquireLatch.await(3, TimeUnit.SECONDS)).isFalse();

    read2.startAndWaitUntilBegin();
    assertThat(read2.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();

    assertThat(write.acquireLatch.await(3, TimeUnit.SECONDS)).isFalse();

    read1.preCommitLatch.countDown();
    assertThat(read1.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(write.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void testTryAcquireLock_Read_H2() {
    String lockId = "test-lock";
    dao.createLock(lockId);
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
          () -> dao.tryAcquireLock(tx, lockId, LockModeType.PESSIMISTIC_READ))
          .withMessage("Embedded database only supports acquiring an exclusive lock.");
      tx.commit();
    }
  }

  @Test
  @PostgresTest
  public void testTryAcquireLock_AllowsConcurrentReads_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread read1 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, false, true);
    LockThread read2 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, false, false);

    read1.startAndWaitUntilBegin();
    assertThat(read1.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    read2.startAndWaitUntilBegin();
    assertThat(read2.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();

    read1.preCommitLatch.countDown();
    assertThat(read1.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @PostgresTest
  public void testTryAcquireLock_ReadDoesNotAllowWrite_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread read = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, false, true);
    LockThread write = new LockThread(lockId, LockModeType.PESSIMISTIC_WRITE, false, false);

    read.startAndWaitUntilBegin();
    assertThat(read.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    write.startAndWaitUntilBegin();
    assertThat(write.endLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(write.acquired).isFalse();
    assertThat(write.exceptionAtomicReference.get()).isInstanceOf(RollbackException.class);

    read.preCommitLatch.countDown();
    assertThat(read.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @PostgresTest
  public void testTryAcquireLock_WriteDoesNotAllowRead_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread write = new LockThread(lockId, LockModeType.PESSIMISTIC_WRITE, false, true);
    LockThread read = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, false, false);

    write.startAndWaitUntilBegin();
    assertThat(write.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    read.startAndWaitUntilBegin();
    assertThat(read.endLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(read.acquired).isFalse();
    assertThat(read.exceptionAtomicReference.get()).isInstanceOf(RollbackException.class);

    write.preCommitLatch.countDown();
    assertThat(write.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  @PostgresTest
  public void testTryAcquireLock_AllowsConcurrentReadsWhilstWriteIsNotAllowed_Postgres() throws Exception {
    String lockId = "test-lock";
    dao.createLock(lockId);
    LockThread read1 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, false, true);
    LockThread read2 = new LockThread(lockId, LockModeType.PESSIMISTIC_READ, false, false);
    LockThread write = new LockThread(lockId, LockModeType.PESSIMISTIC_WRITE, false, true);

    read1.startAndWaitUntilBegin();
    assertThat(read1.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();

    write.startAndWaitUntilBegin();
    assertThat(write.acquireLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(write.acquired).isFalse();

    read2.startAndWaitUntilBegin();
    assertThat(read2.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();

    read1.preCommitLatch.countDown();
    write.preCommitLatch.countDown();
    assertThat(read1.commitLatch.await(3, TimeUnit.SECONDS)).isTrue();
    assertThat(write.endLatch.await(3, TimeUnit.SECONDS)).isTrue();
  }

  private void testDeleteByPrefix() {
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
