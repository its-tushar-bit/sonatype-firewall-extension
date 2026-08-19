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
import com.sonatype.insight.brain.service.MtiqConfigSupport;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.spring.InsightBrainCompatibilityCommand;
import com.sonatype.insight.brain.tenancy.TenantMigrator;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MigrateTenantsCommand
    implements InsightBrainCompatibilityCommand, DatabaseContainerSupport
{
  public static final String NAME = "migrate-mtiq-db";

  public static final String DESCRIPTION =
      "Migrates the database to the latest schema version for the Global schema and all tenants.";

  private static final Logger log = LoggerFactory.getLogger(MigrateTenantsCommand.class);

  private final MultiTenantInsightConfig insightConfig;

  MigrateTenantsCommand() {
    this(new MultiTenantInsightConfig());
  }

  @Inject
  public MigrateTenantsCommand(MultiTenantInsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  public void onError(Object ignoredCli, Object ignoredNamespace, Throwable t) {
    String message = "Error running tenants database migrations.";
    log.error(message, t);
    throw new IllegalStateException(message, t);
  }

  void run(Object ignoredBootstrap, Object ignoredNamespace, MultiTenantInsightConfig runtimeConfig) {
    run(runtimeConfig);
  }

  @Override
  public void run(String... args) {
    run(insightConfig);
  }

  public void run(MultiTenantInsightConfig runtimeConfig) {
    log.info("Starting DB migrations for the Global Schema and all tenants");

    DatabaseContainer databaseContainer = createDatabaseContainer(runtimeConfig);
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
    return new MultiTenantDatabaseContainer(
        MtiqConfigSupport.requireMultiTenantInsightConfig(
            insightConfig,
            "MigrateTenantsCommand.createDatabaseContainer"));
  }
}
