/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.insight.brain.tenancy.TenantVirtualThreadExecutor;
import jakarta.annotation.Nullable;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A virtual-thread executor for policy evaluations.
 * <p>
 * This replaces the previous fixed-size {@code PolicyEvaluationThreadPoolExecutor} (200 platform
 * threads with an unbounded queue) which was susceptible to deadlock: all threads could block
 * waiting for DB connections while operations holding DB connections needed to submit work back
 * to the saturated executor.
 * <p>
 * With virtual threads, each submitted task starts immediately on its own virtual thread. There is
 * no bounded pool and no queue, so the circular wait condition cannot occur.
 */
public class PolicyEvaluationVirtualThreadExecutor
    extends TenantVirtualThreadExecutor
{
  private static final Logger log = LoggerFactory.getLogger(PolicyEvaluationVirtualThreadExecutor.class);

  public PolicyEvaluationVirtualThreadExecutor(@Nullable MeterRegistry meterRegistry, String kind, String name) {
    super(meterRegistry, kind, name);
  }

  // Visible for testing
  @Override
  protected int getActiveTaskCount() {
    return super.getActiveTaskCount();
  }

  @Override
  public void execute(Runnable command) {
    log.debug("Policy evaluation executor state before submit: activeTasks={}",
        getActiveTaskCount());

    super.execute(command);

    log.debug("Policy evaluation executor state after submit: activeTasks={}",
        getActiveTaskCount());
  }
}
