/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.MultiTenantDataSourceFactory;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantDbMigrationCommandTest
{
  // system under test
  private MultiTenantDbMigrationCommand multiTenantDbMigrationCommand;

  @BeforeClass
  public static void beforeClass() {
    new TenantUtil().setGlobalTenant();
  }

  @Test
  public void testQuartzTableDoesExist() {
    try (PostgresServer postgresServer = new PostgresServer()) {
      // do migration (setup global schema) and global quartz table does exist
      setupTest(postgresServer.getDatabaseConfig(), true);
      assertThat(multiTenantDbMigrationCommand.quartzSchedulerStateTableExists()).isTrue();
    }
  }

  @Test
  public void testQuartzTableDoesNotExist() {
    try (PostgresServer postgresServer = new PostgresServer()) {
      // do NOT DO migration (setup global schema) and global quartz table DOES NOT exist
      setupTest(postgresServer.getDatabaseConfig(), false);
      assertThat(multiTenantDbMigrationCommand.quartzSchedulerStateTableExists()).isFalse();
    }
  }

  private void setupTest(final DatabaseConfig databaseConfig, final boolean migrate) {
    MultiTenantInsightConfig insightConfig = new MultiTenantInsightConfig();
    insightConfig.setMainDatabase(databaseConfig);
    insightConfig.setLocksDatabase(databaseConfig); // for testing use the same config for locks

    MultiTenantDataSourceFactory dataSourceFactory = new MultiTenantDataSourceFactory();
    dataSourceFactory.setInsightConfig(insightConfig);

    DatabaseMigrator databaseMigrator = new DatabaseMigrator(dataSourceFactory);

    // only need ODS for this test
    OperationalDataStore operationalDataStore =
        new MultiTenantOperationalDataStore(dataSourceFactory, databaseMigrator);
    OperationalDataStoreProvider.setInstance(operationalDataStore);

    if (migrate) {
      operationalDataStore.initWithMigration(databaseConfig, true);
    }
    else {
      operationalDataStore.initWithoutMigration(databaseConfig);
    }

    multiTenantDbMigrationCommand = new MultiTenantDbMigrationCommand(
        new DatabaseProvisionUtils(operationalDataStore, null, null, null));
  }
}
