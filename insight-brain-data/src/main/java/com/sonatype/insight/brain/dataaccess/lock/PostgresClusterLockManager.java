/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

/**
 * A Postgres implementation of {@link ClusterLockManager}. Locks are stored in the database and clustering is fully
 * supported.
 */
public class PostgresClusterLockManager
    extends AbstractClusterLockManager
{
  private final OperationalDataStore operationalDataStore;

  private final PostgresAdvisoryLockDAO advisoryLockDAO;

  public PostgresClusterLockManager(
      final OperationalDataStore operationalDataStore,
      final PostgresAdvisoryLockDAO advisoryLockDAO)
  {
    this.operationalDataStore = operationalDataStore;
    this.advisoryLockDAO = advisoryLockDAO;
  }

  @Override
  public ClusterLock createClusterLock(ClusterLockId clusterLockId) {
    ClusterLock lock = new PostgresClusterLock(clusterLockId, operationalDataStore, advisoryLockDAO);
    return lock;
  }
}
