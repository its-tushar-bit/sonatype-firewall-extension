/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the entry point for the Default Branch Monitoring feature (part of Continuous Risk Profile). It runs
 * periodically to ensure that the default branch policy evaluations for all SCM enabled applications are not stale. The
 * updated policy evaluations may trigger downstream processes like the creation of automated remediation pull
 * requests.
 *
 * @since 1.118
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class DefaultBranchMonitor
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(DefaultBranchMonitor.class);

  public static final String TASK_NAME = "DefaultBranchMonitor";

  private static final String SOURCE_SCANS_UPDATE_ERROR = "Error when updating default branch source scans";

  private final TaskScheduler taskScheduler;

  private final IqForScmLicenseChecker licenseChecker;

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  private BranchMonitorExecutor branchMonitorExecutor;

  public boolean disableForTesting;

  @Inject
  public DefaultBranchMonitor(
      TaskScheduler taskScheduler,
      IqForScmLicenseChecker licenseChecker,
      ApiConfigFeaturesService apiConfigFeaturesService,
      BranchMonitorExecutor branchMonitorExecutor)
  {
    this.taskScheduler = taskScheduler;
    this.licenseChecker = licenseChecker;
    this.apiConfigFeaturesService = apiConfigFeaturesService;
    this.branchMonitorExecutor = branchMonitorExecutor;
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    scheduleDefaultBranchMonitoring();
  }

  public void scheduleDefaultBranchMonitoring() {
    if (apiConfigFeaturesService.isDefaultBranchMonitoringEnabled()) {
      branchMonitorExecutor.schedule(this);
    }
    else {
      log.debug("default branch monitoring is not enabled");
      taskScheduler.unscheduleTask(this);
    }
  }

  @Override
  public void execute(JobExecutionContext context) {
    execute(() -> {
      if (apiConfigFeaturesService.isDefaultBranchMonitoringEnabled() &&
          apiConfigFeaturesService.isSaasLifecycleScmEnabled() &&
          licenseChecker.isIqForScmSupported()) {
        branchMonitorExecutor.performScan(this);
      }
      else {
        log.debug("skipping default branch monitor execution: enabled={}, scmEnabled={}, licensed={}",
            apiConfigFeaturesService.isDefaultBranchMonitoringEnabled(),
            apiConfigFeaturesService.isSaasLifecycleScmEnabled(),
            licenseChecker.isIqForScmSupported());
      }
    }, log, SOURCE_SCANS_UPDATE_ERROR);
  }

  @Override
  public void deregister() {
    // no-op
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }

  // Visible for testing
  void setBranchMonitorExecutor(BranchMonitorExecutor branchMonitorExecutor) {
    this.branchMonitorExecutor = branchMonitorExecutor;
  }
}
