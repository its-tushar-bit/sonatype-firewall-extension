/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.dataaccess;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.jooq.SQLDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TransactionContext} covering connection pool behavior and connection lifecycle.
 * <p>
 * All tests use a real DBCP2 pool and H2 database, measuring {@link BasicDataSource#getNumActive()}
 * to verify that connections are acquired and released at the right times. Exception path tests
 * use {@link PoolWrappingDataSource} to inject specific JDBC failures while keeping pool tracking accurate.
 * </p>
 */
public class TransactionContextConnectionPoolTest
{
  private static final String SCHEMA = "TEST_SCHEMA";

  private static final String SELECT_TEST_TABLE = "SELECT * FROM " + SCHEMA + ".test_table";

  private BasicDataSource pool;

  @Before
  public void setUp() throws Exception {
    pool = new BasicDataSource();
    pool.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
    pool.setDriverClassName("org.h2.Driver");
    pool.setMaxTotal(5);
    pool.setInitialSize(0);

    try (var conn = pool.getConnection(); var stmt = conn.createStatement()) {
      stmt.execute("CREATE SCHEMA IF NOT EXISTS " + SCHEMA);
      stmt.execute("CREATE TABLE " + SCHEMA + ".test_table (id INT PRIMARY KEY, name VARCHAR(100))");
      stmt.execute("INSERT INTO " + SCHEMA + ".test_table VALUES (1, 'one')");
    }
  }

  @After
  public void tearDown() throws Exception {
    pool.close();
  }

  private TransactionContext newTx() {
    return new TransactionContext(pool, SQLDialect.H2, SCHEMA);
  }

  // --- Pool behavior ---

  @Test
  public void testConstructor_doesNotAcquireConnection() {
    try (TransactionContext tx = newTx()) {
      assertThat(pool.getNumActive()).as("no connection acquired on construction").isZero();
    }
  }

  @Test
  public void testPerQueryMode_returnsConnectionAfterEachQuery() {
    try (TransactionContext tx = newTx()) {
      tx.dsl().fetch(SELECT_TEST_TABLE);
      assertThat(pool.getNumActive()).as("connection returned after first query").isZero();

      tx.dsl().fetch(SELECT_TEST_TABLE);
      assertThat(pool.getNumActive()).as("connection returned after second query").isZero();
    }

    assertThat(pool.getNumActive()).as("no leak after close").isZero();
  }

  @Test
  public void testTransactionMode_holdsConnectionUntilCommit() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      assertThat(pool.getNumActive()).as("connection held after begin").isEqualTo(1);

      tx.dsl().fetch(SELECT_TEST_TABLE);
      assertThat(pool.getNumActive()).as("still held during transaction").isEqualTo(1);

      tx.commit();
      assertThat(pool.getNumActive()).as("returned after commit").isZero();
    }

    assertThat(pool.getNumActive()).as("no leak after close").isZero();
  }

  @Test
  public void testTransactionMode_releasedOnCloseWithoutCommit() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.dsl().fetch(SELECT_TEST_TABLE);
      assertThat(pool.getNumActive()).as("held without commit").isEqualTo(1);
    }

    assertThat(pool.getNumActive()).as("returned after close (rollback on close)").isZero();
  }

  @Test
  public void testPerQueryMode_resumedAfterCommit() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.dsl().fetch(SELECT_TEST_TABLE);
      tx.commit();
      assertThat(pool.getNumActive()).as("returned after commit").isZero();

      tx.dsl().fetch(SELECT_TEST_TABLE);
      assertThat(pool.getNumActive()).as("per-query mode resumed after commit").isZero();
    }

    assertThat(pool.getNumActive()).as("no leak after close").isZero();
  }

  @Test
  public void testMultipleTransactions_noLeak() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.dsl().fetch(SELECT_TEST_TABLE);
      tx.commit();
      assertThat(pool.getNumActive()).as("returned after first commit").isZero();

      tx.begin();
      tx.dsl().fetch(SELECT_TEST_TABLE);
      tx.commit();
      assertThat(pool.getNumActive()).as("returned after second commit").isZero();
    }

    assertThat(pool.getNumActive()).as("no leak after close").isZero();
  }

  @Test
  public void testRollback_releasesConnection() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      assertThat(pool.getNumActive()).isEqualTo(1);

      tx.rollback();
      assertThat(pool.getNumActive()).as("returned after rollback").isZero();
      assertThat(tx.isActive()).isFalse();
    }

    assertThat(pool.getNumActive()).as("no leak after close").isZero();
  }

  @Test
  public void testRollback_withoutBegin_isNoOp() {
    try (TransactionContext tx = newTx()) {
      tx.rollback();
      assertThat(pool.getNumActive()).isZero();
    }
  }

  @Test
  public void testBegin_isIdempotent_noLeak() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.begin();
      assertThat(pool.getNumActive()).as("only one connection held after double begin").isEqualTo(1);
    }

    assertThat(pool.getNumActive()).as("no leak after close").isZero();
  }

  @Test
  public void testCommit_withoutBegin_isNoOp() {
    try (TransactionContext tx = newTx()) {
      tx.commit();
      assertThat(pool.getNumActive()).isZero();
    }
  }

  @Test
  public void testCommit_setsCommittedToTrue() {
    try (TransactionContext tx = newTx()) {
      assertThat(tx.isCommitted()).isFalse();

      tx.begin();
      tx.commit();

      assertThat(tx.isCommitted()).isTrue();
    }
  }

  @Test
  public void testIsCommitted_remainsFalseAfterRollback() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.rollback();

      assertThat(tx.isCommitted()).isFalse();
    }
  }

  @Test
  public void testPerQueryMode_queryFailure_noLeak() {
    try (TransactionContext tx = newTx()) {
      assertThatThrownBy(() -> tx.dsl().fetch("SELECT * FROM nonexistent_table"))
          .isInstanceOf(org.jooq.exception.DataAccessException.class);

      assertThat(pool.getNumActive()).as("connection returned after failed query").isZero();
    }

    assertThat(pool.getNumActive()).as("no leak after close").isZero();
  }

  // --- Schema routing ---

  @Test
  public void testPerQueryMode_setsSchemaOnConnection() {
    try (TransactionContext tx = newTx()) {
      var result = tx.dsl().fetch(SELECT_TEST_TABLE);
      assertThat(result).hasSize(1);
    }
  }

  @Test
  public void testTransactionMode_setsSchemaOnConnection() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      var result = tx.dsl().fetch(SELECT_TEST_TABLE);
      assertThat(result).hasSize(1);
      tx.commit();
    }
  }

  @Test
  public void testNullSchema_doesNotFail() {
    try (TransactionContext tx = new TransactionContext(pool, SQLDialect.H2, null)) {
      tx.dsl().fetch("SELECT 1");
      assertThat(pool.getNumActive()).isZero();

      tx.begin();
      tx.dsl().fetch("SELECT 1");
      tx.commit();
      assertThat(pool.getNumActive()).isZero();
    }
  }

  // --- Exception paths ---

  @Test
  public void testBegin_throwsOnConnectionFailure() {
    DataSource ds = new PoolWrappingDataSource(pool)
    {
      @Override
      public Connection getConnection() throws SQLException {
        throw new SQLException("pool exhausted");
      }
    };
    TransactionContext tx = new TransactionContext(ds, SQLDialect.H2, null);

    assertThatThrownBy(tx::begin)
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to begin transaction")
        .hasCauseInstanceOf(SQLException.class);

    assertThat(pool.getNumActive()).as("no connection borrowed on failure").isZero();
  }

  @Test
  public void testBegin_releasesConnectionWhenSetAutoCommitFails() {
    DataSource ds = new PoolWrappingDataSource(pool)
    {
      @Override
      public Connection getConnection() throws SQLException {
        return new DelegatingConnection<>(pool.getConnection())
        {
          @Override
          public void setAutoCommit(final boolean autoCommit) throws SQLException {
            if (!autoCommit) {
              throw new SQLException("broken connection");
            }
            super.setAutoCommit(autoCommit);
          }
        };
      }
    };
    TransactionContext tx = new TransactionContext(ds, SQLDialect.H2, null);

    assertThatThrownBy(tx::begin).isInstanceOf(RuntimeException.class);

    assertThat(pool.getNumActive()).as("connection returned to pool after begin failure").isZero();
    tx.close();
    assertThat(pool.getNumActive()).as("no double-release on close").isZero();
  }

  @Test
  public void testCommit_rollbacksOnCommitFailure() {
    DataSource ds = new PoolWrappingDataSource(pool)
    {
      @Override
      public Connection getConnection() throws SQLException {
        return new DelegatingConnection<>(pool.getConnection())
        {
          @Override
          public void commit() throws SQLException {
            throw new SQLException("commit failed");
          }
        };
      }
    };
    TransactionContext tx = new TransactionContext(ds, SQLDialect.H2, null);
    tx.begin();

    assertThatThrownBy(tx::commit)
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to commit transaction");

    assertThat(pool.getNumActive()).as("connection returned after commit failure").isZero();
    assertThat(tx.isActive()).isFalse();
  }

  @Test
  public void testClose_noLeak_whenRollbackThrows() {
    DataSource ds = new PoolWrappingDataSource(pool)
    {
      @Override
      public Connection getConnection() throws SQLException {
        return new DelegatingConnection<>(pool.getConnection())
        {
          @Override
          public void rollback() throws SQLException {
            throw new SQLException("rollback failed");
          }
        };
      }
    };
    TransactionContext tx = new TransactionContext(ds, SQLDialect.H2, null);
    tx.begin();

    tx.close(); // must not throw, and must not leak the connection

    assertThat(pool.getNumActive()).as("connection returned to pool despite rollback failure").isZero();
    assertThat(tx.isActive()).isFalse();
  }

  @Test
  public void testRenderMapping_qualifiesTableNamesWithSchema() {
    try (TransactionContext tx = newTx()) {
      // Verify that jOOQ's render mapping qualifies generated table references (which belong to
      // the default/empty schema) with the configured tenant schema, eliminating the need for
      // SET search_path round-trips.
      String sql = tx.dsl()
          .renderInlined(
              tx.dsl().selectFrom(com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION));
      assertThat(sql).containsIgnoringCase(SCHEMA + "\".\"application");
    }
  }

  @Test
  public void testNullSchema_doesNotQualifyTableNames() {
    try (TransactionContext tx = new TransactionContext(pool, SQLDialect.H2, null)) {
      String sql = tx.dsl()
          .renderInlined(
              tx.dsl().selectFrom(com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION));
      // With null schema, renderSchema is false and no mapping is applied
      assertThat(sql.toLowerCase()).doesNotContain(SCHEMA.toLowerCase() + ".");
    }
  }

  @Test
  public void testEmptySchema_doesNotQualifyTableNames() {
    try (TransactionContext tx = new TransactionContext(pool, SQLDialect.H2, "")) {
      String sql = tx.dsl()
          .renderInlined(
              tx.dsl().selectFrom(com.sonatype.insight.brain.jooq.generated.ods.tables.Application.APPLICATION));
      assertThat(sql.toLowerCase()).doesNotContain(SCHEMA.toLowerCase() + ".");
    }
  }

  // --- afterCommit hook tests ---

  @Test
  public void testAfterCommit_hooksRunAfterCommit() {
    var ran = new boolean[]{false};
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.afterCommit(() -> ran[0] = true);
      assertThat(ran[0]).as("hook should not run before commit").isFalse();
      tx.commit();
    }
    assertThat(ran[0]).as("hook should run after commit").isTrue();
  }

  @Test
  public void testAfterCommit_hooksRunInOrder() {
    var order = new java.util.ArrayList<Integer>();
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.afterCommit(() -> order.add(1));
      tx.afterCommit(() -> order.add(2));
      tx.afterCommit(() -> order.add(3));
      tx.commit();
    }
    assertThat(order).containsExactly(1, 2, 3);
  }

  @Test
  public void testAfterCommit_hooksNotRunOnRollback() {
    var ran = new boolean[]{false};
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.afterCommit(() -> ran[0] = true);
      tx.rollback();
    }
    assertThat(ran[0]).as("hook should not run on rollback").isFalse();
  }

  @Test
  public void testAfterCommit_hooksNotRunOnCloseWithoutCommit() {
    var ran = new boolean[]{false};
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.afterCommit(() -> ran[0] = true);
    }
    assertThat(ran[0]).as("hook should not run on implicit rollback").isFalse();
  }

  @Test
  public void testAfterCommit_failingHookDoesNotPreventOthers() {
    var secondRan = new boolean[]{false};
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.afterCommit(() -> {
        throw new RuntimeException("boom");
      });
      tx.afterCommit(() -> secondRan[0] = true);
      tx.commit();
    }
    assertThat(secondRan[0]).as("second hook should run despite first failing").isTrue();
  }

  @Test
  public void testAfterCommit_connectionReleasedBeforeHooksRun() {
    try (TransactionContext tx = newTx()) {
      tx.begin();
      tx.afterCommit(() -> assertThat(pool.getNumActive()).as("connection released before hook").isZero());
      tx.commit();
    }
  }

  /**
   * Minimal DataSource wrapper that delegates all standard methods to a real pool, allowing
   * subclasses to override only {@link #getConnection()} for testing failure scenarios while
   * keeping {@link BasicDataSource#getNumActive()} accurate.
   */
  private abstract static class PoolWrappingDataSource
      implements DataSource
  {
    protected final BasicDataSource delegate;

    PoolWrappingDataSource(final BasicDataSource delegate) {
      this.delegate = delegate;
    }

    @Override
    public Connection getConnection(final String username, final String password) throws SQLException {
      return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
      return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
      delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
      delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
      return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
      return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
      return delegate.isWrapperFor(iface);
    }
  }
}
