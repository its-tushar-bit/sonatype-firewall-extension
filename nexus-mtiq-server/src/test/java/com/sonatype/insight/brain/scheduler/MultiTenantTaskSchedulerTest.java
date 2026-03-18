/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.CopyStorageTask;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantContextJobListener;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;

import static com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler.TASK_SCHEDULER_THREAD_POOL_SIZE;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantTaskSchedulerTest
{
  @Rule
  public TestName testName = new TestName();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private MultiTenantQuartzJobStoreTX mockMultiTenantQuartzJobStoreTX;

  @Mock
  private MultiTenantBatchModeJobStoreTX mockMultiTenantBatchModeJobStoreTX;

  @Mock
  private JobFactory mockJobFactory;

  @Mock
  private QuartzTriggerListener mockQuartzTriggerListener;

  @Mock
  private QuartzConcurrencyListener mockQuartzConcurrencyListener;

  @Mock
  private TenantContextJobListener mockTenantContextJobListener;

  @Mock
  private SystemConfigurationPropertyDAO mockSystemConfigurationPropertyDAO;

  @Mock
  private TenantManager mockTenantManager;

  private TenantUtil spyTenantUtil;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private QuartzJobSchedulingService mockQuartzJobSchedulingService;

  @Mock
  private CopyStorageTask copyStorageTask;

  @Mock
  private ApiConfigurationService apiConfigurationService;

  private MultiTenantTaskScheduler spyUnderTest;

  @Before
  public void setup() {
    when(mockQuartzTriggerListener.getName()).thenReturn("mockQuartzTriggerListener");
    when(mockQuartzConcurrencyListener.getName()).thenReturn("mockQuartzConcurrencyListener");
    when(mockTenantContextJobListener.getName()).thenReturn("mockTenantContextJobListener");
    when(mockTenantManager.areTenantsPreRegistered()).thenReturn(true);
    spyTenantUtil = spy(new TenantUtil());
    spyUnderTest = spy(new MultiTenantTaskScheduler(
        mockMultiTenantQuartzJobStoreTX,
        mockMultiTenantBatchModeJobStoreTX,
        mockJobFactory,
        testName.getMethodName(),
        mockQuartzTriggerListener,
        mockQuartzConcurrencyListener,
        () -> mockTenantContextJobListener,
        mockSystemConfigurationPropertyDAO,
        () -> mockTenantManager,
        spyTenantUtil,
        mockShutdownHandler,
        mockQuartzJobSchedulingService));
  }

  @Test
  public void shouldAddJobListener_whenSchedulerCreated() throws Exception {
    spyUnderTest.createScheduler();

    assertThat(spyUnderTest.getScheduler().getListenerManager().getJobListeners())
        .containsExactly(mockTenantContextJobListener);
  }

  @Test
  public void testCreateScheduler_DoesCreateSchedulerIfShutdownIsNotTriggered() {
    assertThat(spyUnderTest.getScheduler()).isNull();

    spyUnderTest.createScheduler(spyUnderTest.schedulerName, mockMultiTenantQuartzJobStoreTX);

    assertThat(spyUnderTest.getScheduler()).isNotNull();
  }

  @Test
  public void testCreateScheduler_DoesNotCreateSchedulerIfAfterShutdownGracePeriod() {
    assertThat(spyUnderTest.getScheduler()).isNull();
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(true);

    spyUnderTest.createScheduler(spyUnderTest.schedulerName, mockMultiTenantQuartzJobStoreTX);

    assertThat(spyUnderTest.getScheduler()).isNull();
  }

  @Test
  public void shouldLoadPoolSizeFromConfig() {
    int poolSize = 300;

    when(mockSystemConfigurationPropertyDAO.getByName(TASK_SCHEDULER_THREAD_POOL_SIZE)).thenReturn(
        new SystemConfigurationProperty(TASK_SCHEDULER_THREAD_POOL_SIZE, String.valueOf(poolSize)));

    SimpleThreadPool threadPool = spyUnderTest.createThreadPool();

    assertThat(threadPool.getPoolSize()).isEqualTo(poolSize);
  }

  @Test
  public void shouldDefaultPoolSize_whenNoConfigExists() {
    SimpleThreadPool threadPool = spyUnderTest.createThreadPool();

    assertThat(threadPool.getPoolSize()).isEqualTo(10);
  }

  @Test
  public void shouldUnscheduleJobForAllTenants_whenGlobalTenant() throws Exception {
    ImmutableList<String> tenants = ImmutableList.of("tenant1", "tenant2");

    when(spyTenantUtil.isGlobalTenant()).thenReturn(true);
    when(mockMultiTenantQuartzJobStoreTX.getJobGroupNames()).thenReturn(tenants);
    InsightJob mockInsightJob = mock(InsightJob.class);
    when(mockInsightJob.getJobName()).thenReturn(testName.getMethodName());
    doReturn(true).when(spyUnderTest).unscheduleTask(any(), any(InsightJob.class));

    spyUnderTest.unscheduleTask(mockInsightJob);

    for (String tenant : tenants) {
      verify(spyUnderTest).unscheduleTask(spyUnderTest.toJobKey(mockInsightJob, tenant), mockInsightJob);
    }
  }

  @Test
  public void shouldUnscheduleJobForSingleTenant_whenNotGlobal() throws Exception {
    testAsNewTenant(testName, t -> {
      when(mockTenantManager.getTenant()).thenReturn(t);

      when(spyTenantUtil.isGlobalTenant()).thenReturn(false);
      InsightJob mockInsightJob = mock(InsightJob.class);
      when(mockInsightJob.getJobName()).thenReturn(testName.getMethodName());
      doReturn(true).when(spyUnderTest).unscheduleTask(any(), any(InsightJob.class));

      spyUnderTest.unscheduleTask(mockInsightJob);

      verify(spyUnderTest).unscheduleTask(spyUnderTest.toJobKey(mockInsightJob, t.tenantSlug), mockInsightJob);
    });
  }

  @Test
  public void shouldIncludeTenantName_whenGetTriggerKey() {
    testAsNewTenant(testName, t -> {
      when(mockTenantManager.getTenant()).thenReturn(t);
      InsightJob mockInsightJob = mock(InsightJob.class);
      when(mockInsightJob.getJobName()).thenReturn(testName.getMethodName());

      TriggerKey triggerKey = spyUnderTest.toTriggerKey(mockInsightJob);

      assertThat(triggerKey.getGroup()).isEqualTo(t.tenantSlug);
    });
  }

  @Test
  public void testInitialize() {
    String mtiqBatchSchedulerName = spyUnderTest.getMtiqBatchSchedulerName();

    spyUnderTest.initialize();

    assertThat(spyUnderTest.getScheduler()).isNotNull();
    assertThat(spyUnderTest.getScheduler(mtiqBatchSchedulerName)).isNotNull();
  }

  @Test
  public void testStart_Unknown() throws Exception {
    String mtiqBatchSchedulerName = spyUnderTest.getMtiqBatchSchedulerName();
    when(spyTenantUtil.isMtiqBatchMode()).thenReturn(true);

    spyUnderTest.start();

    assertThat(spyUnderTest.getScheduler().isStarted()).isTrue();
    assertThat(spyUnderTest.getScheduler(mtiqBatchSchedulerName).isStarted()).isTrue();
    assertThat(spyUnderTest.getScheduler().isInStandbyMode()).isFalse();
    assertThat(spyUnderTest.getScheduler(mtiqBatchSchedulerName).isInStandbyMode()).isFalse();
  }

  @Test
  public void testGetScheduler_MtiqBatchJob() throws Exception {
    spyUnderTest.initialize();
    Scheduler scheduler = spyUnderTest.getScheduler(copyStorageTask);

    assertThat(scheduler.getSchedulerName()).isEqualTo(spyUnderTest.getMtiqBatchSchedulerName());
  }

  @Test
  public void testGetQuartzJobStoreTX_MtiqBatchJob() {
    spyUnderTest.initialize();
    QuartzJobStoreTX quartzJobStoreTX = spyUnderTest.getQuartzJobStoreTX(copyStorageTask);

    assertThat(quartzJobStoreTX).isEqualTo(mockMultiTenantBatchModeJobStoreTX);
  }

  @Test
  public void testGetScheduler_NonMtiqBatchJob() throws Exception {
    spyUnderTest.initialize();
    Scheduler scheduler = spyUnderTest.getScheduler(apiConfigurationService);

    assertThat(scheduler.getSchedulerName()).isEqualTo(testName.getMethodName());
  }

  @Test
  public void testGetQuartzJobStoreTX_NonMtiqBatchJob() {
    spyUnderTest.initialize();
    QuartzJobStoreTX quartzJobStoreTX = spyUnderTest.getQuartzJobStoreTX(apiConfigurationService);

    assertThat(quartzJobStoreTX).isEqualTo(mockMultiTenantQuartzJobStoreTX);
  }
}
