/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.db.H2DatabaseEngine;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2DiskDatabaseConfigProvider
    implements DatabaseConfigProvider
{
  private static final Logger log = LoggerFactory.getLogger(H2DiskDatabaseConfigProvider.class);

  private static final long MAX_CACHE_SIZE_BYTES = 7L * 1024 * 1024 * 1024;

  private final InsightConfig insightConfig;

  private final Runtime runtime;

  public H2DiskDatabaseConfigProvider(final InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
    this.runtime = Runtime.getRuntime();
  }

  @VisibleForTesting
  public H2DiskDatabaseConfigProvider(final InsightConfig insightConfig, final Runtime runtime) {
    this.insightConfig = insightConfig;
    this.runtime = runtime;
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final DatabaseName databaseName) {
    File databaseDir = new File(insightConfig.getSonatypeWork(), "data");
    log.info("Using embedded database at {}", databaseDir.getAbsolutePath());

    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    StringBuilder urlBuilder = new StringBuilder().append("jdbc:h2:")
        .append(databaseDir.getAbsolutePath())
        .append('/')
        .append(databaseName)
        .append(";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=60000;MV_STORE=FALSE");
    if (databaseName == DatabaseName.ods) {
      // NOTE: H2 uses previous setting if not set in URL, so be explicit about the default size
      long dbCacheSizeInBytes = 16L * 1024 * 1024;
      if (insightConfig.getDbCacheSizePercent() != null) {
        dbCacheSizeInBytes = getMaxMemory() * insightConfig.getDbCacheSizePercent() / 100;
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
    String additionalDBParams = insightConfig.getAdditionalDBParams();
    if (additionalDBParams != null) {
      urlBuilder.append(";").append(additionalDBParams);
    }
    databaseConfig.setUrl(urlBuilder.toString());
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }

  @Override
  public DatabaseEngine getDatabaseEngine() {
    return H2DatabaseEngine.INSTANCE;
  }

  long getMaxMemory() {
    return runtime.maxMemory();
  }
}
