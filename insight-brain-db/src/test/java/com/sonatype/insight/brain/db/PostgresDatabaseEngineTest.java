/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.SortedMap;

import com.sonatype.insight.db.PostgresDatabaseEngine;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgresDatabaseEngineTest
{
  @Test
  public void testGetDatabaseSettings() throws Exception {
    try (PostgresServer postgres = new PostgresServer()) {
      SortedMap<String, String> databaseSettings;
      try (Connection connection =
          DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
        databaseSettings = PostgresDatabaseEngine.INSTANCE.getDatabaseSettings(connection);
      }
      assertThat(databaseSettings).containsEntry("server_encoding", "UTF8");
    }
  }
}
