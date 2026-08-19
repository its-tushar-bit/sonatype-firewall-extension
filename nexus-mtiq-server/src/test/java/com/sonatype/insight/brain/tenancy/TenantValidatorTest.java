/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantValidatorTest
{
  static final String TENANT_DB_SCHEMA = "t_tenant";

  static final String TENANT_NAME = "tenant";

  @Mock
  OperationalDataStore operationalDataStore;

  @Mock
  DataSource dataSource;

  TenantValidator underTest;

  Tenant tenant;

  @BeforeEach
  public void setup() {
    tenant = new Tenant("tenant");
    underTest = new TenantValidator(operationalDataStore);
  }

  @Test
  public void shouldReturnTrue_whenTenantExists() {
    when(operationalDataStore.getDataSourceWithoutInit()).thenReturn(dataSource);

    boolean result = runValidateTenantExists(TENANT_DB_SCHEMA, true);

    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnFalse_whenTenantDoesntExists() {
    when(operationalDataStore.getDataSourceWithoutInit()).thenReturn(dataSource);

    boolean result = runValidateTenantExists(TENANT_DB_SCHEMA, false);

    assertThat(result).isFalse();
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNull() {
    assertThatThrownBy(() -> underTest.validateTenantExists((Tenant) null))
        .withFailMessage("Invalid tenant parameter")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldReturnTrue_whenTenantNameExists() {
    when(operationalDataStore.getDataSourceWithoutInit()).thenReturn(dataSource);

    boolean result = runValidateTenantNameExists(TENANT_DB_SCHEMA, true);

    assertThat(result).isTrue();
  }

  @Test
  public void shouldReturnFalse_whenTenantNameDoesntExists() {
    when(operationalDataStore.getDataSourceWithoutInit()).thenReturn(dataSource);

    boolean result = runValidateTenantNameExists(TENANT_DB_SCHEMA, false);

    assertThat(result).isFalse();
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNameNull() {
    assertThatThrownBy(() -> underTest.validateTenantExists((String) null))
        .withFailMessage("Invalid tenant parameter")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNameEmpty() {
    assertThatThrownBy(() -> underTest.validateTenantExists(""))
        .withFailMessage("Invalid tenant parameter")
        .isInstanceOf(IllegalArgumentException.class);
  }

  private boolean runValidateTenantExists(String schema, boolean expected) {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.databaseSchemaExists(dataSource, schema)).thenReturn(expected);
      return underTest.validateTenantExists(tenant);
    }
  }

  private boolean runValidateTenantNameExists(String schema, boolean expected) {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.databaseSchemaExists(dataSource, schema)).thenReturn(expected);
      return underTest.validateTenantExists(TENANT_NAME);
    }
  }
}
