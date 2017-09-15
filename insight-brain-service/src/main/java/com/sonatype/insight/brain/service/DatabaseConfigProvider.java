/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.db.DatabaseConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.37
 */
public class DatabaseConfigProvider
{
  private static final Logger log = LoggerFactory.getLogger(DatabaseConfigProvider.class);

  private final InsightConfig config;

  public DatabaseConfigProvider(InsightConfig config) {
    this.config = config;
  }

  public DatabaseConfig getDatabaseConfig(DatabaseName databaseName) {
    File databaseDir = new File(config.getSonatypeWork(), "data");
    log.debug("Data directory: {}", databaseDir.getAbsolutePath());

    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    StringBuilder urlBuilder = new StringBuilder().append("jdbc:h2:").append(databaseDir.getAbsolutePath()).append('/')
        .append(databaseName).append(";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=60000");
    if (databaseName == DatabaseName.ods) {
      // NOTE: H2 uses previous setting if not set in URL, so be explicit about the default size
      long dbCacheSizeInBytes = 16L * 1024 * 1024;
      if (config.getDbCacheSizePercent() != null) {
        dbCacheSizeInBytes = Runtime.getRuntime().maxMemory() * config.getDbCacheSizePercent() / 100;
      }
      urlBuilder.append(";CACHE_SIZE=").append(dbCacheSizeInBytes / 1024);
    }
    String additionalDBParams = config.getAdditionalDBParams();
    if (additionalDBParams != null) {
      urlBuilder.append(";").append(additionalDBParams);
    }
    databaseConfig.setUrl(urlBuilder.toString());
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }
}
