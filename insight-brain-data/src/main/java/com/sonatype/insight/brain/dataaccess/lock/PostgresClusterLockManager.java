/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.LockDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * A Postgres implementation of {@link ClusterLockManager}. Locks are stored in the database and clustering is fully
 * supported.
 */
@Named
@Singleton
public class PostgresClusterLockManager
    extends AbstractClusterLockManager
{
  private final OperationalDataStore operationalDataStore;

  private final LockDAO lockDAO;

  @Inject
  public PostgresClusterLockManager(final OperationalDataStore operationalDataStore, final LockDAO lockDAO) {
    this.operationalDataStore = operationalDataStore;
    this.lockDAO = lockDAO;
  }

  @Override
  public ClusterLock createClusterLock(final String lockId) {
    ClusterLock lock = new PostgresClusterLock(lockId, operationalDataStore, lockDAO);
    lockDAO.createLock(lockId);
    return lock;
  }

  @Override
  protected void deleteLock(final TransactionContext tx, final String lockId) {
    lockDAO.deleteLock(tx, lockId);
  }

  @Override
  protected void deleteLocksByPrefix(final TransactionContext tx, final String prefix) {
    lockDAO.deleteByPrefix(tx, prefix);
  }

  @Override
  public void deleteFor(final String lockId) {
    try (TransactionContext tx = lockDAO.createTransactionContext()) {
      tx.begin();
      deleteLock(tx, lockId);
      tx.commit();
    }
  }

  @Override
  public boolean lockExists(final String lockId) {
    return lockDAO.getById(lockId) != null;
  }
}
