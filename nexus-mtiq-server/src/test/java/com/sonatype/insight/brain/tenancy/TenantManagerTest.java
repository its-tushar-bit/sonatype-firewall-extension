/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerTest
    extends MultiTenantTest
{
  static final String TENANT_NAME = "tenant";

  @Mock
  TenantManaged job;

  @Mock
  InsightConfig config;

  @Mock
  TenantLifecycle lifecycle;

  @Mock
  DatabaseProvisionUtils databaseProvisionUtils;

  @Mock
  DatabaseConfigProvider databaseConfigProvider;

  List<TenantManaged> tenantManagedBeans;

  Tenant tenant = new Tenant(TENANT_NAME);

  TenantManager underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    tenantManagedBeans = new ArrayList<>();
    tenantManagedBeans.add(job);

    underTest =
        new TenantManager(tenantManagedBeans, config, () -> lifecycle, databaseProvisionUtils, databaseConfigProvider);
  }

  @Test
  public void shouldSetGlobalTenant_andMultiTenantMode() {
    testAs(new Tenant(TENANT_NAME), tenant -> {
      TenantManager.initGlobalTenant();

      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
      assertThat(TenantUtil.isMultiTenant()).isTrue();
    });
  }

  @Test
  public void shouldSetTenant_whenTenantNameProvided() {
    underTest.setTenant(TENANT_NAME);

    assertThat(underTest.getTenant()).isEqualTo(new Tenant(TENANT_NAME));
  }

  @Test
  public void shouldSetTenant_whenTenantProvided() {
    underTest.setTenant(tenant);

    assertThat(underTest.getTenant()).isEqualTo(tenant);
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNameNull() {
    assertThatThrownBy(() -> underTest.setTenant((String) null)).isInstanceOf(IllegalArgumentException.class)
        .withFailMessage("Tenant parameter cannot be null");
  }

  @Test
  public void shouldThrowIllegalArgumentException_whenTenantNull() {
    assertThatThrownBy(() -> underTest.setTenant((Tenant) null)).isInstanceOf(IllegalArgumentException.class)
        .withFailMessage("Tenant parameter cannot be null");
  }

  @Test
  public void shouldNotRegister_whenGlobalTenant() throws Exception {
    underTest.setTenant(GLOBAL_TENANT);

    verify(job, never()).register();
    verify(lifecycle, never()).bootTenant();
  }

  @Test
  public void shouldOnlyRegisterTenantOnce() throws Exception {
    setTenantAndAssertRegistration();

    // Call set tenant a second time
    underTest.setTenant(tenant);

    // Verify that the registration code wasn't called again
    verify(job, times(1)).register();
    verify(lifecycle, times(1)).bootTenant();
  }

  @Test
  public void shouldNotRegister_globalTenantJobs() throws Exception {
    MockTenantManaged mockGlobalTenantJob = mock(MockTenantManaged.class);

    tenantManagedBeans.add(mockGlobalTenantJob);

    setTenantAndAssertRegistration();

    verify(mockGlobalTenantJob, never()).register();
  }

  private void setTenantAndAssertRegistration() throws Exception {
    doAnswer(invocationOnMock -> {
      assertTenantSet(tenant);
      return null;
    }).when(job).register();

    underTest.setTenant(tenant);

    verify(job).register();
    verify(lifecycle).bootTenant();
    verify(databaseProvisionUtils).initializeDatabases(eq(config), any(DatabaseConfigProvider.class));
  }

  private static class MockTenantManaged
      implements TenantManaged, GlobalTenantJob
  {
    // We need a mock that implements GlobalTenantJob and TenantManaged
  }
}
