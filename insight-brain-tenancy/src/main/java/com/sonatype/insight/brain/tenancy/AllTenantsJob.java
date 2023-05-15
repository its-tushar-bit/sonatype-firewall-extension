/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runForAllTenants;

/**
 * This tells Quartz that this particular job should loop through and attempt to run for all tenants. Jobs that loop
 * through all tenants should only be run on a Mtiq Batch instance.
 */
public interface AllTenantsJob extends Job, TenantManaged, MtiqBatchJob
{
  Logger log = LoggerFactory.getLogger(AllTenantsJob.class);

  String TENANT_LIST = "quartz.tenant.list";

  TenantUtil tenantUtil = new TenantUtil();

  @Override
  default void execute(JobExecutionContext context) {
    if (tenantUtil.isSingleTenant()) {
      executeForTenant(context, Tenant.SINGLE_TENANT);
    }
    else if (tenantUtil.isMtiqBatchMode()) {

      // The list of tenants to run against is set by TenantContextJobListener
      Object tenantList = context.getJobDetail().getJobDataMap().get(TENANT_LIST);

      if (tenantList instanceof List) {
        runForAllTenants((List<String>) tenantList, "QuartzJob:" + context.getJobDetail().getKey(),
            tenant -> {
              if (isLicensed()) {
                executeForTenant(context, tenant);
              }
            });
      }
      else if (tenantUtil.isAllTenantsJob(context.getJobDetail().getJobClass())) {
        log.warn("Attempting to run an all tenant {} job but no tenants set on in the job detail map",
            context.getJobDetail().getJobClass());
      }
    }
  }

  void executeForTenant(JobExecutionContext context, Tenant tenant);

  default boolean isLicensed() {
    return true;
  }
}
