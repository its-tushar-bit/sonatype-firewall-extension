/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import javax.inject.Inject;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.27
 */
public class DbDiagnosticsTest
    extends AbstractComponentTest
{
  @Before
  public void setup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @After
  public void cleanup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Inject
  private OperationalDataStore operationalDataStore;

  @Inject
  private DbDiagnostics dbDiagnostics;

  @Test
  public void testGetDBFileInfo_H2() throws Exception {
    // We need an on-disk database for this test.
    final DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig.setUrl("jdbc:h2:" + tempDir.getRoot() +
        "/SupportTest/ods;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    operationalDataStore.initWithMigration(databaseConfig, false);

    final String dbFileInfo = dbDiagnostics.getDBFileInfo();
    assertThat(dbFileInfo)
        .startsWith("-- Database Diagnostics --\n")
        .contains("Database product name: H2")
        .contains("Database product version: ")
        .contains("Database path: " + tempDir.getRoot().getCanonicalPath())
        .contains("Total database size: ")
        .contains("Schema version: ")
        .contains("Latency Information")
        .contains("Minimum")
        .contains("Maximum")
        .contains("Average")
        .contains("-- Database Settings --\n")
        .contains("DATABASE_TO_UPPER: FALSE");
  }

  // Temporary ignoring flaky test. See CLM-23081
  @Ignore
  @Test
  public void testGetDBFileInfo_Postgres() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      DatabaseConfig databaseConfig = postgres.getDatabaseConfig();
      operationalDataStore.initWithMigration(databaseConfig, false);

      final String dbFileInfo = dbDiagnostics.getDBFileInfo();
      assertThat(dbFileInfo) //
          .startsWith("-- Database Diagnostics --\n") //
          .contains("Database product name: PostgreSQL") //
          .containsPattern("Database product version: [0-9]+") //
          .containsPattern("Schema version: [0-9]+") //
          .contains("Latency Information") //
          .containsPattern("Minimum: [0-9]+ microseconds") //
          .containsPattern("Maximum: [0-9]+ microseconds") //
          .containsPattern("Average: [0-9]+ microseconds") //
          .contains("-- Database Settings --\n") //
          .contains("server_encoding: UTF8");
    }
  }
}
