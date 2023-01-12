/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.Collection;
import java.util.Collections;
import javax.inject.Provider;

import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantManagerTestHelper;
import com.sonatype.insight.brain.test.MultiTenantDatabaseTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantDataStoreTest
{
  @Rule
  public MultiTenantDatabaseTestRule multiTenantDatabaseTestRule = new MultiTenantDatabaseTestRule();

  @Test
  public void testSameDataSource() {
    assertThat(multiTenantDatabaseTestRule.operationalDataStore.getDataSource())
        .isSameAs(multiTenantDatabaseTestRule.aggregationDataStore.getDataSource())
        .isSameAs(multiTenantDatabaseTestRule.dataMartDataStore.getDataSource())
        .isSameAs(multiTenantDatabaseTestRule.thirdPartyScansDataStore.getDataSource());
  }

  @Test
  public void testOdsSpecialProperties() {
    assertThat(multiTenantDatabaseTestRule.operationalDataStore.isDatabaseInMemory()).isFalse();
    assertThat(multiTenantDatabaseTestRule.operationalDataStore.isDatabaseEmbedded()).isFalse();
  }

  @Test
  public void testGlobalSchema() {
    assertThat(multiTenantDatabaseTestRule.operationalDataStore.getDatabaseSchema()).isEqualTo("global");
    assertThat(multiTenantDatabaseTestRule.aggregationDataStore.getDatabaseSchema()).isEqualTo("global");
    assertThat(multiTenantDatabaseTestRule.dataMartDataStore.getDatabaseSchema()).isEqualTo("global");
    assertThat(multiTenantDatabaseTestRule.thirdPartyScansDataStore.getDatabaseSchema()).isEqualTo("global");
  }

  @Test
  public void testTenantSchema() {
    Collection<TenantManaged> tenantManagedBeans = Collections.emptyList();
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);

    TenantManager tenantManager =
        new TenantManager(tenantManagedBeans, multiTenantDatabaseTestRule.insightConfig, tenantLifecycleProvider,
            multiTenantDatabaseTestRule.databaseProvisionUtils, multiTenantDatabaseTestRule.databaseConfigProvider);

    TenantManagerTestHelper.setTestTenant(tenantManager, "test-tenant");

    assertThat(multiTenantDatabaseTestRule.operationalDataStore.getDatabaseSchema()).isEqualTo("t_test_tenant");
    assertThat(multiTenantDatabaseTestRule.aggregationDataStore.getDatabaseSchema()).isEqualTo("t_test_tenant");
    assertThat(multiTenantDatabaseTestRule.thirdPartyScansDataStore.getDatabaseSchema()).isEqualTo("t_test_tenant");
    // datamart is ALWAYS global
    assertThat(multiTenantDatabaseTestRule.dataMartDataStore.getDatabaseSchema()).isEqualTo("global");
  }

  @Test
  public void testDataSourceID() {
    assertThat(multiTenantDatabaseTestRule.operationalDataStore.getID()).isEqualTo(OperationalDataStore.ID);
    assertThat(multiTenantDatabaseTestRule.aggregationDataStore.getID()).isEqualTo(AggregationDataStore.ID);
    assertThat(multiTenantDatabaseTestRule.dataMartDataStore.getID()).isEqualTo(DataMartDataStore.ID);
    assertThat(multiTenantDatabaseTestRule.thirdPartyScansDataStore.getID()).isEqualTo(ThirdPartyScansDataStore.ID);
  }
}
