/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.util.concurrent.Semaphore;

import com.google.common.annotations.VisibleForTesting;

import static com.sonatype.insight.brain.dataaccess.lock.H2ClusterLockManager.LOCKS_BY_ID;

public class H2ClusterLock
    extends AbstractClusterLock
{
  private final Semaphore semaphore;

  public H2ClusterLock(final ClusterLockId clusterLockId, final Semaphore semaphore) {
    super(clusterLockId);
    this.semaphore = semaphore;
  }

  @VisibleForTesting
  Semaphore getSemaphore() {
    return semaphore;
  }

  @Override
  public void lock(final LockType lockType, final boolean waitForLock) {
    this.lockType = lockType;
    this.waitForLock = waitForLock;

    this.acquired = acquireH2();

    // Locking prevents removal/replacement, but check that it wasn't removed/replaced before locking
    if (LOCKS_BY_ID.get(clusterLockId) != semaphore) {
      if (acquired) {
        semaphore.release(lockType.getPermits());
      }
      acquired = false;
      throw new RuntimeException("Could not acquire lock " + clusterLockId.toString());
    }

  }

  @Override
  public void unlock() {
    if (acquired && semaphore != null &&
        (long) semaphore.availablePermits() + lockType.getPermits() <= Integer.MAX_VALUE)
    {
      semaphore.release(lockType.getPermits());
      acquired = false;
    }
  }

  private boolean acquireH2() {
    if (waitForLock) {
      semaphore.acquireUninterruptibly(lockType.getPermits());
      return true;
    }
    else {
      return semaphore.tryAcquire(lockType.getPermits());
    }
  }
}
