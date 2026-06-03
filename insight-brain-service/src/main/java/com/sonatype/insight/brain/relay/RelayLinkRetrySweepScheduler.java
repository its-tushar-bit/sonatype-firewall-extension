/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.time.Duration;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules the hourly {@link RelayLinkRetrySweepTask}. The sweep itself is gated on the
 * relay feature flag (see {@link RelayLinkRetrySweepTask#run()}), so it stays a no-op for
 * tenants that have the integration disabled.
 */
@Named
@Singleton
public class RelayLinkRetrySweepScheduler
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(RelayLinkRetrySweepScheduler.class);

  /** Hard-coded for v1; see the design doc. */
  static final Duration SWEEP_INTERVAL = Duration.ofHours(1);

  private final TaskScheduler taskScheduler;

  private final RelayLinkRetrySweepTask sweepTask;

  // Mutable singleton field for test suppression. See RelayEventLogCleanupScheduler for the same pattern.
  boolean disableForTesting;

  @Inject
  public RelayLinkRetrySweepScheduler(
      final TaskScheduler taskScheduler,
      final RelayLinkRetrySweepTask sweepTask)
  {
    this.taskScheduler = taskScheduler;
    this.sweepTask = sweepTask;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      log.info("Relay link retry sweep task disabled for testing");
      return;
    }
    log.info("Scheduling relay link retry sweep every {}", SWEEP_INTERVAL);
    taskScheduler.schedulePeriodicTask(sweepTask, SWEEP_INTERVAL);
  }

  @Override
  public void deregister() {
    // Same MTIQ contract as RelayEventLogCleanupScheduler: do not unschedule.
  }
}
