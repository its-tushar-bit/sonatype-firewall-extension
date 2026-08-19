/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantDeregistrationJob;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;

/**
 * Service in charge of the logic to provision a new tenant for MTIQ
 */
@Named
public class TenantProvisioningService
{
  private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);

  private final DatabaseProvisioner databaseProvisioner;

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  private final TenantDeregistrationJob tenantDeregistrationJob;

  private final DeletedTenantDAO deletedTenantDAO;

  private final UserDAO userDAO;

  private final IndexService indexService;

  private final MultiTenantInsightConfig config;

  @Inject
  public TenantProvisioningService(
      DatabaseProvisioner databaseProvisioner,
      TenantUtil tenantUtil,
      TenantValidator tenantValidator,
      TenantDeregistrationJob tenantDeregistrationJob,
      DeletedTenantDAO deletedTenantDAO,
      UserDAO userDAO,
      IndexService indexService,
      MultiTenantInsightConfig config)
  {
    this.databaseProvisioner = databaseProvisioner;
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.tenantDeregistrationJob = tenantDeregistrationJob;
    this.deletedTenantDAO = deletedTenantDAO;
    this.userDAO = userDAO;
    this.indexService = indexService;
    this.config = config;
  }

  /**
   * Provision a new tenant. This means creates the schema and executes the init scripts for the new schema
   *
   * @see com.sonatype.insight.brain.tenancy.AdminTenantFilter to check how the tenant is set.
   */
  public void provisionTenant(String tenantSlug) {
    /*
     * Proper validations for the tenant name were executed as part of the AdminTenantFilter.
     * Here we are just checking we are not using the global tenant
     */
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (tenantValidator.validateTenantExists(tenantSlug)) {
      log.error("Tenant {} already exists", tenantSlug.replaceAll("[\n\r]", "_"));
      throw new ConflictException("Tenant already exists");
    }

    removeTenantMarkedForDeletion(tenantSlug);

    databaseProvisioner.initializeDatabaseWithMigration();
    log.info("New Tenant Provisioned: {}", tenantSlug.replaceAll("[\n\r]", "_"));

    adjustDefaultTenantData();

    // TODO make sure the OpenSearch configuration is set up correctly
    if (config.getSearchConfig() != null) {
      indexService.createSearchIndex();
    }
  }

  /**
   * Removes a tenant from the list of records that are marked for deletion.
   * This prevents the accidental deletion of a tenant with the same slug as a tenant that was previously deleted,
   * and whose deletion was scheduled but not yet executed.
   *
   * @param tenantSlug
   */
  private void removeTenantMarkedForDeletion(String tenantSlug) {
    DeletedTenant deletedTenant = deletedTenantDAO.getTenantBySlug(tenantSlug);
    if (deletedTenant != null) {
      log.info("Tenant {} was scheduled for deletion, removing it from the queue", tenantSlug);
      deletedTenantDAO.delete(deletedTenant);
    }
  }

  /**
   * Make adjustments to the rows that the database scripts add to the database. Some of these rows add default entities
   * that are not appropriate for SaaS tenants
   */
  private void adjustDefaultTenantData() {
    // Delete the built-in default admin if configuration is set
    User admin = userDAO.getById("ADMIN");
    if (admin != null && config.isDeleteBuiltInAdmin()) {
      userDAO.delete(admin);
    }
  }

  /**
   * Adds a DeletedTenant record that will later be picked up by a scheduled job. Essentially this puts the tenant into
   * an "archived" state while it is waiting to be deleted. Tenants are not deleted at this stage because deleting the
   * DB schema is a destructive operation, and therefore we need to prevent accidental deletion.
   *
   * @param tenantSlug - the URL slug of the tenant to be deleted
   */
  public void markTenantForDeletion(String tenantSlug) {
    if (GLOBAL_TENANT.tenantSlug.equals(tenantSlug)) {
      throw new BadRequestException("Deleting the global tenant is not allowed");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      throw new BadRequestException(String.format("Tenant %s does not exist", tenantSlug));
    }

    if (deletedTenantDAO.getTenantBySlug(tenantSlug) != null) {
      throw new BadRequestException(String.format("Tenant %s is already scheduled for deletion", tenantSlug));
    }

    deletedTenantDAO.insert(new DeletedTenant(tenantSlug));

    tenantDeregistrationJob.deregisterTenantAcrossAllNodes(tenantSlug);
  }
}
