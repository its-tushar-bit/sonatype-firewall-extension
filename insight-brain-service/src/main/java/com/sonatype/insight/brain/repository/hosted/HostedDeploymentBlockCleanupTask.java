/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;

import com.sonatype.insight.brain.service.AdminTask;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodic 24-hour cleanup of {@code hosted_deployment_block} (and cascaded
 * {@code hosted_deployment_block_violation}) rows older than the configured retention.
 * <p>
 * Retention is read from {@link Configuration#getHostedDeploymentBlockRetentionHours()}
 * (default 24h, system-config-backed and hot-reloadable).
 * <p>
 * Mirrors the {@link com.sonatype.insight.brain.repository.component.QuarantinedComponentAccessPurger}
 * pattern: extends {@link AdminTask} for admin-side manual triggering, implements {@link InsightJob}
 * for tenant-managed registration with the {@link TaskScheduler}.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class HostedDeploymentBlockCleanupTask
    extends AdminTask
    implements InsightJob
{
  public static final String NAME = "HostedDeploymentBlockCleanupTask";

  private static final Duration DEFAULT_PERIOD = Duration.ofHours(24);

  private static final Logger log = LoggerFactory.getLogger(HostedDeploymentBlockCleanupTask.class);

  private static final String CLEANUP_ERROR = "Hosted deployment block cleanup error";

  private final TaskScheduler taskScheduler;

  private final Configuration configuration;

  private final HostedDeploymentBlockCleanupService cleanupService;

  @Inject
  public HostedDeploymentBlockCleanupTask(
      final TaskScheduler taskScheduler,
      final Configuration configuration,
      final HostedDeploymentBlockCleanupService cleanupService)
  {
    super(NAME);
    this.taskScheduler = taskScheduler;
    this.configuration = configuration;
    this.cleanupService = cleanupService;
  }

  @Override
  public void register() {
    taskScheduler.schedulePeriodicTask(this, DEFAULT_PERIOD);
  }

  @Override
  public void deregister() {
    // no-op
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    log.debug("Triggering hosted deployment block cleanup via admin task endpoint");
    taskScheduler.triggerTaskNow(this, null);
    output.println("Triggered hosted deployment block cleanup");
  }

  @Override
  public void execute(final JobExecutionContext context) {
    execute(this::runScheduledCleanup, log, CLEANUP_ERROR);
  }

  private void runScheduledCleanup() {
    Integer retentionHours = configuration.getHostedDeploymentBlockRetentionHours();
    Duration retention = Duration.ofHours(retentionHours == null ? 24 : retentionHours);
    cleanupService.runCleanup(retention);
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
