/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.MultiTenantDatabaseContainer;
import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
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

    MultiTenantInsightConfig insightConfig = new MultiTenantInsightConfig();
    spyMigrateTenantsCommand = spy(new MigrateTenantsCommand(insightConfig)
    {
      @Override
      public DatabaseContainer createDatabaseContainer(final InsightConfig config) {
        return new MultiTenantDatabaseContainer(
            (MultiTenantPostgresDataSourceProvider) databaseRule.getDataSourceProvider(), spyDatabaseProvisioner,
            databaseRule.getOperationalDataStore(), databaseRule.getAggregationDataStore(),
            databaseRule.getDataMartDataStore(), databaseRule.getThirdPartyScansDataStore());
      }
    });
  }

  @Test
  public void testMtiqDbMigrationCommandMetadata() {
    assertThat(spyMigrateTenantsCommand.getName()).isEqualTo(MigrateTenantsCommand.NAME);
    assertThat(spyMigrateTenantsCommand.getDescription()).isEqualTo(MigrateTenantsCommand.DESCRIPTION);
  }

  @Test
  public void testOnErrorWrapsCommandFailures() {
    assertThatThrownBy(() -> spyMigrateTenantsCommand.onError(null, null, new Exception("boom")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Error running tenants database migrations.");
  }

  @Test
  public void testCreateDatabaseContainerFailsWithActionableMessageForSingleTenantConfig() {
    assertThatThrownBy(
        () -> new MigrateTenantsCommand(new MultiTenantInsightConfig()).createDatabaseContainer(new InsightConfig()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MigrateTenantsCommand.createDatabaseContainer")
            .hasMessageContaining(MultiTenantInsightConfig.class.getName())
            .hasMessageContaining(InsightConfig.class.getName())
            .hasMessageContaining("config.class=" + MultiTenantInsightConfig.class.getName());
  }

  @Test
  public void testRunMigration() throws Exception {
    // Provision at least one new tenant
    provisionTestTenant();

    // Reset the counts after provisioning
    Mockito.reset(spyDatabaseProvisioner);

    // Get current total tenant count
    int currentTenantCount = tenantService.getAllTenantsNames().size();

    testAsGlobalTenant(g -> {
      try {
        spyMigrateTenantsCommand.run();

        // One for each tenant and +1 for the global one executed in TenantMigrate.migrateAllSchemas
        verify(spyDatabaseProvisioner, times(currentTenantCount + 2)).initializeDatabaseWithoutMigration();
        verify(spyDatabaseProvisioner, times(currentTenantCount + 1)).initializeDatabaseWithMigration();
        verify(spyDatabaseProvisioner, times(currentTenantCount + 1)).migrateDatabase();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
