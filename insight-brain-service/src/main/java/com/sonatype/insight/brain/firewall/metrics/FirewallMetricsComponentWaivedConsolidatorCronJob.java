/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall.metrics;

import java.time.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
public class FirewallMetricsComponentWaivedConsolidatorCronJob implements InsightJob
{
  private static final Logger log = LoggerFactory
      .getLogger(FirewallMetricsComponentWaivedConsolidatorCronJob.class);

  private static final String CONSOLIDATOR_JOB_ERROR = "Error when running the " +
      " consolidation task for " + FirewallMetricsName.WAIVED_COMPONENTS +
      " metrics";

  // Visible for testing
  static final String TASK_NAME =
      "FirewallMetricsComponentWaivedConsolidatorCronJob";

  // Visible for testing
  static final Duration PERIOD = Duration.ofHours(6);

  private final WaivedComponentMetricsConsolidator consolidator;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public FirewallMetricsComponentWaivedConsolidatorCronJob(
      TaskScheduler taskScheduler,
      WaivedComponentMetricsConsolidator consolidator
  )
  {
    this.taskScheduler = taskScheduler;
    this.consolidator = consolidator;
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(this, PERIOD);
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::consolidateWaivedComponentsMetrics,
        log, CONSOLIDATOR_JOB_ERROR);
  }

  private void consolidateWaivedComponentsMetrics() {
    consolidator.consolidate();
    log.info("{} task executed.", getJobName());
  }

  @Override
  public void deregister() {
    // noop
  }
}
