/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@DisallowConcurrentExecution
public class PolicyWaiverTelemetryBackfillTask
    extends AdminTask
    implements InsightJob, AllTenantsJob
{
  public static final String PATH = "triggerPolicyWaiverTelemetryBackfillTask";

  private static final Logger log = LoggerFactory.getLogger(PolicyWaiverTelemetryBackfillTask.class);

  private static final String NAME = PolicyWaiverTelemetryBackfillTask.class.getSimpleName();

  private static final String TASK_ERROR = "Error running " + NAME;

  @VisibleForTesting
  static final long TASK_STARTUP_DELAY_MINUTES = 15L;

  private final PolicyWaiverTelemetryBackfillService policyWaiverTelemetryBackfillService;

  private final TaskScheduler taskScheduler;

  private final TenantUtil tenantUtil;

  @VisibleForTesting
  public boolean disableForTesting;

  @Inject
  public PolicyWaiverTelemetryBackfillTask(
      PolicyWaiverTelemetryBackfillService policyWaiverTelemetryBackfillService,
      TaskScheduler taskScheduler,
      TenantUtil tenantUtil)
  {
    super(PATH);
    this.policyWaiverTelemetryBackfillService = policyWaiverTelemetryBackfillService;
    this.taskScheduler = taskScheduler;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    if (tenantUtil.isSingleTenant() && policyWaiverTelemetryBackfillService.isTelemetryCollectionComplete()) {
      output.write("Skipping scheduling " + NAME + " as telemetry collection is complete.\n");
      return;
    }

    taskScheduler.scheduleOneTimeTask(this);
    output.write("Scheduled run of " + NAME + "\n");
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    schedulePolicyWaiverTelemetryBackfillTask();
  }

  @Override
  public String getJobName() {
    return NAME;
  }

  @Override
  public void executeForTenant(final JobExecutionContext context, final Tenant tenant) {
    execute(this::collectAndSendPolicyWaiverTelemetryBackfill, log, TASK_ERROR);
  }

  private void collectAndSendPolicyWaiverTelemetryBackfill() {
    log.info("Running {}", NAME);
    policyWaiverTelemetryBackfillService.collectAndSendPolicyWaiverBackfillTelemetry();
  }

  @Override
  public int registrationPriority() {
    // Run this last allowing the tenant startup procedure to complete first
    return Integer.MAX_VALUE - 1;
  }

  private void schedulePolicyWaiverTelemetryBackfillTask() {
    if (tenantUtil.isSingleTenant() && policyWaiverTelemetryBackfillService.isTelemetryCollectionComplete()) {
      log.debug("Skipping scheduling {} as Telemetry collection is complete.", NAME);
      return;
    }

    Date startTime = getStartTimeWithDelayFromNow();
    taskScheduler.schedulePeriodicTask(this, Duration.ofDays(1), startTime);
    log.debug("Scheduled {}, to run every day starting at {}.", NAME, startTime);
  }

  private Date getStartTimeWithDelayFromNow() {
    // Schedule the task to run TASK_STARTUP_DELAY_MINUTES after the server start time, this allows any other
    // servers in the cluster to start up ensuring the task is only scheduled once.
    LocalDateTime startTime = LocalDateTime.now().plusMinutes(TASK_STARTUP_DELAY_MINUTES);
    return Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
