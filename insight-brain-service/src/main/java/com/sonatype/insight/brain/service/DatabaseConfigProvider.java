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

import static java.util.stream.Collectors.joining;

/**
 * @since 1.37
 */
public class DatabaseConfigProvider
{
  private static final long MAX_CACHE_SIZE_BYTES = 7L * 1024 * 1024 * 1024;

  private static final Logger log = LoggerFactory.getLogger(DatabaseConfigProvider.class);

  private final InsightConfig config;

  private final Runtime runtime;

  public DatabaseConfigProvider(InsightConfig config) {
    this(config, Runtime.getRuntime());
  }

  DatabaseConfigProvider(InsightConfig config, Runtime runtime) {
    this.config = config;
    this.runtime = runtime;
  }

  public DatabaseConfig getDatabaseConfig(DatabaseName databaseName) {
    com.sonatype.insight.brain.service.DatabaseConfig dbConfig = config.getDatabase();
    if (dbConfig != null) {
      log.info("Using external database at {}", dbConfig.getHostname());
      return getExternalDatabaseConfig(databaseName, dbConfig);
    }

    File databaseDir = new File(config.getSonatypeWork(), "data");
    log.info("Using embedded database at {}", databaseDir.getAbsolutePath());

    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    StringBuilder urlBuilder = new StringBuilder().append("jdbc:h2:").append(databaseDir.getAbsolutePath()).append('/')
        .append(databaseName).append(";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=60000;MV_STORE=FALSE");
    if (databaseName == DatabaseName.ods) {
      // NOTE: H2 uses previous setting if not set in URL, so be explicit about the default size
      long dbCacheSizeInBytes = 16L * 1024 * 1024;
      if (config.getDbCacheSizePercent() != null) {
        dbCacheSizeInBytes = getMaxMemory() * config.getDbCacheSizePercent() / 100;
        // CLM-8847 enforce a maximum cache size due to a possible overflow problem in h2 
        // see https://github.com/h2database/h2database/issues/630 for more details
        if (dbCacheSizeInBytes > MAX_CACHE_SIZE_BYTES) {
          log.info("Cache size {} bytes for ods h2 database is too large, restricting to {} bytes", dbCacheSizeInBytes,
              MAX_CACHE_SIZE_BYTES);
          dbCacheSizeInBytes = MAX_CACHE_SIZE_BYTES;
        }
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

  // Visible for testing
  long getMaxMemory() {
    return runtime.maxMemory();
  }

  private DatabaseConfig getExternalDatabaseConfig(
      DatabaseName databaseName,
      com.sonatype.insight.brain.service.DatabaseConfig dbConfig)
  {
    String url = "jdbc:postgresql://" + dbConfig.getHostname();
    if (dbConfig.getPort() != null) {
      url += ":" + dbConfig.getPort();
    }
    url += "/" + dbConfig.getName();
    if (dbConfig.getParameters() != null && !dbConfig.getParameters().isEmpty()) {
      url += "?" + dbConfig.getParameters().entrySet().stream()
          .filter(entry -> !"user".equals(entry.getKey()) && !"password".equals(entry.getKey()))
          .map(entry -> entry.getKey() + '=' + entry.getValue()).collect(joining("&"));
    }

    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName(org.postgresql.Driver.class.getName());
    databaseConfig.setUrl(url);
    databaseConfig.setUsername(dbConfig.getUsername());
    databaseConfig.setPassword(dbConfig.getPassword());
    // postgres defaults to max_connections=100, this is a best effort to not hit that limit by default
    databaseConfig.setMaxConnections(45);
    if (!DatabaseName.ods.equals(databaseName)) {
      databaseConfig.setMaxIdleConnections(3);
    }
    return databaseConfig;
  }
}
