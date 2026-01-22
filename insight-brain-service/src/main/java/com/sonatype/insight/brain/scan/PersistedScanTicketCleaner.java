/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.time.Duration;
import java.util.Date;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.scan.PersistedScanTicketDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class PersistedScanTicketCleaner
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(PersistedScanTicketCleaner.class);

  // Visible for testing
  static final String TASK_NAME = "PersistedScanTicketCleaner";

  // Visible for testing
  static final Duration PERIOD = Duration.ofHours(1);

  // Visible for testing
  static final Duration LIFESPAN = Duration.ofHours(2);

  private static final String SCAN_TICKET_CLEAN_ERROR = "Persisted scan ticket cleaner error";

  private final TaskScheduler taskScheduler;

  private final PersistedScanTicketDAO persistedScanTicketDAO;

  public boolean disableForTesting;

  @Inject
  public PersistedScanTicketCleaner(
      TaskScheduler taskScheduler,
      PersistedScanTicketDAO persistedScanTicketDAO)
  {
    this.taskScheduler = taskScheduler;
    this.persistedScanTicketDAO = persistedScanTicketDAO;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    // Note executing every PERIOD means the oldest a PersistedScanTicket can be is PERIOD + LIFESPAN
    taskScheduler.schedulePeriodicTask(this, PERIOD);
  }

  @Override
  public void deregister() {
    // noop
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::deleteExpiredPersistedScanTickets, log, SCAN_TICKET_CLEAN_ERROR);
  }

  private void deleteExpiredPersistedScanTickets() {
    persistedScanTicketDAO.deleteBeforeOrOn(new Date(System.currentTimeMillis() - LIFESPAN.toMillis()));
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
