/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.MultiTenantDatabaseTestSupport;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MigrateTenantsCommandTest
    extends MultiTenantDatabaseTestSupport
{
  // system under test
  private MigrateTenantsCommand spyMigrateTenantsCommand;

  private DatabaseProvisionUtils spyDatabaseProvisionUtils;

  private MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection;

  @Before
  @Override
  public void setUp() {
    super.setUp();

    spyDatabaseProvisionUtils = spy(multiTenantDatabaseTestRule.databaseProvisionUtils);

    spyMigrateTenantsCommand = spy(new MigrateTenantsCommand()
    {
      @Override
      public DatabaseContainer createDatabaseContainer() {
        return new DatabaseContainer(
            multiTenantDatabaseTestRule.multiTenantDataSourceFactory, spyDatabaseProvisionUtils
        );
      }
    });

    OperationalDataStore operationalDataStore = multiTenantDatabaseTestRule.operationalDataStore;
    multiTenantGlobalSchemaProtection =
        new MultiTenantGlobalSchemaProtection(operationalDataStore);
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
    multiTenantGlobalSchemaProtection.createWriteProtection();
    provisionNewTenant();

    testAsGlobalTenant(g -> {
      spyMigrateTenantsCommand.run(null, null, multiTenantDatabaseTestRule.insightConfig);

      verify(spyDatabaseProvisionUtils, times(3)).initializeDatabasesWithoutMigration(
          any(MultiTenantDatabaseConfigProvider.class));
      verify(spyDatabaseProvisionUtils, times(2)).initializeDatabases(any(MultiTenantInsightConfig.class),
          any(MultiTenantDatabaseConfigProvider.class));
      verify(spyDatabaseProvisionUtils, times(2)).migrateDatabasesIfNeeded(any(MultiTenantInsightConfig.class));
    });
  }
}
