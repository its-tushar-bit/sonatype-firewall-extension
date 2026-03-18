/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a scheduled or manually triggered policy monitoring task for applications.
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
 * @since 1.8
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class PolicyMonitoringTask
    implements InsightJob
{
  public static final String NAME = "PolicyMonitoringTask";

  private static final Logger log = LoggerFactory.getLogger(PolicyMonitoringTask.class);

  private final Provider<PolicyMonitor> policyMonitorProvider;

  @Inject
  public PolicyMonitoringTask(Provider<PolicyMonitor> policyMonitorProvider) {
    this.policyMonitorProvider = policyMonitorProvider;
  }

  @Override
  public void execute(JobExecutionContext context) {
    log.info("Request to run Policy Monitor for tenant {}", TenantThreadLocal.getTenant());
    execute(policyMonitorProvider.get()::run, log, "Policy monitoring error");
    log.info("Next Policy Monitor execution scheduled for {}", context.getNextFireTime());
  }

  @Override
  public String getJobName() {
    return NAME;
  }
}
