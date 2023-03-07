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
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class TenantSchemaService
{
  private static final Logger log = LoggerFactory.getLogger(TenantSchemaService.class);

  private final OperationalDataStore operationalDataStore;

  private final DataMartDataStore dataMartDataStore;

  private final TenantUtil tenantUtil;

  private final TenantValidator tenantValidator;

  @Inject
  public TenantSchemaService(
      OperationalDataStore operationalDataStore,
      DataMartDataStore dataMartDataStore,
      TenantUtil tenantUtil,
      TenantValidator tenantValidator)
  {
    this.operationalDataStore = operationalDataStore;
    this.tenantUtil = tenantUtil;
    this.tenantValidator = tenantValidator;
    this.dataMartDataStore = dataMartDataStore;
  }

  /**
   * Gets tenant schema versions for the different data stores for the given tenant
   */
  public Map<String, Integer> getSchemaVersions(String tenantSlug) {
    /* Proper validations for the tenant name were executed as part of the AdminTenantFilter.
     * Here we are just checking we are not using the global tenant */
    if (tenantUtil.isGlobalTenant()) {
      throw new BadRequestException("Invalid tenant");
    }

    if (!tenantValidator.validateTenantExists(tenantSlug)) {
      log.debug("Tenant {} doesn't exist", tenantSlug.replaceAll("[\n\r]", "_"));
      throw new NotFoundException("Tenant doesn't exist");
    }

    Map<String, Integer> schemaVersions =
        DatabaseUtil.getDatabaseSchemaVersions(operationalDataStore.getDataSourceWithoutInit(),
            operationalDataStore.getDatabaseSchema());

    int dataMartDSSchemaVersion = DatabaseUtil.getDatabaseSchemaVersion(
        dataMartDataStore.getDataSource(), dataMartDataStore.getID(), dataMartDataStore.getDatabaseSchema());
    schemaVersions.put(dataMartDataStore.getID(), dataMartDSSchemaVersion);

    return schemaVersions;
  }
}
