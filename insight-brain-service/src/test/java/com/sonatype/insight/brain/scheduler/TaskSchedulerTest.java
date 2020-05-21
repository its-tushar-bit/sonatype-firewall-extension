/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.net.InetAddress;
import java.sql.Connection;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.PostgresDatabaseEngine;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.db.H2DatabaseEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdScheduler;
import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
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

  private TaskScheduler taskSchedulerSpy;

  @Override
  public void configure(Properties properties) {
    properties.put("scheduler.name", TaskScheduler.DEFAULT_SCHEDULER_NAME + "-" + UUID.randomUUID());
  }

  @Before
  public void before() {
    taskSchedulerSpy = spy(taskScheduler);
  }

  @After
  public void after() {
    TestJob.reset();
    tryCleanup(taskScheduler.getScheduler());
  }

  private void tryCleanup(Scheduler scheduler) {
    if (scheduler != null) {
      try {
        scheduler.clear();
      }
      catch (Exception e) {
        // noop
      }
      try {
        scheduler.shutdown(false);
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(taskScheduler.getScheduler()).isNull());
      }
      catch (Exception e) {
        // noop
      }
    }
  }

  @Test
  public void testCreateJobStore_H2() throws Exception {
    when(taskSchedulerSpy.getDatabaseEngine()).thenReturn(H2DatabaseEngine.INSTANCE);

    JobStoreTX jobStoreTX = taskSchedulerSpy.createJobStore(new SimpleThreadPool());

    assertThat(jobStoreTX.isClustered()).isFalse();
    assertThat(jobStoreTX.getDriverDelegateClass()).isEqualTo(HSQLDBDelegate.class.getName());
    testCreateJobStore(jobStoreTX);
  }

  @Test
  public void testCreateJobStore_Postgres() throws Exception {
    when(taskSchedulerSpy.getDatabaseEngine()).thenReturn(PostgresDatabaseEngine.INSTANCE);

    JobStoreTX jobStoreTX = taskSchedulerSpy.createJobStore(new SimpleThreadPool());

    assertThat(jobStoreTX.getDriverDelegateClass()).isEqualTo(PostgreSQLDelegate.class.getName());
    assertThat(jobStoreTX.isClustered()).isTrue();
    assertThat(jobStoreTX.getClusterCheckinInterval()).isEqualTo(3000);
    testCreateJobStore(jobStoreTX);
  }

  private void testCreateJobStore(JobStoreTX jobStoreTX) {
    assertThat(jobStoreTX.getDataSource()).isEqualTo("ods");
    assertThat(jobStoreTX.getTablePrefix()).isEqualTo(OperationalDataStoreProvider.ID + ".QRTZ_");
    assertThat(jobStoreTX.canUseProperties()).isTrue();
  }

  @Test
  public void testCreateThreadPool() {
    SimpleThreadPool simpleThreadPool = taskScheduler.createThreadPool();
    assertThat(simpleThreadPool.getPoolSize()).isEqualTo(10);
    assertThat(simpleThreadPool.isMakeThreadsDaemons()).isTrue();
  }

  @Test
  public void testGetDatabaseEngine() {
    assertThat(taskScheduler.getDatabaseEngine()).isEqualTo(H2DatabaseEngine.INSTANCE);
  }

  @Test
  public void testCreateScheduler_Instantiation() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();
    // We can check the class and that properties are passed along but can't check all properties as most are hidden
    assertThat(scheduler.getMetaData().getThreadPoolSize()).isEqualTo(10);
    assertThat(scheduler).isInstanceOf(StdScheduler.class);
    try (Connection connection = DBConnectionManager.getInstance().getConnection("ods")) {
      assertThat(connection.getSchema()).isEqualTo(OperationalDataStoreProvider.ID);
    }
    assertThat(scheduler.getSchedulerName()).startsWith("QuartzScheduler");
    assertThat(scheduler.getSchedulerInstanceId()).startsWith(InetAddress.getLocalHost().getHostName());
    assertThat(scheduler.getMetaData().getThreadPoolClass()).isEqualTo(SimpleThreadPool.class);
    assertThat(scheduler.getMetaData().getJobStoreClass()).isEqualTo(JobStoreTX.class);
  }

  @Test
  public void testCreateScheduler_JobFactory() throws Exception {
    Scheduler scheduler = taskScheduler.createScheduler();

    scheduler.scheduleJob(createJobDetail(), createTrigger());
    assertThat(TestJob.isExecutionFinished()).isFalse();
    scheduler.start();
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(TestJob.isExecutionFinished()).isTrue());
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
  public void testScheduleDailyTask() throws Exception {
    String name = "TestJob";
    Scheduler scheduler = taskScheduler.createScheduler();

    taskScheduler.scheduleDailyTask(TestJob.class, name, 1);

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
    taskScheduler.scheduleDailyTask(TestJob.class, name, ZonedDateTime.now().plusHours(1).getHour());
    scheduler.triggerJob(JobKey.jobKey(name));

    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(testJobListener.isExecuted()).isTrue());
    JobExecutionException jobExecutionException = testJobListener.getJobExecutionException();
    assertThat(jobExecutionException).hasStackTraceContaining(TestJob.NAME + " exception");
    assertThat(isTaskScheduled(scheduler, name)).isTrue();
  }

  @Test
  public void testGetNextExecutionTime() {
    String name = "TestJob";
    ZonedDateTime now = ZonedDateTime.now();
    taskScheduler.createScheduler();
    taskScheduler.scheduleDailyTask(TestJob.class, name, now.plusHours(1).getHour());

    Date nextExecutionTime = taskScheduler.getNextExecutionTime(name);

    ZonedDateTime nextExecution = ZonedDateTime.ofInstant(nextExecutionTime.toInstant(), ZoneId.systemDefault());
    assertThat(nextExecution).isEqualTo(now.plusHours(1).withMinute(0).withSecond(0).withNano(0));
  }

  @Test
  public void testUnscheduleTask() throws Exception {
    String name = "TestJob";
    Scheduler scheduler = taskScheduler.createScheduler();
    taskScheduler.scheduleDailyTask(TestJob.class, name, 1);
    JobKey jobKey = JobKey.jobKey(name);
    TriggerKey triggerKey = TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup());
    assertThat(scheduler.getJobDetail(jobKey)).isNotNull();
    assertThat(scheduler.getTrigger(triggerKey)).isNotNull();

    taskScheduler.unscheduleTask(name);

    assertThat(scheduler.getJobDetail(jobKey)).isNull();
    assertThat(scheduler.getTrigger(triggerKey)).isNull();
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
}
