/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.Joinpoint;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.After;
import org.junit.Test;
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
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.utils.DBConnectionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TaskSchedulerTest
    extends AbstractComponentTest
{
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

  @Override
  public void configure(Binder binder) {
    // add some AOP to the mix to more closely reflect the normal runtime setup
    MethodInterceptor noop = Joinpoint::proceed;
    binder.bindInterceptor(Matchers.subclassesOf(TestJob.class), Matchers.any(), noop);
    super.configure(binder);
  }

  @After
  public void after() throws Exception {
    TestJob.reset();
    deleteAllSchedulerStateRecords();
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
      assertThat(connection.getSchema()).isEqualTo(OperationalDataStore.ID);
    }
    assertThat(scheduler.getSchedulerName()).isNotBlank();
    assertThat(scheduler.getSchedulerInstanceId()).isNotNull();
    assertThat(scheduler.getMetaData().getThreadPoolClass()).isEqualTo(SimpleThreadPool.class);
    assertThat(QuartzJobStoreTX.class).isAssignableFrom(scheduler.getMetaData().getJobStoreClass());
    List<TriggerListener> triggerListeners = scheduler.getListenerManager().getTriggerListeners();
    assertThat(triggerListeners).hasSize(1);
    assertThat(triggerListeners.get(0)).isInstanceOf(QuartzTriggerListener.class);
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
  public void testNormalizeJobClass_GuiceEnhancedClass() {
    Class<? extends Job> jobClass = getTestJobClass();
    assertThat(jobClass).hasSuperclass(TestJob.class);
    assertThat(TaskScheduler.normalizeJobClass(jobClass)).isEqualTo(TestJob.class);
  }

  private Class<? extends Job> getTestJobClass() {
    // NOTE: unlike TestJob.class, this yields a bytecode enhanced class which is more interesting
    return testJob.getClass();
  }

  @Test
  public void testTriggerTaskNow() throws Exception {
    taskScheduler.start();
    taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(4));
    assertThat(TestJob.getExecutions()).isZero();
    taskScheduler.triggerTaskNow(testJob, null);
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(TestJob.getExecutions()).isOne());
  }

  @Test
  public void testTriggerTaskNow_WithParameters() throws Exception {
    taskScheduler.start();
    taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(4));
    assertThat(TestJob.getExecutions()).isZero();
    Map<String, String> params = Collections.singletonMap("testKey", "testValue");
    taskScheduler.triggerTaskNow(testJob, params);
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(TestJob.getExecutions()).isOne();
      assertThat(TestJob.getJobParameters(0)).containsAllEntriesOf(params);
    });
  }

  @Test
  public void testScheduleDailyTask() throws Exception {
    String name = "TestJob";
    Scheduler scheduler = taskScheduler.createScheduler();

    taskScheduler.scheduleDailyTask(testJob, LocalTime.of(1, 0));

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
    taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(1));
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

    taskScheduler.scheduleOneTimeTask(testJob);

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
  public void testGetNextExecutionTime() {
    ZonedDateTime now = ZonedDateTime.now().withSecond(0).withNano(0);
    taskScheduler.createScheduler();
    taskScheduler.scheduleDailyTask(testJob, LocalTime.of(now.plusHours(1).getHour(), now.getMinute()));

    Date nextExecutionTime = taskScheduler.getNextExecutionTime(testJob);

    ZonedDateTime nextExecution = ZonedDateTime.ofInstant(nextExecutionTime.toInstant(), ZoneId.systemDefault());
    assertThat(nextExecution).isEqualTo(now.plusHours(1));
  }

  @Test
  public void testUnscheduleTask() throws Exception {
    String name = "TestJob";
    Scheduler scheduler = taskScheduler.createScheduler();
    taskScheduler.scheduleDailyTask(testJob, LocalTime.of(1, 0));
    JobKey jobKey = JobKey.jobKey(name);
    TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
    assertThat(scheduler.getJobDetail(jobKey)).isNotNull();
    assertThat(scheduler.getTrigger(triggerKey)).isNotNull();

    taskScheduler.unscheduleTask(testJob);

    assertThat(scheduler.getJobDetail(jobKey)).isNull();
    assertThat(scheduler.getTrigger(triggerKey)).isNull();
  }

  @Test
  public void testScheduleOneTimeTask() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();
    Date now = new Date();

    taskScheduler.scheduleOneTimeTask(testJob, LocalTime.of(23, 0));

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
    taskScheduler.scheduleOneTimeTask(testJob, nextStart);

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

    taskScheduler.schedulePeriodicTask(testJob, Duration.ofMillis(intervalMillis));

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
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(TestJob.getExecutions()).isGreaterThan(1));
    scheduler.standby();
    assertThat(TestJob.getExecutions()).isEqualTo(2);
  }

  @Test
  public void testIsJobTriggered() throws Exception {
    TestJob.setDurations(execution -> 5000);
    Scheduler scheduler = taskScheduler.createScheduler();
    scheduler.start();
    taskScheduler.scheduleDailyTask(testJob, LocalTime.now().plusHours(4));
    assertThat(taskScheduler.isJobTriggered(testJob, Collections.emptyMap())).isTrue();
    assertThat(TestJob.getExecutions()).isZero();
    taskScheduler.triggerTaskNow(testJob, Collections.singletonMap("key", "true"));
    assertThat(taskScheduler.isJobTriggered(testJob, Collections.singletonMap("key", "false"))).isFalse();
    assertThat(taskScheduler.isJobTriggered(testJob, Collections.singletonMap("key", "true"))).isTrue();
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
        assertThat(taskScheduler.isJobTriggered(testJob, Collections.singletonMap("key", "true"))).isFalse());
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

    taskSchedulerSpy.scheduleOneTimeTaskForAllOtherNodes(testJob);

    JobKey jobKey = JobKey.jobKey(TestJob.NAME);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.isDurable()).isFalse();
    assertThat(job.requestsRecovery()).isFalse();
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
    assertThat(triggers).hasSize(2);
    nodeIds.forEach(nodeId -> {
      Trigger trigger = triggers.stream().filter(t -> t.getKey().getName()
          .equals(TestJob.NAME + "For" + nodeId)).findFirst().orElse(null);
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

    taskSchedulerSpy.scheduleOneTimeTaskForAllOtherNodes(testJob, parameters);

    JobKey jobKey = JobKey.jobKey(TestJob.NAME);
    JobDetail job = scheduler.getJobDetail(jobKey);
    assertThat(job).isNotNull();
    assertThat(job.getJobClass()).isEqualTo(TestJob.class);
    assertThat(job.isDurable()).isFalse();
    assertThat(job.requestsRecovery()).isFalse();
    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
    assertThat(triggers).hasSize(2);
    nodeIds.forEach(nodeId -> {
      Trigger trigger = triggers.stream().filter(t -> t.getKey().getName()
          .equals(TestJob.NAME + "For" + nodeId)).findFirst().orElse(null);
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
    String sQuery = "INSERT INTO " + operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE" + //
        " (SCHED_NAME, INSTANCE_NAME, LAST_CHECKIN_TIME, CHECKIN_INTERVAL) " + //
        " VALUES (?1, ?2, ?3, ?4)";
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sQuery)) {
      statement.setString(1, taskScheduler.getScheduler().getSchedulerName());
      statement.setString(2, instanceId);
      statement.setLong(3, checkinTimestamp);
      statement.setLong(4, quartzJobStoreTX.getClusterCheckinInterval());
      statement.execute();
    }
  }

  private void deleteAllSchedulerStateRecords() throws Exception {
    String sQuery = "DELETE FROM " + operationalDataStore.getDatabaseSchema() + ".QRTZ_SCHEDULER_STATE";
    try (Connection connection = operationalDataStore.getDataSource().getConnection();
         PreparedStatement statement = connection.prepareStatement(sQuery)) {
      statement.execute();
    }
  }
}
