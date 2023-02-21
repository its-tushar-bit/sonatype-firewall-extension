/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataSourceFactory;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.postgres.PostgresServer;
import com.sonatype.insight.test.networking.PortAllocator;

public abstract class AbstractMultiTenantBrainServiceTest
    extends AbstractBrainServiceTest
{
  private static PostgresServer postgresServer;

  private static DatabaseContainer mtiqDatabaseContainer;

  private static Configurator mtiqConfigurator;

  @Override
  protected void initServer(Configurator configurator) throws Exception {
    if (configurator == null) {
      // Setup the MTIQ postgres DB via the configurator
      configurator = mtiqConfigurator;
    }

    if (testCLMServer != null && !testCLMServer.isReusable(isProxyRequiredToReachHds(), configurator)) {
      testCLMServer.stop();
      testCLMServer = null;
    }

    if (testCLMServer == null) {
      testCLMServer = new TestCLMServer(isProxyRequiredToReachHds(), hdsMockServer,
          new TestMultiTenantInsightBrainServiceRule(
              PortAllocator.nextFreePort(),
              PortAllocator.nextFreePort(),
              hdsMockServer.getHttpUrl(),
              databaseContainer,
              isProxyRequiredToReachHds(),
              getBrainModules()).setConfigurator(configurator));

      testCLMServer.start();
    }
    setBaseUrl("http://localhost");
    testCLMServer.getCLMServer().setHdsUrl();
  }

  @Override
  protected void initDatabaseContainer() {
    if (mtiqDatabaseContainer != null) {
      databaseContainer = mtiqDatabaseContainer;
      return;
    }

    if (postgresServer == null) {
      postgresServer = new PostgresServer();
    }

    MultiTenantDataSourceFactory multiTenantDataSourceFactory = new MultiTenantDataSourceFactory();
    DatabaseMigrator databaseMigrator = new DatabaseMigrator(multiTenantDataSourceFactory);

    OperationalDataStore operationalDataStore =
        new MultiTenantOperationalDataStore(multiTenantDataSourceFactory, databaseMigrator);
    AggregationDataStore aggregationDataStore =
        new MultiTenantAggregationDataStore(multiTenantDataSourceFactory, databaseMigrator);
    DataMartDataStore dataMartDataStore =
        new MultiTenantDataMartDataStore(multiTenantDataSourceFactory, databaseMigrator);
    ThirdPartyScansDataStore thirdPartyScansDataStore =
        new MultiTenantThirdPartyScansDataStore(multiTenantDataSourceFactory, databaseMigrator);

    DatabaseProvisionUtils databaseProvisionUtils =
        new DatabaseProvisionUtils(operationalDataStore, aggregationDataStore, dataMartDataStore,
            thirdPartyScansDataStore);

    MultiTenantInsightConfig insightConfig = new MultiTenantInsightConfig();
    insightConfig.setMainDatabase(postgresServer.getDatabaseConfig());
    insightConfig.setLocksDatabase(postgresServer.getDatabaseConfig()); // for testing use the same config for locks
    multiTenantDataSourceFactory.setInsightConfig(insightConfig);

    // Reuse the DatabaseContainer and Postgres instance for MTIQ
    mtiqDatabaseContainer = new DatabaseContainer(multiTenantDataSourceFactory, databaseProvisionUtils);
    databaseContainer = mtiqDatabaseContainer;

    // Reuse the configurator to allow reuse of MTIQ server
    mtiqConfigurator = config -> {
      MultiTenantInsightConfig mtiqConfig = (MultiTenantInsightConfig) config;
      mtiqConfig.setMainDatabase(postgresServer.getDatabaseConfig());
      mtiqConfig.setLocksDatabase(postgresServer.getDatabaseConfig());
    };
  }

  @Override
  protected void cleanTaskScheduler() {
    //noop
  }
}
