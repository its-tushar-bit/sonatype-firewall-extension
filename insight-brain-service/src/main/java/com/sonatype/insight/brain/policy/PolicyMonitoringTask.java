/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.PrintWriter;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;
import com.sonatype.insight.brain.security.MDCUsernameScope;

import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
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
    implements Job
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
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) {
    policyMonitor.run();
    output.write("Completed manual Policy Monitor execution\n");
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      policyMonitor.run();
    }
    catch (Exception e) {
      log.error("Policy monitoring error: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(1);
    }
    finally {
      log.info("Next Policy Monitor execution scheduled for {}", context.getNextFireTime());
    }
  }
}
