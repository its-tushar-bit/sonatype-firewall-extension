/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules the continuous monitoring producer cycle for the Hosted Repo flow. Fires the
 * {@link RepositoryEvaluationQueueProducerJob} daily via {@link TaskScheduler#scheduleDailyTask}
 * at {@code policyMonitoringHour + 10 minutes ± continuousMonitoringJitterMinutes}. Jitter is
 * re-rolled per scheduling call (matches the peer pattern in
 * {@link com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler} and
 * {@link com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler}), which keeps
 * per-tenant fires de-correlated across an MTIQ cluster.
 * <p>
 * Re-registration idempotence is provided by cron itself: {@code scheduleDailyTask} installs a
 * cron trigger anchored to a wall-clock time-of-day, and Quartz's {@code scheduleJobs(replace=true)}
 * replaces the trigger in place — the next fire is always the next occurrence of the current cron
 * expression, so back-to-back re-registers never push the fire past 24 hours (the failure mode
 * that {@link TaskScheduler#schedulePeriodicTask}'s absolute-{@code START_TIME} SimpleTrigger had).
 */
@Named
@Singleton
public class RepositoryEvaluationQueueScheduler
    implements TenantManaged, ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryEvaluationQueueScheduler.class);

  private static final int PRODUCER_OFFSET_MINUTES = 10;

  private static final int DEFAULT_JITTER_MINUTES = 5;

  private final Configuration configuration;

  private final TaskScheduler taskScheduler;

  private final RepositoryEvaluationQueueProducerJob producer;

  @VisibleForTesting
  public volatile boolean disableForTesting;

  @Inject
  public RepositoryEvaluationQueueScheduler(
      final Configuration configuration,
      final TaskScheduler taskScheduler,
      final RepositoryEvaluationQueueProducerJob producer)
  {
    this.configuration = configuration;
    this.taskScheduler = taskScheduler;
    this.producer = producer;
  }

  @Override
  public void register() {
    if (SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      startScheduling();
    }
    else {
      log.debug("Hosted repository evaluation feature is disabled, not scheduling producer cycle");
    }
  }

  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }

  public void reschedule() {
    if (SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      startScheduling();
    }
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    boolean flagChanged = propertyNames.contains(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION);
    boolean jitterChanged = propertyNames.contains(SystemConfigurationProperty.CONTINUOUS_MONITORING_JITTER_MINUTES);
    if (!flagChanged && !jitterChanged) {
      return;
    }
    boolean enabled = SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled();
    if (enabled) {
      log.info("Hosted repository evaluation scheduling parameters changed (flagChanged={}, jitterChanged={}), "
          + "rescheduling producer", flagChanged, jitterChanged);
      startScheduling();
    }
    else if (flagChanged) {
      log.info("Hosted repository evaluation feature disabled, stopping producer scheduling");
      stopScheduling();
    }
    else {
      log.info("Hosted repository evaluation is disabled; ignoring jitter-minutes change. "
          + "Enable the feature flag for the new jitter to take effect on next scheduling cycle.");
    }
  }

  private synchronized void startScheduling() {
    if (disableForTesting) {
      return;
    }
    LocalTime startTime = computeStartTime();
    taskScheduler.scheduleDailyTask(producer, startTime);
    log.info("Hosted repository continuous monitoring producer scheduled daily at {}, next fire {}",
        startTime, taskScheduler.getNextExecutionTime(producer));
  }

  /**
   * Time-of-day when the producer fires each day: {@code policyMonitoringHour:10} shifted by a
   * fresh random offset in {@code ±continuousMonitoringJitterMinutes} (default ±5, config range
   * 0..240). {@link LocalTime#plusMinutes} wraps mod-24, so large jitter values may push the
   * effective time across midnight (e.g. {@code hour=23} with {@code jitter=+240} → {@code 03:10})
   * — expected and harmless for a daily cadence.
   */
  private LocalTime computeStartTime() {
    Integer hour = configuration.getPolicyMonitoringHour();
    int baseHour = hour != null ? hour : 0;
    return LocalTime.of(baseHour, 0).plusMinutes(PRODUCER_OFFSET_MINUTES + rollJitterOffset(configuration));
  }

  private static int rollJitterOffset(final Configuration configuration) {
    Integer jitterMinutes = configuration.getContinuousMonitoringJitterMinutes();
    int jitterWindow = jitterMinutes != null ? jitterMinutes : DEFAULT_JITTER_MINUTES;
    if (jitterWindow <= 0) {
      return 0;
    }
    return ThreadLocalRandom.current().nextInt(-jitterWindow, jitterWindow + 1);
  }

  private synchronized void stopScheduling() {
    if (!disableForTesting && taskScheduler.unscheduleTask(producer)) {
      log.info("Hosted repository continuous monitoring producer unscheduled");
    }
  }
}
