/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresDbModifierTest
{
  @Test
  public void testShiftToDate() throws Exception {
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

        postgres.loadSqlDump(Paths.get(getClass().getResource("/dump.sql").toURI()));

        PostgresDbModifier dbModifier = new PostgresDbModifier(postgres.getUsername(), postgres.getPassword(),
            postgres.getHostname(), postgres.getPort(), postgres.getDatabaseName(), "insight_brain_ods");

        dbModifier.shiftToDate(LocalDate.of(2020, 4, 7));

        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT * FROM insight_brain_ods.test_table")) {
          assertThat(results.next()).isTrue();
          assertThat(results.getDate(1)).isEqualTo(Date.valueOf(LocalDate.of(2020, 4, 7)));
          assertThat(results.next()).isFalse();
        }
      }
    }
  }
}
