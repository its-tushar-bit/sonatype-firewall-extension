/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.LockDAO;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@PostgresTest
public class PostgresClusterLockManagerTest
    extends AbstractClusterLockManagerTest
{
  private ApplicationDAO applicationDAO;

  private LockDAO lockDAO;

  private PostgresClusterLockManager postgresClusterLockManager;

  @Override
  @Before
  public void before() {
    DAOFactory daoFactory = new TestDAOFactory(databaseRule);
    lockDAO = daoFactory.createLockDAO();
    applicationDAO = daoFactory.createApplicationDAO();
    super.before();
  }

  @Override
  protected ClusterLockManager createClusterLockManager() {
    this.postgresClusterLockManager = new PostgresClusterLockManager(databaseRule.getOperationalDataStore(), lockDAO);
    return postgresClusterLockManager;
  }

  @Override
  protected ClusterLock createClusterLock(final String lockId) {
    return postgresClusterLockManager.createClusterLock(lockId);
  }

  @Override
  protected Pair<CountDownLatch, Thread> startConcurrentDeleteLockThread(final String lockId) {
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        postgresClusterLockManager.deleteLock(tx, lockId);
        tx.commit();
        commitLatch.countDown();
      }
    });
    other.start();
    return Pair.of(commitLatch, other);
  }

  @Override
  protected void deleteForPdfGeneration(final Application application) {
    try (TransactionContext tx = lockDAO.createTransactionContext()) {
      tx.begin();
      clusterLockManager.deleteForPdfGeneration(tx, application);
      tx.commit();
    }
  }

  @Test
  public void testConstructor_Postgres() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      assertThat(clusterLock.getLockId()).isEqualTo(lockId);
      assertThat(lockDAO.getById(lockId)).isNotNull();
    }
  }

  @Test
  public void testDeleteLock_Postgres() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      assertThat(lockExists(lockId)).isTrue();
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        postgresClusterLockManager.deleteLock(tx, lockId);
        tx.commit();
      }
      assertThat(postgresClusterLockManager.lockExists(lockId)).isFalse();
    }
  }

  @Test
  public void testCannotLockIfDeleted_Postgres() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        postgresClusterLockManager.deleteLock(tx, lockId);
        tx.commit();
      }
      assertThatExceptionOfType(RuntimeException.class).isThrownBy(clusterLock::lock)
          .withMessage("Could not acquire lock test-lock");
    }
  }

  @Test(timeout = 60_000)
  public void testLock_Postgres_LocksDoNotCompeteWithRegularQueriesForConnections() throws Exception {
    String lockId = "test";
    Thread other;
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      clusterLock.lock();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(lockId);
      CountDownLatch latch = countDownLatchThreadPair.getLeft();
      other = countDownLatchThreadPair.getRight();
      assertThat(latch.await(3, TimeUnit.SECONDS)).isFalse();

      // both this and the concurrent thread have one active tx/connection for the lock
      // if these two connections are from the same pool as for regular ODS queries, we'll deadlock next
      applicationDAO.getAll();

      clusterLock.unlock();
      assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    }
    other.join(10000);
  }

  @Test
  public void testLock_FIFO_Postgres() throws Exception {
    testLock_FIFO(false);
  }
}
