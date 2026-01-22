/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runForAllTenantsOnBatch;

/**
 * Quartz job listener for MTIQ to add pre-job-execution hooks to perform various tenant sanity checks. On nodes that do
 * not receive the call from the Admin App to create tenants (secondary `mtiq-server` nodes or `mtiq-batch` nodes) it is
 * through Quartz jobs that tenant are first discovered. This can either be on node boot or more specifically when a new
 * tenant is provisioned.
 */
@Named
public class TenantContextJobListener
    extends JobListenerSupport
{
  private static final Logger log = LoggerFactory.getLogger(TenantContextJobListener.class);

  private final TenantManager tenantManager;

  private final TenantService tenantService;

  private final TenantUtil tenantUtil;

  private final DeletedTenantDAO deletedTenantDAO;

  @Inject
  TenantContextJobListener(
      final TenantManager tenantManager,
      final TenantService tenantService,
      final TenantUtil tenantUtil,
      final DeletedTenantDAO deletedTenantDAO)
  {
    this.tenantManager = tenantManager;
    this.tenantService = tenantService;
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

      Tenant tenant = getTenantFromQuartzJob(context);

      checkAndSetTenant(tenant);

      if (tenantUtil.isAllTenantsJob(context.getJobDetail().getJobClass()) && tenantUtil.isMtiqBatchMode()) {
        registerAllNonDeletedTenants();

        // It is possible registration failed for some tenants so only get tenants that are currently registered
        List<String> tenants = tenantManager.getRegisteredTenants();
        context.getJobDetail().getJobDataMap().put(AllTenantsJob.TENANT_LIST, tenants);
        log.trace("All registered tenants: {}", tenants);
      }
    }
    catch (Exception e) {
      tidyUp();

      throw new RuntimeException(e);
    }
  }

  private Tenant getTenantFromQuartzJob(final JobExecutionContext context) {
    String group = context.getJobDetail().getKey().getGroup();

    Tenant tenant;
    if (tenantUtil.isGlobalTenant(group)) {
      tenant = Tenant.GLOBAL_TENANT;
    }
    else {
      tenant = new Tenant(group);
    }

    tenantUtil.validateTenantForType(context.getJobInstance().getClass(), tenant);

    return tenant;
  }

  /**
   * Calls {@link TenantManager#setTenant(String)} but first checks to see if the tenant is registered in this node.
   * This class is a gateway to quartz job execution and locally this method should be called and not
   * {@link TenantManager#setTenant} directly.
   * <br>
   * If this tenant is not yet registered then Quartz should not be attempting to run jobs against it yet. This can
   * happen on a second node when a tenant is being provisioned on the first node. The second node runs the job too
   * early and since the tenant is not fully provisioned can actually end up trying to populate it as well. To reduce
   * the chance of this race condition we are going to add a delay to the execution of this job. This delay will also
   * execute on a cold start of a node for the first job that attempts to run, but that is ok.
   */
  private void checkAndSetTenant(final Tenant tenant) {
    if (!tenantUtil.isGlobalTenant(tenant.tenantSlug) && !tenantManager.isTenantRegistered(tenant)) {
      log.warn("Tenant {} is not yet registered. Sleeping for 5 seconds", tenant.tenantSlug);

      try {
        Thread.sleep(5_000);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }

    tenantManager.setTenant(tenant);
  }

  private void registerAllNonDeletedTenants() {
    List<String> allTenants = tenantService.getAllTenantsNames();
    List<String> deletedTenants = deletedTenantDAO.getAllTenantDeletions().stream()
        .map(DeletedTenant::getId).collect(Collectors.toList());

    List<String> allNonDeletedTenants = allTenants.stream()
        .filter(t -> !deletedTenants.contains(t)).collect(Collectors.toList());

    runForAllTenantsOnBatch(allNonDeletedTenants, "registerAllTenants",
        t -> {
          try {
            checkAndSetTenant(t);
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
