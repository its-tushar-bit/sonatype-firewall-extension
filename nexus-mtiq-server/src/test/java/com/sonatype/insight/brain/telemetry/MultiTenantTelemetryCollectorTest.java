/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collection;
import java.util.Collections;
import javax.inject.Provider;

import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.test.MultiTenantDatabaseTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;

public class MultiTenantTelemetryCollectorTest
{
  @Rule
  public MultiTenantDatabaseTestRule multiTenantDatabaseTestRule = new MultiTenantDatabaseTestRule();

  protected TenantManager tenantManager;

  protected Tenant tenant1;

  protected Tenant tenant2;

  @Before
  public void helperSetup() {
    Collection<TenantManaged> tenantManagedBeans = Collections.emptyList();
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);
    TenantValidator tenantValidator = new TenantValidator(multiTenantDatabaseTestRule.operationalDataStore);

    tenantManager =
        new TenantManager(tenantManagedBeans, multiTenantDatabaseTestRule.insightConfig, tenantLifecycleProvider,
            multiTenantDatabaseTestRule.databaseProvisionUtils, tenantValidator);

    tenant1 = createTenant("tenant1");
    tenant2 = createTenant("tenant2");

    multiTenantDatabaseTestRule.provisionDatabaseForTenant(tenant1);
    multiTenantDatabaseTestRule.provisionDatabaseForTenant(tenant2);
  }
}
