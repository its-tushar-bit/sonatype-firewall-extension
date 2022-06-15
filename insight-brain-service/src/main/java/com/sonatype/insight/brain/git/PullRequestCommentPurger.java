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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDefaultBranchCommitHistoryDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.security.SystemRunnable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges pull request comment records older than 6 months. It runs every day at 2:00 AM.
 */
@Named
@Singleton
public class PullRequestCommentPurger
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(PullRequestCommentPurger.class);

  /**
   * Default purge window for PR comment records in days; it can be overwritten in 'config.yml'.
   */
  private static final int DEFAULT_COMMENT_PURGE_WINDOW_IN_DAYS = 180;

  private static final int DEFAULT_EVENT_PURGE_WINDOW_IN_DAYS = 14;

  private final SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO;

  private final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlConfigurationDAO sourceControlConfigurationDAO;

  private ScheduledExecutorService executor;

  @Inject
  public PullRequestCommentPurger(
      final SourceControlPullRequestCommentDAO sourceControlPullRequestCommentDAO,
      final SourceControlDefaultBranchCommitHistoryDAO sourceControlDefaultBranchCommitHistoryDAO,
      final SourceControlEventDAO sourceControlEventDAO,
      final SourceControlConfigurationDAO sourceControlConfigurationDAO)
  {
    this.sourceControlPullRequestCommentDAO = sourceControlPullRequestCommentDAO;
    this.sourceControlDefaultBranchCommitHistoryDAO = sourceControlDefaultBranchCommitHistoryDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlConfigurationDAO = sourceControlConfigurationDAO;
  }

  private ScheduledExecutorService newExecutor() {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d")
        .setDaemon(true).setPriority(Thread.MIN_PRIORITY).build();
    return new ScheduledThreadPoolExecutor(1, threadFactory);
  }

  @Override
  public void start() {
    executor = newExecutor();
    Duration period = Duration.ofDays(1);
    Duration initialDelay = Duration.between(LocalTime.now(), LocalTime.of(2, 0));
    while (initialDelay.isNegative()) {
      initialDelay = initialDelay.plusDays(1);
    }
    Runnable purgeTask = new SystemRunnable(() -> {
      try {
        purgeObsoleteRecords();
      }
      catch (RuntimeException e) {
        log.warn("Failed to purge obsolete pull request comments", e);
      }
    });
    executor.scheduleAtFixedRate(purgeTask, initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    log.debug("Scheduled periodic purging of obsolete pull request comments for {}",
        LocalDateTime.now().plus(initialDelay));
  }

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
      log.debug("Stopped periodic purging of obsolete pull request comments");
    }
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
}
