/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;
import com.sonatype.insight.brain.service.InsightJob;

import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.8
 */
@Named
@DisallowConcurrentExecution
public class PolicyMonitoringTask
    extends Task
    implements InsightJob
{
  public static final String NAME = "PolicyMonitoringTask";

  private static final Logger log = LoggerFactory.getLogger(PolicyMonitoringTask.class);

  private PolicyMonitor policyMonitor;

  @Inject
  public PolicyMonitoringTask(PolicyMonitor policyMonitor) {
    super("triggerPolicyMonitor");
    this.policyMonitor = policyMonitor;
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    log.info("Manual request to run Policy Monitor");

    policyMonitor.run();
    output.write("Completed manual Policy Monitor execution\n");
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.info("Automatic request to run Policy Monitor");
    execute(policyMonitor::run, log, "Policy monitoring error");
    log.info("Next Policy Monitor execution scheduled for {}", context.getNextFireTime());
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
