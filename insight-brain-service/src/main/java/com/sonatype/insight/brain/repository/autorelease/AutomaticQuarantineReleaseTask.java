/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a scheduled or manually triggered automatic quarantine release task for repositories.
 * 
 * The @DisallowConcurrentExecution annotation on this class can be confusing/misleading, especially when combined with
 * the @Singleton annotation.
 * Here is how it works:
 * The @DisallowConcurrentExecution annotation is a Quartz annotation that doesn't allow Quartz to run two jobs with the
 * same Quartz job key concurrently. It does not act on java instances. This means we can have a singleton that triggers
 * concurrent jobs (as long as the jobs have different keys).
 * This is particularly important in MTIQ, where Quartz jobs have the tenant slug in their job key, which allows MTIQ to
 * run a job/task of this type per tenant in parallel (despite of the @DisallowConcurrentExecution annotation).
 * 
 * @since 1.152
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class AutomaticQuarantineReleaseTask
    extends Task
    implements InsightJob, MtiqBatchJob
{
  public static final String NAME = "AutomaticQuarantineReleaseTask";

  private static final Logger log = LoggerFactory.getLogger(AutomaticQuarantineReleaseTask.class);

  private final Provider<AutomaticQuarantineRelease> automaticQuarantineReleaseProvider;

  @Inject
  public AutomaticQuarantineReleaseTask(Provider<AutomaticQuarantineRelease> automaticQuarantineReleaseProvider) {
    super("triggerAutomaticQuarantineRelease");
    this.automaticQuarantineReleaseProvider = automaticQuarantineReleaseProvider;
  }

  // To tigger the task:
  // curl -X POST -u <user>:<password> http://localhost:8071/tasks/triggerAutomaticQuarantineRelease
  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    log.info("Manual request to run Automatic Quarantine Release");

    automaticQuarantineReleaseProvider.get().run();
    output.write("Completed manual Automatic Quarantine Release execution\n");
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.info("Automatic request to run Automatic Quarantine Release for tenant {}", TenantThreadLocal.getTenant());
    execute(automaticQuarantineReleaseProvider.get()::run, log, "Error executing Automatic Quarantine Release");
    log.info("Next Automatic Quarantine Release execution scheduled for {}", context.getNextFireTime());
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
