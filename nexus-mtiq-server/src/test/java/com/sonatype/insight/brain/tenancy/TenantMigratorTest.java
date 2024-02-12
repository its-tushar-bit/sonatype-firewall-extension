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
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.test.LogOutput;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TenantMigratorTest
    extends AbstractMultiTenantDatabaseTest
{
  @Rule
  public final LogOutput logOutput = new LogOutput(TenantMigrator.class);

  private TenantMigrator underTest;

  private DatabaseProvisioner spyDatabaseProvisioner;

  private MultiTenantGlobalSchemaProtection spyMultiTenantGlobalSchemaProtection;

  @Before
  @Override
  public void setup() {
    super.setup();
    spyDatabaseProvisioner = databaseRule.getDatabaseContainer().getDatabaseProvisioner();
    spyMultiTenantGlobalSchemaProtection =
        spy(new MultiTenantGlobalSchemaProtection(databaseRule.getOperationalDataStore()));
    underTest = new TenantMigrator(spyDatabaseProvisioner, spyMultiTenantGlobalSchemaProtection);

    spyMultiTenantGlobalSchemaProtection.createWriteProtection();
  }

  @After
  public void after() {
    // this test migrates test tenants and we need clean/new tenants for each run
    databaseRule.markDatabaseAsDirty();
  }

  @Test
  public void shouldRunMigrationsForGlobalSchema() {
    underTest.migrateGlobalSchema();

    verify(spyMultiTenantGlobalSchemaProtection).disableWriteProtection();
    verify(spyMultiTenantGlobalSchemaProtection).enableWriteProtection();
    verify(spyDatabaseProvisioner).initializeDatabaseWithMigration();
  }

  @Test
  public void shouldThrowError_whenGlobalSchemaMigrationThrows() {
    doThrow(new RuntimeException()).when(spyDatabaseProvisioner)
        .initializeDatabaseWithMigration();

    assertThatThrownBy(underTest::migrateGlobalSchema).isInstanceOf(
        RuntimeException.class).hasMessage("Error trying to migrate the database for Global Schema.");

    verify(spyMultiTenantGlobalSchemaProtection).enableWriteProtection();
  }

  @Test
  public void shouldThrowError_whenMultiTenantGlobalSchemaProtection_disableWriteProtection_Throws() {
    doThrow(new RuntimeException("Error trying to disable write protection for MultiTenant Global schema."))
        .when(spyMultiTenantGlobalSchemaProtection).disableWriteProtection();

    assertThatThrownBy(underTest::migrateGlobalSchema).isInstanceOf(
        RuntimeException.class).hasMessage("Error trying to disable write protection for MultiTenant Global schema.");
  }

  @Test
  public void shouldThrowError_whenMultiTenantGlobalSchemaProtection_enableWriteProtection_Throws() {
    doThrow(new RuntimeException("Error trying to enable write protection for MultiTenant Global schema."))
        .when(spyMultiTenantGlobalSchemaProtection).enableWriteProtection();

    assertThatThrownBy(underTest::migrateGlobalSchema).isInstanceOf(
        RuntimeException.class).hasMessage("Error trying to enable write protection for MultiTenant Global schema.");
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

      try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class, CALLS_REAL_METHODS)) {
        dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(any(DataSource.class))).thenReturn(expectedSchemaList);

        doThrow(new RuntimeException()).when(spyDatabaseProvisioner).initializeDatabaseWithMigration();

        assertThatThrownBy(underTest::migrateAllSchemas).isInstanceOf(
            RuntimeException.class).hasMessage("Error trying to migrate the database for tenant: tenant-1.");
      }
    });
  }

  @Test
  public void shouldNotRunMigrations_whenNoTenantSchemasExist() {
    testAsGlobalTenant(global -> {
      runMigrateAllSchemas(Arrays.asList("global", "public"));

      verify(spyDatabaseProvisioner, never()).initializeDatabaseWithMigration();
    });
  }

  private void runMigrateAllSchemas(List<String> expectedSchemaList) {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class, CALLS_REAL_METHODS)) {
      dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(any(DataSource.class))).thenReturn(expectedSchemaList);
      underTest.migrateAllSchemas();
    }
  }

  private void assertMigrationExecutedForTheExpectedNumberOfTenants(int numberOfTenants) {
    verify(spyDatabaseProvisioner, times(numberOfTenants)).initializeDatabaseWithoutMigration();
    verify(spyDatabaseProvisioner, times(numberOfTenants)).initializeDatabaseWithMigration();
  }
}
