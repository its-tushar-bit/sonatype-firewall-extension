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

  private PostgresAdvisoryLockDAO postgresAdvisoryLockDAO;

  private PostgresClusterLockManager postgresClusterLockManager;

  @Override
  @Before
  public void before() {
    DAOFactory daoFactory = new TestDAOFactory(databaseRule);
    lockDAO = daoFactory.createLockDAO();
    postgresAdvisoryLockDAO = daoFactory.createPostgresAdvisoryLockDAO();
    applicationDAO = daoFactory.createApplicationDAO();
    super.before();
  }

  @Override
  protected ClusterLockManager createClusterLockManager() {
    this.postgresClusterLockManager =
        new PostgresClusterLockManager(databaseRule.getOperationalDataStore(), lockDAO, postgresAdvisoryLockDAO);
    return postgresClusterLockManager;
  }

  @Override
  protected ClusterLock createClusterLock(ClusterLockId clusterLockId) {
    return postgresClusterLockManager.createClusterLock(clusterLockId);
  }

  @Override
  protected Pair<CountDownLatch, Thread> startConcurrentDeleteLockThread(ClusterLockId clusterLockId) {
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        postgresClusterLockManager.deleteLock(tx, clusterLockId);
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
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      assertThat(clusterLock.getLockId()).isEqualTo("data-migration");
      assertThat(lockDAO.getById("data-migration")).isNotNull();
    }
  }

  @Test
  public void testDeleteLock_Postgres() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      assertThat(lockExists(clusterLockId)).isTrue();
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        postgresClusterLockManager.deleteLock(tx, clusterLockId);
        tx.commit();
      }
      assertThat(postgresClusterLockManager.lockExists(clusterLockId)).isFalse();
    }
  }

  @Test
  public void testCannotLockIfDeleted_Postgres() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      try (TransactionContext tx = lockDAO.createTransactionContext()) {
        tx.begin();
        postgresClusterLockManager.deleteLock(tx, clusterLockId);
        tx.commit();
      }
      assertThatExceptionOfType(RuntimeException.class).isThrownBy(clusterLock::lock)
          .withMessage("Lock row does not exist: data-migration");
    }
  }

  @Test(timeout = 60_000)
  public void testLock_Postgres_LocksDoNotCompeteWithRegularQueriesForConnections() throws Exception {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    Thread other;
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      clusterLock.lock();
      Pair<CountDownLatch, Thread> countDownLatchThreadPair = startConcurrentLockThread(clusterLockId);
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
