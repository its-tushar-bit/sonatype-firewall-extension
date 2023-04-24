/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.MultiTenantDatabaseConfigProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAs;

public class TenantMigrator
{
  private static final Logger log = LoggerFactory.getLogger(TenantMigrator.class);

  private final DatabaseProvisionUtils databaseProvisionUtils;

  private final MultiTenantInsightConfig insightConfig;

  private final MultiTenantDatabaseConfigProvider databaseConfigProvider;

  public TenantMigrator(
      DatabaseProvisionUtils databaseProvisionUtils,
      MultiTenantInsightConfig insightConfig)
  {
    this.databaseProvisionUtils = databaseProvisionUtils;
    this.insightConfig = insightConfig;
    databaseConfigProvider = new MultiTenantDatabaseConfigProvider(insightConfig);
  }

  public void migrateAllSchemas() {
    databaseProvisionUtils.initializeDatabasesWithoutMigration(databaseConfigProvider);

    List<String> schemas = DatabaseUtil.getSchemasList(OperationalDataStoreProvider.getInstance().getDataSource());

    List<Tenant> tenants = schemas.stream().filter(schema -> schema.startsWith("t_")).map(this::createTenantFromSchema)
        .collect(Collectors.toList());

    log.debug("Tenants to migrate: {}", tenants.stream().map(tenant -> tenant.tenantSlug).collect(Collectors.toList()));

    for (Tenant tenant : tenants) {
      runAs(tenant, () -> {
        migrateSchema();
        tenant.invalidate();
        return null;
      });
    }
  }

  private void migrateSchema() {
    try {
      DatabaseMigrator.setForceEnableMigration(true);

      log.debug("Running DB migrations for tenant: {}", TenantThreadLocal.getTenant().databaseSchema);

      databaseProvisionUtils.initializeDatabases(insightConfig, databaseConfigProvider);
    }
    finally {
      DatabaseMigrator.setForceEnableMigration(false);
    }
  }

  public Tenant createTenantFromSchema(String schema) {
    String tenantSlug = schema.replaceFirst("t_", "").replace('_', '-');
    return new Tenant(tenantSlug);
  }
}
