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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.utils.Retry;

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

  private final ShutdownHandler shutdownHandler;

  protected final QuartzJobSchedulingService quartzJobSchedulingService;

  public boolean disableForTesting;

  @Inject
  public TaskScheduler(
      QuartzJobStoreTX quartzJobStoreTX,
      JobFactory jobFactory,
      @Named("${scheduler.name:-" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener,
      ShutdownHandler shutdownHandler,
      QuartzJobSchedulingService quartzJobSchedulingService)
  {
    this.quartzJobStoreTX = quartzJobStoreTX;
    this.jobFactory = jobFactory;
    this.schedulerName = schedulerName;
    this.quartzTriggerListener = quartzTriggerListener;
    this.shutdownHandler = shutdownHandler;
    this.quartzJobSchedulingService = quartzJobSchedulingService;
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
    if (shutdownHandler.isAfterGracePeriod()) {
      return null;
    }
    try {
      String schedulerInstanceId = UUID.randomUUID().toString().replace("-", "");
      // This reuses the schedulerName and schedulerInstanceId for the Scheduler, ThreadPool, and JobStore
      DirectSchedulerFactory.getInstance().createScheduler(schedulerName, schedulerInstanceId, createThreadPool(),
              jobStoreTX, null, 0, IDLE_WAIT_TIME, -1);
      Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
      scheduler.setJobFactory(jobFactory);
      scheduler.addCalendar(NeverPastCalendar.CALENDAR_NAME, new NeverPastCalendar(), true, false);
      scheduler.getListenerManager().addTriggerListener(quartzTriggerListener);
      shutdownHandler.add(scheduler, ShutdownPriority.QUARTZ_SCHEDULERS);
      return scheduler;
    }
    catch (SchedulerException e) {
      throw new IllegalStateException("Could not create job scheduler", e);
    }
  }

  public void initialize() {
    createScheduler(schedulerName, quartzJobStoreTX);
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
    startScheduler(scheduler);
  }

  public void startScheduler(Scheduler scheduler) throws SchedulerException {
    if (scheduler != null && !scheduler.isShutdown() && (!scheduler.isStarted() || scheduler.isInStandbyMode())) {
      scheduler.start();
      log.info("Started task scheduler {}", scheduler.getSchedulerName());
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

    scheduleTask(insightJob, job, JobLogger.daily(this::getNextExecutionTime, insightJob, job.getKey()), trigger);
  }

  public void scheduleOneTimeTask(InsightJob insightJob) {
    JobDetail job = newJob(insightJob)
        .build();
    Trigger trigger = TriggerBuilder.newTrigger()
        .withIdentity(job.getKey().getName(), job.getKey().getGroup())
        .startNow()
        .build();

    scheduleTask(insightJob, job, JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()),
        trigger);
  }

  public void scheduleOneTimeTask(InsightJob insightJob, Map<String, String> parameters) {
    JobDetail job = newJob(insightJob)
        .build();
    JobDataMap jobDataMap = new JobDataMap(parameters);
    Trigger trigger = TriggerBuilder.newTrigger()
        .withIdentity(job.getKey().getName(), job.getKey().getGroup())
        .usingJobData(jobDataMap)
        .startNow()
        .build();

    scheduleTask(insightJob, job, JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()),
        trigger);
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

    scheduleTask(insightJob, job, JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()),
        trigger);
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
    scheduleTask(insightJob, job, JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()),
        trigger);
  }

  public boolean isJobTriggered(InsightJob insightJob, Map<String, Object> data) {
    try {
      for (Trigger trigger : getTriggersForJob(insightJob)) {
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

  private List<? extends Trigger> getTriggersForJob(InsightJob insightJob) throws SchedulerException {
    JobKey jobKey = toJobKey(insightJob);
    Scheduler scheduler = getScheduler(insightJob);
    if (scheduler != null) {
      return scheduler.getTriggersOfJob(jobKey);
    }
    return getQuartzJobStoreTX(insightJob).getTriggersForJob(jobKey);
  }

  public void schedulePeriodicTask(InsightJob insightJob, Duration interval) {
    schedulePeriodicTask(insightJob, interval, null);
  }

  public void schedulePeriodicTask(InsightJob insightJob, Duration interval, Date startTime) {
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
    scheduleTask(insightJob, job, JobLogger.periodic(this::getNextExecutionTime, insightJob, job.getKey(), interval),
        trigger);
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
    scheduleTask(insightJob, job, JobLogger.onOtherNodes(insightJob, job.getKey(), otherNodeIds),
        triggers.toArray(new Trigger[0]));
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

  protected void scheduleTask(InsightJob insightJob, JobDetail job, JobLogger jobLogger, Trigger... triggers) {
    Scheduler scheduler = getScheduler(insightJob);

    scheduleTask(scheduler, job, jobLogger, triggers);
  }

  protected void scheduleTask(Scheduler scheduler, JobDetail job, JobLogger jobLogger, Trigger... triggers) {
    if (scheduler != null) {
      quartzJobSchedulingService.scheduleTask(scheduler, job, Set.of(triggers), jobLogger);
    }
    else {
      log.warn("Cannot schedule task, jobKey '{}' for tenant {} because a scheduler is not available.", job.getKey(),
          TenantThreadLocal.getTenant(), new Exception("Scheduler is not available."));
    }
  }

  public boolean unscheduleTask(InsightJob insightJob) {
    return unscheduleTask(toJobKey(insightJob), insightJob);
  }

  protected boolean unscheduleTask(JobKey jobKey, InsightJob insightJob) {
    Scheduler scheduler = getScheduler(insightJob);
    if (scheduler != null) {
      return unscheduleTask(jobKey, scheduler);
    }
    return unscheduleTask(jobKey, getQuartzJobStoreTX(insightJob));
  }

  protected boolean unscheduleTask(JobKey jobKey, Scheduler scheduler) {
    Retry retry = new Retry(
        "unscheduleTask " + jobKey,
        4,
        null,
        e -> e.getMessage().contains("Unable to unschedule trigger"),
        Duration::ofSeconds
    );
    try {
      return retry.executeCallable(() -> quartzJobSchedulingService.unscheduleTask(scheduler, jobKey));
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  protected boolean unscheduleTask(JobKey jobKey, QuartzJobStoreTX quartzJobStoreTX) {
    try {
      return quartzJobStoreTX.removeJob(jobKey);
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

  public void shutdownScheduler(Scheduler scheduler) throws SchedulerException {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
      log.info("Stopped task scheduler {}", scheduler.getSchedulerName());
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

  protected QuartzJobStoreTX getQuartzJobStoreTX(@SuppressWarnings("unused") InsightJob insightJob) {
    return quartzJobStoreTX;
  }

  public List<String> getJobGroupNames(QuartzJobStoreTX quartzJobStoreTX) throws JobPersistenceException {
    return quartzJobStoreTX.getJobGroupNames();
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

  public void standbyScheduler(Scheduler scheduler) throws SchedulerException {
    if (scheduler != null && !scheduler.isShutdown() && !scheduler.isInStandbyMode()) {
      scheduler.standby();
      log.info("Standby task scheduler {}", scheduler.getSchedulerName());
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
      Scheduler scheduler = getScheduler(insightJob);
      if (scheduler != null) {
        return scheduler.checkExists(jobKey);
      }
      return getQuartzJobStoreTX(insightJob).checkExists(jobKey);
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
