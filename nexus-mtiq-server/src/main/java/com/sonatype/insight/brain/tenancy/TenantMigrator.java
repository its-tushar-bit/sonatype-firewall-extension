/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.List;

import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.migrations.DatabaseMigrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAs;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

public class TenantMigrator
{
  private static final Logger log = LoggerFactory.getLogger(TenantMigrator.class);

  private final DatabaseProvisioner databaseProvisioner;

  public TenantMigrator(final DatabaseProvisioner databaseProvisioner) {
    this.databaseProvisioner = databaseProvisioner;
  }

  public void migrateGlobalSchema() {
    runAsGlobal(() -> {
      try {
        log.info("Running database migrations for Global Schema");
        migrateSchema();
      }
      catch (Exception e) {
        throw new IllegalStateException("Error trying to migrate the database for Global Schema.", e);
      }

      return null;
    });
  }

  public void migrateAllSchemas() {
    List<String> schemas = DatabaseUtil.getTenantSchemas(databaseProvisioner.getOperationalDataStore().getDataSource());
    log.info("Total of {} tenant schemas to migrate", schemas.size());
    schemas.forEach(schema -> {
      final Tenant tenant = createTenantFromSchema(schema);
      runAs(tenant, () -> {
        try {
          log.info("Running database migrations for tenant {}", TenantThreadLocal.getTenant().databaseSchema);
          migrateSchema();
        }
        catch (Exception e) {
          throw new IllegalStateException(
              String.format("Error migrating the database for tenant %s", TenantThreadLocal.getTenant().tenantSlug),
              e);
        }
        tenant.invalidate();
        return null;
      });
    });
  }

  private void migrateSchema() {
    try {
      DatabaseMigrations.setForceEnableMigration(true);

      databaseProvisioner.initializeDatabaseWithMigration();
    }
    finally {
      DatabaseMigrations.setForceEnableMigration(false);
    }
  }

  public static Tenant createTenantFromSchema(String schema) {
    String tenantSlug = schema.replaceFirst("t_", "").replace('_', '-');
    return new Tenant(tenantSlug);
  }
}
