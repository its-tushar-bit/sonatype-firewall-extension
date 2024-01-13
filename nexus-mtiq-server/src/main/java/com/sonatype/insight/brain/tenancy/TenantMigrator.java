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
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAs;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

public class TenantMigrator
{
  private static final Logger log = LoggerFactory.getLogger(TenantMigrator.class);

  private final DatabaseProvisionUtils databaseProvisionUtils;

  private final MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection;

  public TenantMigrator(
      final DatabaseProvisionUtils databaseProvisionUtils,
      final MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection)
  {
    this.databaseProvisionUtils = databaseProvisionUtils;
    this.multiTenantGlobalSchemaProtection = multiTenantGlobalSchemaProtection;
  }

  public void migrateGlobalSchema() {
    runAsGlobal(() -> {
      log.debug("Disabling Global Schema write protection");
      multiTenantGlobalSchemaProtection.disableWriteProtection();

      try {
        log.info("Running database migrations for Global Schema");
        migrateSchema();
      }
      catch (Exception e) {
        throw new IllegalStateException("Error trying to migrate the database for Global Schema.", e);
      }
      finally {
        log.debug("Restoring Global Schema write protection");
        multiTenantGlobalSchemaProtection.enableWriteProtection();
      }

      return null;
    });
  }

  public void migrateAllSchemas() {
    List<String> schemas =
        DatabaseUtil.getSchemasList(databaseProvisionUtils.getOperationalDataStore().getDataSource());

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

      databaseProvisionUtils.initializeDatabasesWithMigration();
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
