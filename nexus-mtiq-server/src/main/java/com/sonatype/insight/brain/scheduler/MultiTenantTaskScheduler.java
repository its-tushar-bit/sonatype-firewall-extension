/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.List;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.cluster.CloudyClusterConfigReader;
import com.sonatype.insight.brain.cluster.CloudyClusterState;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantContextJobListener;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.jetbrains.annotations.NotNull;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.order.Order;

@Named
@Singleton
@Priority(TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY)
@Order(Integer.MAX_VALUE - TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY)
public class MultiTenantTaskScheduler
    extends TaskScheduler
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantTaskScheduler.class);

  //Visible for test
  static final String TASK_SCHEDULER_THREAD_POOL_SIZE = "TASK_SCHEDULER_THREAD_POOL_SIZE";

  private final TenantContextJobListener tenantContextJobListener;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final TenantManager tenantManager;

  private final TenantUtil tenantUtil;

  private final QuartzJobStoreTX mtiqBatchJobStoreTX;

  private final CloudyClusterConfigReader cloudyClusterConfigReader;

  @Inject
  public MultiTenantTaskScheduler(
      MultiTenantQuartzJobStoreTX quartzJobStoreTX,
      MultiTenantBatchModeJobStoreTX mtiqBatchJobStoreTX,
      JobFactory jobFactory,
      @Named("${scheduler.name:-" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener,
      TenantContextJobListener tenantContextJobListener,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      TenantManager tenantManager,
      TenantUtil tenantUtil,
      ShutdownHandler shutdownHandler,
      CloudyClusterConfigReader cloudyClusterConfigReader)
  {
    super(quartzJobStoreTX, jobFactory, schedulerName, quartzTriggerListener, shutdownHandler);

    this.mtiqBatchJobStoreTX = mtiqBatchJobStoreTX;
    this.tenantContextJobListener = tenantContextJobListener;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
    this.cloudyClusterConfigReader = cloudyClusterConfigReader;
  }

  @Override
  public void initialize() {
    createScheduler(schedulerName, quartzJobStoreTX);

    if (tenantUtil.isMtiqBatchMode()) {
      createScheduler(getMtiqBatchSchedulerName(), mtiqBatchJobStoreTX);
    }
  }

  @Override
  public void start() throws Exception {
    assertTenantsArePreRegistered();

    startOrStandbyTaskSchedulers();
  }

  private void assertTenantsArePreRegistered() {
    if (!tenantManager.areTenantsPreRegistered()) {
      // If this ever fails, ensure TenantManager is started BEFORE MultiTenantTaskScheduler
      System.err.println("Fatal error: Task scheduler is trying to start but tenants are not pre-registered yet");
      System.exit(11);
    }
  }

  public void startOrStandbyTaskSchedulers() throws Exception {
    if (shouldStartTaskSchedulers()) {
      doStart();
    }
    else {
      standby();
    }
  }

  private boolean shouldStartTaskSchedulers() {
    CloudyClusterState cloudyClusterState = cloudyClusterConfigReader.getClusterConfig().getState();
    switch (cloudyClusterState) {
      case UNKNOWN:
      case ACTIVE:
      case FILLING: {
        log.trace("Starting the task schedulers if needed due to the cluster state {}.", cloudyClusterState);
        return true;
      }
      case DRAINING:
      case INACTIVE: {
        log.trace("Standby the task schedulers if needed due to the cluster state {}.", cloudyClusterState);
        return false;
      }
      default:
        throw new IllegalArgumentException(String.format("Unrecognized cluster state %s.", cloudyClusterState));
    }
  }

  private void doStart() throws Exception {
    /*
     * When running in "mtiq batch mode" all quartz jobs that implement AllTenantsJob are scheduled via a
     * separate Quartz scheduler however those nodes still need to be able to handle the clustering events.
     *
     * For normal MTIQ all jobs run through a single Quartz scheduler.
     */
    startScheduler(schedulerName, quartzJobStoreTX);

    if (tenantUtil.isMtiqBatchMode()) {
      startScheduler(getMtiqBatchSchedulerName(), mtiqBatchJobStoreTX);
    }
  }

  @Override
  public void stop() throws Exception {
    super.stop();

    if (tenantUtil.isMtiqBatchMode()) {
      shutdownScheduler(getScheduler(getMtiqBatchSchedulerName()));
    }
  }

  // Visible for testing
  @NotNull
  String getMtiqBatchSchedulerName() {
    return "MtiqMtiqBatch" + schedulerName;
  }

  @Override
  public Scheduler createScheduler(String schedulerName, QuartzJobStoreTX jobStoreTX) {
    try {
      Scheduler scheduler = super.createScheduler(schedulerName, jobStoreTX);
      scheduler.getListenerManager().addJobListener(tenantContextJobListener);
      return scheduler;
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  SimpleThreadPool createThreadPool() {
    SystemConfigurationProperty configuration =
        systemConfigurationPropertyDAO.getByName(TASK_SCHEDULER_THREAD_POOL_SIZE);
    int threadPoolSize = configuration != null ? Integer.parseInt(configuration.getValue()) : 10;

    SimpleThreadPool threadPool = super.createThreadPool();
    threadPool.setThreadCount(threadPoolSize);

    return threadPool;
  }

  @Override
  protected void scheduleTask(JobDetail job, InsightJob insightJob, Trigger... triggers) {
    Scheduler scheduler = getSchedulerForJobType(job.getJobClass());

    super.scheduleTask(job, scheduler, triggers);
  }

  @Override
  public boolean unscheduleTask(InsightJob insightJob) {
    // When no tenant is specified (e.g. global) unschedule this task for all tenants
    if (tenantUtil.isGlobalTenant()) {
      try {
        List<String> tenantSlugs = getJobGroupNames(getQuartzJobStoreTX(insightJob));

        boolean unscheduled = false;
        for (String tenantSlug : tenantSlugs) {
          boolean result = unscheduleTask(toJobKey(insightJob, tenantSlug), insightJob);

          if (result) {
            unscheduled = true;
          }
        }

        return unscheduled;
      }
      catch (SchedulerException e) {
        throw new RuntimeException(e);
      }
    }
    else {
      return unscheduleTask(toJobKey(insightJob), insightJob);
    }
  }

  @Override
  protected JobBuilder newJob(InsightJob insightJob) {
    return JobBuilder.newJob(normalizeJobClass(insightJob.getClass()))
        .withIdentity(toJobKey(insightJob));
  }

  @Override
  protected JobKey toJobKey(InsightJob insightJob) {
    return toJobKey(insightJob, tenantManager.getTenant().tenantSlug);
  }

  protected JobKey toJobKey(InsightJob insightJob, String tenantSlug) {
    return JobKey.jobKey(insightJob.getJobName(), tenantSlug);
  }

  @Override
  protected TriggerKey toTriggerKey(InsightJob insightJob) {
    return TriggerKey.triggerKey(insightJob.getJobName(), tenantManager.getTenant().tenantSlug);
  }

  @Override
  public void clear() throws Exception {
    clearScheduler(getScheduler());
    clearScheduler(getScheduler(getMtiqBatchSchedulerName()));
  }

  @Override
  public void standby() throws Exception {
    standbyScheduler(getScheduler());
    standbyScheduler(getScheduler(getMtiqBatchSchedulerName()));
  }

  @Override
  public boolean isSchedulerInitialized() {
    return getScheduler() != null && getScheduler(getMtiqBatchSchedulerName()) != null;
  }

  @Override
  public Scheduler getScheduler(InsightJob insightJob) {
    return getSchedulerForJobType(insightJob.getClass());
  }

  @Override
  protected QuartzJobStoreTX getQuartzJobStoreTX(InsightJob insightJob) {
    return getQuartzJobStoreTXForJobType(insightJob.getClass());
  }

  @Override
  protected boolean unscheduleTask(JobKey jobKey, InsightJob insightJob) {
    return super.unscheduleTask(jobKey, insightJob);
  }

  private Scheduler getSchedulerForJobType(Class<? extends Job> jobType) {
    if (tenantUtil.isMtiqBatchMode() && tenantUtil.isMtiqBatchJob(jobType)) {
      return getScheduler(getMtiqBatchSchedulerName());
    }
    else {
      return getScheduler();
    }
  }

  private QuartzJobStoreTX getQuartzJobStoreTXForJobType(Class<? extends Job> jobType) {
    if (tenantUtil.isMtiqBatchMode() && tenantUtil.isMtiqBatchJob(jobType)) {
      return mtiqBatchJobStoreTX;
    }
    else {
      return quartzJobStoreTX;
    }
  }
}
