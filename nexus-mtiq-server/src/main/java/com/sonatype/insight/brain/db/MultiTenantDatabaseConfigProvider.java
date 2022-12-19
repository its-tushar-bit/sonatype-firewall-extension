/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.service.DatabaseConfigProvider;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.db.DatabaseConfig;

public class MultiTenantDatabaseConfigProvider
    extends DatabaseConfigProvider
{
  public MultiTenantDatabaseConfigProvider(final InsightConfig config) {
    super(config);
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
    MultiTenantInsightConfig multiTenantInsightConfig = (MultiTenantInsightConfig) config;
    // Return the custom `mainDatabase` entry from the MTIQ config
    return multiTenantInsightConfig.getMainDatabase();
  }
}
