/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;

import com.google.common.collect.ImmutableMap;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Named
@Singleton
@DisallowConcurrentExecution
public class TenantDeregistrationJob
    implements InsightJob
{
  private static final String JOB_NAME = "TenantDeregstrationJob";

  private static final String TENANT_NAME_KEY = "TENANT_NAME";

  private final TenantManager tenantManager;

  private final MultiTenantTaskScheduler taskScheduler;

  @Inject
  public TenantDeregistrationJob(TenantManager tenantManager,
                                 MultiTenantTaskScheduler taskScheduler)
  {
    this.tenantManager = tenantManager;
    this.taskScheduler = taskScheduler;
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    String tenantName = context.getJobDetail().getJobDataMap().getString(TENANT_NAME_KEY);

    tenantManager.deregisterTenant(tenantName);
  }

  public void deregisterTenantAcrossAllNodes(String tenantSlug) {
    tenantManager.deregisterTenant(tenantSlug);

    taskScheduler.scheduleOneTimeTaskForAllOtherNodes(this, ImmutableMap.of(TENANT_NAME_KEY, tenantSlug));
  }
}
