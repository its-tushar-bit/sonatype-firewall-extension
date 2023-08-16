/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.postgres;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PostgresServerTest
{
  @Test
  public void testPostgresServer() {
    try (PostgresServer postgres = new PostgresServer()) {
      assertThat(postgres.getHostname()).isNotBlank();
      assertThat(postgres.getPort()).isPositive();
      assertThat(postgres.getUsername()).isNotBlank();
      assertThat(postgres.getPassword()).isNotBlank();
      assertThat(postgres.getName()).isNotBlank();
    }
  }

  @Test
  public void testPostgresServer_loadSqlDump() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {

      postgres.loadSqlDump(Paths.get(getClass().getResource("/dump-valid.sql").toURI()));

      try (Connection connection =
               DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM insight_brain_test.test_table")) {
          assertThat(results.next()).isTrue();
          assertThat(results.getString(1)).isEqualTo("test-value");
          assertThat(results.next()).isFalse();
        }
      }
    }
  }

  @Test
  public void testPostgresServer_loadSqlDump_invalidDump() {
    try (PostgresServer postgres = new PostgresServer()) {
      assertThatExceptionOfType(IllegalStateException.class)
          .isThrownBy(() -> postgres.loadSqlDump(Paths.get(getClass().getResource("/dump-invalid.sql").toURI())))
          .withStackTraceContaining("psql returned 3");
    }
  }

  @Test
  public void testPostgresServer_dumpSchema() throws Exception {
    Path sqlPath = Paths.get(getClass().getResource("/dump-valid.sql").toURI());

    try (PostgresServer postgres = new PostgresServer()) {
      postgres.loadSqlDump(sqlPath);

      String schemaDump = postgres.dumpSchema("insight_brain_test");

      assertThat(schemaDump).isNotNull();
      assertThat(schemaDump)
          .contains("insight_brain_test")
          .contains("insight_brain_test.test_table");
    }
  }
}
