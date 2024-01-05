/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import com.sonatype.insight.brain.model.Application;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
  protected ClusterLock createClusterLock(final String lockId) {
    return h2ClusterLockManager.createClusterLock(lockId);
  }

  @Override
  protected CountDownLatch startConcurrentDeleteLockThread(final String lockId) {
    CountDownLatch commitLatch = new CountDownLatch(1);
    Thread other = new Thread(() -> {
      h2ClusterLockManager.deleteLock(null, lockId);
      commitLatch.countDown();
    });
    other.start();
    return commitLatch;
  }

  @Override
  protected void deleteForPdfGeneration(final Application application) {
    clusterLockManager.deleteForPdfGeneration(null, application);
  }

  @Test
  public void testConstructor_H2() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      assertThat(clusterLock.getLockId()).isEqualTo(lockId);
      Semaphore semaphore = H2ClusterLockManager.LOCKS_BY_ID.get(clusterLock.getLockId());
      assertThat(semaphore).isNotNull();
      H2ClusterLock h2Lock = (H2ClusterLock) clusterLock;
      assertThat(h2Lock.getSemaphore()).isEqualTo(semaphore);
    }
  }

  @Test
  public void testDeleteLock_H2() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      assertThat(lockExists(lockId)).isTrue();
      h2ClusterLockManager.deleteLock(null, lockId);
      assertThat(h2ClusterLockManager.lockExists(lockId)).isFalse();
    }
  }

  @Test
  public void testCannotLockIfDeleted_H2() {
    String lockId = "test-lock";
    try (ClusterLock clusterLock = createClusterLock(lockId)) {
      h2ClusterLockManager.deleteLock(null, lockId);
      assertThatExceptionOfType(RuntimeException.class).isThrownBy(clusterLock::lock)
          .withMessage("Could not acquire lock test-lock");
    }
  }

  @Test
  public void testLock_FIFO_H2() {
    testLock_FIFO(true);
  }
}
