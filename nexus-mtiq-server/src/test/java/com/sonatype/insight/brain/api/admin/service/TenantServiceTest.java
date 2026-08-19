/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TenantServiceTest
    extends AbstractMultiTenantTest
{
  @Mock
  OperationalDataStore mockOperationalDataStore;

  @Mock
  DataSource dataSource;

  private TenantService underTest;

  @BeforeEach
  public void setup() {
    underTest = new TenantService(mockOperationalDataStore);

    when(mockOperationalDataStore.getDataSource()).thenReturn(dataSource);
  }

  @Test
  public void shouldReturnEmptyTenantListWhenThereAreNoTenants() {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.getTenantSchemas(dataSource)).thenReturn(emptyList());

      List<String> tenants = underTest.getAllTenantsNames();

      assertThat(tenants).isEmpty();
    }
  }

  @Test
  public void shouldReturnCorrectTenantListForAllTenants() {
    List<String> schemaList = Arrays.asList("t_tenant_1", "t_tenant_2");

    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.getTenantSchemas(dataSource)).thenReturn(schemaList);

      List<String> tenants = underTest.getAllTenantsNames();

      assertThat(tenants).hasSize(2).containsExactlyInAnyOrder("tenant-1", "tenant-2");
    }
  }
}
