/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.27
 */
@Category(SlowTest.class)
public class DbDiagnosticsTest
    extends AbstractDatabaseTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  @H2DiskTest
  public void testGetDBFileInfo_H2() throws Exception {
    DbDiagnostics dbDiagnostics = new DbDiagnostics(databaseRule.getOperationalDataStore());
    String result = dbDiagnostics.getDBFileInfo();
    String expectedPath = (String) databaseRule.getMetadata().get(H2DiskTest.DATABASE_PATH);
    // getCanonicalPath() resolves symlinks, so on macOS /var becomes /private/var
    // We need to check for the canonical path to handle this variation
    String canonicalExpectedPath = new java.io.File(expectedPath).getCanonicalPath();
    assertThat(result)
        .startsWith("-- Database Diagnostics --\n")
        .contains("Database product name: H2")
        .contains("Database product version: ")
        .contains("Database path: " + canonicalExpectedPath)
        .contains("Total database size: ")
        .contains("Schema version: ")
        .contains("Latency Information")
        .contains("Minimum")
        .contains("Maximum")
        .contains("Average")
        .contains("-- Database Settings --\n")
        .contains("DATABASE_TO_UPPER: FALSE");
  }

  @Test
  @PostgresTest
  public void testGetDBFileInfo_Postgres() throws Exception {
    DbDiagnostics dbDiagnostics = new DbDiagnostics(databaseRule.getOperationalDataStore());
    String result = dbDiagnostics.getDBFileInfo();
    assertThat(result) //
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
