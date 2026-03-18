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

import static java.util.stream.Collectors.joining;

public class PostgresDatabaseConfigProvider
    implements DatabaseConfigProvider
{
  protected final InsightConfig insightConfig;

  public PostgresDatabaseConfigProvider(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
    com.sonatype.insight.brain.service.DatabaseConfig dbConfig = insightConfig.getDatabase();
    String url = "jdbc:postgresql://" + dbConfig.getHostname();
    if (dbConfig.getPort() != null) {
      url += ":" + dbConfig.getPort();
    }
    url += "/" + dbConfig.getName();
    if (dbConfig.getParameters() != null && !dbConfig.getParameters().isEmpty()) {
      url += "?" + dbConfig.getParameters()
          .entrySet()
          .stream()
          .filter(entry -> !"user".equals(entry.getKey()) && !"password".equals(entry.getKey()))
          .map(entry -> entry.getKey() + '=' + entry.getValue())
          .collect(joining("&"));
    }

    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName(org.postgresql.Driver.class.getName());
    databaseConfig.setUrl(url);
    databaseConfig.setUsername(dbConfig.getUsername());
    databaseConfig.setPassword(dbConfig.getPassword());
    // postgres defaults to max_connections=100, this is a best effort to not hit that limit by default
    databaseConfig.setMaxConnections(Objects.requireNonNullElse(dbConfig.getMaxConnections(), 45));
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
