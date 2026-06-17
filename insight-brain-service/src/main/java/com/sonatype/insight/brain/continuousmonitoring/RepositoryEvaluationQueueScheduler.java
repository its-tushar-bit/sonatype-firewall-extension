/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
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
 * Schedules the unified continuous monitoring producer cycle for the Hosted Repo flow
 * (CLM-40039 Section 6.1). Replaces the legacy {@code HostedRepositoryMonitorScheduler} —
 * instead of running an in-line monitor, this scheduler fires the
 * {@link RepositoryEvaluationQueueProducerJob} which enqueues parent + satellite rows into the
 * unified {@code continuous_monitoring_queue}; the {@link RepositoryEvaluationQueueConsumer}
 * dispatches the work asynchronously.
 * <p>
 * Active when {@link SystemConfigurationPropertyFeature#HOSTED_REPOSITORY_EVALUATION} is
 * enabled. Anchored at {@code policyMonitoringHour + 10 minutes} with a random jitter window
 * of {@code ±continuousMonitoringJitterMinutes} (default 5) per CLM-40039 §6.1 / AT-011, then
 * repeats every 24 hours via {@link TaskScheduler#schedulePeriodicTask}.
 */
@Named
@Singleton
public class RepositoryEvaluationQueueScheduler
    implements TenantManaged, ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryEvaluationQueueScheduler.class);

  private static final int PRODUCER_OFFSET_MINUTES = 10;

  private static final Duration PRODUCER_INTERVAL = Duration.ofHours(24);

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
    stopScheduling();
  }

  public void reschedule() {
    if (SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.isEnabled()) {
      startScheduling();
    }
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    // Reschedule when the feature flag toggles or when any scheduling-relevant property changes.
    // Note: jitter changes trigger reschedule; monitoring hour changes are picked up via the
    // parent configuration listener in Configuration.
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
      // Feature flag was just toggled off — explicitly stop the running schedule.
      log.info("Hosted repository evaluation feature disabled, stopping producer scheduling");
      stopScheduling();
    }
    else {
      // Jitter changed while the feature is (and remained) disabled — nothing to do beyond a
      // diagnostic log so an operator who tuned jitter doesn't conclude CM was unexpectedly
      // stopped.
      log.info("Hosted repository evaluation is disabled; ignoring jitter-minutes change. "
          + "Enable the feature flag for the new jitter to take effect on next scheduling cycle.");
    }
  }

  private synchronized void startScheduling() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.unscheduleTask(producer);
    Date startAt = computeStartTime();
    taskScheduler.schedulePeriodicTask(producer, PRODUCER_INTERVAL, startAt);
    log.info("Hosted repository continuous monitoring producer scheduled at {} (interval {})",
        startAt, PRODUCER_INTERVAL);
  }

  private Date computeStartTime() {
    Integer hour = configuration.getPolicyMonitoringHour();
    Integer jitterMinutes = configuration.getContinuousMonitoringJitterMinutes();
    int jitterWindow = jitterMinutes != null ? jitterMinutes : 5;
    int jitterOffset = jitterWindow > 0
        ? ThreadLocalRandom.current().nextInt(-jitterWindow, jitterWindow + 1)
        : 0;
    // Use LocalDateTime arithmetic throughout to avoid LocalTime.plusMinutes wraparound at midnight.
    // When hour=23 and jitter=60, the anchor math correctly lands on tomorrow rather than wrapping
    // back to today at 00:10 (which would be in the past).
    LocalDateTime anchor = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour != null ? hour : 0, 0))
        .plusMinutes(PRODUCER_OFFSET_MINUTES + jitterOffset);
    if (anchor.isBefore(LocalDateTime.now())) {
      anchor = anchor.plusDays(1);
    }
    return Date.from(anchor.atZone(ZoneId.systemDefault()).toInstant());
  }

  private synchronized void stopScheduling() {
    if (!disableForTesting && taskScheduler.unscheduleTask(producer)) {
      log.info("Hosted repository continuous monitoring producer unscheduled");
    }
  }
}
