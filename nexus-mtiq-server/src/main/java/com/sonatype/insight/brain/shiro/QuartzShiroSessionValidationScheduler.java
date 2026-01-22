/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shiro;

import java.time.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.apache.shiro.session.mgt.SessionValidationScheduler;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces the built-in Shiro session validation which uses an ExecutorService and instead makes use of Quartz.
 * Note: This implements GlobalTenantJob because any requests via the API that access the global tenant will also
 * create a Shiro session.
 */
@Named
@Singleton
public class QuartzShiroSessionValidationScheduler
    implements SessionValidationScheduler, InsightJob, GlobalTenantJob
{
  private static final Logger log = LoggerFactory.getLogger(QuartzShiroSessionValidationScheduler.class);

  //Visible for testing
  static final String TASK_NAME = "QuartzShiroSessionValidationScheduler";

  private final TaskScheduler taskScheduler;

  private final DefaultWebSessionManager sessionManager;

  private final TenantReference<Boolean> enabled = new TenantReference<>(() -> false);

  @Inject
  public QuartzShiroSessionValidationScheduler(TaskScheduler taskScheduler, DefaultWebSessionManager sessionManager) {
    this.taskScheduler = taskScheduler;
    this.sessionManager = sessionManager;

    sessionManager.setSessionValidationScheduler(this);
  }

  @Override
  public boolean isEnabled() {
    return enabled.get();
  }

  @Override
  public void enableSessionValidation() {
    taskScheduler.schedulePeriodicTask(this, Duration.ofMinutes(30L));
    enabled.set(true);
  }

  @Override
  public void disableSessionValidation() {
    taskScheduler.unscheduleTask(this);
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.debug("Executing Shiro session validation Quartz job...");

    sessionManager.validateSessions();

    log.debug("Shiro session validation Quartz job complete.");
  }

  /**
   * This task exists for every tenant and runs very often. That means all tenants will be re-registered on startup
   * if this job persists beyond shutdown. To prevent that we unschedule the job during shutdown/deregister.
   */
  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
