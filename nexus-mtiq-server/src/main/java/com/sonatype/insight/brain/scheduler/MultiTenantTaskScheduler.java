/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService.BuiltJob;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantContextJobListener;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;

@Named
@Singleton
@Primary
public class MultiTenantTaskScheduler
    extends TaskScheduler
    implements SmartInitializingSingleton
{
  // Visible for test
  static final String TASK_SCHEDULER_THREAD_POOL_SIZE = "TASK_SCHEDULER_THREAD_POOL_SIZE";

  private final Provider<TenantContextJobListener> tenantContextJobListener;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final Provider<TenantManager> tenantManager;

  private final TenantUtil tenantUtil;

  private final QuartzJobStoreTX mtiqBatchJobStoreTX;

  @Inject
  public MultiTenantTaskScheduler(
      MultiTenantQuartzJobStoreTX quartzJobStoreTX,
      MultiTenantBatchModeJobStoreTX mtiqBatchJobStoreTX,
      JobFactory jobFactory,
      @Value("${scheduler.name:" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener,
      QuartzConcurrencyListener quartzConcurrencyListener,
      Provider<TenantContextJobListener> tenantContextJobListener,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      Provider<TenantManager> tenantManager,
      TenantUtil tenantUtil,
      ShutdownHandler shutdownHandler,
      QuartzJobSchedulingService quartzJobSchedulingService)
  {
    super(quartzJobStoreTX, jobFactory, schedulerName, quartzTriggerListener, quartzConcurrencyListener,
        shutdownHandler, quartzJobSchedulingService);

    this.mtiqBatchJobStoreTX = mtiqBatchJobStoreTX;
    this.tenantContextJobListener = tenantContextJobListener;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void initialize() {
    createScheduler(schedulerName, quartzJobStoreTX);
    createScheduler(getMtiqBatchSchedulerName(), mtiqBatchJobStoreTX);
  }

  @Override
  public void start() throws Exception {
    ensureTenantsArePreRegistered();

    doStart();
  }

  @Override
  public void afterSingletonsInstantiated() {
    ensureTenantsArePreRegistered();
    try {
      doStart();
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to start multi-tenant task scheduler", e);
    }
  }

  private void ensureTenantsArePreRegistered() {
    tenantManager.get().ensureTenantsPreRegistered();
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
      if (scheduler != null) {
        scheduler.getListenerManager().addJobListener(tenantContextJobListener.get());
      }
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
  protected void scheduleTask(InsightJob insightJob, Supplier<BuiltJob> builder) {
    // Route to the correct scheduler (main vs. MTIQ batch) by AOP-normalized job class. The normalization here must
    // match what TaskScheduler#newJob applies inside the supplier when it constructs the JobDetail; otherwise the
    // scheduler we pick now would not match the JobDetail's job class at flush time. The old override that this
    // replaces routed by JobDetail#getJobClass() (already-normalized by newJob), so this preserves that behavior.
    Class<? extends Job> normalizedClass = normalizeJobClass(insightJob.getClass());
    Scheduler scheduler = getSchedulerForJobType(normalizedClass);
    super.scheduleTask(scheduler, toJobKey(insightJob), builder);
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
    return toJobKey(insightJob, tenantManager.get().getTenant().tenantSlug);
  }

  protected JobKey toJobKey(InsightJob insightJob, String tenantSlug) {
    return JobKey.jobKey(insightJob.getJobName(), tenantSlug);
  }

  @Override
  protected TriggerKey toTriggerKey(InsightJob insightJob) {
    return TriggerKey.triggerKey(insightJob.getJobName(), tenantManager.get().getTenant().tenantSlug);
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
    if (tenantUtil.isMtiqBatchJob(jobType)) {
      return getScheduler(getMtiqBatchSchedulerName());
    }
    else {
      return getScheduler();
    }
  }

  private QuartzJobStoreTX getQuartzJobStoreTXForJobType(Class<? extends Job> jobType) {
    if (tenantUtil.isMtiqBatchJob(jobType)) {
      return mtiqBatchJobStoreTX;
    }
    else {
      return quartzJobStoreTX;
    }
  }
}
