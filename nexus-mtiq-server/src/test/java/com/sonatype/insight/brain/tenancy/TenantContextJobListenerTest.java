/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import static com.sonatype.insight.brain.tenancy.AllTenantsJob.TENANT_LIST;
import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantContextJobListenerTest
    extends MultiTenantTestSupport
{
  @Mock
  TenantManager tenantManager;

  @Mock
  JobExecutionContext context;

  @Mock
  JobDetail detail;

  @Mock
  JobDataMap jobDataMap;

  @Mock
  Job job;

  @Mock
  TenantUtil tenantUtil;

  TenantContextJobListener underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    when(context.getJobDetail()).thenReturn(detail);
    when(detail.getJobDataMap()).thenReturn(jobDataMap);
    when(context.getJobInstance()).thenReturn(job);
    when(tenantUtil.isAllTenantsJob(any())).thenReturn(false);

    underTest = new TenantContextJobListener(tenantManager, tenantUtil);
  }

  @Test
  public void shouldRunAsSpecifiedTenant() throws Exception {
    String tenantName = "tenant";
    when(detail.getKey()).thenReturn(new JobKey("name", tenantName));

    underTest.jobToBeExecuted(context);

    verify(tenantManager).setTenant(new Tenant(tenantName));
  }

  @Test
  public void shouldRunAsGlobalTenant_whenGlobal() throws Exception {
    when(detail.getKey()).thenReturn(new JobKey("name", "global"));

    underTest.jobToBeExecuted(context);

    verify(tenantManager).setTenant(GLOBAL_TENANT);
  }

  @Test
  public void shouldInvalidateTenant_afterJobExecution() throws Exception {
    underTest =  new TenantContextJobListener(tenantManager, new TenantUtil());

    testAs(new Tenant("tenant"), tenant -> {
      underTest.jobWasExecuted(context, null);

      assertThat(tenant.isInvalid()).isTrue();
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldInvalidateTenant_onJobVeto() throws Exception {
    underTest =  new TenantContextJobListener(tenantManager, new TenantUtil());

    testAs(new Tenant("tenant"), tenant -> {
      underTest.jobExecutionVetoed(context);

      assertThat(tenant.isInvalid()).isTrue();
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
    });
  }

  /**
   * https://issues.sonatype.org/browse/CLM-25106 If an exception is thrown before a job is executed (i.e. by a
   * listener) then jobWasExecuted or jobExecutionVetoed are not called. These methods are used to invalidate a tenant.
   * Without these being called a tenant is left against the thread in a "valid" state, meaning the thread cannot be
   * reused.
   */
  @Test
  public void shouldInvalidateTenant_beforeSettingUpJobExecution() throws Exception {
    underTest =  new TenantContextJobListener(tenantManager, new TenantUtil());
    when(detail.getJobClass()).thenAnswer(i -> job.getClass());

    String tenantForJob = "tenant-for-job";
    when(detail.getKey()).thenReturn(new JobKey("name", tenantForJob));

    testAsNewTenant(tenant -> {

      underTest.jobToBeExecuted(context);

      assertThat(tenant.isInvalid()).isTrue();
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);

      verify(tenantManager).setTenant(new Tenant(tenantForJob));
    });
  }

  /**
   * The tenant currently against the thread "tenant" will be invalidated immediately by jobToBeExecuted (see:
   * shouldInvalidateTenant_beforeSettingUpJobExecution). jobTenant is the tenant retrieved from the Quartz job
   * table. If it has been set by the test and an exception is thrown AFTER it has been set then it also should be
   * invalidated.
   */
  @Test
  public void shouldInvalidateTenant_whenExceptionThrown() throws Exception {
    underTest =  new TenantContextJobListener(tenantManager, new TenantUtil());

    Tenant jobTenant = TenantTestHelper.createTenant(testName);

    when(detail.getKey()).thenAnswer(i -> {
      TenantTestHelper.setTenant(jobTenant);

      throw new RuntimeException("Intentional failure thrown after tenant has been set");
    });

    testAsNewTenant(tenant -> {
      try {
        underTest.jobToBeExecuted(context);
      }
      catch (Exception e) {
        //no-op
      }

      assertThat(jobTenant.isInvalid()).isTrue();
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldRegisterAllTenants_whenInMtiqBatchMode() throws Exception {
    ImmutableList<String> tenantNames = ImmutableList.of("tenant1", "tenant2");
    List<Tenant> tenants = tenantNames.stream().map(Tenant::new).collect(toList());

    when(tenantUtil.isMtiqBatchMode()).thenReturn(true);
    when(tenantUtil.isAllTenantsJob(any())).thenReturn(true);
    when(tenantUtil.getAllTenants()).thenReturn(tenants);

    when(detail.getKey()).thenReturn(new JobKey("name", "global"));

    when(tenantManager.getRegisteredTenants()).thenReturn(tenantNames);

    underTest.jobToBeExecuted(context);

    verify(jobDataMap).put(TENANT_LIST, tenantNames);

    verify(tenantManager).setTenant(tenants.get(0));
    verify(tenantManager).setTenant(tenants.get(1));

    assertThat(tenants.get(0).isInvalid()).isTrue();
    assertThat(tenants.get(1).isInvalid()).isTrue();
  }
}
