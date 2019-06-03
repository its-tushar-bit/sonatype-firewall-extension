/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.postgres;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
      }
    }
  }
}
