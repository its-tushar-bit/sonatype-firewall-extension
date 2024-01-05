/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.postgres;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.db.datasource.MultiTenantPostgresDataSourceProvider;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

public class MultiTenantPostgresDatabaseFixture
    extends PostgresDatabaseFixture
    implements DatabaseFixture
{
  private final MultiTenantInsightConfig multiTenantInsightConfig;

  public MultiTenantPostgresDatabaseFixture(final String testName, final PostgresTest postgresTest) {
    super(testName, postgresTest);

    this.multiTenantInsightConfig = new MultiTenantInsightConfig();
    multiTenantInsightConfig.setMainDatabase(getDatabaseConfig());
    multiTenantInsightConfig.setLocksDatabase(getDatabaseConfig());
  }

  @Override
  protected PostgresTestCluster getPostgresTestCluster() {
    return MultiTenantPostgresTestCluster.getInstance();
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return new MultiTenantPostgresDataSourceProvider(multiTenantInsightConfig.getMainDatabase(),
        multiTenantInsightConfig.getLocksDatabase());
  }

  public void cloneTenant(final String databaseSchema) {
    MultiTenantPostgresTestCluster multiTenantPostgresTestCluster =
        (MultiTenantPostgresTestCluster) getPostgresTestCluster();
    multiTenantPostgresTestCluster.cloneTenant(databaseName, databaseSchema);
  }

  private DatabaseConfig getDatabaseConfig() {
    // no-arg helper - postgres doesn't use the name for the connection config
    return getDatabaseConfig("");
  }
}
