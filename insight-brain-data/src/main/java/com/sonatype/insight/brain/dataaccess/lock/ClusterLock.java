/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import javax.persistence.LockModeType;

import static com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType.EXCLUSIVE;

public interface ClusterLock
    extends AutoCloseable
{
  /**
   * Return the locks ID
   */
  String getLockId();

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
