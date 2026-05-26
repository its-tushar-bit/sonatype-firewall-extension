/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted.monitoring;

import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules daily continuous monitoring for hosted repositories.
 * <p>
 * Activates when the {@link SystemConfigurationPropertyFeature#HOSTED_REPOSITORY_EVALUATION}
 * feature flag is enabled. Uses the same {@code policyMonitoringHour} configuration as application
 * CM, with a random-minute jitter within a 120-minute window to avoid coordinated HDS load spikes.
 * <p>
 * Admin trigger endpoint: {@code POST /tasks/triggerHostedRepositoryMonitor}
 */
@Named
@Singleton
public class HostedRepositoryMonitorScheduler
    extends AdminTask
    implements TenantManaged, ConfigurationListener
{
  public static final String PATH = "triggerHostedRepositoryMonitor";

  private static final Logger log = LoggerFactory.getLogger(HostedRepositoryMonitorScheduler.class);

  private static final int TIME_WINDOW_MINUTES = 120;

  private final Configuration configuration;

  private final TaskScheduler taskScheduler;

  private final HostedRepositoryMonitoringTask hostedRepositoryMonitoringTask;

  private final Provider<HostedRepositoryMonitor> hostedRepositoryMonitorProvider;

  public volatile boolean disableForTesting;

  @Inject
  public HostedRepositoryMonitorScheduler(
      final Configuration configuration,
      final TaskScheduler taskScheduler,
      final HostedRepositoryMonitoringTask hostedRepositoryMonitoringTask,
      final Provider<HostedRepositoryMonitor> hostedRepositoryMonitorProvider)
  {
    super(PATH);
    this.configuration = configuration;
    this.taskScheduler = taskScheduler;
    this.hostedRepositoryMonitoringTask = hostedRepositoryMonitoringTask;
    this.hostedRepositoryMonitorProvider = hostedRepositoryMonitorProvider;
  }

  @Override
  public void register() {
    if (SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      startMonitoring();
    }
    else {
      log.debug("Hosted repository evaluation feature is disabled, not scheduling CM");
    }
  }

  @Override
  public void deregister() {
    stopMonitoring();
  }

  public void reschedule() {
    if (SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      startMonitoring();
    }
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (!propertyNames.contains(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION)) {
      return;
    }
    if (SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      log.info("Hosted repository evaluation feature enabled, starting CM scheduling");
      startMonitoring();
    }
    else {
      log.info("Hosted repository evaluation feature disabled, stopping CM scheduling");
      stopMonitoring();
    }
  }

  private synchronized void startMonitoring() {
    if (disableForTesting) {
      return;
    }
    Integer hour = configuration.getPolicyMonitoringHour();
    taskScheduler.unscheduleTask(hostedRepositoryMonitoringTask);
    LocalTime startTime = LocalTime.of(hour != null ? hour : 0, 0)
        .plusMinutes(ThreadLocalRandom.current().nextInt(TIME_WINDOW_MINUTES));
    taskScheduler.scheduleDailyTask(hostedRepositoryMonitoringTask, startTime);
    log.info("Hosted repository CM scheduled daily at {}", startTime);
  }

  private synchronized void stopMonitoring() {
    if (!disableForTesting && taskScheduler.unscheduleTask(hostedRepositoryMonitoringTask)) {
      log.info("Hosted repository CM stopped");
    }
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    hostedRepositoryMonitorProvider.get().run();
  }
}
