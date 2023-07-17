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

    List<Tenant> tenants = schemas.stream()
        .filter(schema -> schema.startsWith("t_"))
        .map(this::createTenantFromSchema)
        .sorted() // sort so we run the migrations in a consistent order
        .collect(Collectors.toList());

    log.info("Total of {} tenants to migrate: {}", tenants.size(),
        tenants.stream().map(tenant -> tenant.tenantSlug).collect(Collectors.toList()));

    int index = 1;
    for (Tenant tenant : tenants) {
      Integer finalIndex = index;
      runAs(tenant, () -> {
        try {
          log.info("Running database migrations {} of {}. Processing tenant: {}", finalIndex, tenants.size(),
              TenantThreadLocal.getTenant().databaseSchema);
          migrateSchema();
        }
        catch (Exception e) {
          String message = String.format("Error trying to migrate the database for tenant: %s.",
              TenantThreadLocal.getTenant().tenantSlug);
          throw new IllegalStateException(message, e);
        }
        tenant.invalidate();
        return null;
      });
      index++;
    }
  }

  private void migrateSchema() {
    try {
      DatabaseMigrator.setForceEnableMigration(true);

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
