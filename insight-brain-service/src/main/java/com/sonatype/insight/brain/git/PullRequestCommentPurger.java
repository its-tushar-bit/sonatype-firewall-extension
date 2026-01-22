/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import com.google.common.annotations.VisibleForTesting;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges pull request comment records older than 6 months. It runs every day at 2:00 AM.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class PullRequestCommentPurger
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentPurger.class);

  /**
   * Default purge window for PR comment records in days; it can be overwritten in 'config.yml'.
   */
  private static final int DEFAULT_COMMENT_PURGE_WINDOW_IN_DAYS = 180;

  private static final int DEFAULT_EVENT_PURGE_WINDOW_IN_DAYS = 14;

  // Visible for testing
  static final String TASK_NAME = "PullRequestCommentPurger";

  private final SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO;

  private final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlConfigurationDAO sourceControlConfigurationDAO;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public PullRequestCommentPurger(
      final SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO,
      final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final SourceControlConfigurationDAO sourceControlConfigurationDAO,
      final TaskScheduler taskScheduler)
  {
    this.sourceControlPullRequestCommentDAO = sourceControlPullRequestCommentDAO;
    this.sourceControlDefaultBranchCommitHistoryDAO = sourceControlDefaultBranchCommitHistoryDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlConfigurationDAO = sourceControlConfigurationDAO;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    LocalTime startTime = LocalTime.of(2, 0);
    taskScheduler.scheduleDailyTask(this, startTime);
    Duration initialDelay = Duration.between(LocalTime.now(), startTime);
    while (initialDelay.isNegative()) {
      initialDelay = initialDelay.plusDays(1);
    }
    log.debug("Scheduled periodic purging of obsolete pull request comments for {}",
        LocalDateTime.now().plus(initialDelay));
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    execute(this::purgeObsoleteRecords, log, "Failed to purge obsolete pull request comments");
  }

  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }

  @VisibleForTesting
  void purgeObsoleteRecords() {
    int commentPurgeWindowInDays = DEFAULT_COMMENT_PURGE_WINDOW_IN_DAYS;
    int eventPurgeWindowInDays = DEFAULT_EVENT_PURGE_WINDOW_IN_DAYS;

    SourceControlConfiguration sourceControlConfiguration = sourceControlConfigurationDAO.get();
    if (sourceControlConfiguration != null) {
      if (sourceControlConfiguration.getPrCommentPurgeWindow() != null) {
        commentPurgeWindowInDays = sourceControlConfiguration.getPrCommentPurgeWindow();
      }
      if (sourceControlConfiguration.getPrEventPurgeWindow() != null) {
        eventPurgeWindowInDays = sourceControlConfiguration.getPrEventPurgeWindow();
      }
    }

    Date commentCutoffDate = Date.from(ZonedDateTime.now().minusDays(commentPurgeWindowInDays).toInstant());
    purgePullRequestComments(commentCutoffDate);
    purgeDefaultBranchCommitHistory(commentCutoffDate);

    Date eventCutoffDate = Date.from(ZonedDateTime.now().minusDays(eventPurgeWindowInDays).toInstant());
    purgeSourceControlEvents(eventCutoffDate);
  }

  void purgePullRequestComments(Date cutoffDate) {
    int deletedRows = sourceControlPullRequestCommentDAO.deleteAllBeforeDate(cutoffDate);
    if (deletedRows > 0) {
      log.info("Purged {} obsolete pull request comments older than {}", deletedRows, cutoffDate);
    }
  }

  private void purgeDefaultBranchCommitHistory(final Date cutoffDate) {
    int deletedRows = sourceControlDefaultBranchCommitHistoryDAO.deleteAllBeforeDate(cutoffDate);
    if (deletedRows > 0) {
      log.info("Purged {} obsolete default branch commit history older than {}", deletedRows, cutoffDate);
    }
  }

  private void purgeSourceControlEvents(final Date cutoffDate) {
    int deletedRows = sourceControlEventDAO.deleteAllBeforeDate(cutoffDate);
    if (deletedRows > 0) {
      log.info("Purged {} obsolete source control events older than {}", deletedRows, cutoffDate);
    }
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
