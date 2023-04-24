/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.MultiTenantDataSourceFactory;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantMigrator;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

import io.dropwizard.cli.Cli;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MtiqDbMigrationCommand
    extends ConfiguredCommand<MultiTenantInsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(MtiqDbMigrationCommand.class);

  private final DatabaseContainer databaseContainer;

  public MtiqDbMigrationCommand(final DatabaseContainer databaseContainer) {
    super("migrate-mtiq-db", "Migrates the database to the latest schema version for all MTIQ tenants.");
    this.databaseContainer = databaseContainer;
  }

  @Override
  public void onError(Cli cli, Namespace namespace, Throwable t) {
    // throw up to let our main() method do the desired error logging/handling
    String message = String.format("Error trying to migrate the database for tenant: %s. Error: %s.",
        TenantThreadLocal.getTenant().databaseSchema, t.getMessage());
    log.error(message, t);
    throw new IllegalStateException(message, t);
  }

  @Override
  protected void run(
      Bootstrap<MultiTenantInsightConfig> bootstrap,
      Namespace namespace,
      MultiTenantInsightConfig insightConfig)
  {
    log.info("Starting DB migrations for tenants");

    // The MTIQ has additional control over the 'locks' DataSource object. The configuration for this comes from a
    // custom property defined in MultiTenantInsightConfig which we then need to set into the factory.
    MultiTenantDataSourceFactory dataSourceFactory =
        (MultiTenantDataSourceFactory) databaseContainer.getDataSourceFactory();
    dataSourceFactory.setInsightConfig(insightConfig);

    TenantMigrator tenantMigrator =
        new TenantMigrator(databaseContainer.getDatabaseProvisionUtils(), insightConfig);
    tenantMigrator.migrateAllSchemas();

    log.info("DB migrations for tenants Finished");
  }
}
