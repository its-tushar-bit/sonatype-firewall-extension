/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.time.Duration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.insight.brain.dataaccess.configuration.FirewallIgnorePatternsDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.GlobalTenantJob;
import com.sonatype.insight.error.exception.BadGatewayException;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class FirewallIgnorePatternUpdater
    implements InsightJob, GlobalTenantJob
{
  private static final Logger log = LoggerFactory.getLogger(FirewallIgnorePatternUpdater.class);

  public static final String HDS_IGNORE_PATTERNS_PATH = "rest/component/details/firewall/ignorePatterns";

  static final String TASK_NAME = "FirewallIgnorePatternUpdater";

  private static final String FIREWALL_IGNORE_PATTERNS_UPDATE_ERROR = "Error when updating firewall ignore patterns";

  private final FirewallIgnorePatternsDAO firewallIgnorePatternsDAO;

  private final HdsClient hdsClient;

  private final TaskScheduler taskScheduler;

  public boolean disableForTesting;

  @Inject
  public FirewallIgnorePatternUpdater(
      HdsClient hdsClient,
      FirewallIgnorePatternsDAO firewallIgnorePatternsDAO,
      TaskScheduler taskScheduler)
  {
    this.hdsClient = hdsClient;
    this.firewallIgnorePatternsDAO = firewallIgnorePatternsDAO;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(this, Duration.ofHours(6));
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(this::updateFirewallIgnorePatterns, log, FIREWALL_IGNORE_PATTERNS_UPDATE_ERROR);
  }

  void updateFirewallIgnorePatterns() {
    com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns firewallIgnorePatterns =
        new com.sonatype.insight.brain.model.configuration.FirewallIgnorePatterns(fetchFirewallIgnorePatterns());
    firewallIgnorePatternsDAO.update(firewallIgnorePatterns);
  }

  private FirewallIgnorePatterns fetchFirewallIgnorePatterns() {
    try {
      return hdsClient.get(FirewallIgnorePatterns.class, HDS_IGNORE_PATTERNS_PATH);
    }
    catch (BadGatewayException e) {
      throw new RuntimeException("Failed to get ignore patterns from remote: " + e.getMessage(), e);
    }
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
