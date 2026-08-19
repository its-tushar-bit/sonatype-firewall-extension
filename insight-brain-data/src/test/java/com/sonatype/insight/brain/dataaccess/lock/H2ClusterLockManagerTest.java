/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class H2ClusterLockManagerTest
    extends AbstractClusterLockManagerTest
{
  private H2ClusterLockManager h2ClusterLockManager;

  @Override
  protected ClusterLockManager createClusterLockManager() {
    this.h2ClusterLockManager = new H2ClusterLockManager();
    return h2ClusterLockManager;
  }

  @Override
  protected ClusterLock createClusterLock(ClusterLockId clusterLockId) {
    return h2ClusterLockManager.createClusterLock(clusterLockId);
  }

  @Test
  public void testConstructor_H2() {
    ClusterLockId clusterLockId = ClusterLockId.forDataMigration();
    try (ClusterLock clusterLock1 = createClusterLock(clusterLockId);
        ClusterLock clusterLock2 = createClusterLock(clusterLockId))
    {
      assertThat(clusterLock1.getClusterLockId()).isEqualTo(clusterLock2.getClusterLockId());
    }
  }
}
