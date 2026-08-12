/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.spring.config.ScheduledConfiguration;
import com.sonatype.insight.brain.testing.SpringBrainInjectedTest;
import com.sonatype.insight.test.LogOutput;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.lang.time.DateUtils;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.CronTrigger;
import org.quartz.DailyTimeIntervalTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.StdScheduler;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.quartz.utils.DBConnectionManager;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = TaskSchedulerTest.TaskSchedulerTestConfiguration.class)
public class TaskSchedulerTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(TaskScheduler.class);

  @Inject
  private TaskScheduler taskScheduler;

  @Inject
  private QuartzJobStoreTX quartzJobStoreTX;

  @Inject
  private TestJob testJob;

  @Inject
  private NonConcurrentTestJob nonConcurrentTestJob;

  @Inject
  private OperationalDataStore operationalDataStore;

  @Inject
  private QuartzJobSchedulingService quartzJobSchedulingService;

  @Inject
  private ShutdownHandler mockShutdownHandler;

  @After
  public void after() throws Exception {
    if (taskScheduler != null) {
      taskScheduler.stop();
    }
    Mockito.reset(mockShutdownHandler);
    TestJob.reset();
    deleteAllSchedulerStateRecords();
  }

  @Override
  protected boolean preserveAopProxies() {
    return true;
  }

  @Test
  public void testCreateThreadPool() {
    SimpleThreadPool simpleThreadPool = taskScheduler.createThreadPool();
    assertThat(simpleThreadPool.getPoolSize()).isEqualTo(10);
    assertThat(simpleThreadPool.isMakeThreadsDaemons()).isTrue();
  }

  @Test
  @H2DiskTest
  public void testCreateScheduler_Instantiation() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();
    // We can check the class and that properties are passed along but can't check all properties as most are hidden
    assertThat(scheduler.getMetaData().getThreadPoolSize()).isEqualTo(10);
    assertThat(scheduler).isInstanceOf(StdScheduler.class);
    try (Connection connection = DBConnectionManager.getInstance().getConnection("ods")) {
      assertThat(connection.getSchema()).isIn(OperationalDataStore.ID, "PUBLIC");
    }
    assertThat(scheduler.getSchedulerName()).isNotBlank();
    assertThat(scheduler.getSchedulerInstanceId()).isNotNull();
    assertThat(scheduler.getMetaData().getThreadPoolClass()).isEqualTo(SimpleThreadPool.class);
    assertThat(QuartzJobStoreTX.class).isAssignableFrom(scheduler.getMetaData().getJobStoreClass());
    List<TriggerListener> triggerListeners = scheduler.getListenerManager().getTriggerListeners();
    assertThat(triggerListeners).hasSize(2);
    boolean hasQuartzTriggerListener = triggerListeners.stream()
        .anyMatch(listener -> listener instanceof QuartzTriggerListener);
    boolean hasQuartzConcurrencyListener = triggerListeners.stream()
        .anyMatch(listener -> listener instanceof QuartzConcurrencyListener);
    assertThat(hasQuartzTriggerListener).isTrue();
    assertThat(hasQuartzConcurrencyListener).isTrue();
    verify(mockShutdownHandler).add(scheduler, ShutdownPriority.QUARTZ_SCHEDULERS);
  }

  @Test
  @H2DiskTest
  public void testCreateScheduler_ReusesExistingSchedulerWithWarning() throws Exception {
    Scheduler firstScheduler = taskScheduler.createScheduler();

    Scheduler reusedScheduler = taskScheduler.createScheduler(taskScheduler.schedulerName, quartzJobStoreTX);

    assertThat(reusedScheduler).isSameAs(firstScheduler);
    logOutput.assertThat()
        .atWarnLevel()
        .contains("Reusing existing Quartz scheduler " + taskScheduler.schedulerName);
  }

  @Test
  public void testCreateScheduler_JobFactory() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();

    scheduler.scheduleJob(createJobDetail(), createTrigger());
    assertThat(TestJob.getExecutions()).isEqualTo(0);
    scheduler.start();
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(TestJob.getExecutions()).isEqualTo(1));
  }

  @Test
  public void testStart() throws Exception {
    // Not started
    taskScheduler.start();
    assertThat(taskScheduler.getScheduler().isStarted()).isTrue();

    // Already started
    taskScheduler.start();
    assertThat(taskScheduler.getScheduler().isStarted()).isTrue();
  }

  @Test
  public void testStop() throws Exception {
    // Initial start
    taskScheduler.start();
    assertThat(taskScheduler.getScheduler().isStarted()).isTrue();

    // Not stopped
    taskScheduler.stop();
    assertThat(taskScheduler.getScheduler()).isNull();

    // Already stopped
    taskScheduler.stop();
    assertThat(taskScheduler.getScheduler()).isNull();
  }

  @Test
  public void testNormalizeJobClass_NormalClass() {
    assertThat(TaskScheduler.normalizeJobClass(TestJob.class)).isEqualTo(TestJob.class);
  }

  @Test
  public void testNormalizeJobClass_ProxyEnhancedClass() {
    Class<? extends Job> jobClass = getTestJobClass();
    assertThat(jobClass).hasSuperclass(TestJob.class);
    assertThat(TaskScheduler.normalizeJobClass(jobClass)).isEqualTo(TestJob.class);
  }

  private Class<? extends Job> getTestJobClass() {
    // NOTE: unlike TestJob.class, this yields a bytecode enhanced class which is more interesting
    return testJob.getClass();
  }

  @Test
  public void testUnscheduleSchedule() throws Exception {
    taskScheduler.start();
    Runnable runnable = () -> {
      taskScheduler.unscheduleTask(testJob);
      // We don't need the job to start, just for it to be scheduled
      taskScheduler.schedulePeriodicTask(testJob, Duration.ofMinutes(15), DateUtils.addHours(new Date(), 5));
    };

    testInMultipleThreadsAndThrowAnyException(runnable, 4, Duration.ofSeconds(3));

    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(quartzJobSchedulingService);
    assertThat(taskScheduler.isTaskScheduled(testJob)).isTrue();
  }

  @Test
  public void testScheduleUnschedule() throws Exception {
    taskScheduler.start();
    Runnable runnable = () -> {
      // We don't need the job to start, just for it to be scheduled
      taskScheduler.schedulePeriodicTask(testJob, Duration.ofMinutes(15), DateUtils.addHours(new Date(), 5));
      taskScheduler.unscheduleTask(testJob);
    };

    testInMultipleThreadsAndThrowAnyException(runnable, 4, Duration.ofSeconds(3));

    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(quartzJobSchedulingService);
    assertThat(taskScheduler.isTaskScheduled(testJob)).isFalse();
  }

  @Test
  public void testTriggerTaskNow() throws Exception {
    taskScheduler.start();
    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(4));
    });
    assertThat(TestJob.getExecutions()).isZero();
    runTestAndWaitForReady(() -> {
      taskScheduler.triggerTaskNow(testJob, null);
    });
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(TestJob.getExecutions()).isOne());
  }

  @Test
  public void testTriggerTaskNow_WithParameters() throws Exception {
    taskScheduler.start();
    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(4));
    });
    assertThat(TestJob.getExecutions()).isZero();
    Map<String, String> params = Collections.singletonMap("testKey", "testValue");

    runTestAndWaitForReady(() -> {
      taskScheduler.triggerTaskNow(testJob, params);
    });

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(TestJob.getExecutions()).isOne();
      assertThat(TestJob.getJobParameters(0)).containsAllEntriesOf(params);
    });
  }

  @Test
  public void testScheduleDailyTask() throws Exception {
    String name = "TestJob";
    Scheduler scheduler = taskScheduler.createScheduler();

    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleDailyTask(testJob, LocalTime.of(1, 0));
    });

    JobKey jobKey = JobKey.jobKey(name);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.requestsRecovery()).isFalse();
    Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
    assertThat(trigger).isInstanceOf(CronTrigger.class);
    CronTrigger cronTrigger = (CronTrigger) trigger;
    assertThat(cronTrigger.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
    assertThat(cronTrigger.getCronExpression()).isEqualTo("0 0 1 ? * *");
  }

  @Test
  public void testScheduleDailyTask_StillScheduledAfterException() throws Exception {
    TestJob.setShouldThrowException(true);
    TestJobListener testJobListener = new TestJobListener();
    Scheduler scheduler = taskScheduler.createScheduler();
    scheduler.getListenerManager().addJobListener(testJobListener);
    scheduler.start();

    String name = "TestJob";
    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(1));
    });
    scheduler.triggerJob(JobKey.jobKey(name));

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testJobListener.getExecutions()).isEqualTo(1));
    JobExecutionException jobExecutionException = testJobListener.getJobExecutionException();
    assertThat(jobExecutionException).hasStackTraceContaining(TestJob.NAME + " exception");
    assertThat(isTaskScheduled(scheduler, name)).isTrue();
  }

  @Test
  public void testScheduleOneTimeTask_NoTime_StartsNow() throws Exception {
    String name = "TestJob";
    Scheduler scheduler = taskScheduler.createScheduler();
    Date now = new Date();

    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleOneTimeTask(testJob);
    });

    JobKey jobKey = JobKey.jobKey(name);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.requestsRecovery()).isFalse();
    Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
    assertThat(trigger).isInstanceOf(SimpleTrigger.class);
    SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
    assertThat(simpleTrigger.getMisfireInstruction()).isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY);
    assertThat(simpleTrigger.getStartTime()).isAfterOrEqualTo(now);
  }

  @Test
  public void testScheduleOneTimeTask_NoTime_StartsNow_WithParameters() throws Exception {
    String name = "TestJob";
    Map<String, String> parameters = new HashMap<>();
    parameters.put("key1", "value1");
    parameters.put("key2", "value2");
    Scheduler scheduler = taskScheduler.createScheduler();
    Date now = new Date();

    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleOneTimeTask(testJob, parameters);
    });

    JobKey jobKey = JobKey.jobKey(name);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.requestsRecovery()).isFalse();
    Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
    assertThat(trigger).isInstanceOf(SimpleTrigger.class);
    SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
    assertThat(simpleTrigger.getMisfireInstruction()).isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY);
    assertThat(simpleTrigger.getStartTime()).isAfterOrEqualTo(now);
    assertThat(simpleTrigger.getJobDataMap().getString("key1")).isEqualTo("value1");
    assertThat(simpleTrigger.getJobDataMap().getString("key2")).isEqualTo("value2");
  }

  @Test
  public void testGetNextExecutionTime() {
    ZonedDateTime now = ZonedDateTime.now().withSecond(0).withNano(0);
    taskScheduler.createScheduler();
    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleDailyTask(testJob, LocalTime.of(now.plusHours(1).getHour(), now.getMinute()));
    });

    Date nextExecutionTime = taskScheduler.getNextExecutionTime(testJob);

    ZonedDateTime nextExecution = ZonedDateTime.ofInstant(nextExecutionTime.toInstant(), ZoneId.systemDefault());
    assertThat(nextExecution).isEqualTo(now.plusHours(1));
  }

  @Test
  public void testGetNextExecutionTime_WhenTriggerMissing() throws Exception {
    Scheduler mockScheduler = mock(Scheduler.class);
    TaskScheduler spiedTaskScheduler = spy(taskScheduler);
    when(spiedTaskScheduler.getScheduler(testJob)).thenReturn(mockScheduler);
    when(mockScheduler.getTrigger(any())).thenReturn(null);

    assertThat(spiedTaskScheduler.getNextExecutionTime(testJob)).isNull();
  }

  @Test
  public void testUnscheduleTask() throws Exception {
    String name = "TestJob";
    Scheduler scheduler = taskScheduler.createScheduler();

    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleDailyTask(testJob, LocalTime.of(1, 0));
    });

    JobKey jobKey = JobKey.jobKey(name);
    TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
    assertThat(scheduler.getJobDetail(jobKey)).isNotNull();
    assertThat(scheduler.getTrigger(triggerKey)).isNotNull();

    boolean result = taskScheduler.unscheduleTask(testJob);

    assertThat(result).isTrue();
    assertThat(scheduler.getJobDetail(jobKey)).isNull();
    assertThat(scheduler.getTrigger(triggerKey)).isNull();
  }

  @Test
  public void testScheduleTask_NullScheduler() {
    JobKey jobKey = JobKey.jobKey(TestJob.NAME);

    assertThatNoException()
        .isThrownBy(() -> taskScheduler.scheduleTask((Scheduler) null, jobKey, () -> {
          throw new AssertionError("builder must not run when scheduler is null");
        }));
    logOutput.assertThat()
        .atWarnLevel()
        .contains(
            "Cannot schedule task, jobKey 'DEFAULT.TestJob' " +
                "for tenant Tenant[tenantSlug='notused', createdByThread='main', valid='true'] " +
                "because a scheduler is not available.");
  }

  @Test
  public void testScheduleOneTimeTask() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();
    Date now = new Date();

    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleOneTimeTask(testJob, LocalTime.of(23, 0));
    });

    JobKey jobKey = JobKey.jobKey(TestJob.NAME);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.requestsRecovery()).isFalse();
    Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
    assertThat(trigger).isInstanceOf(DailyTimeIntervalTrigger.class);
    DailyTimeIntervalTrigger dailyTimeIntervalTrigger = (DailyTimeIntervalTrigger) trigger;
    assertThat(dailyTimeIntervalTrigger.getMisfireInstruction())
        .isEqualTo(DailyTimeIntervalTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
    Date nextFireTime = dailyTimeIntervalTrigger.getNextFireTime();
    assertThat(nextFireTime.toInstant()).isAfterOrEqualTo(now.toInstant().minusSeconds(1));
    assertThat(nextFireTime).hasHourOfDay(23);
  }

  @Test
  public void testScheduleOneDateTimeTask() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();

    LocalDateTime nextStart = LocalDateTime.now().plusDays(1).plusMinutes(1);
    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleOneTimeTask(testJob, nextStart);
    });

    JobKey jobKey = JobKey.jobKey(TestJob.NAME);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.requestsRecovery()).isFalse();
    Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
    assertThat(trigger).isInstanceOf(SimpleTrigger.class);
    SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
    assertThat(simpleTrigger.getMisfireInstruction())
        .isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
    assertThat(simpleTrigger.getRepeatCount()).isEqualTo(0);
    Date nextFireTime = simpleTrigger.getNextFireTime();

    assertThat(nextFireTime).isAfterOrEqualTo(Date.from(nextStart.atZone(ZoneId.systemDefault()).toInstant()));
  }

  @Test
  public void testSchedulePeriodicTask() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();
    int intervalMillis = 10000;

    runTestAndWaitForReady(() -> {
      taskScheduler.schedulePeriodicTask(testJob, Duration.ofMillis(intervalMillis));
    });

    JobKey jobKey = JobKey.jobKey(TestJob.NAME);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.requestsRecovery()).isFalse();
    Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
    assertThat(trigger).isInstanceOf(SimpleTrigger.class);
    SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
    assertThat(simpleTrigger.getMisfireInstruction())
        .isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
    assertThat(simpleTrigger.getRepeatCount()).isEqualTo(SimpleTrigger.REPEAT_INDEFINITELY);
    assertThat(simpleTrigger.getRepeatInterval()).isEqualTo(intervalMillis);
  }

  /**
   * Regression test for CLM-42076. Before that fix, {@code schedulePeriodicTask} built its trigger at enqueue time,
   * baking in a {@code startTime} that was {@code DELAY_MILLIS} milliseconds stale by the time Quartz stored it. The
   * {@link NeverPastCalendar} then treated the (past) startTime as excluded and shifted the first fire out by a full
   * interval, meaning a daily task registered at startup would not actually fire until 24 hours later, and every
   * restart would push the fire out again.
   * <p>
   * The fix makes the batching queue hold {@link java.util.function.Supplier}s of
   * {@link com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService.BuiltJob}, invoked at flush time. So the
   * trigger's {@code startTime} is set to "now" as of the flush moment, which is when Quartz stores it. First fire
   * happens shortly after registration; subsequent fires follow at the requested interval.
   */
  @Test
  public void testSchedulePeriodicTask_FiresShortlyAfterRegistration() throws Exception {
    // A one-day interval means the only way for the job to execute inside this test window is if the first
    // periodic fire lands at (or near) registration time. If NeverPastCalendar were still pushing the first fire
    // out by one interval, this test would fail.
    taskScheduler.start();
    assertThat(TestJob.getExecutions()).isZero();

    runTestAndWaitForReady(() -> {
      taskScheduler.schedulePeriodicTask(testJob, Duration.ofDays(1));
    });

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(TestJob.getExecutions()).isOne());

    // And the trigger's next execution time is roughly one interval out from registration — confirms the periodic
    // schedule is correctly set up and that Quartz won't re-fire in the near future.
    Date nextFireTime = taskScheduler.getNextExecutionTime(testJob);
    assertThat(nextFireTime).isAfter(new Date(System.currentTimeMillis() + Duration.ofHours(23).toMillis()));
  }

  @Test
  public void testSchedulePeriodicTask_DoesNothingOnMisfire() throws Exception {
    quartzJobStoreTX.setMisfireThreshold(1);
    assertThat(TestJob.getExecutions()).isEqualTo(0);
    Scheduler scheduler = taskScheduler.createScheduler();
    int intervalMillis = 100;
    int desiredJobExecutions = 10;
    scheduler.start();
    scheduler.standby();
    taskScheduler.schedulePeriodicTask(testJob, Duration.ofMillis(intervalMillis));
    // we want to miss desiredJobExecutions in total
    Thread.sleep(intervalMillis * (desiredJobExecutions - 1));

    scheduler.start();

    // we want to execute desiredJobExecutions in total
    Thread.sleep(intervalMillis * desiredJobExecutions);
    scheduler.standby();
    // if we didn't ignore misfires, then we would expect 2 * desiredJobExecutions
    // since we ignore misfires, we would expect desiredJobExecutions
    // actually test it's less than desiredJobExecutions + 2 in case of random Thread slowness
    assertThat(TestJob.getExecutions()).isLessThan(desiredJobExecutions + 2);
  }

  @Test
  public void testSchedulePeriodicTask_NoRapidCatchUpFiringAfterOverlongExecution() throws Exception {
    int intervalMillis = 3000;
    int overlongExecution = 8000;
    TestJob.setDurations(execution -> execution == 0 ? overlongExecution : 0);
    Scheduler scheduler = taskScheduler.createScheduler();
    scheduler.start();
    taskScheduler.schedulePeriodicTask(nonConcurrentTestJob, Duration.ofMillis(intervalMillis));
    await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> assertThat(TestJob.getExecutions()).isGreaterThan(1));
    scheduler.standby();
    assertThat(TestJob.getExecutions()).isEqualTo(2);
  }

  @Test
  public void testIsJobTriggered() throws Exception {
    TestJob.setDurations(execution -> 5000);
    Scheduler scheduler = taskScheduler.createScheduler();
    scheduler.start();

    runTestAndWaitForReady(() -> {
      taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(4));
    });

    assertThat(taskScheduler.isJobTriggered(testJob, Collections.emptyMap())).isTrue();
    assertThat(TestJob.getExecutions()).isZero();
    taskScheduler.triggerTaskNow(testJob, Collections.singletonMap("key", "true"));
    assertThat(taskScheduler.isJobTriggered(testJob, Collections.singletonMap("key", "false"))).isFalse();
    assertThat(taskScheduler.isJobTriggered(testJob, Collections.singletonMap("key", "true"))).isTrue();
    await().atMost(30, TimeUnit.SECONDS)
        .untilAsserted(
            () -> assertThat(taskScheduler.isJobTriggered(testJob, Collections.singletonMap("key", "true"))).isFalse());
  }

  @Test
  public void testSchedulePeriodicTask_RefireAfterError() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();
    TestJob.setShouldThrowException(true);
    TestJobListener testJobListener = new TestJobListener();
    scheduler.getListenerManager().addJobListener(testJobListener);

    taskScheduler.schedulePeriodicTask(testJob, Duration.ofSeconds(1));

    scheduler.start();
    await().atMost(10, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(testJobListener.getExecutions()).isGreaterThan(1));
    assertThat(TestJob.getExecutions()).isGreaterThan(1);
    JobExecutionException jobExecutionException = testJobListener.getJobExecutionException();
    assertThat(jobExecutionException).hasStackTraceContaining(TestJob.NAME + " exception");
    assertThat(isTaskScheduled(scheduler, TestJob.NAME)).isTrue();
  }

  @Test
  public void testGetOtherNodeIds_BeforeSchedulerCreated_ReturnsEmptySet() {
    assertThat(taskScheduler.getOtherNodeIds()).isEmpty();
  }

  @Test
  public void testGetOtherNodeIds_NoSchedulerStateRecords() {
    taskScheduler.createScheduler();

    assertThat(taskScheduler.getOtherNodeIds()).isEmpty();
  }

  @Test
  public void testGetOtherNodeIds_OnlyOwnSchedulerStateRecord() throws Exception {
    taskScheduler.createScheduler();
    createSchedulerStateRecord(quartzJobStoreTX.getInstanceId(), System.currentTimeMillis());

    assertThat(taskScheduler.getOtherNodeIds()).isEmpty();
  }

  @Test
  public void testGetOtherNodeIds_MultipleSchedulerStateRecords() throws Exception {
    taskScheduler.createScheduler();
    createSchedulerStateRecord(quartzJobStoreTX.getInstanceId(), System.currentTimeMillis());
    String otherInstanceId = "other";
    createSchedulerStateRecord(otherInstanceId, System.currentTimeMillis());

    assertThat(taskScheduler.getOtherNodeIds()).containsExactly(otherInstanceId);
  }

  @Test
  public void testScheduleOneTimeTaskForAllOtherNodes() throws Exception {
    TaskScheduler taskSchedulerSpy = spy(taskScheduler);
    Scheduler scheduler = taskSchedulerSpy.createScheduler();
    Set<String> nodeIds = Sets.newHashSet("node1", "node2");
    when(taskSchedulerSpy.getOtherNodeIds()).thenReturn(nodeIds);

    runTestAndWaitForReady(() -> {
      taskSchedulerSpy.scheduleOneTimeTaskForAllOtherNodes(testJob);
    });

    JobKey jobKey = JobKey.jobKey(TestJob.NAME);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.isDurable()).isFalse();
    assertThat(job.requestsRecovery()).isFalse();
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
    assertThat(triggers).hasSize(2);
    nodeIds.forEach(nodeId -> {
      Trigger trigger = triggers.stream()
          .filter(t -> t.getKey()
              .getName()
              .equals(TestJob.NAME + "For" + nodeId))
          .findFirst()
          .orElse(null);
      assertThat(trigger).isInstanceOf(SimpleTrigger.class);
      SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
      assertThat(simpleTrigger.getMisfireInstruction())
          .isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      assertThat(simpleTrigger.getKey().getName()).isEqualTo(TestJob.NAME + "For" + nodeId);
      assertThat(simpleTrigger.getJobDataMap().getString(TaskScheduler.QUARTZ_NODE_ID)).isEqualTo(nodeId);
      Date now = new Date();
      assertThat(simpleTrigger.getStartTime()).isBeforeOrEqualTo(now).isCloseTo(now, 10000);
    });
  }

  @Test
  public void testScheduleOneTimeTaskForAllOtherNodes_WithParameters() throws Exception {
    TaskScheduler taskSchedulerSpy = spy(taskScheduler);
    Scheduler scheduler = taskSchedulerSpy.createScheduler();
    Set<String> nodeIds = Sets.newHashSet("node1", "node2");
    when(taskSchedulerSpy.getOtherNodeIds()).thenReturn(nodeIds);
    Map<String, String> parameters = new HashMap<>();
    parameters.put("key1", "value1");
    parameters.put("key2", "value2");

    runTestAndWaitForReady(() -> {
      taskSchedulerSpy.scheduleOneTimeTaskForAllOtherNodes(testJob, parameters);
    });

    JobKey jobKey = JobKey.jobKey(TestJob.NAME);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.isDurable()).isFalse();
    assertThat(job.requestsRecovery()).isFalse();
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
    assertThat(triggers).hasSize(2);
    nodeIds.forEach(nodeId -> {
      Trigger trigger = triggers.stream()
          .filter(t -> t.getKey()
              .getName()
              .equals(TestJob.NAME + "For" + nodeId))
          .findFirst()
          .orElse(null);
      assertThat(trigger).isInstanceOf(SimpleTrigger.class);
      SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
      assertThat(simpleTrigger.getMisfireInstruction())
          .isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
      assertThat(simpleTrigger.getKey().getName()).isEqualTo(TestJob.NAME + "For" + nodeId);
      assertThat(simpleTrigger.getJobDataMap().getString(TaskScheduler.QUARTZ_NODE_ID)).isEqualTo(nodeId);
      assertThat(simpleTrigger.getJobDataMap().getString("key1")).isEqualTo("value1");
      assertThat(simpleTrigger.getJobDataMap().getString("key2")).isEqualTo("value2");
      Date now = new Date();
      assertThat(simpleTrigger.getStartTime()).isBeforeOrEqualTo(now).isCloseTo(now, 10000);
    });
  }

  @Test
  public void testInitialize() throws Exception {
    assertThat(taskScheduler.getScheduler()).isNull();

    taskScheduler.initialize();

    assertThat(taskScheduler.getScheduler()).isNotNull();
    assertThat(taskScheduler.getScheduler().getSchedulerName()).isEqualTo(taskScheduler.schedulerName);
    assertThat(taskScheduler.getScheduler().isStarted()).isFalse();
    assertThat(taskScheduler.getScheduler().isInStandbyMode()).isTrue();
  }

  @Test
  public void testStartStandby() throws Exception {
    assertThatNoException().isThrownBy(() -> taskScheduler.startScheduler(null));
    assertThatNoException().isThrownBy(() -> taskScheduler.standbyScheduler(null));

    taskScheduler.start();

    assertThat(taskScheduler.getScheduler().isInStandbyMode()).isFalse();

    taskScheduler.standby();

    assertThat(taskScheduler.getScheduler().isInStandbyMode()).isTrue();

    taskScheduler.start();

    assertThat(taskScheduler.getScheduler().isInStandbyMode()).isFalse();
  }

  @Test
  public void testCreateScheduler_DoesCreateSchedulerIfShutdownIsNotAfterGracePeriod() {
    assertThat(taskScheduler.getScheduler()).isNull();

    taskScheduler.createScheduler(taskScheduler.schedulerName, quartzJobStoreTX);

    assertThat(taskScheduler.getScheduler()).isNotNull();
  }

  @Test
  public void testCreateScheduler_DoesNotCreateSchedulerIfShutdownIsAfterGracePeriod() {
    assertThat(taskScheduler.getScheduler()).isNull();
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(true);

    taskScheduler.createScheduler(taskScheduler.schedulerName, quartzJobStoreTX);

    assertThat(taskScheduler.getScheduler()).isNull();
  }

  @Test
  public void testStartScheduler() throws Exception {
    Scheduler mockScheduler = mock(Scheduler.class);
    taskScheduler.startScheduler(mockScheduler);
    verify(mockScheduler).start();

    Mockito.reset(mockScheduler);
    when(mockScheduler.isStarted()).thenReturn(true);
    when(mockScheduler.isInStandbyMode()).thenReturn(true);
    taskScheduler.startScheduler(mockScheduler);
    verify(mockScheduler).start();

    Mockito.reset(mockScheduler);
    when(mockScheduler.isShutdown()).thenReturn(true);
    taskScheduler.startScheduler(mockScheduler);
    verify(mockScheduler, never()).start();

    Mockito.reset(mockScheduler);
    when(mockScheduler.isStarted()).thenReturn(true);
    taskScheduler.startScheduler(mockScheduler);
    verify(mockScheduler, never()).start();
  }

  @Test
  public void testStandbyScheduler() throws Exception {
    Scheduler mockScheduler = mock(Scheduler.class);
    taskScheduler.standbyScheduler(mockScheduler);
    verify(mockScheduler).standby();

    Mockito.reset(mockScheduler);
    when(mockScheduler.isStarted()).thenReturn(true);
    taskScheduler.standbyScheduler(mockScheduler);
    verify(mockScheduler).standby();

    Mockito.reset(mockScheduler);
    when(mockScheduler.isInStandbyMode()).thenReturn(true);
    taskScheduler.standbyScheduler(mockScheduler);
    verify(mockScheduler, never()).standby();

    Mockito.reset(mockScheduler);
    when(mockScheduler.isShutdown()).thenReturn(true);
    taskScheduler.standbyScheduler(mockScheduler);
    verify(mockScheduler, never()).standby();
  }

  @Test
  public void testShutdownScheduler() throws Exception {
    Scheduler mockScheduler = mock(Scheduler.class);
    taskScheduler.shutdownScheduler(mockScheduler);
    verify(mockScheduler).shutdown();

    Mockito.reset(mockScheduler);
    when(mockScheduler.isShutdown()).thenReturn(true);
    taskScheduler.shutdownScheduler(mockScheduler);
    verify(mockScheduler, never()).shutdown();
  }

  private JobDetail createJobDetail() {
    return JobBuilder.newJob(TestJob.class).withIdentity(TestJob.NAME).build();
  }

  private Trigger createTrigger() {
    return TriggerBuilder.newTrigger().withIdentity(TestJob.NAME).build();
  }

  private boolean isTaskScheduled(Scheduler scheduler, String name) throws Exception {
    Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(name));
    return trigger != null && trigger.mayFireAgain();
  }

  private void createSchedulerStateRecord(String instanceId, long checkinTimestamp) throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      DSL.using(connection)
          .insertInto(DSL.table(operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE"))
          .columns(
              DSL.field("SCHED_NAME"),
              DSL.field("INSTANCE_NAME"),
              DSL.field("LAST_CHECKIN_TIME"),
              DSL.field("CHECKIN_INTERVAL"))
          .values(
              taskScheduler.getScheduler().getSchedulerName(),
              instanceId,
              checkinTimestamp,
              quartzJobStoreTX.getClusterCheckinInterval())
          .execute();
    }
  }

  private void deleteAllSchedulerStateRecords() throws Exception {
    try (Connection connection = operationalDataStore.getDataSource().getConnection()) {
      DSL.using(connection)
          .deleteFrom(DSL.table(operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE"))
          .execute();
    }
  }

  private void runTestAndWaitForReady(Runnable runnable) {
    runnable.run();
    quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(quartzJobSchedulingService);
  }

  private void testInMultipleThreadsAndThrowAnyException(
      final Runnable runnable,
      final int threadCount,
      final Duration duration) throws Exception
  {
    AtomicReference<Exception> exception = new AtomicReference<>();

    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      threads.add(new Thread(() -> {
        try {
          long start = System.currentTimeMillis();
          while (System.currentTimeMillis() - start < duration.toMillis() && exception.get() == null) {
            runnable.run();
          }
        }
        catch (Exception e) {
          exception.set(e);
        }
      }));
    }
    threads.forEach(Thread::start);
    for (Thread thread : threads) {
      thread.join();
    }
    if (exception.get() != null) {
      throw exception.get();
    }
  }

  @TestConfiguration
  static class TaskSchedulerTestConfiguration
  {
    @Bean
    public OperationalDataStore operationalDataStore() {
      DatabaseContainerRule rule = DatabaseContainerRule.getInstance(SpringBrainInjectedTest.class);
      rule.ensureInitializedForSpringContext();
      return rule.getOperationalDataStore();
    }

    @Bean
    public InsightConfig insightConfig() {
      return new InsightConfig();
    }

    @Bean
    public TestProductLicenseManager testProductLicenseManager() {
      return new TestProductLicenseManager();
    }

    @Bean
    public ProductLicense productLicense(final TestProductLicenseManager testProductLicenseManager) {
      return new TestProductLicense(testProductLicenseManager);
    }

    @Bean
    public QuartzJobStoreTX quartzJobStoreTX(
        final ProductLicense productLicense,
        final InsightConfig insightConfig,
        final OperationalDataStore operationalDataStore) throws InvalidConfigurationException
    {
      return new TestQuartzJobStoreTx(productLicense, insightConfig, operationalDataStore);
    }

    @Bean
    public QuartzTriggerListener quartzTriggerListener() {
      return new QuartzTriggerListener();
    }

    @Bean
    public QuartzConcurrencyListener quartzConcurrencyListener(final QuartzJobStoreTX quartzJobStoreTX) {
      return new QuartzConcurrencyListener(quartzJobStoreTX);
    }

    @Bean
    public QuartzJobSchedulingService quartzJobSchedulingService() {
      return new QuartzJobSchedulingService();
    }

    @Bean
    public JobFactory jobFactory(final ApplicationContext applicationContext) {
      ScheduledConfiguration.AutowiringSpringBeanJobFactory jobFactory =
          new ScheduledConfiguration.AutowiringSpringBeanJobFactory();
      jobFactory.setApplicationContext(applicationContext);
      return jobFactory;
    }

    @Bean
    public ShutdownHandler shutdownHandler() {
      return mock(ShutdownHandler.class);
    }

    @Bean
    public TaskScheduler taskScheduler(
        final QuartzJobStoreTX quartzJobStoreTX,
        final JobFactory jobFactory,
        final QuartzTriggerListener quartzTriggerListener,
        final QuartzConcurrencyListener quartzConcurrencyListener,
        final OperationalDataStore operationalDataStore,
        final ShutdownHandler shutdownHandler,
        final QuartzJobSchedulingService quartzJobSchedulingService)
    {
      return new TestTaskScheduler(
          quartzJobStoreTX,
          jobFactory,
          quartzTriggerListener,
          quartzConcurrencyListener,
          operationalDataStore,
          shutdownHandler,
          quartzJobSchedulingService)
      {

        @Override
        public void start() throws Exception {
          super.start();
        }
      };
    }

    @Bean
    public TestJob testJob() {
      ProxyFactory proxyFactory = new ProxyFactory(new TestJob());
      proxyFactory.setProxyTargetClass(true);
      proxyFactory.addAdvice((MethodInterceptor) invocation -> invocation.proceed());
      return (TestJob) proxyFactory.getProxy();
    }

    @Bean
    public NonConcurrentTestJob nonConcurrentTestJob() {
      return new NonConcurrentTestJob();
    }
  }
}
