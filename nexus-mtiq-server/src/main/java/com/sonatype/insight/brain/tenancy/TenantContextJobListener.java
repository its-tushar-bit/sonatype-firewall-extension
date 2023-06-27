/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runForAllTenants;

@Named
public class TenantContextJobListener
    extends JobListenerSupport
{
  private static final Logger log = LoggerFactory.getLogger(TenantContextJobListener.class);

  private final TenantManager tenantManager;

  private final TenantUtil tenantUtil;

  private final DeletedTenantDAO deletedTenantDAO;

  @Inject
  TenantContextJobListener(
      final TenantManager tenantManager,
      final TenantUtil tenantUtil,
      final DeletedTenantDAO deletedTenantDAO)
  {
    this.tenantManager = tenantManager;
    this.tenantUtil = tenantUtil;
    this.deletedTenantDAO = deletedTenantDAO;
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

      if (tenantUtil.isAllTenantsJob(context.getJobDetail().getJobClass()) && tenantUtil.isMtiqBatchMode()) {
        registerAllNonDeletedTenants();

        // It is possible registration failed for some tenants so only get tenants that are currently registered
        List<String> tenants = tenantManager.getRegisteredTenants();
        context.getJobDetail().getJobDataMap().put(AllTenantsJob.TENANT_LIST, tenants);
      }
    }
    catch (Exception e) {
      tidyUp();

      throw new RuntimeException(e);
    }
  }

  private void registerAllNonDeletedTenants() {
    List<String> allTenants = tenantUtil.getAllTenants();
    List<String> deletedTenants = deletedTenantDAO.getAllTenantDeletions().stream()
        .map(DeletedTenant::getId).collect(Collectors.toList());

    List<String> allNonDeletedTenants = allTenants.stream()
        .filter(t -> !deletedTenants.contains(t)).collect(Collectors.toList());

    runForAllTenants(allNonDeletedTenants, "registerAllTenants",
        t -> {
          log.trace("Setting tenant {} for quartz job execution", t);
          try {
            tenantManager.setTenant(t);
          }
          catch (Exception e) {
            log.error("Failed to register tenant {} for execution of quartz jobs", t, e);
          }
        });
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
