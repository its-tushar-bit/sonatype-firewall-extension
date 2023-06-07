/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.autorelease;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.152
 */
@Named
@DisallowConcurrentExecution
public class AutomaticQuarantineReleaseTask
    extends Task
    implements InsightJob, MtiqBatchJob
{
  public static final String NAME = "AutomaticQuarantineReleaseTask";

  private static final Logger log = LoggerFactory.getLogger(AutomaticQuarantineReleaseTask.class);

  private AutomaticQuarantineRelease automaticQuarantineRelease;

  @Inject
  public AutomaticQuarantineReleaseTask(AutomaticQuarantineRelease automaticQuarantineRelease) {
    super("triggerAutomaticQuarantineRelease");
    this.automaticQuarantineRelease = automaticQuarantineRelease;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    log.info("Manual request to run automatic quarantine release");

    automaticQuarantineRelease.run();
    output.write("Completed manual automatic quarantine release execution\n");
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.info("Automatic request to run automatic quarantine release");
    execute(automaticQuarantineRelease::run, log, "Error executing automatic quarantine release");
    log.info("Next automatic quarantine release execution scheduled for {}", context.getNextFireTime());
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
