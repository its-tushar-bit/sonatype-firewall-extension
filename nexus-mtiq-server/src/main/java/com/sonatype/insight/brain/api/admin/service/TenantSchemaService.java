/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantValidator;
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

  private final DatabaseProvisioner databaseProvisioner;

  @Inject
  public TenantSchemaService(
      OperationalDataStore operationalDataStore,
      DataMartDataStore dataMartDataStore,
      TenantValidator tenantValidator,
      DatabaseProvisioner databaseProvisioner)
  {
    this.operationalDataStore = operationalDataStore;
    this.tenantValidator = tenantValidator;
    this.dataMartDataStore = dataMartDataStore;
    this.databaseProvisioner = databaseProvisioner;
  }

  /**
   * Gets tenant schema versions for the different data stores for the given tenant
   */
  public Map<String, Integer> getSchemaVersions(String tenantSlug) {
    validateCurrentTenant(tenantSlug);

    Map<String, Integer> schemaVersions =
        DatabaseUtil.getDatabaseSchemaVersions(operationalDataStore.getDataSourceWithoutInit(),
            operationalDataStore.getDatabaseSchema());

    int dataMartDSSchemaVersion = DatabaseUtil.getLegacyDatabaseSchemaVersion(dataMartDataStore);
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
      databaseProvisioner.initializeDatabaseWithMigration();
    }
    catch (RuntimeException e) {
      // we are passing up any exception when migrating a Tenant schema
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
