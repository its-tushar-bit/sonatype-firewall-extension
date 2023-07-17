/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantMigratorTest
    extends MultiTenantTestSupport
{
  @Rule
  public final LogOutput logOutput = new LogOutput(TenantMigrator.class);

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

  @Test
  public void shouldRunMigrationsForExistingTenants() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(Arrays.asList("t_tenant_1", "t_tenant_z", "t_tenant_2", "t_tenant_a"));

      assertMigrationExecutedForTheExpectedNumberOfTenants(4);

      // used to assert sort order
      assertThat(logOutput).atInfoLevel()
          .contains("Total of 4 tenants to migrate: [tenant-1, tenant-2, tenant-a, tenant-z]");
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
  public void shouldThrowError_withTenantName_whenTenantMigrationThrows() {
    testAsGlobalTenant(global -> {
      List<String> expectedSchemaList = Arrays.asList("t_tenant_1", "global");

      when(operationalDataStore.getDataSource()).thenReturn(dataSource);
      OperationalDataStoreProvider.setInstance(operationalDataStore);

      try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
        dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(dataSource)).thenReturn(expectedSchemaList);

        doThrow(new RuntimeException()).when(databaseProvisionUtils)
            .initializeDatabases(any(InsightConfig.class), any(DatabaseConfigProvider.class));

        assertThatThrownBy(underTest::migrateAllSchemas).isInstanceOf(
            RuntimeException.class).hasMessage("Error trying to migrate the database for tenant: tenant-1.");
      }
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
