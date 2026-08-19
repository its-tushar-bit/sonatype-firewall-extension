/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.policy.violation.PolicyViolationConstraintFactsJsonAsyncDbMigration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.Configuration;
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
import java.time.ZonedDateTime;
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
public class HistoricalPolicyViolationTelemetryTask
    extends AdminTask
    implements InsightJob, AllTenantsJob
{
  public static final String PATH = "triggerHistoricalPolicyViolationTelemetryTask";

  private static final Logger log = LoggerFactory.getLogger(HistoricalPolicyViolationTelemetryTask.class);

  public static final String NAME = "HistoricalPolicyViolationTelemetryTask";

  private static final String TASK_ERROR = "Error running HistoricalPolicyViolationTelemetryTask.";

  private static final long TASK_STARTUP_DELAY_MINUTES = 15L;

  private final Configuration configuration;

  private final HistoricalPolicyViolationTelemetryService historicalPolicyViolationTelemetryService;

  private final TaskScheduler taskScheduler;

  private final TenantUtil tenantUtil;

  private final MigrationTrackerDAO migrationTrackerDAO;

  public boolean disableForTesting;

  @Inject
  HistoricalPolicyViolationTelemetryTask(
      final Configuration configuration,
      final HistoricalPolicyViolationTelemetryService historicalPolicyViolationTelemetryService,
      final TaskScheduler taskScheduler,
      final TenantUtil tenantUtil,
      final MigrationTrackerDAO migrationTrackerDAO)
  {
    super(PATH);
    this.configuration = configuration;
    this.historicalPolicyViolationTelemetryService = historicalPolicyViolationTelemetryService;
    this.taskScheduler = taskScheduler;
    this.tenantUtil = tenantUtil;
    this.migrationTrackerDAO = migrationTrackerDAO;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) throws Exception {
    if (tenantUtil.isSingleTenant() && historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()) {
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
    scheduleHistoricalPolicyViolationTelemetryTask();
  }

  /**
   * Schedule a task to collect and send the historical policy violation telemetry. The task will run daily at the hour
   * defined by the historicalPolicyViolationTelemetryHour configuration option. If the
   * historicalPolicyViolationTelemetryHour configuration option is not set, the task will be scheduled to run daily
   * TASK_STARTUP_DELAY_MINUTES minutes after the server start time.
   *
   * @see Configuration#getHistoricalPolicyViolationTelemetryHour()
   */
  public void scheduleHistoricalPolicyViolationTelemetryTask() {
    if (tenantUtil.isSingleTenant() && historicalPolicyViolationTelemetryService.isTelemetryCollectionComplete()) {
      log.debug("Skipping scheduling {} as Telemetry collection is complete.", NAME);
      return;
    }

    Date startTime = getStartTime(LocalDateTime.now());
    taskScheduler.schedulePeriodicTask(this, Duration.ofDays(1), startTime);
    log.debug("Scheduled {}, to run every day starting at {}.", NAME, startTime);
  }

  private boolean isPolicyViolationConstraintFactsJsonMigrationComplete() {
    return migrationTrackerDAO.isTrackerPresent(
        PolicyViolationConstraintFactsJsonAsyncDbMigration.class.getSimpleName());
  }

  // Visible for testing
  Date getStartTime(final LocalDateTime now) {
    Integer historicalPolicyViolationTelemetryHour = configuration.getHistoricalPolicyViolationTelemetryHour();
    if (historicalPolicyViolationTelemetryHour != null) {
      return getNextStartTimeFromHour(now, historicalPolicyViolationTelemetryHour);
    }

    return getNextStartTimeDefault(now);
  }

  private Date getNextStartTimeFromHour(final LocalDateTime now, final int startHour) {
    LocalDateTime startTime = now.withHour(startHour).withMinute(0).withSecond(0).withNano(0);

    ZonedDateTime zonedStartTime = startTime.atZone(ZoneId.systemDefault());
    if (!zonedStartTime.isAfter(now.atZone(ZoneId.systemDefault()))) {
      zonedStartTime = zonedStartTime.plusDays(1);
    }

    return Date.from(zonedStartTime.toInstant());
  }

  private Date getNextStartTimeDefault(final LocalDateTime now) {
    // Schedule the task to run TASK_STARTUP_DELAY_MINUTES after the server start time, this allows any other
    // servers in the cluster to start up ensuring the task is only scheduled once.
    LocalDateTime startTime = now.plusMinutes(TASK_STARTUP_DELAY_MINUTES);
    return Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  @Override
  public void executeForTenant(final JobExecutionContext context, final Tenant tenant) {
    execute(this::collectAndSendPolicyViolationTelemetry, log, TASK_ERROR);
  }

  private void collectAndSendPolicyViolationTelemetry() {
    if (!isPolicyViolationConstraintFactsJsonMigrationComplete()) {
      log.warn("Skipping {} as the PolicyViolationConstraintFactsJson migration is not yet complete", NAME);
      return;
    }

    log.info("Running {}", NAME);
    historicalPolicyViolationTelemetryService.collectAndSendPolicyViolationTelemetry();
  }

  @Override
  public String getJobName() {
    return NAME;
  }

  @Override
  public int registrationPriority() {
    // Run this last allowing the tenant startup procedure to complete first
    return Integer.MAX_VALUE - 1;
  }
}
