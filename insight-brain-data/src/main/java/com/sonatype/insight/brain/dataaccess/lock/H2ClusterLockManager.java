/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * H2 implementation of {@link ClusterLockManager}. H2 uses in-memory locks instead of row-level locks because H2 does
 * not support row-level locking, due to it being configured to use the PageStore engine. So this implementation can
 * only be used in a non-clustered environment.
 */
public class H2ClusterLockManager
    extends AbstractClusterLockManager
{
  static final ConcurrentMap<ClusterLockId, Semaphore> LOCKS_BY_ID = new ConcurrentHashMap<>();

  @Override
  protected ClusterLock createClusterLock(final ClusterLockId clusterLockId) {
    Semaphore semaphore = LOCKS_BY_ID.computeIfAbsent(clusterLockId, key -> new Semaphore(Integer.MAX_VALUE, true));
    return new H2ClusterLock(clusterLockId, semaphore);
  }

  @Override
  protected void deleteLock(final TransactionContext tx /* unused */, final ClusterLockId clusterLockId) {
    Semaphore semaphore = LOCKS_BY_ID.get(clusterLockId);
    if (semaphore != null) {
      try (ClusterLock lock = new H2ClusterLock(clusterLockId, semaphore)) {
        lock.lock(LockType.EXCLUSIVE, true);
        LOCKS_BY_ID.remove(clusterLockId);
      }
    }
  }

  @Override
  public void deleteFor(final ClusterLockId clusterLockId) {
    deleteLock(null, clusterLockId);
  }

  @Override
  public boolean lockExists(final ClusterLockId clusterLockId) {
    return LOCKS_BY_ID.containsKey(clusterLockId);
  }

  @Override
  protected void deleteLocksByPrefix(final TransactionContext tx /* unused */, final String prefix) {
    LOCKS_BY_ID.keySet()
        .stream()
        .filter(key -> key.getOldStyleLockId().startsWith(prefix))
        .forEach(clusterLockId -> deleteLock(null, clusterLockId));
  }
}
