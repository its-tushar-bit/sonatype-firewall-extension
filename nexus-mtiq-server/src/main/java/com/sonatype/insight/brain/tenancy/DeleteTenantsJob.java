/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightJob;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAs;

@Named
@Singleton
@DisallowConcurrentExecution
public class DeleteTenantsJob
    implements InsightJob, MtiqBatchJob, GlobalTenantJob
{
  private static final Logger log = LoggerFactory.getLogger(DeleteTenantsJob.class);

  // Visible for testing
  static final String TENANT_RETENTION_PERIOD_CONFIG_KEY = "TenantRetentionPeriodInHours";

  // Visible for testing
  static final long DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS = 96L;

  // Visible for testing
  static final long JOB_FREQUENCY_IN_HOURS = 24L;

  static final String JOB_NAME = "DeleteTenantJob";

  private final TaskScheduler taskScheduler;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final DeletedTenantDAO deletedTenantDAO;

  private final InsightConfig config;

  private final TenantUtil tenantUtil;

  private final MultiTenantAuth0ManagementService auth0ManagementService;

  private final TenantMetadataDAO tenantMetadataDAO;

  private final TenantManager tenantManager;

  private final TenantValidator tenantValidator;

  private final OperationalDataStore operationalDataStore;

  @Inject
  public DeleteTenantsJob(
      MultiTenantTaskScheduler taskScheduler,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      DeletedTenantDAO deletedTenantDAO,
      InsightConfig config,
      TenantUtil tenantUtil,
      MultiTenantAuth0ManagementService auth0ManagementService,
      TenantMetadataDAO tenantMetadataDAO,
      TenantManager tenantManager,
      TenantValidator tenantValidator,
      OperationalDataStore operationalDataStore)
  {
    this.taskScheduler = taskScheduler;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.deletedTenantDAO = deletedTenantDAO;
    this.config = config;
    this.tenantUtil = tenantUtil;
    this.auth0ManagementService = auth0ManagementService;
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.tenantManager = tenantManager;
    this.tenantValidator = tenantValidator;
    this.operationalDataStore = operationalDataStore;
  }

  @Override
  public void register() {
    taskScheduler.schedulePeriodicTask(this, Duration.ofHours(JOB_FREQUENCY_IN_HOURS));
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    if (tenantUtil.isSingleTenant()) {
      throw new RuntimeException("Delete tenantsJob should never be run in single tenant mode");
    }

    List<DeletedTenant> tenantsToDelete = getTenantsToDelete();

    deleteTenants(tenantsToDelete);
  }

  private List<DeletedTenant> getTenantsToDelete() {
    long retentionPeriod = tenantRetentionPeriodInHours();

    log.info("Executing DeleteTenantJob with retention period {}", retentionPeriod);

    List<DeletedTenant> tenantsToDelete =
        deletedTenantDAO.getAllTenantDeletionsOlderThanRetentionPeriod(retentionPeriod);

    if (!tenantsToDelete.isEmpty()) {
      log.info("{} tenants marked for deletion are older than the retention period {} and will be permanently deleted",
          tenantsToDelete.size(), retentionPeriod);
    }
    else {
      log.debug("No tenants marked for deletion are older than the retention period");
    }
    return tenantsToDelete;
  }

  private void deleteTenants(List<DeletedTenant> tenantsToDelete) {
    for (DeletedTenant tenant : tenantsToDelete) {
      try {
        deleteTenant(tenant);
      }
      catch (Exception e) {
        log.error("Tenant delete {} failed with.", tenant.getId(), e);
      }
    }
  }

  // Visible for testing
  void deleteTenant(DeletedTenant tenant) {
    if (GLOBAL_TENANT.tenantSlug.equals(tenant.getId())) {
      throw new RuntimeException("Deleting the global tenant is not valid");
    }

    log.info("Permanently deleting tenant {}", tenant.getId());

    boolean tenantExists = tenantValidator.validateTenantExists(tenant.getId());

    Date today = new Date();
    tenant.setLastUpdated(today);
    deletedTenantDAO.update(tenant);

    if (tenantExists && !deleteAuth0Resources(tenant)) {
      log.warn("Not able to delete Auth0 resources for tenant {}", tenant.getId());
      return;
    }

    if (!deleteJobs(tenant)) {
      log.warn("Not able to delete tenants jobs for tenant {}", tenant.getId());
      return;
    }

    if (tenantExists && !deleteDatabaseSchema(tenant)) {
      log.warn("Not able to delete schema for tenant {}", tenant.getId());
      return;
    }

    if (!deleteFilesOnDisk(tenant)) {
      log.warn("Not able to delete files on disk for tenant {}", tenant.getId());
      return;
    }

    tenant.setDeleteCompletedDate(today);
    deletedTenantDAO.update(tenant);
    log.info("Tenant {} deleted successfully", tenant.getId());
  }

  boolean deleteAuth0Resources(final DeletedTenant deletedTenant) {
    boolean success = false;

    try {
      success = tenantManager.performDatabaseRegistrationAndRunAs(deletedTenant.getId(), () -> {
        TenantMetadata tenantMetadata = tenantMetadataDAO.get();
        if (tenantMetadata == null) {
          log.info("Tenant {} metadata not found, not deleting auth0 tenant.", deletedTenant.getId());
          return true;
        }
        return auth0ManagementService.deleteTenant(tenantMetadata.getApplicationId(),
            tenantMetadata.getConnectionId(), tenantMetadata.getOrganizationId());
      });
    }
    catch (IllegalArgumentException e) {
      log.error("Delete tenant {} Auth0 failed.", deletedTenant.getId(), e);
    }

    return success;
  }

  boolean deleteJobs(DeletedTenant deletedTenant) {
    boolean success = false;

    try {
      Set<JobKey> jobKeys = taskScheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(deletedTenant.getId()));

      if (CollectionUtils.isEmpty(jobKeys)) {
        success = true;
      }

      success = taskScheduler.getScheduler().deleteJobs(new ArrayList<>(jobKeys));
    }
    catch (Exception e) {
      log.error("Failed to delete quartz jobs for tenant {}", deletedTenant.getId(), e);
    }

    return success;
  }

  boolean deleteDatabaseSchema(DeletedTenant deletedTenant) {
    boolean success = false;

    try (Connection connection = operationalDataStore.getDataSource().getConnection();
        Statement statement = connection.createStatement())
    {
      connection.setAutoCommit(true);

      String tenantSchema = new Tenant(deletedTenant.getId()).databaseSchema;
      statement.executeUpdate("DROP SCHEMA " + tenantSchema + " CASCADE;");

      success = true;
    }
    catch (Exception e) {
      log.error("Failed to delete schema for tenant {}", deletedTenant.getId(), e);
    }

    return success;
  }

  boolean deleteFilesOnDisk(DeletedTenant deletedTenant) {
    boolean success = runAs(new Tenant(deletedTenant.getId()), () -> {
      boolean sonatypeWorkDeleted = deleteDirectory(config.getSonatypeWork(), deletedTenant);
      boolean clusterDeleted = deleteDirectory(config.getClusterDirectory(), deletedTenant);

      return sonatypeWorkDeleted && clusterDeleted;
    });

    return success;
  }

  private boolean deleteDirectory(File directory, DeletedTenant deletedTenant) {
    try {
      if (directory.exists()) {
        FileUtils.deleteDirectory(directory);
      }

      return true;
    }
    catch (Exception e) {
      log.error("Failed to delete sonatype-work for tenant {}", deletedTenant.getId(), e);
    }
    return false;
  }

  private long tenantRetentionPeriodInHours() {
    SystemConfigurationProperty configuration =
        systemConfigurationPropertyDAO.getByName(TENANT_RETENTION_PERIOD_CONFIG_KEY);

    if (configuration != null && configuration.getValue() != null && !configuration.getValue().isEmpty()) {
      try {
        String value = configuration.getValue();
        return Long.parseLong(value);
      }
      catch (NumberFormatException e) {
        log.error("Failed to parse DeleteTenantJob configuration for Job Frequency. Configured value = {}. Using " +
            "default value instead. Default = {}", configuration.getValue(),
            DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS, e);
      }
    }

    return DEFAULT_TENANT_RETENTION_PERIOD_IN_HOURS;
  }
}
