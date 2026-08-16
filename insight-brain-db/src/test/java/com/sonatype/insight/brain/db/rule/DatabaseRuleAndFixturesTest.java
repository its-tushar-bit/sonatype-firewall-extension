/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.rule;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2InMemoryTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.db.DatabaseConfig;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DatabaseRuleAndFixturesTest
{
  public DatabaseRule databaseRule = new DatabaseRule();

  @BeforeEach
  public void jupiterInitDatabaseRule(final TestInfo testInfo) {
    databaseRule.beforeFromJupiter(testInfo.getTestClass().orElse(null), testInfo.getTestMethod().orElse(null));
  }

  @AfterEach
  public void jupiterCleanupDatabaseRule() {
    databaseRule.afterFromJupiter();
  }

  @Test
  public void testRegularDatabaseFixture() throws SQLException {
    assertDatabaseConfig("jdbc:h2:mem:sharedInMemoryDatabase;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1", "sa", "");
    assertConnection("jdbc:h2:mem:sharedInMemoryDatabase");
  }

  @Test
  @H2InMemoryTest(customSettings = "DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE")
  public void testRegularDatabaseFixture_withCustomDBSettings() throws SQLException {
    assertDatabaseConfig("jdbc:h2:mem:tempInMemoryDatabase;DATABASE_TO_UPPER=FALSE;LOCK_TIMEOUT=10000;MV_STORE=FALSE",
        "sa", "");
    assertConnection("jdbc:h2:mem:tempInMemoryDatabase");
  }

  @Test
  @H2InMemoryTest(cleanDatabase = true)
  public void testRegularDatabaseFixture_withCleanDatabase() throws SQLException {
    assertDatabaseConfig("jdbc:h2:mem:tempInMemoryDatabase;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1",
        "sa", "");
    assertConnection("jdbc:h2:mem:tempInMemoryDatabase");
  }

  @Test
  @H2DiskTest
  public void testH2Disk() throws SQLException {
    assertDatabaseConfig(
        "jdbc:h2:.*ods;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE",
        "sa", "");
    // Note with H2 it drops the config options used in the URL
    assertConnection("jdbc:h2:.*ods");
    assertDBFilesAreCreated();
  }

  @Test
  @PostgresTest
  public void testPostgres() throws SQLException {
    String postgresUrlPattern = "jdbc:postgresql://localhost:\\d+\\/testpostgres";
    assertDatabaseConfig(postgresUrlPattern, "testuser", "testpass");
    assertConnection(postgresUrlPattern);
  }

  @Test
  @PostgresTest
  public void testPostgresServer_loadSqlDump() throws Exception {
    databaseRule.loadSqlDump(Paths.get(getClass().getResource("/dump-valid.sql").toURI()));

    DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();
    try (Connection connection = dataSource.getConnection()) {
      try (Statement statement = connection.createStatement();
          ResultSet results = statement.executeQuery("SELECT * FROM insight_brain_test.test_table"))
      {
        Assertions.assertThat(results.next()).isTrue();
        Assertions.assertThat(results.getString(1)).isEqualTo("test-value");
        Assertions.assertThat(results.next()).isFalse();
      }
    }
  }

  @Test
  @PostgresTest
  public void testPostgresServer_loadSqlDump_invalidDump() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> databaseRule.loadSqlDump(Paths.get(getClass().getResource("/dump-invalid.sql").toURI())))
        .withStackTraceContaining("Could not load SQL dump");
  }

  @Test
  @PostgresTest
  public void testPostgresServer_dumpSchema() throws Exception {
    Path sqlPath = Paths.get(getClass().getResource("/dump-valid.sql").toURI());

    databaseRule.loadSqlDump(sqlPath);

    String schemaDump = databaseRule.dumpSchema("insight_brain_test");

    Assertions.assertThat(schemaDump).isNotNull();
    Assertions.assertThat(schemaDump)
        .contains("insight_brain_test")
        .contains("insight_brain_test.test_table");
  }

  private void assertDatabaseConfig(
      final String urlPattern,
      final String username,
      final String password)
  {
    DatabaseConfig databaseConfig = databaseRule.getDatabaseConfig(DatabaseName.ods.name());
    assertThat(databaseConfig.getUrl()).containsPattern(urlPattern);
    assertThat(databaseConfig.getUsername()).isEqualTo(username);
    assertThat(databaseConfig.getPassword()).isEqualTo(password);
  }

  private void assertConnection(final String urlPattern) throws SQLException {
    DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();

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

  private void assertDBFilesAreCreated() {
    String databasePath = (String) databaseRule.getMetadata().get(H2DiskTest.DATABASE_PATH);

    assertThat(databasePath).isNotNull();
    assertThat(new File(databasePath, "ods.h2.db")).isFile();
    assertThat(new File(databasePath, "dm.h2.db")).isFile();
    assertThat(new File(databasePath, "third_party_scans.h2.db")).isFile();
    assertThat(new File(databasePath, "aggregation.h2.db")).isFile();
  }
}
