/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import org.junit.Ignore;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.MultiTenantDatabaseContainer;
import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
@Ignore // CLM-39084
public class MigrateTenantsCommandTest
    extends AbstractMultiTenantDatabaseTest
{
  // system under test
  private MigrateTenantsCommand spyMigrateTenantsCommand;

  private DatabaseProvisioner spyDatabaseProvisioner;

  private TenantService tenantService;

  @Before
  @Override
  public void setup() {
    super.setup();

    tenantService = new TenantService(databaseRule.getOperationalDataStore());

    // get the spy out of TestDatabaseContainer
    spyDatabaseProvisioner = databaseRule.getDatabaseContainer().getDatabaseProvisioner();

    spyMigrateTenantsCommand = spy(new MigrateTenantsCommand()
    {
      @Override
      public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
        return new MultiTenantDatabaseContainer(
            (MultiTenantPostgresDataSourceProvider) databaseRule.getDataSourceProvider(), spyDatabaseProvisioner,
            databaseRule.getOperationalDataStore(), databaseRule.getAggregationDataStore(),
            databaseRule.getDataMartDataStore(), databaseRule.getThirdPartyScansDataStore());
      }
    });
  }

  @Test
  public void testMtiqDbMigrationCommand() {
    assertThat(spyMigrateTenantsCommand.getName()).isEqualTo("migrate-mtiq-db");
    assertThat(spyMigrateTenantsCommand.getDescription()).isEqualTo(
        "Migrates the database to the latest schema version for the Global schema and all tenants.");
  }

  @Test
  public void testOnError() {
    testAsNewTenant(tenant -> {
      assertThatThrownBy(() -> spyMigrateTenantsCommand.onError(null, null, new Exception("Error"))).isInstanceOf(
          IllegalStateException.class).hasMessage("Error running tenants database migrations.");
    });
  }

  @Test
  public void testRunMigration() {
    // Provision at least one new tenant
    provisionTestTenant();

    // Reset the counts after provisioning
    Mockito.reset(spyDatabaseProvisioner);

    // Get current total tenant count
    int currentTenantCount = tenantService.getAllTenantsNames().size();

    testAsGlobalTenant(g -> {
      MultiTenantInsightConfig insightConfig = new MultiTenantInsightConfig();
      spyMigrateTenantsCommand.run(null, null, insightConfig);

      // One for each tenant and +1 for the global one executed in TenantMigrate.migrateAllSchemas
      verify(spyDatabaseProvisioner, times(currentTenantCount + 2)).initializeDatabaseWithoutMigration();
      verify(spyDatabaseProvisioner, times(currentTenantCount + 1)).initializeDatabaseWithMigration();
      verify(spyDatabaseProvisioner, times(currentTenantCount + 1)).migrateDatabase();
    });
  }
}
