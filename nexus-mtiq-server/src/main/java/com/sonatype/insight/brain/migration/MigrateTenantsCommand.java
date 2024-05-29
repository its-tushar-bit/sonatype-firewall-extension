/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseContainerSupport;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.MultiTenantDatabaseContainer;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantMigrator;

import io.dropwizard.cli.Cli;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateTenantsCommand
    extends ConfiguredCommand<MultiTenantInsightConfig>
    implements DatabaseContainerSupport
{
  private static final Logger log = LoggerFactory.getLogger(MigrateTenantsCommand.class);

  public MigrateTenantsCommand() {
    super("migrate-mtiq-db",
        "Migrates the database to the latest schema version for the Global schema and all tenants.");
  }

  @Override
  public void onError(Cli cli, Namespace namespace, Throwable t) {
    // throw up to let our main() method do the desired error logging/handling
    String message = "Error running tenants database migrations.";
    log.error(message, t);
    throw new IllegalStateException(message, t);
  }

  @Override
  protected void run(
      Bootstrap<MultiTenantInsightConfig> bootstrap,
      Namespace namespace,
      MultiTenantInsightConfig insightConfig)
  {
    log.info("Starting DB migrations for the Global Schema and all tenants");

    DatabaseContainer databaseContainer = createDatabaseContainer(insightConfig);
    DatabaseProvisioner databaseProvisioner = databaseContainer.getDatabaseProvisioner();
    databaseProvisioner.initializeDatabaseWithoutMigration();

    TenantMigrator tenantMigrator = new TenantMigrator(databaseContainer.getDatabaseProvisioner());

    tenantMigrator.migrateGlobalSchema();
    log.info("DB migrations for Global schema finished.");

    tenantMigrator.migrateAllSchemas();
    log.info("DB migrations for tenants finished.");
  }

  @Override
  public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
    return new MultiTenantDatabaseContainer((MultiTenantInsightConfig) insightConfig);
  }
}
