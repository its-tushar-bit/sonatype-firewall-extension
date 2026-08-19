/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

public class MultiTenantDatabaseConfigProvider
    extends PostgresDatabaseConfigProvider
{
  public MultiTenantDatabaseConfigProvider(final InsightConfig config) {
    super(config);
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
    MultiTenantInsightConfig multiTenantInsightConfig = (MultiTenantInsightConfig) insightConfig;

    // Return the custom `mainDatabase` entry from the MTIQ config
    DatabaseConfig databaseConfig = multiTenantInsightConfig.getMainDatabase();

    // Set DriverClassName as expected by our DatabaseUtil class
    databaseConfig.setDriverClassName("org.postgresql.Driver");

    return databaseConfig;
  }
}
