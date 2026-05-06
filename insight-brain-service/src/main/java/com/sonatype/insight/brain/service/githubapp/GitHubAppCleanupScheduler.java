/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.githubapp;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Schedules weekly cleanup of inactive GitHub App records.
 *
 */
@Named
@Singleton
public class GitHubAppCleanupScheduler
    implements TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(GitHubAppCleanupScheduler.class);

  private static final DayOfWeek CLEANUP_DAY = DayOfWeek.SUNDAY;

  private static final LocalTime CLEANUP_TIME = LocalTime.of(3, 0);

  private final TaskScheduler taskScheduler;

  private final GitHubAppCleanupTask gitHubAppCleanupTask;

  // Package-private field for test suppression. Mutable singleton fields can cause issues in MTIQ.
  boolean disableForTesting;

  @Inject
  public GitHubAppCleanupScheduler(
      final TaskScheduler taskScheduler,
      final GitHubAppCleanupTask gitHubAppCleanupTask)
  {
    this.taskScheduler = taskScheduler;
    this.gitHubAppCleanupTask = gitHubAppCleanupTask;
  }

  @Override
  public void register() {
    if (!disableForTesting) {
      scheduleGitHubAppCleanup();
    }
    else {
      log.info("GitHub App cleanup task disabled for testing");
    }
  }

  private void scheduleGitHubAppCleanup() {
    log.info("Scheduling GitHub App cleanup to run weekly on {} at {}", CLEANUP_DAY, CLEANUP_TIME);
    taskScheduler.scheduleWeeklyTask(gitHubAppCleanupTask, CLEANUP_DAY, CLEANUP_TIME);
  }

  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }
}
