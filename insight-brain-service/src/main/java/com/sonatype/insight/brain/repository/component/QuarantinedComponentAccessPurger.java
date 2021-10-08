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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
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
    implements Managed, Job
{
  public static final int DEFAULT_PURGE_WINDOW_IN_DAYS = 30;

  public static final String NAME = "QuarantinedComponentAccessPurger";

  private static final Logger log = LoggerFactory.getLogger(QuarantinedComponentAccessPurger.class);

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
  public void start() {
    taskScheduler.schedulePeriodicTask(QuarantinedComponentAccessPurger.class, NAME,
        Duration.ofDays(1));
    Date nextExecutionTime = taskScheduler.getNextExecutionTime(NAME);
    log.debug("Scheduled periodic purging of obsolete quarantined component report access entries for {}",
        nextExecutionTime);
  }

  @Override
  public void stop() {
    // no-op
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    log.debug("Triggering purging of obsolete quarantined component report access entries");
    taskScheduler.triggerTaskNow(NAME, null);
    output.println("Triggered purging of obsolete quarantined component report access entries");
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      purgeObsoleteRecords();
    }
    catch (Exception e) {
      log.error("Quarantined component access entries purging error: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
  }

  @VisibleForTesting
  void purgeObsoleteRecords() {
    Date cutoffDate = DateUtils.addDays(new Date(), -DEFAULT_PURGE_WINDOW_IN_DAYS);
    int deletedRows = quarantinedComponentAccessDAO.deleteAllBeforeDate(cutoffDate);
    if (deletedRows > 0) {
      log.info("Purged {} obsolete quarantined component access entries older than {}", deletedRows, cutoffDate);
    }
  }
}
