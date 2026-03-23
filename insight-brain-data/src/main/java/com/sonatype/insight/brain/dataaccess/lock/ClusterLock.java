/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import static com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType.EXCLUSIVE;

/**
 * A cluster-wide advisory lock, intended to be used to control access to resources that are shared across a cluster,
 * outside of the database (for instance, app report files which are stored on the file system).
 *
 * These locks _should_ be acquired in a try-with-resources block, at the end of which they will automatically unlock.
 * Note: a lock object may also automatically unlock if it goes out of scope and is garbage collected. If used properly
 * within try-with-resources, this should be moot.
 *
 * Individual ClusterLock instances are not guaranteed to be thread-safe. Different threads wishing to lock the same
 * resource should use separate ClusterLock instances.
 *
 * A given thread should take care not to attempt to lock multiple ClusterLocks with the same ClusterLockId, as this
 * will result in a deadlock. Deadlocks are also possible between ClusterLocks with different ClusterLockIds, if
 * they are locked by different threads in different orders. Finally, it is possible for multiple ClusterLockIds to
 * be equivalent at the database level, in which case the risk of deadlock can increase. Threads that only ever lock
 * a single ClusterLock at a time should be safe from all of these types of deadlocks.
 */
public interface ClusterLock
    extends AutoCloseable
{
  ClusterLockId getClusterLockId();

  /**
   * Implementation of {@link AutoCloseable#close()}. Invokes {@link #unlock()}.
   */
  @Override
  default void close() {
    unlock();
  }

  /**
   * Invokes {@link #lock(LockType)} with {@link LockType#EXCLUSIVE} and waits for the lock to be acquired.
   */
  void lock();

  /**
   * Locks the lock with given {@link LockType} and waits for the lock to be acquired.
   *
   * @param lockType
   */
  void lock(LockType lockType);

  /**
   * Locks the lock with the given {@link LockType} and optionally waits for the lock to be acquired.
   */
  void lock(LockType lockType, boolean waitForLock);

  /**
   * Unlocks the lock.
   */
  void unlock();

  /**
   * Locks the lock if the lock has not already been acquired in {@link LockType#EXCLUSIVE} mode.
   *
   * @return
   */
  default boolean tryLock() {
    return tryLock(EXCLUSIVE);
  }

  /**
   * Locks the lock if the lock has not already been acquired.
   */
  boolean tryLock(LockType lockType);

  enum LockType
  {
    SHARED(1),
    EXCLUSIVE(Integer.MAX_VALUE);

    private final int permits;

    LockType(int permits) {
      this.permits = permits;
    }

    int getPermits() {
      return permits;
    }
  }
}
