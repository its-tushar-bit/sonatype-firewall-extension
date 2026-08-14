/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TenantMigratorTest
    extends AbstractMultiTenantDatabaseTest
{
  private TenantMigrator underTest;

  private DatabaseProvisioner spyDatabaseProvisioner;

  @Override
  public void setup() {
    super.setup();
    spyDatabaseProvisioner = databaseRule.getDatabaseContainer().getDatabaseProvisioner();
    underTest = new TenantMigrator(spyDatabaseProvisioner);
  }

  @AfterEach
  public void after() {
    // this test migrates test tenants and we need clean/new tenants for each run
    databaseRule.markFixtureAsDirty();
  }

  @Test
  public void shouldRunMigrationsForGlobalSchema() {
    underTest.migrateGlobalSchema();

    verify(spyDatabaseProvisioner).initializeDatabaseWithMigration();
  }

  @Test
  public void shouldThrowError_whenGlobalSchemaMigrationThrows() {
    doThrow(new RuntimeException()).when(spyDatabaseProvisioner)
        .initializeDatabaseWithMigration();

    assertThatThrownBy(underTest::migrateGlobalSchema)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Error trying to migrate the database for Global Schema.");
  }

  @Test
  public void shouldRunMigrationsForExistingTenants() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(Arrays.asList("t_tenant_1", "t_tenant_z", "t_tenant_2", "t_tenant_a"));

      assertMigrationExecutedForTheExpectedNumberOfTenants(4);
    });
  }

  @Test
  public void shouldNotRunMigrations_forNonTenantsSchemas() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(List.of("t_tenant_1"));

      assertMigrationExecutedForTheExpectedNumberOfTenants(1);
    });
  }

  @Test
  public void shouldThrowError_withTenantName_whenTenantMigrationThrows() {
    testAsGlobalTenant(global -> {
      List<String> expectedSchemaList = List.of("t_tenant_1");

      try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class, CALLS_REAL_METHODS)) {
        dataBaseUtil.when(() -> DatabaseUtil.getTenantSchemas(any(DataSource.class))).thenReturn(expectedSchemaList);

        doThrow(new RuntimeException()).when(spyDatabaseProvisioner).initializeDatabaseWithMigration();

        assertThatThrownBy(underTest::migrateAllSchemas)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("tenant-1");
      }
    });
  }

  @Test
  public void shouldNotRunMigrations_whenNoTenantSchemasExist() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(List.of());

      verify(spyDatabaseProvisioner, never()).initializeDatabaseWithMigration();
    });
  }

  private void runMigrateAllSchemas(List<String> expectedSchemaList) {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class, CALLS_REAL_METHODS)) {
      dataBaseUtil.when(() -> DatabaseUtil.getTenantSchemas(any(DataSource.class))).thenReturn(expectedSchemaList);
      underTest.migrateAllSchemas();
    }
  }

  private void assertMigrationExecutedForTheExpectedNumberOfTenants(int numberOfTenants) {
    verify(spyDatabaseProvisioner, times(numberOfTenants)).initializeDatabaseWithoutMigration();
    verify(spyDatabaseProvisioner, times(numberOfTenants)).initializeDatabaseWithMigration();
  }
}
