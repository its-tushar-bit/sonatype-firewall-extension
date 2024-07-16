/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob implements InsightJob
{
  public static final String NAME = "DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob";

  private static final Logger log = LoggerFactory.getLogger(
      DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob.class);

  private static final String JOB_ERROR = "Unable to delete old metrics for safe components auto-selected today!";

  private final FirewallMetricsDAO firewallMetricsDAO;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob(
      FirewallMetricsDAO firewallMetricsDAO,
      TaskScheduler taskScheduler
  )
  {
    this.firewallMetricsDAO = firewallMetricsDAO;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.scheduleDailyTask(this, LocalTime.of(1,
        30 + ThreadLocalRandom.current().nextInt(30))); // randomize minute to avoid coordinated load spike
    log.debug(
        "Scheduled delete old metrics for safe components auto-selected for {}",
        taskScheduler.getNextExecutionTime(this));
  }

  @Override
  public void deregister() {
    // Do not unschedule task otherwise it will break MTIQ - SDEV-1312
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.debug("Starting execution of job {}", NAME);
    execute(this::deleteOldMetrics, log, JOB_ERROR);
  }

  private void deleteOldMetrics() {
    firewallMetricsDAO.deleteRecordsOlderThanOneYear(FirewallMetricsName.SAFE_VERSIONS_SELECTED_AUTOMATICALLY);
    log.info("{} task executed.", getJobName());
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
