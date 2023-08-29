/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseContainerSupport;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataSourceFactory;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.tenancy.TenantMigrator;
import com.sonatype.insight.brain.utils.DatabaseProvisionUtils;

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
    super("migrate-mtiq-db", "Migrates the database to the latest schema version for all MTIQ tenants.");
  }

  @Override
  public void onError(Cli cli, Namespace namespace, Throwable t) {
    // throw up to let our main() method do the desired error logging/handling
    String message = "Error running tenant database migrations.";
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

    // TODO MTIQ - soon InsightConfig will be a parameter to create the DatabaseContainer
    DatabaseContainer databaseContainer = createDatabaseContainer();

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

  @Override
  public DatabaseContainer createDatabaseContainer() {
    MultiTenantDataSourceFactory multiTenantDataSourceFactory = new MultiTenantDataSourceFactory();

    DatabaseMigrator databaseMigrator = new DatabaseMigrator(multiTenantDataSourceFactory);

    OperationalDataStore operationalDataStore =
        new MultiTenantOperationalDataStore(multiTenantDataSourceFactory, databaseMigrator);
    AggregationDataStore aggregationDataStore =
        new MultiTenantAggregationDataStore(multiTenantDataSourceFactory, databaseMigrator);
    DataMartDataStore dataMartDataStore =
        new MultiTenantDataMartDataStore(multiTenantDataSourceFactory, databaseMigrator);
    ThirdPartyScansDataStore thirdPartyScansDataStore =
        new MultiTenantThirdPartyScansDataStore(multiTenantDataSourceFactory, databaseMigrator);

    DatabaseProvisionUtils databaseProvisionUtils =
        new DatabaseProvisionUtils(operationalDataStore, aggregationDataStore, dataMartDataStore,
            thirdPartyScansDataStore);

    return new DatabaseContainer(multiTenantDataSourceFactory, databaseProvisionUtils);
  }
}
