/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
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
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TestRestTenantUtil;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;
import static org.mockito.Mockito.when;

public class MultiTenantBrainServiceTestHelper
{
  private static final TestRestTenantUtil tenantUtil = new TestRestTenantUtil();

  private static final Logger log = LoggerFactory.getLogger(MultiTenantBrainServiceTestHelper.class);

  private static PostgresServer postgresServer;

  private static Tenant testTenant = null;

  /**
   * setup will initialize MultiTenantBrainServiceTestFactory ready for multi tenant testing with
   * AbstractBrainServiceTest.
   */
  public static void setup() {
    MultiTenantBrainServiceTestService.setup(
        TestMultiTenantInsightBrainService.class,
        MultiTenantBrainServiceTestHelper::createDatabaseContainer,
        MultiTenantBrainServiceTestHelper::beforeTest,
        MultiTenantBrainServiceTestHelper::afterTest,
        MultiTenantBrainServiceTestHelper::shutdownMtiq,
        MultiTenantBrainServiceTestHelper.addBrainModules()
    );
  }

  /**
   * createDatabaseContainer will initialize DatabaseContainer ready for multi tenant testing MTIQ with
   * AbstractBrainServiceTest. This is called by initDatabaseContainer in AbstractBrainServiceTest.
   */
  public static DatabaseContainer createDatabaseContainer() {
    if (postgresServer == null) {
      // Reuse the PostgresServer as it is slow to start.
      postgresServer = new PostgresServer(PostgresServer.MTIQ_IMAGE_VERSION);
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

    DatabaseConfig mainDbConfig = postgresServer.getDatabaseConfig();
    DatabaseConfig locksDbConfig = postgresServer.getDatabaseConfig();

    MultiTenantInsightConfig insightConfig = new MultiTenantInsightConfig();
    insightConfig.setMainDatabase(mainDbConfig);
    insightConfig.setLocksDatabase(locksDbConfig); // for testing use the same database for locks

    multiTenantDataSourceFactory.setInsightConfig(insightConfig);

    // Reuse the DatabaseContainer and Postgres instance for MTIQ
    DatabaseContainer mtiqDatabaseContainer = new DatabaseContainer(multiTenantDataSourceFactory,
        databaseProvisionUtils);

    MultiTenantBrainServiceTestService.setDatabaseContainer(mtiqDatabaseContainer);

    // Reuse the configurator to allow reuse of MTIQ server DB settings
    MultiTenantBrainServiceTestService.setConfigurator(config -> {
      MultiTenantInsightConfig mtiqConfig = (MultiTenantInsightConfig) config;
      mtiqConfig.setDeleteBuiltInAdmin(false);
      mtiqConfig.setMainDatabase(mainDbConfig);
      mtiqConfig.setLocksDatabase(locksDbConfig);
    });

    return mtiqDatabaseContainer;
  }

  /**
   * setBrainModules adds MTIQ modules into AbstractBrainServiceTest.
   */
  public static AbstractModule addBrainModules() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        bind(TenantUtil.class).toInstance(tenantUtil);
      }
    };
  }

  /**
   * beforeTest will setup the mock JWT auth handling for AbstractBrainServiceTest and create, provision
   * and set the tenant url filter to a new tenant ready for the test.
   * Should be call on @before.
   * @param brainServiceTest AbstractBrainServiceTest for test setup.
   */
  public static void beforeTest(AbstractBrainServiceTest brainServiceTest) {
    TestInsightBrainServiceRule clmServer = brainServiceTest.getCLMServer();
    MultiTenantJwkProvider multiTenantJwkTestProvider = clmServer.getInstance(MultiTenantJwkProvider.class);

    try {
      String jwt = AuthorizationTestHelper.createJwt();
      DecodedJWT decodedJWT = JWT.decode(jwt);
      Jwk jwk = AuthorizationTestHelper.createJwk(decodedJWT.getKeyId());

      when(multiTenantJwkTestProvider.denyRequest()).thenReturn(false);
      when(multiTenantJwkTestProvider.getJsonWebKey(decodedJWT.getKeyId())).thenReturn(jwk);
      when(multiTenantJwkTestProvider.getIssuer()).thenReturn(decodedJWT.getIssuer());
    }
    catch (Exception e) {
      log.error("Failed to setup mock JWT for TestMultiTenantInsightBrainService", e);
    }

    testTenant = TenantTestHelper.setupNewTestTenant(brainServiceTest.testName);
    try {
      provisionTenant(brainServiceTest, testTenant.tenantSlug);
    }
    catch (Exception e) {
      log.error("Failed to provision MTIQ tenant", e);
    }
    setTenantBySlug(testTenant.tenantSlug);
  }

  /**
   * The global tenant is used for cleaning up TemporaryEntity. Should be call on @after.
   * @param brainServiceTest AbstractBrainServiceTest for test clear down.
   */
  public static void afterTest(AbstractBrainServiceTest brainServiceTest) {
    testTenant = null;
    TenantTestHelper.setGlobalTenant();
  }

  /**
   * getTestTenant returns the pre provisioned tenant ready for testing.
   */
  public static Tenant getTestTenant() {
    return testTenant;
  }

  /**
   * This results in slower tests but means we get a clean slate after each MTIQ Integration test and prevents MTIQ
   * implementations leaking into non-MTIQ tests.
   * Should be called on @afterClass.
   */
  public static void shutdownMtiq(TestCLMServer testCLMServer) {
    if (testCLMServer != null) {
      try {
        testCLMServer.stop();
      }
      catch (Exception e) {
        log.error("Failed to stop MTIQ", e);
      }
    }

    if (postgresServer != null) {
      postgresServer.close();
      postgresServer = null;
    }

    TenantTestHelper.resetAfterTest();

    DatabaseContainer mtiqDatabaseContainer = MultiTenantBrainServiceTestService.getDatabaseContainer();
    if (mtiqDatabaseContainer != null) {
      MultiTenantDataSourceFactory factory =
          (MultiTenantDataSourceFactory) mtiqDatabaseContainer.getDataSourceFactory();
      factory.setInsightConfig(null);
    }

    MultiTenantBrainServiceTestService.resetTestInstances();

    tenantUtil.setTenantSlug(null);
  }

  /**
   * stop will remove MTIQ overrides and allow AbstractBrainServiceTest to be run as normal for single tenant tests.
   * Must be called once the MTIQ integration tests have finished.
   */
  public static void stop() {
    MultiTenantBrainServiceTestService.stop();
    shutdownMtiq(null);
  }

  public static void setTenantBySlug(String tenantSlug) {
    tenantUtil.setTenantSlug(tenantSlug);
  }

  public static HttpResponse provisionTenant(AbstractBrainServiceTest brainServiceTest, String tenantName)
      throws Exception
  {
    return brainServiceTest.adminRequest()
        .path("api/")
        .path(ADMIN_TENANT_PROVISIONING_PATH)
        .parameter(tenantName)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt())
        .post();
  }
}
