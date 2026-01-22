/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.servlets.tasks.Task;
import org.apache.commons.lang3.time.DateUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purges quarantined component report access records older than 60 days. Runs daily.
 *
 * @since 1.125
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class QuarantinedComponentAccessPurger
    extends Task
    implements InsightJob
{
  public static final int DEFAULT_PURGE_WINDOW_IN_DAYS = 30;

  public static final String NAME = "QuarantinedComponentAccessPurger";

  private static final Logger log = LoggerFactory.getLogger(QuarantinedComponentAccessPurger.class);

  private static final String PURGE_ERROR = "Quarantined component access entries purging error";

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private final TaskScheduler taskScheduler;

  @Inject
  public QuarantinedComponentAccessPurger(
      final TaskScheduler taskScheduler,
      final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO)
  {
    super(NAME);
    this.taskScheduler = taskScheduler;
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
  }

  @Override
  public void register() {
    taskScheduler.schedulePeriodicTask(this, Duration.ofDays(1));
  }

  @Override
  public void deregister() {
    // no-op
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    log.debug("Triggering purging of obsolete quarantined component report access entries");
    taskScheduler.triggerTaskNow(this, null);
    output.println("Triggered purging of obsolete quarantined component report access entries");
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::purgeObsoleteRecords, log, PURGE_ERROR);
  }

  @VisibleForTesting
  void purgeObsoleteRecords() {
    Date cutoffDate = DateUtils.addDays(new Date(), -DEFAULT_PURGE_WINDOW_IN_DAYS);
    int deletedRows = quarantinedComponentAccessDAO.deleteAllBeforeDate(cutoffDate);
    if (deletedRows > 0) {
      log.info("Purged {} obsolete quarantined component access entries older than {}", deletedRows, cutoffDate);
    }
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
