/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database.fixture.postgres;

import com.sonatype.insight.brain.database.datasource.DataSourceProvider;
import com.sonatype.insight.brain.database.datasource.PostgresDataSourceProvider;
import com.sonatype.insight.brain.database.fixture.DatabaseFixture;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provisions a Postgres database for testing
 */
public class PostgresDatabaseFixture
    implements DatabaseFixture
{
  private static final Logger log = LoggerFactory.getLogger(PostgresDatabaseFixture.class);

  private final PostgresServer postgresServer;

  public PostgresDatabaseFixture() {
    log.info("Creating new Postgres test database");
    postgresServer = new PostgresServer();
  }

  @Override
  public void close() {
    log.info("Stopping Postgres test database");
    postgresServer.close();
  }

  /**
   * Return an IQ {@link DatabaseConfig} object
   */
  @Override
  public DatabaseConfig getDatabaseConfig() {
    DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName(org.postgresql.Driver.class.getName());
    databaseConfig.setUrl(postgresServer.getJdbcUrl());
    databaseConfig.setUsername(postgresServer.getUsername());
    databaseConfig.setPassword(postgresServer.getPassword());
    databaseConfig.setMaxConnections(50);
    return databaseConfig;
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    return new PostgresDataSourceProvider();
  }
}
