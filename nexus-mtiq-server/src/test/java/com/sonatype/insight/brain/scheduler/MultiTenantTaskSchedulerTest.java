/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantContextJobListener;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;

import static com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler.TASK_SCHEDULER_THREAD_POOL_SIZE;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantTaskSchedulerTest
{
  @Rule
  public TestName testName = new TestName();

  @Mock
  QuartzJobStoreTX quartzJobStoreTX;

  @Mock
  JobFactory jobFactory;

  @Mock
  QuartzTriggerListener quartzTriggerListener;

  @Mock
  TenantContextJobListener tenantContextJobListener;

  @Mock
  SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Mock
  TenantManager tenantManager;

  @Mock
  TenantUtil tenantUtil;

  @Mock
  Scheduler scheduler;

  @Mock
  ListenerManager listenerManager;

  MultiTenantTaskScheduler underTest;

  @Before
  public void setup() {
    try {
      underTest = new TestMultiTenantTaskScheduler(quartzJobStoreTX, jobFactory, testName.getMethodName(),
          quartzTriggerListener, tenantContextJobListener, systemConfigurationPropertyDAO, tenantManager,
          scheduler, tenantUtil);

      when(scheduler.getListenerManager()).thenReturn(listenerManager);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void shouldAddJobListener_whenSchedulerCreated() {
    underTest.createScheduler();

    verify(listenerManager).addJobListener(tenantContextJobListener);
  }

  @Test
  public void shouldLoadPoolSizeFromConfig() {
    int poolSize = 300;

    when(systemConfigurationPropertyDAO.getByName(TASK_SCHEDULER_THREAD_POOL_SIZE)).thenReturn(
        new SystemConfigurationProperty(TASK_SCHEDULER_THREAD_POOL_SIZE, String.valueOf(poolSize)));

    SimpleThreadPool threadPool = underTest.createThreadPool();

    assertThat(threadPool.getPoolSize()).isEqualTo(poolSize);
  }

  @Test
  public void shouldDefaultPoolSize_whenNoConfigExists() {
    SimpleThreadPool threadPool = underTest.createThreadPool();

    assertThat(threadPool.getPoolSize()).isEqualTo(10);
  }

  @Test
  public void shouldUnscheduleJobForAllTenants_whenGlobalTenant() throws Exception {
    ImmutableList<String> tenants = ImmutableList.of("tenant1", "tenant2");

    when(tenantUtil.isGlobalTenant()).thenReturn(true);
    when(scheduler.getJobGroupNames()).thenReturn(tenants);
    InsightJob mockInsightJob = mock(InsightJob.class);
    when(mockInsightJob.getJobName()).thenReturn(testName.getMethodName());

    underTest.unscheduleTask(mockInsightJob);

    for (String tenant : tenants) {
      verify(scheduler).deleteJob(underTest.toJobKey(mockInsightJob, tenant));
    }
  }

  @Test
  public void shouldUnscheduleJobForSingleTenant_whenNotGlobal() throws Exception {
    testAsNewTenant(testName, t -> {
      when(tenantManager.getTenant()).thenReturn(t);

      when(tenantUtil.isGlobalTenant()).thenReturn(false);
      InsightJob mockInsightJob = mock(InsightJob.class);
      when(mockInsightJob.getJobName()).thenReturn(testName.getMethodName());

      underTest.unscheduleTask(mockInsightJob);

      verify(scheduler).deleteJob(underTest.toJobKey(mockInsightJob, t.tenantSlug));
    });
  }

  @Test
  public void shouldIncludeTenantName_whenGetTriggerKey() {
    testAsNewTenant(testName, t -> {
      when(tenantManager.getTenant()).thenReturn(t);
      InsightJob mockInsightJob = mock(InsightJob.class);
      when(mockInsightJob.getJobName()).thenReturn(testName.getMethodName());

      TriggerKey triggerKey = underTest.toTriggerKey(mockInsightJob);

      assertThat(triggerKey.getGroup()).isEqualTo(t.tenantSlug);
    });
  }

  private static class TestMultiTenantTaskScheduler
      extends MultiTenantTaskScheduler
  {
    private final Scheduler scheduler;

    public TestMultiTenantTaskScheduler(
        QuartzJobStoreTX quartzJobStoreTX,
        JobFactory jobFactory,
        String schedulerName,
        QuartzTriggerListener quartzTriggerListener,
        TenantContextJobListener tenantContextJobListener,
        SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
        TenantManager tenantManager,
        Scheduler scheduler,
        TenantUtil tenantUtil)
    {
      super(quartzJobStoreTX, jobFactory, schedulerName, quartzTriggerListener, tenantContextJobListener,
          systemConfigurationPropertyDAO, tenantManager, tenantUtil);
      this.scheduler = scheduler;
    }

    @Override
    protected Scheduler superCreateScheduler() {
      return scheduler;
    }

    @Override
    public Scheduler getScheduler() {
      return scheduler;
    }
  }
}
