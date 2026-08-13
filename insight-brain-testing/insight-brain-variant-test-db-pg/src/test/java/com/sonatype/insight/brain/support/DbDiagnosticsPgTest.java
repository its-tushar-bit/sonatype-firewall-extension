/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link DbDiagnosticsTest} (CLM-45228).
 *
 * @since 1.27
 */
@PostgresTest
public class DbDiagnosticsPgTest
    extends AbstractDatabaseTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
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
