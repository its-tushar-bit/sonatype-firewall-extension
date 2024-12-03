/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.util.concurrent.CountDownLatch;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
public class H2ClusterLockManagerTest
    extends AbstractClusterLockManagerTest
{
  private H2ClusterLockManager h2ClusterLockManager;

  @Override
  protected ClusterLockManager createClusterLockManager() {
    this.h2ClusterLockManager = new H2ClusterLockManager();
    return h2ClusterLockManager;
  }

  @Override
  protected ClusterLock createClusterLock(ClusterLockId clusterLockId) {
    return h2ClusterLockManager.createClusterLock(clusterLockId);
  }

  @Override
  protected Pair<CountDownLatch, Thread> startConcurrentDeleteLockThread(ClusterLockId clusterLockId) {
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      h2ClusterLockManager.deleteLock(null, clusterLockId);
      commitLatch.countDown();
    });
    other.start();
    return Pair.of(commitLatch, other);
  }

  @Override
  protected void deleteForPdfGeneration(final Application application) {
    clusterLockManager.deleteForPdfGeneration(null, application);
  }

  @Test
  public void testConstructor_H2() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock1 = createClusterLock(clusterLockId);
        ClusterLock clusterLock2 = createClusterLock(clusterLockId)) {
      assertThat(clusterLock1.getClusterLockId()).isEqualTo(clusterLock2.getClusterLockId());
      assertThat(clusterLock1.getLockId()).isEqualTo(clusterLock2.getLockId());
    }
  }

  @Test
  public void testDeleteLock_H2() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      assertThat(lockExists(clusterLockId)).isTrue();
      h2ClusterLockManager.deleteLock(null, clusterLockId);
      assertThat(h2ClusterLockManager.lockExists(clusterLockId)).isFalse();
    }
  }

  @Test
  public void testCannotLockIfDeleted_H2() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock = createClusterLock(clusterLockId)) {
      h2ClusterLockManager.deleteLock(null, clusterLockId);
      assertThatExceptionOfType(RuntimeException.class).isThrownBy(clusterLock::lock)
          .withMessage("Could not acquire lock data-migration");
    }
  }

  @Test
  public void testLock_FIFO_H2() throws Exception {
    testLock_FIFO(true);
  }
}
