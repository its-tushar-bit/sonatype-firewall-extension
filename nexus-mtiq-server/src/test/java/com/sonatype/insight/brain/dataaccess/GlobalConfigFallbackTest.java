/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collection;
import java.util.Collections;
import javax.inject.Provider;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.test.MultiTenantDatabaseTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantManagerTestHelper.setTestTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.createTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;

public class GlobalConfigFallbackTest
{
  @Rule
  public MultiTenantDatabaseTestRule multiTenantDatabaseTestRule = new MultiTenantDatabaseTestRule();

  private final SystemConfigurationPropertyDAO underTest = new SystemConfigurationPropertyDAO();

  private Tenant tenant1;

  @Before
  public void setUp() {
    tenant1 = createTenant("tenant1");
    multiTenantDatabaseTestRule.provisionDatabaseForTenant(tenant1);
  }

  @Test
  public void testConfigFallbackToGlobal_whenTenantValueNotSet() {
    String configKey = "key";
    String globalConfigValue = "global-value";
    String tenantConfigValue = "tenant-value";

    Collection<TenantManaged> tenantManagedBeans = Collections.emptyList();
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);
    TenantValidator tenantValidator = new TenantValidator(multiTenantDatabaseTestRule.operationalDataStore);

    TenantManager tenantManager =
        new TenantManager(tenantManagedBeans, multiTenantDatabaseTestRule.insightConfig, tenantLifecycleProvider,
            multiTenantDatabaseTestRule.databaseProvisionUtils, tenantValidator);

    testAs(GLOBAL_TENANT, t -> underTest.insert(new SystemConfigurationProperty(configKey, globalConfigValue)));

    testAs(tenant1, t -> {
      setTestTenant(tenantManager, tenant1);

      String value = underTest.get(configKey);

      assertThat(value).isEqualTo(globalConfigValue);
    });

    testAs(tenant1, t -> underTest.insert(new SystemConfigurationProperty(configKey, tenantConfigValue)));

    testAs(tenant1, t -> {
      String value = underTest.get(configKey);

      assertThat(value).isEqualTo(tenantConfigValue);
    });
  }
}
