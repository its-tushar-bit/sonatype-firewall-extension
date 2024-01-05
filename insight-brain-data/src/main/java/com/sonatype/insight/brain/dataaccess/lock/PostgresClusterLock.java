/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.dataaccess.LockDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;

public class PostgresClusterLock
    extends AbstractClusterLock
{
  private final OperationalDataStore operationalDataStore;

  private final LockDAO lockDAO;

  private volatile TransactionContext transactionContext;

  protected PostgresClusterLock(
      final String lockId,
      final OperationalDataStore operationalDataStore,
      final LockDAO lockDAO)
  {
    super(lockId);
    this.operationalDataStore = operationalDataStore;
    this.lockDAO = lockDAO;
  }

  @Override
  public void lock(final LockType lockType, final boolean waitForLock) {
    this.lockType = lockType;
    this.waitForLock = waitForLock;
    this.acquired = acquire();
  }

  @Override
  public void unlock() {
    acquired = false;
    if (transactionContext != null) {
      transactionContext.close();
      transactionContext = null;
    }
  }

  private boolean acquire() {
    TransactionContext tempTx =
        new TransactionContext(operationalDataStore.getEntityManagerFactoryForLocks().createEntityManager());
    tempTx.begin();
    try {
      if (waitForLock) {
        lockDAO.acquireLock(tempTx, lockId, lockType.getLockModeType());
        transactionContext = tempTx;
        return true;
      }
      else {
        if (lockDAO.tryAcquireLock(tempTx, lockId, lockType.getLockModeType())) {
          transactionContext = tempTx;
          return true;
        }
        // Failed to acquire lock
        tempTx.close();
        return false;
      }
    }
    catch (RuntimeException e) {
      try {
        tempTx.close();
      }
      catch (Exception closeException) {
        e.addSuppressed(closeException);
      }
      throw e;
    }
  }
}
