/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.datasource;

import java.time.Duration;

import javax.sql.DataSource;

import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDataSourceProvider
    implements DataSourceProvider
{
  static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);

  static final int DEFAULT_MAX_CONNECTIONS = 50;

  /**
   * Produces an Apache DBCP2 {@link BasicDataSource} from the given config.
   *
   * This method should be used only to create the data source(s) at startup and in very limited case where a new
   * DataSource is needed. For all other purposes, use getDataSource.
   */
  @Override
  public DataSource createNewDataSource(final DatabaseConfig databaseConfig) {
    long start = System.currentTimeMillis();

    log.debug("DB URL: '{}'", databaseConfig.getUrl());
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(databaseConfig.getDriverClassName());
    dataSource.setUrl(databaseConfig.getUrl());
    dataSource.setUsername(databaseConfig.getUsername());
    dataSource.setPassword(databaseConfig.getPassword());
    int maxConnections = DEFAULT_MAX_CONNECTIONS;
    if (databaseConfig.getMaxConnections() != null) {
      maxConnections = databaseConfig.getMaxConnections();
    }
    dataSource.setMaxConn(Duration.ofSeconds(databaseConfig.getMaxConnectionLifetimeSeconds()));
    dataSource.setLogExpiredConnections(false);
    log.debug("Setting database connection pool max size to {}.", maxConnections);
    dataSource.setMaxTotal(maxConnections);
    int maxIdleConnections = maxConnections;
    if (databaseConfig.getMaxIdleConnections() != null) {
      maxIdleConnections = databaseConfig.getMaxIdleConnections();
    }
    dataSource.setMaxIdle(maxIdleConnections);
    dataSource.setDefaultReadOnly(databaseConfig.isReadOnly());
    dataSource.setAutoCommitOnReturn(databaseConfig.isAutoCommitOnReturnToPool());
    dataSource.setTestOnBorrow(true);
    dataSource.setValidationQueryTimeout(Duration.ofSeconds(databaseConfig.getConnectionValidationTimeoutSeconds()));
    dataSource.setMaxWait(Duration.ofSeconds(databaseConfig.getMaxWaitSeconds()));
    dataSource.setAccessToUnderlyingConnectionAllowed(databaseConfig.isAccessToUnderlyingConnectionAllowed());
    if (databaseConfig.getSessionVariables() != null) {
      dataSource.addConnectionProperty("sessionVariables", databaseConfig.getSessionVariables());
    }
    if (databaseConfig.getOptions() != null) {
      dataSource.addConnectionProperty("options", databaseConfig.getOptions());
    }
    if (databaseConfig.getApplicationName() != null) {
      dataSource.addConnectionProperty("ApplicationName", databaseConfig.getApplicationName());
    }

    log.debug("Created data source for url {} in {} ms.", databaseConfig.getUrl(), System.currentTimeMillis() - start);
    return dataSource;
  }
}
