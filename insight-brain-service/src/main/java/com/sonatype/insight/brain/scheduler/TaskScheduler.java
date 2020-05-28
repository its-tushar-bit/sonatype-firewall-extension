/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.PostgresDatabaseEngine;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;

import io.dropwizard.lifecycle.Managed;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.quartz.spi.ThreadPool;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class TaskScheduler
    implements Managed
{
  // Visible for testing
  static final String DEFAULT_SCHEDULER_NAME = "QuartzScheduler";

  private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

  private static final String DATA_SOURCE_NAME = "ods";

  private final JobFactory jobFactory;

  private final String schedulerName;

  public boolean disableForTesting;

  @Inject
  public TaskScheduler(
      JobFactory jobFactory,
      @Named("${scheduler.name:-" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName)
  {
    this.jobFactory = jobFactory;
    this.schedulerName = schedulerName;
  }

  // Visible for testing
  JobStoreTX createJobStore(ThreadPool threadPool) throws InvalidConfigurationException {
    JobStoreTX jobStoreTX = new JobStoreTX();
    jobStoreTX.setDataSource(DATA_SOURCE_NAME);
    jobStoreTX.setTablePrefix(OperationalDataStoreProvider.ID + ".QRTZ_");
    jobStoreTX.setUseProperties("true");
    jobStoreTX.setThreadPoolSize(threadPool.getPoolSize());
    DatabaseEngine dbEngine = getDatabaseEngine();
    if (H2DatabaseEngine.INSTANCE.equals(dbEngine)) {
      jobStoreTX.setIsClustered(false);
      jobStoreTX.setDriverDelegateClass(HSQLDBDelegate.class.getName());
    }
    else if (PostgresDatabaseEngine.INSTANCE.equals(dbEngine)) {
      jobStoreTX.setIsClustered(true);
      jobStoreTX.setClusterCheckinInterval(3000);
      jobStoreTX.setDriverDelegateClass(PostgreSQLDelegate.class.getName());
    }
    return jobStoreTX;
  }

  // Visible for testing
  SimpleThreadPool createThreadPool() {
    SimpleThreadPool simpleThreadPool = new SimpleThreadPool();
    simpleThreadPool.setMakeThreadsDaemons(true);
    simpleThreadPool.setThreadCount(10);
    return simpleThreadPool;
  }

  // Visible for testing
  DatabaseEngine getDatabaseEngine() {
    return DataSourceFactory.getDatabaseEngine(OperationalDataStoreProvider.getDataSource());
  }

  // Visible for testing
  Scheduler createScheduler() {
    try {
      DBConnectionManager.getInstance().addConnectionProvider(DATA_SOURCE_NAME, new QuartzConnectionProvider());
      String schedulerInstanceId = UUID.randomUUID().toString().replace("-", "");
      ThreadPool threadPool = createThreadPool();
      // This reuses the schedulerName and schedulerInstanceId for the Scheduler, ThreadPool, and JobStore
      DirectSchedulerFactory.getInstance()
          .createScheduler(schedulerName, schedulerInstanceId, threadPool, createJobStore(threadPool));
      Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
      scheduler.setJobFactory(jobFactory);
      return scheduler;
    }
    catch (SchedulerException | InvalidConfigurationException e) {
      throw new IllegalStateException("Could not create job scheduler", e);
    }
  }

  @Override
  public void start() throws Exception {
    if (disableForTesting) {
      return;
    }
    Scheduler scheduler = getScheduler();
    if (scheduler == null) {
      scheduler = createScheduler();
    }
    if (!scheduler.isStarted()) {
      scheduler.start();
      log.info("Started task scheduler");
    }
  }

  public void scheduleDailyTask(Class<? extends Job> jobClass, String name, LocalTime localTime) {
    CronScheduleBuilder schedule = CronScheduleBuilder.dailyAtHourAndMinute(localTime.getHour(), localTime.getMinute())
        .withMisfireHandlingInstructionDoNothing();

    JobDetail job = JobBuilder.newJob(jobClass) //
        .withIdentity(name) //
        .build();

    Trigger trigger = TriggerBuilder.newTrigger() //
        .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
        .withSchedule(schedule) //
        .build();
    scheduleTask(job, trigger);
  }

  private void scheduleTask(JobDetail job, Trigger... triggers) {
    try {
      getScheduler().scheduleJob(job, new HashSet<>(Arrays.asList(triggers)), true);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean unscheduleTask(String name) {
    return unscheduleTask(JobKey.jobKey(name));
  }

  private boolean unscheduleTask(JobKey jobKey) {
    try {
      return getScheduler().deleteJob(jobKey);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public Date getNextExecutionTime(String name) {
    return getTrigger(TriggerKey.triggerKey(name)).getNextFireTime();
  }

  private Trigger getTrigger(TriggerKey triggerKey) {
    try {
      return getScheduler().getTrigger(triggerKey);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() throws Exception {
    Scheduler scheduler = getScheduler();
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      log.info("Stopped task scheduler");
    }
  }

  // Visible for testing
  Scheduler getScheduler() {
    try {
      return DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }
}
