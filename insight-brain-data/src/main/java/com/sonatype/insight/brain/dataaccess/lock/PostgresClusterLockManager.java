/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.dataaccess.LockDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * A Postgres implementation of {@link ClusterLockManager}. Locks are stored in the database and clustering is fully
 * supported.
 */
public class PostgresClusterLockManager
    extends AbstractClusterLockManager
{
  private final OperationalDataStore operationalDataStore;

  private final LockDAO lockDAO;

  private final PostgresAdvisoryLockDAO advisoryLockDAO;

  public PostgresClusterLockManager(
      final OperationalDataStore operationalDataStore,
      final LockDAO lockDAO,
      final PostgresAdvisoryLockDAO advisoryLockDAO)
  {
    this.operationalDataStore = operationalDataStore;
    this.lockDAO = lockDAO;
    this.advisoryLockDAO = advisoryLockDAO;
  }

  @Override
  public ClusterLock createClusterLock(ClusterLockId clusterLockId) {
    ClusterLock lock = new PostgresClusterLock(clusterLockId, operationalDataStore, lockDAO, advisoryLockDAO);
    lockDAO.createLock(lock.getLockId());
    return lock;
  }

  @Override
  protected void deleteLock(final TransactionContext tx, final ClusterLockId clusterLockId) {
    lockDAO.deleteLock(tx, clusterLockId.getOldStyleLockId());
  }

  // Note: will be removed after table based lock mechanism is removed
  @Override
  protected void deleteLocksByPrefix(final TransactionContext tx, final String prefix) {
    lockDAO.deleteByPrefix(tx, prefix);
  }

  @Override
  public void deleteFor(final ClusterLockId clusterLockId) {
    try (TransactionContext tx = lockDAO.createTransactionContext()) {
      tx.begin();
      deleteLock(tx, clusterLockId);
      tx.commit();
    }
  }

  @Override
  public boolean lockExists(final ClusterLockId clusterLockId) {
    return lockDAO.getById(clusterLockId.getOldStyleLockId()) != null;
  }
}
