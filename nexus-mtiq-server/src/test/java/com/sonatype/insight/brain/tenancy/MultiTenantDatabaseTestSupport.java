/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.Collection;
import java.util.Collections;
import javax.inject.Provider;

import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;
import com.sonatype.insight.brain.test.MultiTenantDatabaseTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mockito;

public class MultiTenantDatabaseTestSupport
    extends MultiTenantTestSupport
{
  @Rule
  public MultiTenantDatabaseTestRule multiTenantDatabaseTestRule = new MultiTenantDatabaseTestRule();

  protected TenantManager tenantManager;

  @Before
  public void setUp() {
    Collection<TenantManaged> tenantManagedBeans = Collections.emptyList();
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);
    TenantValidator tenantValidator = new TenantValidator(multiTenantDatabaseTestRule.operationalDataStore);

    tenantManager =
        new TenantManager(tenantManagedBeans, multiTenantDatabaseTestRule.insightConfig, tenantLifecycleProvider,
            multiTenantDatabaseTestRule.databaseProvisionUtils, tenantValidator);
  }

  @Override
  protected Tenant testAsNewTenant(ConsumerWithException<Tenant> test) {
    return super.testAsNewTenant(t -> {
      multiTenantDatabaseTestRule.provisionDatabaseForTenant(t);

      tenantManager.setTenant(t);

      test.accept(t);
    });
  }
}
