/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TenantContextJobListenerTest
    extends MultiTenantTest
{
  @Mock
  TenantManager tenantManager;

  @Mock
  JobExecutionContext context;

  @Mock
  JobDetail detail;

  @Mock
  Job job;

  TenantContextJobListener underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    when(context.getJobDetail()).thenReturn(detail);
    when(context.getJobInstance()).thenReturn(job);

    underTest = new TenantContextJobListener(tenantManager, new TenantUtil());
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
    testAs(new Tenant("tenant"), tenant -> {
      underTest.jobWasExecuted(context, null);

      assertThat(tenant.isInvalid()).isTrue();
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
    });
  }

  @Test
  public void shouldInvalidateTenant_onJobVeto() throws Exception {
    testAs(new Tenant("tenant"), tenant -> {
      underTest.jobExecutionVetoed(context);

      assertThat(tenant.isInvalid()).isTrue();
      assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(GLOBAL_TENANT);
    });
  }
}
