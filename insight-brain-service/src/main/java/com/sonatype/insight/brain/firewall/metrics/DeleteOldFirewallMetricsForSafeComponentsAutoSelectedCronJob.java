/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.firewall.metrics;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob
 * <p>
 * no-op placeholder class
 */
@Deprecated
@Named
@Singleton
@DisallowConcurrentExecution
public class DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob
    implements InsightJob
{
  public static final String NAME = "DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob";

  public boolean disableForTesting;

  public DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob() {
    // no-op
  }

  @Override
  public void execute(JobExecutionContext context) {
    // no-op
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
