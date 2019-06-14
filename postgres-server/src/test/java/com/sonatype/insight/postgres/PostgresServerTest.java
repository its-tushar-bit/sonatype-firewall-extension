/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.postgres;

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
  public void testPostgresServer() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      assertThat(postgres.getHostname()).isNotBlank();
      assertThat(postgres.getPort()).isPositive();
      assertThat(postgres.getUsername()).isNotBlank();
      assertThat(postgres.getPassword()).isNotBlank();
      assertThat(postgres.getDatabaseName()).isNotBlank();

      try (Connection connection =
          DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        assertThat(connection.isValid(10)).isTrue();
        assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        assertThat(connection.getMetaData().getDatabaseProductVersion()).isEqualTo("10.7");

        postgres.loadSqlDump(Paths.get(getClass().getResource("/dump-valid.sql").toURI()));
        try (Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery("SELECT * FROM insight_brain_test.test_table")) {
          assertThat(results.next()).isTrue();
          assertThat(results.getString(1)).isEqualTo("test-value");
          assertThat(results.next()).isFalse();
        }

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
          postgres.loadSqlDump(Paths.get(getClass().getResource("/dump-invalid.sql").toURI()));
        }).withStackTraceContaining("psql returned 3");
      }
    }
  }
}
