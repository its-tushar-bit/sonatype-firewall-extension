/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service in charge of the logic to provision a new tenant for MTIQ
 */
public class TenantProvisioningService
{
  private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);

  private final InsightConfig insightConfig;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  private final DatabaseConfigProvider databaseConfigProvider;

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  @Inject
  public TenantProvisioningService(
      InsightConfig insightConfig,
      DatabaseProvisionUtils databaseProvisionUtils,
      DatabaseConfigProvider databaseConfigProvider,
      TenantUtil tenantUtil,
      TenantValidator tenantValidator)
  {
    this.insightConfig = insightConfig;
    this.databaseProvisionUtils = databaseProvisionUtils;
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.databaseConfigProvider = databaseConfigProvider;
  }

  /**
   * Provision a new tenant. This means creates the schema and executes the init scripts for the new schema
   *
   * @see com.sonatype.insight.brain.tenancy.AdminTenantFilter to check how the tenant is set.
   */
  public void provisionTenant(String tenantSlug) {
    /* Proper validations for the tenant name were executed as part of the AdminTenantFilter.
     * Here we are just checking we are not using the global tenant */
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} already exists", tenantSlug.replaceAll("[\n\r]", "_"));
      throw new ConflictException("Tenant already exists");
    }

    databaseProvisionUtils.initializeDatabases(insightConfig, databaseConfigProvider);
    log.debug("New Tenant Provisioned: {}", tenantSlug.replaceAll("[\n\r]", "_"));
  }
}
