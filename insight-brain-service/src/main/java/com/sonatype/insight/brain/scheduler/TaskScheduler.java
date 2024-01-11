/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;

import io.dropwizard.lifecycle.Managed;
import org.quartz.CronScheduleBuilder;
import org.quartz.DailyTimeIntervalScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
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
import ru.vyarus.dropwizard.guice.module.installer.order.Order;

@Named
@Singleton
@Priority(TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY)
@Order(Integer.MAX_VALUE - TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY)
public class TaskScheduler
    implements Managed
{
  public static final int TASK_SCHEDULER_BEAN_PRIORITY = 1;

  // Visible for testing
  public static final String DEFAULT_SCHEDULER_NAME = "QuartzScheduler";

  static final String QUARTZ_NODE_ID = "quartz.nodeId";

  private static final long IDLE_WAIT_TIME = 5000;

  private static final Logger log = LoggerFactory.getLogger(TaskScheduler.class);

  protected final QuartzJobStoreTX quartzJobStoreTX;

  private final JobFactory jobFactory;

  protected final String schedulerName;

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

  SimpleThreadPool createThreadPool() {
    SimpleThreadPool simpleThreadPool = new SimpleThreadPool();
    simpleThreadPool.setMakeThreadsDaemons(true);
    simpleThreadPool.setThreadCount(10);
    return simpleThreadPool;
  }

  // Visible for testing
  public Scheduler createScheduler() {
    return createScheduler(schedulerName, quartzJobStoreTX);
  }

  protected Scheduler createScheduler(String schedulerName, QuartzJobStoreTX jobStoreTX) {
    try {
      String schedulerInstanceId = UUID.randomUUID().toString().replace("-", "");
      // This reuses the schedulerName and schedulerInstanceId for the Scheduler, ThreadPool, and JobStore
      DirectSchedulerFactory.getInstance().createScheduler(schedulerName, schedulerInstanceId, createThreadPool(),
              jobStoreTX, null, 0, IDLE_WAIT_TIME, -1);
      Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
      scheduler.setJobFactory(jobFactory);
      scheduler.addCalendar(NeverPastCalendar.CALENDAR_NAME, new NeverPastCalendar(), true, false);
      scheduler.getListenerManager().addTriggerListener(quartzTriggerListener);
      return scheduler;
    }
    catch (SchedulerException e) {
      throw new IllegalStateException("Could not create job scheduler", e);
    }
  }

  @Override
  public void start() throws Exception {
    startScheduler(schedulerName, quartzJobStoreTX);
  }

  protected void startScheduler(String schedulerName, QuartzJobStoreTX jobStoreTX) throws SchedulerException {
    Scheduler scheduler = getScheduler(schedulerName);
    if (scheduler == null) {
      scheduler = createScheduler(schedulerName, jobStoreTX);
    }
    if (disableForTesting) {
      return;
    }
    if (!scheduler.isStarted() || scheduler.isInStandbyMode()) {
      scheduler.start();
      log.info("Started task scheduler");
    }
  }

  public void triggerTaskNow(InsightJob insightJob, Map<String, String> parameters) {
    try {
      getScheduler(insightJob).triggerJob(toJobKey(insightJob), parameters != null ? new JobDataMap(parameters) : null);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public static Class<? extends Job> normalizeJobClass(Class<? extends Job> jobClass) {
    if (jobClass.getName().contains("Guice$$")) {
      // components employing AOP have runtime-generated subclasses, those aren't persistable for jobs
      jobClass = jobClass.getSuperclass().asSubclass(Job.class);
    }
    return jobClass;
  }

  protected JobBuilder newJob(InsightJob insightJob) {
    return JobBuilder.newJob(normalizeJobClass(insightJob.getClass()))
        .withIdentity(insightJob.getJobName());
  }

  public void scheduleDailyTask(InsightJob insightJob, LocalTime localTime) {
    CronScheduleBuilder schedule = CronScheduleBuilder.dailyAtHourAndMinute(localTime.getHour(), localTime.getMinute())
        .withMisfireHandlingInstructionDoNothing();
    JobDetail job = newJob(insightJob) //
        .build();

    Trigger trigger = TriggerBuilder.newTrigger() //
        .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
        .withSchedule(schedule) //
        .build();
    scheduleTask(job, insightJob, trigger);
  }

  public void scheduleOneTimeTask(InsightJob insightJob) {
    JobDetail job = newJob(insightJob)
        .build();
    Trigger trigger = TriggerBuilder.newTrigger()
        .withIdentity(job.getKey().getName(), job.getKey().getGroup())
        .startNow()
        .build();
    scheduleTask(job, insightJob, trigger);
  }

  public void scheduleOneTimeTask(InsightJob insightJob, LocalTime localTime) {
    JobDetail job = newJob(insightJob) //
        .build();
    Trigger trigger = TriggerBuilder.newTrigger() //
        .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
        .withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule() //
            .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(localTime.getHour(), localTime.getMinute())) //
            .withRepeatCount(0) //
            .withMisfireHandlingInstructionDoNothing()) //
        .build();
    scheduleTask(job, insightJob, trigger);
  }

  public void scheduleOneTimeTask(InsightJob insightJob, LocalDateTime localTime) {
    Date convertedDate = Date.from(localTime.atZone(ZoneId.systemDefault()).toInstant());

    JobDetail job = newJob(insightJob) //
        .build();
    Trigger trigger = TriggerBuilder.newTrigger() //
        .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0).withMisfireHandlingInstructionFireNow())
        .startAt(convertedDate)
        .build();
    scheduleTask(job, insightJob, trigger);
  }

  public boolean isJobTriggered(InsightJob insightJob, Map<String, Object> data) {
    try {
      for (Trigger trigger : getScheduler(insightJob).getTriggersOfJob(toJobKey(insightJob))) {
        if (data.equals(trigger.getJobDataMap().getWrappedMap())) {
          return true;
        }
      }
      return false;
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public void schedulePeriodicTask(InsightJob insightJob, Duration interval) {
    schedulePeriodicTask(insightJob, interval, null);
  }

  public void schedulePeriodicTask(InsightJob insightJob, Duration interval, Date startTime) {
    log.debug("Scheduling periodic task {} to run every {} starting at {}", insightJob.getJobName(), interval,
        startTime);
    JobDetail job = newJob(insightJob) //
        .build();
    TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder.newTrigger() //
        .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
        .withSchedule(SimpleScheduleBuilder.simpleSchedule() //
            .withIntervalInMilliseconds(interval.toMillis()) //
            .repeatForever() //
            .withMisfireHandlingInstructionNextWithRemainingCount()) //
        .modifiedByCalendar(NeverPastCalendar.CALENDAR_NAME);

    if (startTime != null) {
      triggerBuilder.startAt(startTime);
    }

    Trigger trigger = triggerBuilder.build();
    scheduleTask(job,insightJob,  trigger);
  }

  public void scheduleOneTimeTaskForAllOtherNodes(InsightJob insightJob) {
    scheduleOneTimeTaskForAllOtherNodes(insightJob, Collections.emptyMap());
  }

  public void scheduleOneTimeTaskForAllOtherNodes(
      InsightJob insightJob,
      Map<String, String> parameters)
  {
    Set<String> otherNodeIds = getOtherNodeIds();
    if (otherNodeIds.isEmpty()) {
      return;
    }

    JobDetail job = newJob(insightJob) //
        // non-durable for automatic removal once last trigger is gone
        // recovery/retry by another node doesn't make sense when binding execution to specific node
        .build();
    Set<Trigger> triggers = new HashSet<>();
    SimpleScheduleBuilder rightNowSchedule =
        // don't reschedule orphaned misfired triggers, somebody takes over ownership of them eventually
        SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires();

    // create one trigger for each node
    log.debug("Scheduling {} to be executed once on nodes {}.", insightJob.getJobName(), otherNodeIds);
    for (String nodeId : otherNodeIds) {
      JobDataMap jobDataMap = new JobDataMap(parameters);
      jobDataMap.put(QUARTZ_NODE_ID, nodeId); // bind to node
      Trigger trigger = TriggerBuilder.newTrigger() //
          .withIdentity(job.getKey().getName() + "For" + nodeId, job.getKey().getGroup()) //
          .usingJobData(jobDataMap)
          .withSchedule(rightNowSchedule) //
          .startNow() //
          .build();
      triggers.add(trigger);
    }
    scheduleTask(job, insightJob, triggers.toArray(new Trigger[0]));
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

  protected void scheduleTask(JobDetail job, InsightJob insightJob, Trigger... triggers) {
    scheduleTask(job, getScheduler(insightJob), triggers);
  }

  protected void scheduleTask(JobDetail job, Scheduler scheduler, Trigger... triggers) {
    try {
      scheduler.scheduleJob(job, new HashSet<>(Arrays.asList(triggers)), true);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean unscheduleTask(InsightJob insightJob) {
    return unscheduleTask(toJobKey(insightJob), insightJob);
  }

  protected boolean unscheduleTask(JobKey jobKey, InsightJob insightJob) {
    Scheduler scheduler = getScheduler(insightJob);
    return unscheduleTask(jobKey, scheduler);
  }

  protected boolean unscheduleTask(JobKey jobKey, Scheduler scheduler) {
    try {
      return scheduler.deleteJob(jobKey);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  public Date getNextExecutionTime(InsightJob insightJob) {
    return getTrigger(toTriggerKey(insightJob), insightJob).getNextFireTime();
  }

  private Trigger getTrigger(TriggerKey triggerKey, InsightJob insightJob) {
    try {
      return getScheduler(insightJob).getTrigger(triggerKey);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() throws Exception {
    Scheduler scheduler = getScheduler();
    shutdownScheduler(scheduler);
  }

  protected void shutdownScheduler(Scheduler scheduler) throws SchedulerException {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      log.info("Stopped task scheduler");
    }
  }

  /**
   *
   * @param insightJob - this is not used by TaskScheduler but is passed for usage by subclasses of TaskScheduler
   * @return Scheduler
   */
  protected Scheduler getScheduler(InsightJob insightJob) {
    return getScheduler();
  }

  public Scheduler getScheduler() {
    return getScheduler(schedulerName);
  }

  public Scheduler getScheduler(String schedulerName) {
    try {
      return DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  // Visible for testing
  public void clear() throws Exception {
    clearScheduler(getScheduler());
  }

  protected void clearScheduler(Scheduler scheduler) throws SchedulerException {
    if (scheduler != null) {
      scheduler.clear();
    }
  }

  // Visible for testing
  public void standby() throws Exception {
    standbyScheduler(getScheduler());
  }

  protected void standbyScheduler(Scheduler scheduler) throws SchedulerException {
    if (scheduler != null) {
      scheduler.standby();
    }
  }

  public boolean isSchedulerInitialized() {
    return getScheduler() != null;
  }

  public boolean isTaskScheduled(InsightJob insightJob) {
    return isTaskScheduled(toJobKey(insightJob), insightJob);
  }

  private boolean isTaskScheduled(JobKey jobKey, InsightJob insightJob) {
    try {
      return getScheduler(insightJob).checkExists(jobKey);
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  protected JobKey toJobKey(InsightJob insightJob) {
    return JobKey.jobKey(insightJob.getJobName());
  }

  protected TriggerKey toTriggerKey(InsightJob insightJob) {
    return TriggerKey.triggerKey(insightJob.getJobName());
  }
}
