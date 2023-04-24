/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantMigratorTest
    extends MultiTenantTestSupport
{
  @Mock
  private MultiTenantInsightConfig insightConfig;

  @Mock
  private DatabaseProvisionUtils databaseProvisionUtils;

  @Mock
  OperationalDataStore operationalDataStore;

  @Mock
  DataSource dataSource;

  private TenantMigrator underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    underTest = new TenantMigrator(databaseProvisionUtils, insightConfig);
  }

  @AfterClass
  public static void tearDown() {
    OperationalDataStoreProvider.setInstance(null);
    AggregationDataStoreProvider.setInstance(null);
    ThirdPartyScansProvider.setInstance(null);
    DatamartProvider.setInstance(null);
  }

  @Test
  public void shouldRunMigrationsForExistingTenants() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(Arrays.asList("t_tenant_1", "t_tenant_2"));

      assertMigrationExecutedForTheExpectedNumberOfTenants(2);
    });
  }

  @Test
  public void shouldNotRunMigrations_forNonTenantsSchemas() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(Arrays.asList("t_tenant_1", "global", "public", "postgres"));

      assertMigrationExecutedForTheExpectedNumberOfTenants(1);
    });
  }

  @Test
  public void shouldNotRunMigrations_whenNoTenantSchemasExist() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(Arrays.asList("global", "public"));

      verify(databaseProvisionUtils).initializeDatabasesWithoutMigration(
          any(MultiTenantDatabaseConfigProvider.class));
      verify(databaseProvisionUtils, never()).initializeDatabases(any(MultiTenantInsightConfig.class),
          any(MultiTenantDatabaseConfigProvider.class));
    });
  }

  private void runMigrateAllSchemas(List<String> expectedSchemaList) {
    when(operationalDataStore.getDataSource()).thenReturn(dataSource);
    OperationalDataStoreProvider.setInstance(operationalDataStore);

    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(dataSource)).thenReturn(expectedSchemaList);
      underTest.migrateAllSchemas();
    }
  }

  private void assertMigrationExecutedForTheExpectedNumberOfTenants(int numberOfTenants) {
    verify(databaseProvisionUtils).initializeDatabasesWithoutMigration(
        any(MultiTenantDatabaseConfigProvider.class));
    verify(databaseProvisionUtils, times(numberOfTenants)).initializeDatabases(any(MultiTenantInsightConfig.class),
        any(MultiTenantDatabaseConfigProvider.class));
  }
}
