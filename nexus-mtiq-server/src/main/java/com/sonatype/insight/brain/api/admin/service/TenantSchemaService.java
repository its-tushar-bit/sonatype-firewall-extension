/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class TenantSchemaService
{
  private static final Logger log = LoggerFactory.getLogger(TenantSchemaService.class);

  private final OperationalDataStore operationalDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final TenantValidator tenantValidator;

  private final DatabaseProvisionUtils databaseProvisionUtils;

  @Inject
  public TenantSchemaService(
      OperationalDataStore operationalDataStore,
      DataMartDataStore dataMartDataStore,
      TenantValidator tenantValidator,
      DatabaseProvisionUtils databaseProvisionUtils)
  {
    this.operationalDataStore = operationalDataStore;
    this.tenantValidator = tenantValidator;
    this.dataMartDataStore = dataMartDataStore;
    this.databaseProvisionUtils = databaseProvisionUtils;
  }

  /**
   * Gets tenant schema versions for the different data stores for the given tenant
   */
  public Map<String, Integer> getSchemaVersions(String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    Map<String, Integer> schemaVersions =
        DatabaseUtil.getDatabaseSchemaVersions(operationalDataStore.getDataSourceWithoutInit(),
            operationalDataStore.getDatabaseSchema());

    int dataMartDSSchemaVersion = DatabaseUtil.getDatabaseSchemaVersion(
        dataMartDataStore.getDataSource(), dataMartDataStore.getID(), dataMartDataStore.getDatabaseSchema());
    schemaVersions.put(dataMartDataStore.getID(), dataMartDSSchemaVersion);

    return schemaVersions;
  }

  /**
   * Calls the database initialization process of the given tenant which initializes the Datastores and migrates the
   * tenant schema to the latest version
   */
  public void migrateSchema(String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    try {
      databaseProvisionUtils.initializeDatabasesWithMigration();
    }
    catch (RuntimeException e) {
      //we are passing up any exception when migrating a Tenant schema
      log.warn("Failed to perform schema migration for tenant: {}. Exception message is: {}", tenantSlug,
          e.getMessage());
    }
  }

  private void validateCurrentTenant(String tenantSlug) {
    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug);
      throw new NotFoundException("Tenant doesn't exist");
    }
  }
}
