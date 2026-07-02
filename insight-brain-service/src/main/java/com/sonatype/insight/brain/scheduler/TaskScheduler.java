/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService.BuiltJob;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.utils.Retry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.time.DayOfWeek;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sonatype.insight.brain.lifecycle.Managed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.ClassUtils;

@Named
@Singleton
@DependsOn("staticInjectionInitializer")
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

  private final QuartzConcurrencyListener quartzConcurrencyListener;

  protected final ShutdownHandler shutdownHandler;

  protected final QuartzJobSchedulingService quartzJobSchedulingService;

  public boolean disableForTesting;

  @Inject
  public TaskScheduler(
      QuartzJobStoreTX quartzJobStoreTX,
      JobFactory jobFactory,
      @Value("${scheduler.name:" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener,
      QuartzConcurrencyListener quartzConcurrencyListener,
      ShutdownHandler shutdownHandler,
      QuartzJobSchedulingService quartzJobSchedulingService)
  {
    this.quartzJobStoreTX = quartzJobStoreTX;
    this.jobFactory = jobFactory;
    this.schedulerName = schedulerName;
    this.quartzTriggerListener = quartzTriggerListener;
    this.quartzConcurrencyListener = quartzConcurrencyListener;
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
      Scheduler existingScheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
      if (existingScheduler != null) {
        if (!existingScheduler.isShutdown()) {
          log.warn("Reusing existing Quartz scheduler {}; this may indicate duplicate TaskScheduler initialization " +
              "or leaked scheduler state from a previous test.", schedulerName);
          return existingScheduler;
        }
        SchedulerRepository.getInstance().remove(schedulerName);
      }

      String schedulerInstanceId = UUID.randomUUID().toString().replace("-", "");
      // This reuses the schedulerName and schedulerInstanceId for the Scheduler, ThreadPool, and JobStore
      DirectSchedulerFactory.getInstance()
          .createScheduler(schedulerName, schedulerInstanceId, createThreadPool(),
              jobStoreTX, null, 0, IDLE_WAIT_TIME, -1);
      Scheduler scheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerName);
      scheduler.setJobFactory(jobFactory);
      scheduler.addCalendar(NeverPastCalendar.CALENDAR_NAME, new NeverPastCalendar(), true, false);
      scheduler.getListenerManager().addTriggerListener(quartzTriggerListener);
      scheduler.getListenerManager().addTriggerListener(quartzConcurrencyListener);
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

  /**
   * Overrides the bridge default so that Spring's bean initialization does NOT auto-start the scheduler.
   * DefaultApplicationLifecycle.boot() creates the scheduler, and DefaultTenantManagedInitializer
   * starts it explicitly via {@link #start()} before tenant job registration.
   */
  @Override
  public void afterPropertiesSet() {
    // no-op — defer scheduler startup
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
    Class<?> userClass = ClassUtils.getUserClass(jobClass);
    if (userClass != jobClass && Job.class.isAssignableFrom(userClass)) {
      // components employing AOP may be runtime-generated subclasses, which are not persistable Quartz job classes
      return userClass.asSubclass(Job.class);
    }
    return jobClass;
  }

  protected JobBuilder newJob(InsightJob insightJob) {
    return JobBuilder.newJob(normalizeJobClass(insightJob.getClass()))
        .withIdentity(insightJob.getJobName());
  }

  public void scheduleDailyTask(InsightJob insightJob, LocalTime localTime) {
    scheduleTask(insightJob, () -> {
      CronScheduleBuilder schedule =
          CronScheduleBuilder.dailyAtHourAndMinute(localTime.getHour(), localTime.getMinute())
              .withMisfireHandlingInstructionDoNothing();
      JobDetail job = newJob(insightJob) //
          .build();
      Trigger trigger = TriggerBuilder.newTrigger() //
          .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
          .withSchedule(schedule) //
          .build();
      return new BuiltJob(job, Set.of(trigger),
          JobLogger.daily(this::getNextExecutionTime, insightJob, job.getKey()));
    });
  }

  public void scheduleWeeklyTask(InsightJob insightJob, DayOfWeek dayOfWeek, LocalTime localTime) {
    scheduleTask(insightJob, () -> {
      // DayOfWeek.getValue() returns 1 (Mon) – 7 (Sun); Calendar uses 1 (Sun) – 7 (Sat)
      int calendarDay = (dayOfWeek.getValue() % 7) + 1;
      CronScheduleBuilder schedule =
          CronScheduleBuilder.weeklyOnDayAndHourAndMinute(calendarDay, localTime.getHour(), localTime.getMinute())
              .withMisfireHandlingInstructionDoNothing();
      JobDetail job = newJob(insightJob).build();
      Trigger trigger = TriggerBuilder.newTrigger()
          .withIdentity(job.getKey().getName(), job.getKey().getGroup())
          .withSchedule(schedule)
          .build();
      return new BuiltJob(job, Set.of(trigger),
          JobLogger.weekly(this::getNextExecutionTime, insightJob, job.getKey()));
    });
  }

  public void scheduleOneTimeTask(InsightJob insightJob) {
    scheduleTask(insightJob, () -> {
      JobDetail job = newJob(insightJob).build();
      Trigger trigger = TriggerBuilder.newTrigger()
          .withIdentity(job.getKey().getName(), job.getKey().getGroup())
          .startNow()
          .build();
      return new BuiltJob(job, Set.of(trigger),
          JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()));
    });
  }

  public void scheduleOneTimeTask(InsightJob insightJob, Map<String, String> parameters) {
    scheduleTask(insightJob, () -> {
      JobDetail job = newJob(insightJob).build();
      JobDataMap jobDataMap = new JobDataMap(parameters);
      Trigger trigger = TriggerBuilder.newTrigger()
          .withIdentity(job.getKey().getName(), job.getKey().getGroup())
          .usingJobData(jobDataMap)
          .startNow()
          .build();
      return new BuiltJob(job, Set.of(trigger),
          JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()));
    });
  }

  public void scheduleOneTimeTask(InsightJob insightJob, LocalTime localTime) {
    scheduleTask(insightJob, () -> {
      JobDetail job = newJob(insightJob) //
          .build();
      Trigger trigger = TriggerBuilder.newTrigger() //
          .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
          .withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule() //
              .startingDailyAt(TimeOfDay.hourAndMinuteOfDay(localTime.getHour(), localTime.getMinute())) //
              .withRepeatCount(0) //
              .withMisfireHandlingInstructionDoNothing()) //
          .build();
      return new BuiltJob(job, Set.of(trigger),
          JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()));
    });
  }

  public void scheduleOneTimeTask(InsightJob insightJob, LocalDateTime localTime) {
    scheduleTask(insightJob, () -> {
      Date convertedDate = Date.from(localTime.atZone(ZoneId.systemDefault()).toInstant());
      JobDetail job = newJob(insightJob) //
          .build();
      Trigger trigger = TriggerBuilder.newTrigger() //
          .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
          .withSchedule(
              SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0).withMisfireHandlingInstructionFireNow())
          .startAt(convertedDate)
          .build();
      return new BuiltJob(job, Set.of(trigger),
          JobLogger.oneTime(this::getNextExecutionTime, insightJob, job.getKey()));
    });
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
    scheduleTask(insightJob, () -> {
      JobDetail job = newJob(insightJob) //
          .build();
      TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder.newTrigger() //
          .withIdentity(job.getKey().getName(), job.getKey().getGroup()) //
          .withSchedule(SimpleScheduleBuilder.simpleSchedule() //
              .withIntervalInMilliseconds(interval.toMillis()) //
              .repeatForever() //
              .withMisfireHandlingInstructionNextWithRemainingCount()) //
          .modifiedByCalendar(NeverPastCalendar.CALENDAR_NAME);
      // If startTime is null, TriggerBuilder.newTrigger() defaults it to "now" at build time — evaluated on the
      // batching thread (see QuartzJobSchedulingService javadoc for why that matters vs. enqueue time).
      if (startTime != null) {
        triggerBuilder.startAt(startTime);
      }
      Trigger trigger = triggerBuilder.build();
      return new BuiltJob(job, Set.of(trigger),
          JobLogger.periodic(this::getNextExecutionTime, insightJob, job.getKey(), interval));
    });
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

    scheduleTask(insightJob, () -> {
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
      return new BuiltJob(job, triggers, JobLogger.onOtherNodes(insightJob, job.getKey(), otherNodeIds));
    });
  }

  // Visible for testing
  Set<String> getOtherNodeIds() {
    if (!quartzJobStoreTX.isReadyForClusterQueries()) {
      return Collections.emptySet();
    }
    Set<String> otherNodeIds;
    try {
      otherNodeIds = quartzJobStoreTX.getSchedulerStateRecords()
          .stream()
          .map(SchedulerStateRecord::getSchedulerInstanceId)
          .collect(Collectors.toSet());
    }
    catch (JobPersistenceException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    otherNodeIds.remove(quartzJobStoreTX.getInstanceId());
    return otherNodeIds;
  }

  /**
   * Enqueues an {@link InsightJob} for batched scheduling. Returns as soon as the job is queued; the {@code builder}
   * is invoked later on the shared scheduling thread. Callers may {@link #unscheduleTask(InsightJob)} immediately
   * after this returns — the job key is captured eagerly, so the pending record can be dropped before the supplier
   * ever runs. See {@link QuartzJobSchedulingService} for the batching rationale and the reason builds happen at
   * flush time.
   */
  protected void scheduleTask(InsightJob insightJob, Supplier<BuiltJob> builder) {
    Scheduler scheduler = getScheduler(insightJob);
    scheduleTask(scheduler, toJobKey(insightJob), builder);
  }

  protected void scheduleTask(Scheduler scheduler, JobKey jobKey, Supplier<BuiltJob> builder) {
    if (scheduler != null) {
      quartzJobSchedulingService.scheduleTask(scheduler, jobKey, builder);
    }
    else {
      log.warn("Cannot schedule task, jobKey '{}' for tenant {} because a scheduler is not available.", jobKey,
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
        Duration::ofSeconds);
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
    Trigger trigger = getTrigger(toTriggerKey(insightJob), insightJob);
    return trigger != null ? trigger.getNextFireTime() : null;
  }

  private Trigger getTrigger(TriggerKey triggerKey, InsightJob insightJob) {
    try {
      Scheduler scheduler = getScheduler(insightJob);
      return scheduler != null ? scheduler.getTrigger(triggerKey) : null;
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
    if (scheduler == null) {
      return;
    }

    String schedulerName = scheduler.getSchedulerName();
    if (!scheduler.isShutdown()) {
      scheduler.shutdown();
      log.info("Stopped task scheduler {}", schedulerName);
    }
    SchedulerRepository.getInstance().remove(schedulerName);
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
