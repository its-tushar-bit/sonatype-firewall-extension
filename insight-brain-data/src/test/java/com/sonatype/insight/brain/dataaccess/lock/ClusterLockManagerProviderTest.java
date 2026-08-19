/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClusterLockManagerProviderTest
{
  @Mock
  public OperationalDataStore operationalDataStore;

  @Mock
  private PostgresAdvisoryLockDAO postgresAdvisoryLockDAO;

  @Test
  public void testH2() {
    test("org.h2.Driver", H2ClusterLockManager.class);
  }

  @Test
  public void testPostgres() {
    test("org.postgresql.Driver", PostgresClusterLockManager.class);
  }

  private void test(final String driverClassName, final Class<? extends ClusterLockManager> clazz) {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName(driverClassName);

    when(operationalDataStore.getDatabaseConfig()).thenReturn(databaseConfig);

    ClusterLockManagerProvider clusterLockManagerProvider = new ClusterLockManagerProvider(operationalDataStore,
        postgresAdvisoryLockDAO);
    ClusterLockManager clusterLockManager = clusterLockManagerProvider.get();
    assertThat(clusterLockManager).isInstanceOf(clazz);
  }
}
