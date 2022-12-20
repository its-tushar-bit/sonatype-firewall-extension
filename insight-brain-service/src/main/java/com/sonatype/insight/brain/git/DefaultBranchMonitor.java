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
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.utils.DateUtils;

import com.google.common.annotations.VisibleForTesting;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the entry point for the Default Branch Monitoring feature (part of Continuous Risk Profile). It runs
 * periodically to ensure that the default branch policy evaluations for all SCM enabled applications are not stale. The
 * updated policy evaluations may trigger downstream processes like the creation of automated remediation pull
 * requests.
 *
 * @since 1.118
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class DefaultBranchMonitor
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(DefaultBranchMonitor.class);

  public static final String TASK_NAME = "DefaultBranchMonitor";

  private static final String SOURCE_SCANS_UPDATE_ERROR = "Error when updating default branch source scans";

  private final TaskScheduler taskScheduler;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final Configuration configuration;

  private final SourceControlDAO sourceControlDAO;

  private final IqForScmLicenseChecker licenseChecker;

  public boolean disableForTesting;

  private int intervalInMinutes;

  private final int randomizedStartOffsetInMinutes = new Random().nextInt(10);

  @Inject
  public DefaultBranchMonitor(
      TaskScheduler taskScheduler,
      SourceControlEventPublisher sourceControlEventPublisher,
      Configuration configuration,
      SourceControlDAO sourceControlDAO,
      IqForScmLicenseChecker licenseChecker)
  {
    this.taskScheduler = taskScheduler;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.configuration = configuration;
    this.sourceControlDAO = sourceControlDAO;
    this.licenseChecker = licenseChecker;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }

    scheduleDefaultBranchMonitoring();
  }

  public void scheduleDefaultBranchMonitoring() {
    taskScheduler.unscheduleTask(TASK_NAME);

    if (!SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.isEnabled()) {
      return;
    }
    // If we schedule the task every intervalInHours (as configured),
    // then apps that have policy evals after now - intervalInHours are not included in the next default branch
    // monitoring execution,
    // which means they will be included only in the subsequent execution,
    // resulting in a policy eval triggered by monitoring every 2 * intervalInHours.
    // That's why we use half intervalInHours below.
    SourceControlConfiguration sourceControlConfiguration = configuration.getSourceControlConfigurationOrDefault();
    intervalInMinutes = sourceControlConfiguration.getDefaultBranchMonitoringIntervalHours() * 60 / 2;

    Date defaultBranchMonitorStartTime = getDefaultBranchMonitorStartTime(sourceControlConfiguration);
    taskScheduler.schedulePeriodicTask(DefaultBranchMonitor.class, TASK_NAME, Duration.ofMinutes(intervalInMinutes),
        defaultBranchMonitorStartTime);

    log.debug("DefaultBranchMonitor scheduled to start at {} and repeat every {} hours.", defaultBranchMonitorStartTime,
        (double) intervalInMinutes / 60);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(() -> {
      if (licenseChecker.isIqForScmSupported()) {
        updateDefaultBranchScans();
      }
    }, log, SOURCE_SCANS_UPDATE_ERROR);
  }

  // Visible for tests
  void updateDefaultBranchScans() {
    long start = System.currentTimeMillis();
    log.debug("Updating default branch source scans.");

    Date scanLimitDate =
        Date.from(LocalDateTime.now().minusMinutes(intervalInMinutes).atZone(ZoneId.systemDefault()).toInstant());

    List<SourceControl> sourceControlList
        = sourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(scanLimitDate);

    for (SourceControl sourceControl : sourceControlList) {
      initiateDefaultBranchSourceScans(sourceControl);
    }

    log.debug("Initiated default branch source scans for {} applications in {} ms.", sourceControlList.size(),
        System.currentTimeMillis() - start);
  }

  private void initiateDefaultBranchSourceScans(SourceControl sourceControl) {
    String statusId = UUID.randomUUID().toString().replace("-", "");

    SourceControlEvent sourceControlEvent = new SourceControlEvent()
        .forSourceControlEvaluation()
        .setApplicationId(sourceControl.getOwnerId())
        .setStageTypeId(Stage.ID_SOURCE)
        .setScanTriggerType(ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING)
        .setStatusId(statusId)
        .setBranchName(sourceControl.getBaseBranch());

    log.debug("Initiating a source control evaluation for application {}, stage {} and branch {} with status ID {}.",
        sourceControlEvent.getApplicationId(),
        sourceControlEvent.getStageTypeId(),
        sourceControlEvent.getBranchName(),
        sourceControlEvent.getStatusId());

    sourceControlEventPublisher.publishEvent(sourceControlEvent);
  }

  @Override
  public void deregister() {
    // no-op
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

  @VisibleForTesting
  int getIntervalInMinutes() {
    return intervalInMinutes;
  }
}
