/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.fixture.postgres;

import java.nio.file.Path;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.PostgresDataSourceProvider;
import com.sonatype.insight.brain.db.fixture.DatabaseFixture;
import com.sonatype.insight.db.DatabaseConfig;

import org.apache.commons.dbcp2.BasicDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test fixture responsible for provisioning a Postgres database
 */
public class PostgresDatabaseFixture
    implements DatabaseFixture
{
  private static final Logger log = LoggerFactory.getLogger(PostgresDatabaseFixture.class);

  protected final String databaseName;

  private final PostgresTestCluster postgresTestCluster;

  private final DatabaseConfig databaseConfig;

  private PostgresDataSourceProvider dataSourceProvider;

  public PostgresDatabaseFixture(final String testName, final PostgresTest postgresTest) {
    this(testName, postgresTest.suppressMigrations(), postgresTest.maxConnections());
  }

  public PostgresDatabaseFixture(final String testName, final boolean suppressMigrations, final int maxConnections) {
    databaseName = getDatabaseNameFromTestName(testName);

    log.info("Creating new Postgres database fixture with name '{}' for test '{}'", databaseName, testName);

    assertMaxConnectionsIsValid(maxConnections);

    postgresTestCluster = getPostgresTestCluster();

    // If `suppressMigrations` is on then all that is needed is an empty databased. Else clone the template database.
    if (suppressMigrations) {
      postgresTestCluster.createNewDatabase(databaseName);
    }
    else {
      postgresTestCluster.cloneFullyMigratedTemplateDatabase(databaseName);
    }

    databaseConfig = postgresTestCluster.getDatabaseConfig(databaseName);
    databaseConfig.setMaxConnections(maxConnections);
  }

  private String getDatabaseNameFromTestName(final String testName) {
    return testName.toLowerCase()
        // support parameterized tests by replacing brackets with underscores
        .replaceAll("\\[|\\]", "_");
  }

  protected PostgresTestCluster getPostgresTestCluster() {
    return PostgresTestCluster.getInstance();
  }

  private void assertMaxConnectionsIsValid(final int maxConnections) {
    if (maxConnections <= 0) {
      throw new UnsupportedOperationException(
          "Configuration Error: maxConnections configuration should be greater than 0");
    }
  }

  @Override
  public void close() {
    closeDataSourcePool();
    postgresTestCluster.destroyDatabase(databaseName);
  }

  /**
   * Close the DBCP2 connection pool before the database is dropped.
   * This prevents stale connections from surviving in a reused Surefire JVM
   * when the fixture is re-initialized (e.g., switching between H2 and Postgres test classes).
   * Without this, the pool retains connections to the now-deleted database,
   * causing "database does not exist" errors in subsequent tests.
   */
  private void closeDataSourcePool() {
    if (dataSourceProvider == null) {
      return;
    }
    DataSource ds = dataSourceProvider.getDataSource(databaseConfig, "close");
    if (ds instanceof BasicDataSource basicDataSource && !basicDataSource.isClosed()) {
      try {
        basicDataSource.close();
        log.info("Closed connection pool for database '{}'", databaseName);
      }
      catch (Exception e) {
        log.warn("Error closing connection pool for database '{}': {}", databaseName, e.getMessage(), e);
      }
    }
  }

  @Override
  public DatabaseConfig getDatabaseConfig(final String databaseName /* unused */) {
    return databaseConfig;
  }

  @Override
  public DataSourceProvider getDataSourceProvider() {
    if (dataSourceProvider == null) {
      dataSourceProvider = new PostgresDataSourceProvider();
    }
    return dataSourceProvider;
  }

  @Override
  public boolean isFixtureReusable() {
    return true;
  }

  @Override
  public void loadSqlDump(final Path sqlFile) {
    postgresTestCluster.loadSqlDump(databaseName, sqlFile);
  }

  @Override
  public String dumpSchema(final String schema) {
    return postgresTestCluster.dumpSchema(databaseName, schema);
  }
}
