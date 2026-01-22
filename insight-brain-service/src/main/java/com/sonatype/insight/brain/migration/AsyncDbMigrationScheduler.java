/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.model.HasStringId;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AsyncDbMigrationScheduler runs all the async database migrations, that extend the {@link AsyncDbMigration} class.
 * The migrations are run in order of their priority determined by the
 * {@link AsyncDbMigration#migrationPriority()} method.
 * -
 * This implements AllTenantsJob therefore when in multi-tenant mode, the migrator runs all the migrations for each
 * tenant on a batch node.
 * The migrations will only run on a single node in a multi-node cluster.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class AsyncDbMigrationScheduler
    extends Task
    implements InsightJob, AllTenantsJob
{
  public static final String TASK_PATH = "runAsyncDbMigrations";

  private static final Logger log = LoggerFactory.getLogger(AsyncDbMigrationScheduler.class);

  private final TaskScheduler taskScheduler;

  private final Set<AsyncDbMigration<? extends HasStringId>> jobs;

  @Inject
  public AsyncDbMigrationScheduler(
      final Set<AsyncDbMigration<? extends HasStringId>> jobs,
      final TaskScheduler taskScheduler)
  {
    super(TASK_PATH);
    this.jobs = jobs;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public void register() {
    taskScheduler.scheduleOneTimeTask(this);
  }

  @Override
  public void execute(final Map<String, List<String>> map, final PrintWriter printWriter) throws Exception {
    log.info("Manual request to run {}", getJobName());

    taskScheduler.scheduleOneTimeTask(this);

    printWriter.write("Scheduled run of " + getJobName() + "\n");
  }

  @Override
  public void executeForTenant(JobExecutionContext context, Tenant tenant) {
    log.info("Automatic request to run {}", getJobName());
    runAllAsyncMigrations();
  }

  private void runAllAsyncMigrations() {
    var prioritizedJobs = jobs.stream().sorted().toList();
    log.debug("Running {} migrations", prioritizedJobs);
    for (var job : prioritizedJobs) {
      job.runMigration();
    }

    log.info("Completed request to run {}", getJobName());
  }

  @Override
  public int registrationPriority() {
    // Run this last allowing the tenant startup procedure to complete first
    return Integer.MAX_VALUE - 1;
  }

  @Override
  public String getJobName() {
    return getClass().getSimpleName();
  }
}
