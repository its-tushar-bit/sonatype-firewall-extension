/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shiro;

import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.apache.shiro.session.mgt.SessionValidationScheduler;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class QuartzShiroSessionValidationScheduler
    implements SessionValidationScheduler, InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(QuartzShiroSessionValidationScheduler.class);

  private static final String TASK_NAME = "QuartzShiroSessionValidationScheduler";

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
    taskScheduler.schedulePeriodicTask(QuartzShiroSessionValidationScheduler.class, TASK_NAME, Duration.ofMinutes(30L));
    enabled.set(true);
  }

  @Override
  public void disableSessionValidation() {
    taskScheduler.unscheduleTask(TASK_NAME);
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.debug("Executing Shiro session validation Quartz job...");

    sessionManager.validateSessions();

    log.debug("Shiro session validation Quartz job complete.");
  }
}
