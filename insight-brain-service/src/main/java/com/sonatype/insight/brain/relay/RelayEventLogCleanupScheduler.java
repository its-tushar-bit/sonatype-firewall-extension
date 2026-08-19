/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.time.LocalTime;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules a daily prune of {@code relay_event_log}. The cleanup itself is gated on the relay
 * feature flag (see {@link RelayEventLogCleanupTask#run()}), so it stays a no-op for tenants
 * that have the integration disabled.
 */
@Named
@Singleton
public class RelayEventLogCleanupScheduler
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(RelayEventLogCleanupScheduler.class);

  private static final LocalTime CLEANUP_TIME = LocalTime.of(2, 30);

  private final TaskScheduler taskScheduler;

  private final RelayEventLogCleanupTask cleanupTask;

  // Mutable singleton field for test suppression. See GitHubAppCleanupScheduler for the same pattern.
  boolean disableForTesting;

  @Inject
  public RelayEventLogCleanupScheduler(
      final TaskScheduler taskScheduler,
      final RelayEventLogCleanupTask cleanupTask)
  {
    this.taskScheduler = taskScheduler;
    this.cleanupTask = cleanupTask;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      log.info("Relay event log cleanup task disabled for testing");
      return;
    }
    log.info("Scheduling relay event log cleanup daily at {}", CLEANUP_TIME);
    taskScheduler.scheduleDailyTask(cleanupTask, CLEANUP_TIME);
  }

  @Override
  public void deregister() {
    // Same MTIQ contract as GitHubAppCleanupScheduler: do not unschedule.
  }
}
