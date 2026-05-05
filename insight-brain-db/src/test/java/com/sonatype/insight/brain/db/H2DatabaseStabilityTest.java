/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class H2DatabaseStabilityTest
    extends AbstractDatabaseTest
{
  // H2 1.4.197 cannot open a database created by version 1.3.174 twice in a row
  // https://github.com/gitbucket/gitbucket/issues/2279
  // https://github.com/h2database/h2database/issues/1073
  // https://github.com/h2database/h2database/issues/1247
  // https://github.com/infiniteautomation/ma-core-public/issues/1344
  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "H2DatabaseStabilityTest")
  public void testOpenTwice1_3_174Database() throws Exception {
    DatabaseConfig databaseConfig = databaseRule.getDatabaseConfig(DatabaseName.ods.name());
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

  /**
   * Verifies timestamps written by 1.3.174 can be loaded and do not suffer from:
   * https://github.com/h2database/h2database/issues/551
   * https://github.com/h2database/h2database/issues/1284
   */
  @Test
  @H2DiskTest(
      suppressMigrations = true,
      copyExistingDatabase = "H2DatabaseStabilityTest")
  public void testLegacyTimestampFormat() throws Exception {
    DatabaseConfig databaseConfig = getDatabaseConfig("timestamp");
    try (Connection connection = DriverManager.getConnection(databaseConfig.getUrl(), "", "");
        Statement statement = connection.createStatement();
        ResultSet results = statement.executeQuery("SELECT open_time FROM policy_violation"))
    {
      int rows = 0;
      while (results.next()) {
        rows++;
        assertThat(results.getTimestamp(1).toString()).matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:59:59.999");
      }
      assertThat(rows).isEqualTo(24);
    }
  }
}
