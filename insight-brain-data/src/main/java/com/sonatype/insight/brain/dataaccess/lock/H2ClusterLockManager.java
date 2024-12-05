/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import com.google.common.collect.MapMaker;

/**
 * H2 implementation of {@link ClusterLockManager}. H2 uses in-memory locks instead of row-level locks because H2 does
 * not support row-level locking, due to it being configured to use the PageStore engine. So this implementation can
 * only be used in a non-clustered environment.
 */
public class H2ClusterLockManager
    extends AbstractClusterLockManager
{
  /*
   * Mappings for weak values delete automatically when the value is no longer referenced elsewhere, preventing this map
   * from indefinitely accumulating all ClusterLocks ever created within this run of the server
   *
   * Note: we must use weakValues instead of weakKeys because those methods cause value and key comparisons,
   * respectively, to be done by identity rather than .equals(). For the keys that is unacceptable: two different
   * ClusterLockIds with the same semantic value must map to the same Semaphore.
   */
  static final ConcurrentMap<ClusterLockId, Semaphore> LOCKS_BY_ID = new MapMaker()
      .weakValues()
      .initialCapacity(64)
      .makeMap();

  @Override
  protected ClusterLock createClusterLock(final ClusterLockId clusterLockId) {
    Semaphore semaphore = LOCKS_BY_ID.computeIfAbsent(clusterLockId, key -> new Semaphore(Integer.MAX_VALUE, true));
    return new H2ClusterLock(clusterLockId, semaphore);
  }
}
