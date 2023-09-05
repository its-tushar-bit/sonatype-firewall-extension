/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import com.sonatype.insight.brain.database.DatabaseRule.H2DiskTest;
import com.sonatype.insight.brain.database.DatabaseRule.PostgresTest;
import com.sonatype.insight.brain.database.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.db.DatabaseConfig;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DatabaseRuleAndFixturesTest
{
  @Rule
  public DatabaseRule databaseRule = new DatabaseRule();

  @Test
  public void testRegularDatabaseFixture() throws SQLException {
    assertDatabaseConfig("jdbc:h2:mem:;DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE", "sa", "");
    // Note with H2 in-memory the URL from the database itself is different
    assertConnection("jdbc:h2:mem:inMemoryDatabase");
  }

  @Test
  @H2DiskTest
  public void testH2Disk() throws SQLException {
    assertDatabaseConfig(
        "jdbc:h2:.*testdb;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE",
        "sa", "");
    // Note with H2 it drops the config options used in the URL
    assertConnection("jdbc:h2:.*testdb");
  }

  @Test
  @PostgresTest
  public void testPostgres() throws SQLException {
    String postgresUrlPattern = "jdbc:postgresql://localhost:\\d\\d\\d\\d\\d\\/testdata";
    assertDatabaseConfig(postgresUrlPattern, "testuser", "testpass");
    assertConnection(postgresUrlPattern);
  }

  private void assertDatabaseConfig(
      final String urlPattern,
      final String username,
      final String password)
  {
    DatabaseConfig databaseConfig = databaseRule.getDatabaseConfig();
    assertThat(databaseConfig.getUrl()).containsPattern(urlPattern);
    assertThat(databaseConfig.getUsername()).isEqualTo(username);
    assertThat(databaseConfig.getPassword()).isEqualTo(password);
  }

  private void assertConnection(final String urlPattern) throws SQLException {
    DatabaseConfig databaseConfig = databaseRule.getDatabaseConfig();
    DataSourceProvider dataSourceProvider = databaseRule.getDataSourceProvider();
    DataSource dataSource = dataSourceProvider.getDataSource(databaseConfig, OperationalDataStore.ID);

    try (Connection connection = dataSource.getConnection()) {
      String databaseURL = connection.getMetaData().getURL();
      assertThat(databaseURL).containsPattern(urlPattern);

      try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT 1")) {
        result.next();
        String str = result.getString(1);
        assertThat(str).isEqualTo("1");
      }
    }
  }
}
