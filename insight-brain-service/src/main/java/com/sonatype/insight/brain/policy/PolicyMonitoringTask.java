/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.PrintWriter;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

/**
 * @since 1.8
 */
@Named
public class PolicyMonitoringTask
    extends Task
{
  private PolicyMonitor policyMonitor;

  @Inject
  public PolicyMonitoringTask(PolicyMonitor policyMonitor) {
    super("triggerPolicyMonitor");
    this.policyMonitor = policyMonitor;
  }

  @Override
  public void execute(final ImmutableMultimap<String, String> parameters, final PrintWriter output) throws Exception {
    policyMonitor.run();
    output.write("Completed manual Policy Monitor execution\n");
  }
}
