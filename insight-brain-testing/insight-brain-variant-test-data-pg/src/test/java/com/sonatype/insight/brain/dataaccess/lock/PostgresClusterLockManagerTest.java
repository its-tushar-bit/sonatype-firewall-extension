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
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThat;

@PostgresTest
public class PostgresClusterLockManagerTest
    extends AbstractClusterLockManagerTest
{
  private ApplicationDAO applicationDAO;

  private PostgresAdvisoryLockDAO postgresAdvisoryLockDAO;

  private PostgresClusterLockManager postgresClusterLockManager;

  @Override
  @BeforeEach
  public void before() {
    DAOFactory daoFactory = new TestDAOFactory(databaseRule);
    postgresAdvisoryLockDAO = daoFactory.createPostgresAdvisoryLockDAO();
    applicationDAO = daoFactory.createApplicationDAO();
    super.before();
  }

  @Override
  protected ClusterLockManager createClusterLockManager() {
    this.postgresClusterLockManager =
        new PostgresClusterLockManager(databaseRule.getOperationalDataStore(), postgresAdvisoryLockDAO);
    return postgresClusterLockManager;
  }

  @Override
  protected ClusterLock createClusterLock(ClusterLockId clusterLockId) {
    return postgresClusterLockManager.createClusterLock(clusterLockId);
  }

  @Test
  public void testConstructor_Postgres() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      assertThat(clusterLock.getClusterLockId()).isEqualTo(ClusterLockId.forDataMigration());
    }
  }

  @Test
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
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
}
