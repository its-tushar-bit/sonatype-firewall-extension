/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import javax.inject.Inject;
import javax.inject.Named;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;

import static com.sonatype.insight.brain.tenancy.TenantUtil.isGlobalTenant;

@Named
public class TenantContextJobListener
    extends JobListenerSupport
{
  private final TenantManager tenantManager;

  @Inject
  TenantContextJobListener(final TenantManager tenantManager) {
    this.tenantManager = tenantManager;
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    String group = context.getJobDetail().getKey().getGroup();

    Tenant tenant;
    if (isGlobalTenant(group)) {
      tenant = Tenant.GLOBAL_TENANT;
    }
    else {
      tenant = new Tenant(group);
    }

    TenantUtil.validateTenantForType(context.getJobInstance().getClass(), tenant);

    tenantManager.setTenant(tenant);
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    tidyUp();
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {
    tidyUp();
  }

  private static void tidyUp() {
    TenantThreadLocal.invalidateTenant();

    TenantManager.initGlobalTenant();
  }
}
