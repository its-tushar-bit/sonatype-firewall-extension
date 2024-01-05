/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import javax.inject.Inject;
import javax.inject.Provider;

import com.sonatype.insight.brain.dataaccess.LockDAO;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

public class ClusterLockManagerProvider
    implements Provider<ClusterLockManager>
{
  private final OperationalDataStore operationalDataStore;

  private final LockDAO lockDAO;

  @Inject
  public ClusterLockManagerProvider(final OperationalDataStore operationalDataStore, final LockDAO lockDAO) {
    this.operationalDataStore = operationalDataStore;
    this.lockDAO = lockDAO;
  }

  @Override
  public ClusterLockManager get() {
    if (DatabaseUtil.isDatabaseEmbedded(operationalDataStore.getDatabaseConfig())) {
      return new H2ClusterLockManager();
    }
    else {
      return new PostgresClusterLockManager(operationalDataStore, lockDAO);
    }
  }
}
