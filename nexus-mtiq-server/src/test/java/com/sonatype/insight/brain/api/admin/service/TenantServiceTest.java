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
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantServiceTest
    extends MultiTenantTestSupport
{
  @Mock
  OperationalDataStore operationalDataStore;

  @Mock
  DataSource dataSource;

  private TenantService underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    underTest = new TenantService(new TenantUtil());

    when(operationalDataStore.getDataSource()).thenReturn(dataSource);
    OperationalDataStoreProvider.setInstance(operationalDataStore);
  }

  @After
  public void after() {
    OperationalDataStoreProvider.setInstance(null);
  }

  @Test
  public void shouldReturnEmptyTenantListWhenThereAreNoTenants() {
    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(dataSource)).thenReturn(emptyList());

      List<String> tenants = underTest.getAllTenantsNames();

      assertThat(tenants).isEmpty();
    }
  }

  @Test
  public void shouldReturnCorrectTenantListForAllTenants() {
    List<String> schemaList = Arrays.asList("t_tenant_1", "t_tenant_2", "global", "public", "postgres");

    try (MockedStatic<DatabaseUtil> dataBaseUtil = mockStatic(DatabaseUtil.class)) {
      dataBaseUtil.when(() -> DatabaseUtil.getSchemasList(dataSource)).thenReturn(schemaList);

      List<String> tenants = underTest.getAllTenantsNames();

      assertThat(tenants).hasSize(2).containsExactlyInAnyOrder("tenant-1", "tenant-2");
    }
  }
}
