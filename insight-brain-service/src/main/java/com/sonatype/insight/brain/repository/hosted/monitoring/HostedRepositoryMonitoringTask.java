/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted.monitoring;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz job that triggers a hosted repository continuous monitoring run.
 * <p>
 * Scheduled daily by {@link HostedRepositoryMonitorScheduler}.
 * {@code @DisallowConcurrentExecution} prevents the same tenant's job from running twice
 * concurrently; different tenants still run in parallel (each has a unique Quartz job key).
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class HostedRepositoryMonitoringTask
    implements InsightJob
{
  public static final String NAME = "HostedRepositoryMonitoringTask";

  private static final Logger log = LoggerFactory.getLogger(HostedRepositoryMonitoringTask.class);

  private final Provider<HostedRepositoryMonitor> hostedRepositoryMonitorProvider;

  @Inject
  public HostedRepositoryMonitoringTask(final Provider<HostedRepositoryMonitor> hostedRepositoryMonitorProvider) {
    this.hostedRepositoryMonitorProvider = hostedRepositoryMonitorProvider;
  }

  @Override
  public void execute(final JobExecutionContext context) {
    log.info("Starting hosted repository CM for tenant {}", TenantThreadLocal.getTenant());
    execute(hostedRepositoryMonitorProvider.get()::run, log, "Hosted repository CM error");
    log.info("Next hosted repository CM scheduled for {}", context.getNextFireTime());
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
