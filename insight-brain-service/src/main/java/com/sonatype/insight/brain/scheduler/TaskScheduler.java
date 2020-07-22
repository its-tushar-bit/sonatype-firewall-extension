/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import org.quartz.CronScheduleBuilder;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TimeOfDay;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class TaskScheduler
    implements Managed
{
  // Visible for testing
  static final String DEFAULT_SCHEDULER_NAME = "QuartzScheduler";

  static final String QUARTZ_NODE_ID = "quartz.nodeId";

  private static final long IDLE_WAIT_TIME = 5000;

  private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

  private final QuartzJobStoreTX quartzJobStoreTX;

  private final JobFactory jobFactory;

  private final String schedulerName;

  private final QuartzTriggerListener quartzTriggerListener;

  public boolean disableForTesting;

  @Inject
  public TaskScheduler(
      QuartzJobStoreTX quartzJobStoreTX,
      JobFactory jobFactory,
      @Named("${scheduler.name:-" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener)
  {
    this.quartzJobStoreTX = quartzJobStoreTX;
    this.jobFactory = jobFactory;
    this.schedulerName = schedulerName;
    this.quartzTriggerListener = quartzTriggerListener;
  }

  // Visible for testing
  SimpleThreadPool createThreadPool() {
    SimpleThreadPool simpleThreadPool = new SimpleThreadPool();
    simpleThreadPool.setMakeThreadsDaemons(true);
    simpleThreadPool.setThreadCount(10);
    return simpleThreadPool;
  }

  // Visible for testing
  public Scheduler createScheduler() {
    try {
      String schedulerInstanceId = UUID.randomUUID().toString().replace("-", "");
      // This reuses the schedulerName and schedulerInstanceId for the Scheduler, ThreadPool, and JobStore
      DirectSchedulerFactory.getInstance().createScheduler(schedulerName, schedulerInstanceId, createThreadPool(),
          quartzJobStoreTX, null, 0, IDLE_WAIT_TIME, -1);
      Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
      scheduler.setJobFactory(jobFactory);
      scheduler.getListenerManager().addTriggerListener(quartzTriggerListener);
      return scheduler;
    }
    catch (SchedulerException e) {
      throw new IllegalStateException("Could not create job scheduler", e);
    }
  }

  @Override
  public void start() throws Exception {
    Scheduler scheduler = getScheduler();
    if (scheduler == null) {
      scheduler = createScheduler();
    }
    if (disableForTesting) {
      return;
    }
    if (!scheduler.isStarted()) {
      scheduler.start();
      log.info("Started task scheduler");
    }
  }

  public void triggerTaskNow(String name) {
    try {
      getScheduler().triggerJob(JobKey.jobKey(name));
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
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

  public void scheduleOneTimeTask(Class<? extends Job> jobClass, String name, LocalTime localTime) {
    JobDetail job = JobBuilder.newJob(jobClass) //
        .withIdentity(name) //
        .build();
    Trigger trigger = TriggerBuilder.newTrigger() //
        .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
        .withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule() //
            .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(localTime.getHour(), localTime.getMinute())) //
            .withRepeatCount(0) //
            .withMisfireHandlingInstructionDoNothing()) //
        .build();
    scheduleTask(job, trigger);
  }

  public void schedulePeriodicTask(Class<? extends Job> jobClass, String name, Duration interval) {
    JobDetail job = JobBuilder.newJob(jobClass) //
        .withIdentity(name) //
        .build();
    Trigger trigger = TriggerBuilder.newTrigger() //
        .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
        .withSchedule(SimpleScheduleBuilder.simpleSchedule() //
            .withIntervalInMilliseconds(interval.toMillis()) //
            .repeatForever() //
            .withMisfireHandlingInstructionIgnoreMisfires()) //
        .build();
    scheduleTask(job, trigger);
  }

  public void scheduleOneTimeTaskForAllOtherNodes(Class<? extends Job> jobClass, String name) {
    Set<String> otherNodeIds = getOtherNodeIds();
    if (otherNodeIds.isEmpty()) {
      return;
    }

    JobDetail job = JobBuilder.newJob(jobClass) //
        .withIdentity(name) //
        // non-durable for automatic removal once last trigger is gone
        // recovery/retry by another node doesn't make sense when binding execution to specific node
        .build();
    Set<Trigger> triggers = new HashSet<>();
    SimpleScheduleBuilder rightNowSchedule =
        // don't reschedule orphaned misfired triggers, somebody takes over ownership of them eventually
        SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires();

    // create one trigger for each node
    log.debug("Scheduling {} to be executed once on nodes {}.", name, otherNodeIds);
    for (String nodeId : otherNodeIds) {
      Trigger trigger = TriggerBuilder.newTrigger() //
          .withIdentity(job.getKey().getName() + "For" + nodeId, job.getKey().getGroup()) //
          .usingJobData(QUARTZ_NODE_ID, nodeId) // bind to node
          .withSchedule(rightNowSchedule) //
          .startNow() //
          .build();
      triggers.add(trigger);
    }
    scheduleTask(job, triggers.toArray(new Trigger[0]));
  }

  // Visible for testing
  Set<String> getOtherNodeIds() {
    Set<String> otherNodeIds;
    try {
      otherNodeIds = quartzJobStoreTX.getSchedulerStateRecords().stream()
          .map(SchedulerStateRecord::getSchedulerInstanceId)
          .collect(Collectors.toSet());
    }
    catch (JobPersistenceException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    otherNodeIds.remove(quartzJobStoreTX.getInstanceId());
    return otherNodeIds;
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
  public Scheduler getScheduler() {
    try {
      return DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  // Visible for testing
  public void clear() throws Exception {
    Scheduler scheduler = getScheduler();
    if (scheduler != null) {
      scheduler.clear();
    }
  }

  // Visible for testing
  public void standby() throws Exception {
    Scheduler scheduler = getScheduler();
    if (scheduler != null) {
      scheduler.standby();
    }
  }
}
