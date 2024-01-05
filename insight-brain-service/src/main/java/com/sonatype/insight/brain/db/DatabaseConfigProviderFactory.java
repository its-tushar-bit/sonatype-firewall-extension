/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.service.InsightConfig;

public class DatabaseConfigProviderFactory
{
  public static DatabaseConfigProvider createDatabaseConfigProvider(final InsightConfig insightConfig) {
    // No database config at all means using the H2 disk based database
    if (insightConfig.getDatabase() != null) {
      return new PostgresDatabaseConfigProvider(insightConfig);
    }
    else {
      return new H2DiskDatabaseConfigProvider(insightConfig);
    }
  }
}
