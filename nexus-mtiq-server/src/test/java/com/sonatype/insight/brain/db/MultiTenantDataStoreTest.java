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
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantManagerTestHelper;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.test.MultiTenantDatabaseTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantDataStoreTest
{
  @Rule
  public MultiTenantDatabaseTestRule multiTenantDatabaseTestRule = new MultiTenantDatabaseTestRule();

  private Tenant tenant1;

  @Before
  public void setUp() {
    tenant1 = createTenant("tenant1");
    multiTenantDatabaseTestRule.provisionDatabaseForTenant(tenant1);
  }

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
    setTenant(Tenant.GLOBAL_TENANT);

    assertThat(multiTenantDatabaseTestRule.operationalDataStore.getDatabaseSchema()).isEqualTo("global");
    assertThat(multiTenantDatabaseTestRule.aggregationDataStore.getDatabaseSchema()).isEqualTo("global");
    assertThat(multiTenantDatabaseTestRule.dataMartDataStore.getDatabaseSchema()).isEqualTo("global");
    assertThat(multiTenantDatabaseTestRule.thirdPartyScansDataStore.getDatabaseSchema()).isEqualTo("global");
  }

  @Test
  public void testTenantSchema() {
    Collection<TenantManaged> tenantManagedBeans = Collections.emptyList();
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);
    TenantValidator tenantValidator = new TenantValidator(multiTenantDatabaseTestRule.operationalDataStore);

    TenantManager tenantManager =
        new TenantManager(tenantManagedBeans, multiTenantDatabaseTestRule.insightConfig, tenantLifecycleProvider,
            multiTenantDatabaseTestRule.databaseProvisionUtils, tenantValidator);

    TenantManagerTestHelper.setTestTenant(tenantManager, tenant1.tenantSlug);

    assertThat(multiTenantDatabaseTestRule.operationalDataStore.getDatabaseSchema()).isEqualTo("t_tenant1");
    assertThat(multiTenantDatabaseTestRule.aggregationDataStore.getDatabaseSchema()).isEqualTo("t_tenant1");
    assertThat(multiTenantDatabaseTestRule.thirdPartyScansDataStore.getDatabaseSchema()).isEqualTo("t_tenant1");
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
