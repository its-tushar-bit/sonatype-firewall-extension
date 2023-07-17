/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
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
public class MtiqDbMigrationCommandTest
    extends MultiTenantDatabaseTestSupport
{
  // system under test
  private MtiqDbMigrationCommand mtiqDbMigrationCommand;

  private DatabaseProvisionUtils spyDatabaseProvisionUtils;

  @Before
  @Override
  public void setUp() {
    super.setUp();

    spyDatabaseProvisionUtils = spy(multiTenantDatabaseTestRule.databaseProvisionUtils);

    mtiqDbMigrationCommand = new MtiqDbMigrationCommand(
        new DatabaseContainer(multiTenantDatabaseTestRule.multiTenantDataSourceFactory, spyDatabaseProvisionUtils));
  }

  @Test
  public void testMtiqDbMigrationCommand() {
    assertThat(mtiqDbMigrationCommand.getName()).isEqualTo("migrate-mtiq-db");
    assertThat(mtiqDbMigrationCommand.getDescription()).isEqualTo(
        "Migrates the database to the latest schema version for all MTIQ tenants.");
  }

  @Test
  public void testOnError() {
    testAsNewTenant(tenant -> {
      assertThatThrownBy(() -> mtiqDbMigrationCommand.onError(null, null, new Exception("Error"))).isInstanceOf(
          IllegalStateException.class).hasMessage("Error running tenant database migrations.");
    });
  }

  @Test
  public void testRunMigration() {
    provisionNewTenant();

    testAsGlobalTenant(g -> {
      mtiqDbMigrationCommand.run(null, null, multiTenantDatabaseTestRule.insightConfig);

      verify(spyDatabaseProvisionUtils, times(2)).initializeDatabasesWithoutMigration(
          any(MultiTenantDatabaseConfigProvider.class));
      verify(spyDatabaseProvisionUtils).initializeDatabases(any(MultiTenantInsightConfig.class),
          any(MultiTenantDatabaseConfigProvider.class));
      verify(spyDatabaseProvisionUtils).migrateDatabasesIfNeeded(any(MultiTenantInsightConfig.class));
    });
  }
}
