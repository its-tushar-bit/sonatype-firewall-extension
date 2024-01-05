/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

public abstract class AbstractClusterLock
    implements ClusterLock
{
  protected volatile LockType lockType;

  protected volatile boolean waitForLock;

  protected volatile boolean acquired;

  protected final String lockId;

  protected AbstractClusterLock(final String lockId) {
    this.lockId = lockId;
  }

  @Override
  public String getLockId() {
    return lockId;
  }

  @Override
  public void lock() {
    lock(LockType.EXCLUSIVE);
  }

  @Override
  public void lock(LockType lockType) {
    if (!acquired) {
      lock(lockType, true);
    }
  }

  @Override
  public boolean tryLock(LockType lockType) {
    if (!acquired) {
      lock(lockType, false);
    }
    return acquired;
  }

  @Override
  public void close() {
    unlock();
  }
}
