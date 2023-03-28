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

@Named
public class TenantContextJobListener
    extends JobListenerSupport
{
  private final TenantManager tenantManager;

  private final TenantUtil tenantUtil;

  @Inject
  TenantContextJobListener(final TenantManager tenantManager, final TenantUtil tenantUtil) {
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext context) {
    try {
      tidyUp();

      String group = context.getJobDetail().getKey().getGroup();

      Tenant tenant;
      if (tenantUtil.isGlobalTenant(group)) {
        tenant = Tenant.GLOBAL_TENANT;
      }
      else {
        tenant = new Tenant(group);
      }

      tenantUtil.validateTenantForType(context.getJobInstance().getClass(), tenant);

      tenantManager.setTenant(tenant);
    }
    catch (Exception e) {
      tidyUp();

      throw new RuntimeException(e);
    }
  }

  @Override
  public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    tidyUp();
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext context) {
    tidyUp();
  }

  private void tidyUp() {
    TenantThreadLocal.invalidateTenant();

    tenantUtil.setGlobalTenant();
  }
}
