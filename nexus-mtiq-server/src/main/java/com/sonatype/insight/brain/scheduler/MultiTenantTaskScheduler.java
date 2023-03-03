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

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantContextJobListener;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;

@Named
@Singleton
@Priority(TaskScheduler.TASK_SCHEDULER_BEAN_PRIORITY)
public class MultiTenantTaskScheduler
    extends TaskScheduler
{
  //Visible for test
  static final String TASK_SCHEDULER_THREAD_POOL_SIZE = "TASK_SCHEDULER_THREAD_POOL_SIZE";

  private final TenantContextJobListener tenantContextJobListener;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final TenantManager tenantManager;

  private final TenantUtil tenantUtil;

  @Inject
  public MultiTenantTaskScheduler(
      QuartzJobStoreTX quartzJobStoreTX,
      JobFactory jobFactory,
      @Named("${scheduler.name:-" + DEFAULT_SCHEDULER_NAME + "}") String schedulerName,
      QuartzTriggerListener quartzTriggerListener,
      TenantContextJobListener tenantContextJobListener,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      TenantManager tenantManager,
      TenantUtil tenantUtil)
  {
    super(quartzJobStoreTX, jobFactory, schedulerName, quartzTriggerListener);

    this.tenantContextJobListener = tenantContextJobListener;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public Scheduler createScheduler() {
    try {
      Scheduler scheduler = superCreateScheduler();
      scheduler.getListenerManager().addJobListener(tenantContextJobListener);
      return scheduler;
    }
    catch (SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  // This is a separate method so that it can be overriden during testing
  protected Scheduler superCreateScheduler() {
    return super.createScheduler();
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
  public boolean unscheduleTask(InsightJob insightJob) {
    // When no tenant is specified (e.g. global) unschedule this task for all tenants
    if (tenantUtil.isGlobalTenant()) {
      try {
        List<String> tenantSlugs = getScheduler().getJobGroupNames();

        boolean unscheduled = false;
        for (String tenantSlug : tenantSlugs) {
          boolean result = unscheduleTask(toJobKey(insightJob, tenantSlug));

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
      return unscheduleTask(toJobKey(insightJob));
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
}
