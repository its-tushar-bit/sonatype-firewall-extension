/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import javax.persistence.LockModeType;

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
   * Return a concatenated lock ID that uniquely identifies this lock.
   * Warning: The concatenation of the lockClass and lockSubId must be unique.
   * @Deprecated getLockId will be removed in favor of getClusterLockId
   */
  default String getLockId() {
    return getClusterLockId().getOldStyleLockId();
  }

  /**
   * Implementation of {@link AutoCloseable#close()}. Invokes {@link #unlock()}.
   */
  @Override
  void close();

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
    SHARED(1, LockModeType.PESSIMISTIC_READ),
    EXCLUSIVE(Integer.MAX_VALUE, LockModeType.PESSIMISTIC_WRITE);

    private final int permits;

    private final LockModeType lockModeType;

    LockType(int permits, LockModeType lockModeType) {
      this.permits = permits;
      this.lockModeType = lockModeType;
    }

    int getPermits() {
      return permits;
    }

    LockModeType getLockModeType() {
      return lockModeType;
    }
  }
}
