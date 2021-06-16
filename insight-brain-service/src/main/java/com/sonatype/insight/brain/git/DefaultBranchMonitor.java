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
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.brain.utils.DateUtils;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.118
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class DefaultBranchMonitor
    implements Managed, Job
{
  private static final Logger log = LoggerFactory.getLogger(DefaultBranchMonitor.class);

  static final String TASK_NAME = "DefaultBranchMonitor";

  private final InsightConfig insightConfig;

  private final TaskScheduler taskScheduler;

  private final SourceControlEventPublisher sourceControlEventPublisher;

  private final SourceControlDAO sourceControlDAO;

  public boolean disableForTesting;

  private int intervalInHours;

  @Inject
  public DefaultBranchMonitor(
      InsightConfig insightConfig,
      TaskScheduler taskScheduler,
      SourceControlEventPublisher sourceControlEventPublisher,
      SourceControlDAO sourceControlDAO)
  {
    this.insightConfig = insightConfig;
    this.taskScheduler = taskScheduler;
    this.sourceControlEventPublisher = sourceControlEventPublisher;
    this.sourceControlDAO = sourceControlDAO;
  }

  @Override
  public void start() throws Exception {
    if (disableForTesting) {
      return;
    }

    if (!insightConfig.isExperimentalFeatureEnabled(Feature.DEFAULT_BRANCH_MONITORING)) {
      taskScheduler.unscheduleTask(TASK_NAME);
      return;
    }

    intervalInHours = insightConfig.getDefaultBranchMonitoring().getIntervalInHours();

    taskScheduler.schedulePeriodicTask(DefaultBranchMonitor.class, TASK_NAME, Duration.ofHours(intervalInHours),
        getDefaultBranchMonitorStartTime());

    log.debug("DefaultBranchMonitor scheduled to start at {} and repeat every {} hours.",
        getDefaultBranchMonitorStartTime(), intervalInHours);
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      updateDefaultBranchScans();
    }
    catch (Exception e) {
      log.error("Error when updating default branch source scans: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  // Visible for tests
  void updateDefaultBranchScans() {
    long start = System.currentTimeMillis();
    log.debug("Updating default branch source scans.");

    Date scanLimitDate = Date.from(LocalDateTime.now().minusHours(intervalInHours)
        .atZone(ZoneId.systemDefault()).toInstant());

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
  public void stop() {
    // no-op
  }

  /**
   * The start time and interval form a continual series of date-times.  This method gets the next datetime that
   * is closest to now.  If the interval start time is in the future we back it up one day. Then, in all cases, we
   * repeatedly bump the time forward one interval until we find the first series datetime that is in the future.
   */
  @VisibleForTesting
  Date getDefaultBranchMonitorStartTime() {
    LocalTime intervalStartTime =
        DateUtils.getLocalTimeForHoursAndMinutes(insightConfig.getDefaultBranchMonitoring().getStartTime());

    LocalDateTime now = LocalDateTime.now();

    // superimpose the interval start time onto today's date
    LocalDateTime intervalStartDateTime = now
        .withHour(intervalStartTime.getHour())
        .withMinute(intervalStartTime.getMinute())
        .withSecond(0)
        .withNano(0);

    LocalDateTime effectiveStartDate = DateUtils.getClosestFutureDateTime(now, intervalStartDateTime,
            insightConfig.getDefaultBranchMonitoring().getIntervalInHours());

    return Date.from(effectiveStartDate.atZone(ZoneId.systemDefault()).toInstant());
  }

  @VisibleForTesting
  int getIntervalInHours() {
    return intervalInHours;
  }
}
