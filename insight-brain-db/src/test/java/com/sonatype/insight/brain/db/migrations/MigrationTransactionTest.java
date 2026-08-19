/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.migrations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import com.sonatype.insight.brain.db.AbstractDatabaseTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.H2DiskTest;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify connection autoCommit behavior for migration scripts.
 *
 * Note: These tests verify H2 connection behavior. The safety argument for
 * CREATE INDEX CONCURRENTLY in PostgreSQL migrations relies on:
 * - PostgreSQL JDBC driver defaults to autoCommit=true
 * - DBCP2's autoCommitOnReturn=true (DatabaseConfig default) resets connections
 * to autoCommit=true when returned to the pool
 * - Spring ResourceDatabasePopulator does not alter connection autoCommit state
 *
 * The H2 test validates the DBCP2 pool behavior and serves as documentation
 * of intent, but does not directly test PostgreSQL JDBC behavior.
 */
public class MigrationTransactionTest
    extends AbstractDatabaseTest
{
  /**
   * Test that when a script fails, previous statements are NOT rolled back.
   * This validates DBCP2 pool autoCommit behavior using H2.
   *
   * Note: This test uses ResourceDatabasePopulator directly, which differs
   * from LegacyDataStoreMigrator.runScript() that pre-executes SET SEARCH_PATH.
   * However, both use the same pool with autoCommitOnReturn=true. The safety
   * argument for PostgreSQL CONCURRENTLY ultimately rests on JDBC defaults.
   */
  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testMigrationScripts_AutoCommit_StatementsPersistOnFailure() throws Exception {
    DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();

    // Create a test table
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement())
    {
      stmt.execute("CREATE TABLE test_autocommit (id INT PRIMARY KEY, value VARCHAR(100))");
    }

    // Create a populator with two statements: one valid INSERT and one invalid statement
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new DefaultResourceLoader().getResource(
        "/MigrationTransactionTest/test_autocommit_statement.sql"));

    // Run the script - it should fail on the second statement
    boolean exceptionThrown = false;
    try (Connection conn = dataSource.getConnection()) {
      populator.populate(conn);
    }
    catch (Exception e) {
      exceptionThrown = true;
      // Expected: second statement in script is invalid SQL
    }

    assertThat(exceptionThrown).isTrue();

    // Check if the first INSERT was committed (autoCommit=true behavior)
    // If autoCommit=false, this would be rolled back and the count would be 0
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_autocommit"))
    {
      rs.next();
      int count = rs.getInt(1);

      // If autoCommit=true, the first INSERT should persist (count = 1)
      // If autoCommit=false (transaction), both would be rolled back (count = 0)
      assertThat(count).isEqualTo(1)
          .as(
              "If count=1, autoCommit=true (CONCURRENTLY would work). If count=0, autoCommit=false (CONCURRENTLY won't work)");
    }

    // Cleanup
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement())
    {
      stmt.execute("DROP TABLE IF EXISTS test_autocommit");
    }
  }

  /**
   * Test that connections from the DBCP2 pool have autoCommit=true by default.
   * This validates the pool configuration (autoCommitOnReturn=true) that also
   * applies to PostgreSQL connections in production.
   */
  @Test
  @H2DiskTest(suppressMigrations = true)
  public void testRunScript_ConnectionAutoCommitState() throws Exception {
    DataSource dataSource = databaseRule.getOperationalDataStore().getDataSource();

    // Check the autoCommit state of connections from the pool
    try (Connection conn = dataSource.getConnection()) {
      boolean autoCommit = conn.getAutoCommit();
      // This should be true by default in DBCP2 with autoCommitOnReturn=true
      assertThat(autoCommit).isTrue();
    }
  }
}
