/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.27
 */
public class DbDiagnosticsTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @BeforeClass
  public static void setup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @AfterClass
  public static void cleanup() {
    DataSourceFactory.clear_ForTestsOnly();
  }

  @Test
  public void testGetDBFileInfo() throws Exception {
    // We need an on-disk database for this test.
    final DatabaseConfig databaseConfig = new DatabaseConfig();
    databaseConfig.setDriverClassName("org.h2.Driver");
    databaseConfig.setUrl("jdbc:h2:" + tempDir.getRoot() +
        "/SupportTest/ods;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
    databaseConfig.setUsername("sa");
    databaseConfig.setPassword("");
    OperationalDataStoreProvider.init(databaseConfig, false);

    final String dbDiagnostics = DbDiagnostics.getDBFileInfo();
    assertThat(dbDiagnostics)
        .startsWith("-- Database Diagnostics --\n")
        .contains("Database product name: H2")
        .contains("Database product version: ")
        .contains("Database path: " + tempDir.getRoot().getCanonicalPath())
        .contains("Total database size: ")
        .contains("Schema version: ")
        .contains("Latency Information")
        .contains("Minimum")
        .contains("Maximum")
        .contains("Average");
  }
}
