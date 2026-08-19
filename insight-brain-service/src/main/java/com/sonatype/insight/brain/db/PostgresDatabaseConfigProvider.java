/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.Objects;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

public class PostgresDatabaseConfigProvider
    implements DatabaseConfigProvider
{
  protected final InsightConfig insightConfig;

  public PostgresDatabaseConfigProvider(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
    DatabaseConfig databaseConfig = new DatabaseConfig(insightConfig.getDatabase());
    databaseConfig.setDriverClassName(org.postgresql.Driver.class.getName());
    // postgres defaults to max_connections=100, this is a best effort to not hit that limit by default
    databaseConfig.setMaxConnections(Objects.requireNonNullElse(databaseConfig.getMaxConnections(), 45));
    if (!DatabaseName.ods.equals(databaseName)) {
      databaseConfig.setMaxIdleConnections(3);
    }
    databaseConfig.setApplicationName(new DbApplicationNameGenerator().generateApplicationNameWithHost("IQ"));
    return databaseConfig;
  }

  @Override
  public DatabaseEngine getDatabaseEngine() {
    return PostgresDatabaseEngine.INSTANCE;
  }
}
