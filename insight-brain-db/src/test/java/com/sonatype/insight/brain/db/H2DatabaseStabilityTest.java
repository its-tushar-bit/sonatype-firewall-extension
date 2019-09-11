/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Test;

public class H2DatabaseStabilityTest
    extends AbstractDatabaseTest
{
  // H2 1.4.197 cannot open a database created by version 1.3.174 twice in a row 
  // https://github.com/gitbucket/gitbucket/issues/2279
  // https://github.com/h2database/h2database/issues/1073
  // https://github.com/h2database/h2database/issues/1247
  // https://github.com/infiniteautomation/ma-core-public/issues/1344
  @Test
  public void testOpenTwice1_3_174Database() throws Exception {
    File databaseDir = tempDir.newFolder();
    copyDatabase(databaseDir, getClass().getSimpleName());
    DatabaseConfig databaseConfig = getDatabaseConfig(databaseDir, "ods");
    try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), "sa", "")) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("SET SCHEMA insight_brain_ods;");
        statement.executeQuery("SELECT count(*) FROM insight_brain_ods.application");
        statement.execute("SHUTDOWN");
      }
    }
    try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), "sa", "")) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("SET SCHEMA insight_brain_ods;");
        statement.executeQuery("SELECT count(*) FROM insight_brain_ods.application");
      }
    }
  }
}
