/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.utils.DateUtils;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DefaultBranchMonitorExecutor
    extends BranchMonitorExecutor
{
  private static final Logger log = LoggerFactory.getLogger(DefaultBranchMonitorExecutor.class);

  private final Configuration configuration;

  private final TaskScheduler taskScheduler;

  private int intervalInMinutes;

  private final int randomizedStartOffsetInMinutes = new Random().nextInt(10);

  @Inject
  public DefaultBranchMonitorExecutor(
      Configuration configuration,
      TaskScheduler taskScheduler,
      SourceControlDAO sourceControlDAO,
      SourceControlEventPublisher sourceControlEventPublisher)
  {
    super(sourceControlDAO, sourceControlEventPublisher);
    this.configuration = configuration;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void schedule(InsightJob job) {
    SourceControlConfiguration sourceControlConfiguration = configuration.getSourceControlConfigurationOrDefault();
    intervalInMinutes = sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours() * 60;

    Date defaultBranchMonitorStartTime = getDefaultBranchMonitorStartTime(sourceControlConfiguration);
    taskScheduler.schedulePeriodicTask(job, Duration.ofMinutes(intervalInMinutes), defaultBranchMonitorStartTime);

    log.debug("DefaultBranchMonitor scheduled to start at {} and repeat every {} hours.", defaultBranchMonitorStartTime,
        (double) intervalInMinutes / 60);
  }

  @Override
  public void performScan(InsightJob job) {
    updateDefaultBranchScans(intervalInMinutes);
  }

  /**
   * The start time and interval form a continual series of date-times.  This method gets the next datetime that is
   * closest to now.  If the interval start time is in the future we back it up one day. Then, in all cases, we
   * repeatedly bump the time forward one interval until we find the first series datetime that is in the future.
   */
  @VisibleForTesting
  Date getDefaultBranchMonitorStartTime(SourceControlConfiguration sourceControlConfiguration) {
    LocalTime intervalStartTime = sourceControlConfiguration.getDefaultBranchMonitoringStartTime();

    if (intervalStartTime == null) {
      // randomize minute to avoid coordinated load spike for HDS scan processing
      intervalStartTime = LocalTime.parse(SourceControlConfiguration.DEFAULT_BRANCH_MONITORING_START_TIME,
          SourceControlConfiguration.DATE_TIME_FORMATTER).plusMinutes(getRandomizedStartOffsetInMinutes());
    }

    LocalDateTime now = LocalDateTime.now();

    // superimpose the interval start time onto today's date
    LocalDateTime intervalStartDateTime = now
        .withHour(intervalStartTime.getHour())
        .withMinute(intervalStartTime.getMinute())
        .withSecond(0)
        .withNano(0);

    LocalDateTime effectiveStartDate = DateUtils.getClosestFutureDateTime(now, intervalStartDateTime,
        sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours());

    return Date.from(effectiveStartDate.atZone(ZoneId.systemDefault()).toInstant());
  }

  // Visible for testing
  int getRandomizedStartOffsetInMinutes() {
    return randomizedStartOffsetInMinutes;
  }
}
