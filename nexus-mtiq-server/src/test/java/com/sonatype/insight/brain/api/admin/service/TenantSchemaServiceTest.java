/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantSchemaServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  private OperationalDataStore operationalDataStore;

  @Mock
  private DataMartDataStore dataMartDataStore;

  @Mock
  private DataSource operationalDataSource;

  @Mock
  private TenantValidator tenantValidator;

  @Mock
  private DatabaseProvisioner databaseProvisioner;

  private TenantSchemaService underTest;

  @BeforeEach
  public void setup() {
    underTest =
        new TenantSchemaService(operationalDataStore, dataMartDataStore, tenantValidator, databaseProvisioner);
  }

  @Test
  public void shouldGetTenantSchemaVersion() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      Map<String, Integer> result = runGetSchemaVersions(tenant);

      assertThat(result).hasSize(4);
      assertThat(result).containsEntry("insight_brain_ods", 279);
      assertThat(result).containsEntry("insight_brain_third_party_scans", 12);
      assertThat(result).containsEntry("insight_brain_aggregation", 12);
      assertThat(result).containsEntry("insight_brain_dm", 12);
    });
  }

  @Test
  public void shouldGetTenantSchemaVersion_forGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(tenantValidator.validateTenantExists(global.tenantSlug)).thenReturn(true);

      Map<String, Integer> result = runGetSchemaVersions(global);

      assertThat(result).hasSize(4);
      assertThat(result).containsEntry("insight_brain_ods", 279);
      assertThat(result).containsEntry("insight_brain_third_party_scans", 12);
      assertThat(result).containsEntry("insight_brain_aggregation", 12);
      assertThat(result).containsEntry("insight_brain_dm", 12);
    });
  }

  @Test
  public void shouldThrowRuntimeException_getTenantSchemaVersion_whenTenantDoesntExist() {
    final String errorMessage = "Tenant doesn't exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(() -> underTest.getSchemaVersions(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  @Test
  public void shouldMigrateSchema() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);

      underTest.migrateSchema(tenant.tenantSlug);

      verify(databaseProvisioner).initializeDatabaseWithMigration();
    });
  }

  @Test
  public void shouldMigrateSchema_forGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(tenantValidator.validateTenantExists(global.tenantSlug)).thenReturn(true);

      underTest.migrateSchema(global.tenantSlug);

      verify(databaseProvisioner).initializeDatabaseWithMigration();
    });
  }

  @Test
  public void shouldPassUpExceptions_migrateSchema() {
    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(true);
      doThrow(new RuntimeException()).when(databaseProvisioner).initializeDatabaseWithMigration();

      assertThatNoException().isThrownBy(() -> underTest.migrateSchema(tenant.tenantSlug));
    });
  }

  @Test
  public void shouldThrowRuntimeException_migrateSchema_whenTenantDoesntExist() {
    final String errorMessage = "Tenant doesn't exist";

    testAsNewTenant(tenant -> {
      when(tenantValidator.validateTenantExists(tenant.tenantSlug)).thenReturn(false);

      assertThatThrownBy(() -> underTest.migrateSchema(tenant.tenantSlug))
          .withFailMessage(errorMessage)
          .isInstanceOf(NotFoundException.class);
    });
  }

  private Map<String, Integer> runGetSchemaVersions(Tenant tenant) {
    lenient().when(operationalDataStore.getDataSourceWithoutInit()).thenReturn(operationalDataSource);
    lenient().when(operationalDataStore.getDatabaseSchema()).thenReturn(tenant.databaseSchema);

    lenient().when(dataMartDataStore.getID()).thenReturn("insight_brain_dm");

    Map<String, Integer> mockedSchemaVersions = new HashMap<>();
    {
      mockedSchemaVersions.put("insight_brain_ods", 279);
      mockedSchemaVersions.put("insight_brain_third_party_scans", 12);
      mockedSchemaVersions.put("insight_brain_aggregation", 12);
    }

    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.getDatabaseSchemaVersions(operationalDataSource, tenant.databaseSchema))
          .thenReturn(mockedSchemaVersions);
      dataBaseUtil.when(() -> DatabaseUtil.getLegacyDatabaseSchemaVersion(dataMartDataStore))
          .thenReturn(12);

      return underTest.getSchemaVersions(tenant.tenantSlug);
    }
  }
}
