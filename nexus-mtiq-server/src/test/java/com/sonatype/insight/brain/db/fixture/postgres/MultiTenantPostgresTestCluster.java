/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.postgres;

import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.MultiTenantAggregationDataStore;
import com.sonatype.insight.brain.db.MultiTenantDataMartDataStore;
import com.sonatype.insight.brain.db.MultiTenantOperationalDataStore;
import com.sonatype.insight.brain.db.MultiTenantThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.datastore.AggregationDataStore;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;

public class MultiTenantPostgresTestCluster
    extends PostgresTestCluster
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantPostgresTestCluster.class);

  private static final String MTIQ_IMAGE_VERSION = "14.0-alpine";

  private static final String TEMPLATE_TENANT_NAME = "templatetenant";

  private static PostgresTestCluster INSTANCE;

  public static PostgresTestCluster getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new MultiTenantPostgresTestCluster(MTIQ_IMAGE_VERSION);
    }
    return INSTANCE;
  }

  public MultiTenantPostgresTestCluster(final String version) {
    super(version);
  }

  @Override
  protected void createFullyMigratedTemplateDatabase() {
    createNewDatabase(TEMPLATE_DATABASE);

    DatabaseConfig databaseConfig = getDatabaseConfig(TEMPLATE_DATABASE);

    MultiTenantPostgresDataSourceProvider dataSourceProvider =
        new MultiTenantPostgresDataSourceProvider(databaseConfig, databaseConfig);
    OperationalDataStore operationalDataStore = new MultiTenantOperationalDataStore(dataSourceProvider, databaseConfig);
    AggregationDataStore aggregationDataStore = new MultiTenantAggregationDataStore(dataSourceProvider, databaseConfig);
    DataMartDataStore dataMartDataStore = new MultiTenantDataMartDataStore(dataSourceProvider, databaseConfig);
    ThirdPartyScansDataStore thirdPartyScansDataStore =
        new MultiTenantThirdPartyScansDataStore(dataSourceProvider, databaseConfig);

    operationalDataStore.initialize();
    aggregationDataStore.initialize();
    dataMartDataStore.initialize();
    thirdPartyScansDataStore.initialize();
    (new DatabaseMigrator(operationalDataStore, aggregationDataStore, dataMartDataStore,
        thirdPartyScansDataStore)).migrate(true);

    // now create a tenant to use as a template
    TenantTestHelper.testAsNewTenant(TEMPLATE_TENANT_NAME, x -> {
      operationalDataStore.initialize();
      aggregationDataStore.initialize();
      dataMartDataStore.initialize();
      thirdPartyScansDataStore.initialize();

      (new DatabaseMigrator(operationalDataStore, aggregationDataStore, dataMartDataStore,
          thirdPartyScansDataStore)).migrate(true);
    });
  }

  public void cloneTenant(final String databaseName, final String databaseSchema) {
    log.info("Cloning tenant into '{}.{}'", databaseName, databaseSchema);

    try {
      String[] pgDumpCmd = {
          "/usr/local/bin/pg_dump",
          "--username", postgresTestContainer.getUsername(),
          "--dbname", databaseName,
          "--schema", "t_" + TEMPLATE_TENANT_NAME,
          "|",
          "sed",
          "'s/t_templatetenant/" + databaseSchema + "/g'",
          "|",
          "psql",
          "--username", postgresTestContainer.getUsername(),
          "--dbname", databaseName
      };
      String[] cmd = {"sh", "-c", String.join(" ", pgDumpCmd)};

      ExecResult execResult = postgresTestContainer.execInContainer(cmd);
      if (execResult.getExitCode() != 0) {
        maybeHandlePsqlError(execResult);
      }
    }
    catch (Exception e) {
      throw new IllegalStateException("Could not clone tenant", e);
    }
  }
}
