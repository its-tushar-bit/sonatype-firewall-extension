/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.rule;

import java.lang.annotation.Annotation;

import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.brain.db.fixture.postgres.MultiTenantPostgresDatabaseFixture;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantUtil;

public class MultiTenantDatabaseContainerRule
    extends DatabaseContainerRule
{
  private static final MultiTenantDatabaseContainerRule INSTANCE = new MultiTenantDatabaseContainerRule();

  private static Class<?> lastTestClass;

  private final TenantUtil tenantUtil = new TenantUtil();

  private static final PostgresTest DEFAULT_MTIQ_POSTGRES_TEST = new PostgresTest()
  {
    @Override
    public Class<? extends Annotation> annotationType() {
      return PostgresTest.class;
    }

    @Override
    public boolean suppressMigrations() {
      return false;
    }

    @Override
    public boolean cleanDatabase() {
      return false;
    }

    @Override
    public int maxConnections() {
      return 50;
    }
  };

  private MultiTenantDatabaseContainerRule() {
    // private constructor
  }

  public static MultiTenantDatabaseContainerRule getInstance() {
    return INSTANCE;
  }

  @Override
  public void before() throws Throwable {
    if (!tenantUtil.isMultiTenant()) {
      throw new UnsupportedOperationException("Multi-tenant tests require multi-tenant mode. Did you set the tenant?");
    }

    // Track test class changes to force fixture re-creation when a different test class runs.
    // This is critical for fork reuse (reuseForks=true) where multiple test classes may run
    // sequentially in the same JVM, and we need to ensure each test class gets a clean database
    // without leftover tenant schemas from previous test classes.
    if (currentTestClass != null && currentTestClass != lastTestClass) {
      log.info("Test class changed from '{}' to '{}'. Marking fixture as dirty.",
          lastTestClass != null ? lastTestClass.getSimpleName() : "null",
          currentTestClass.getSimpleName());
      markFixtureAsDirty();
    }
    lastTestClass = currentTestClass;

    super.before();
  }

  @Override
  protected void createNewDataStores() {
    MultiTenantPostgresDataSourceProvider multiTenantPostgresDataSourceProvider =
        (MultiTenantPostgresDataSourceProvider) getDataSourceProvider();
    this.operationalDataStore = new MultiTenantOperationalDataStore(multiTenantPostgresDataSourceProvider,
        getDatabaseConfig(DatabaseName.ods.name()));
    this.aggregationDataStore = new MultiTenantAggregationDataStore(multiTenantPostgresDataSourceProvider,
        getDatabaseConfig(DatabaseName.aggregation.name()));
    this.dataMartDataStore = new MultiTenantDataMartDataStore(multiTenantPostgresDataSourceProvider,
        getDatabaseConfig(DatabaseName.dm.name()));
    this.thirdPartyScansDataStore = new MultiTenantThirdPartyScansDataStore(multiTenantPostgresDataSourceProvider,
        getDatabaseConfig(DatabaseName.third_party_scans.name()));
  }

  @Override
  protected DatabaseFixture createNewFixture() {
    if (DatabaseRuleAnnotations.isH2InMemoryTest(annotation) || DatabaseRuleAnnotations.isH2DiskTest(annotation)) {
      throw new RuntimeException("MTIQ tests cannot use H2");
    }
    PostgresTest postgresTest = DatabaseRuleAnnotations.getPostgresTest(annotation);
    if (postgresTest == null) {
      postgresTest = DEFAULT_MTIQ_POSTGRES_TEST;
    }
    return new MultiTenantPostgresDatabaseFixture(testName, postgresTest);
  }

  @Override
  protected DatabaseType getType() {
    return DatabaseType.POSTGRES_DB;
  }

  public void provisionDatabaseForTenant(Tenant tenant) {
    TenantTestHelper.testAsTenant(tenant, t -> {
      cloneTenant(tenant.databaseSchema);

      DatabaseProvisioner databaseProvisioner = getDatabaseContainer().getDatabaseProvisioner();
      databaseProvisioner.initializeDatabaseWithMigration();
    });
  }

  private void cloneTenant(final String tenantName) {
    MultiTenantPostgresDatabaseFixture localFixture = (MultiTenantPostgresDatabaseFixture) fixture;
    localFixture.cloneTenant(tenantName);
  }

  public void setTestName(final String testName) {
    this.testName = testName;
  }
}
