/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.dataaccess;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import jakarta.annotation.Nullable;

import com.sonatype.insight.brain.db.jooq.EnumAwareRecordUnmapperProvider;
import com.sonatype.insight.brain.db.jooq.JooqSqlCounterListener;

import org.jooq.DSLContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the transaction context for jOOQ-based data access. It can be used to begin and commit transactions. Instances
 * of this class need to be closed.
 *
 * <h3>Connection lifecycle</h3>
 * <p>
 * By default, connections are borrowed from the pool per-query and returned immediately after each statement,
 * matching OpenJPA's on-demand connection behavior before the jOOQ migration. No connection is held at
 * construction time.
 * </p>
 * <p>
 * For write operations, call {@link #begin()} to acquire a connection and start a database transaction.
 * The connection is held until {@link #commit()} is called, at which point it is returned to the pool
 * immediately — not deferred to {@link #close()}. If {@link #close()} is reached without a commit, the
 * open transaction is rolled back and the connection is released.
 * </p>
 *
 * <p>
 * Instances are not thread-safe and must not be shared across threads.
 * </p>
 *
 * @since 2.1.2
 */
public class TransactionContext
    implements AutoCloseable
{
  private static final Logger log = LoggerFactory.getLogger(TransactionContext.class);

  /**
   * Placeholder schema name used in jOOQ codegen. Schema mapping replaces this with the real
   * schema at runtime, which lets jOOQ distinguish real table references (which carry this
   * placeholder and get mapped) from CTEs and other unqualified names (which have no schema
   * and are left alone).
   *
   * <p>
   * Must match the {@code jooq.placeholder.schema} Maven property defined in the root
   * {@code pom.xml} and referenced by the jOOQ codegen {@code <outputSchema>} in
   * {@code insight-brain-db/pom.xml}.
   * </p>
   */
  static final String PLACEHOLDER_SCHEMA = "__placeholder_schema__";

  private static final Settings BASE_SETTINGS = new Settings()
      .withRenderFormatted(false)
      .withRenderSchema(false)
      .withMapJPAAnnotations(true);

  /**
   * Creates jOOQ settings with schema-aware render mapping when a schema is provided.
   *
   * <p>
   * Maps jOOQ's placeholder schema to the given schema name so that all generated table
   * references are rendered as {@code "schema"."table"}. This eliminates the need for
   * {@code SET search_path} round-trips on every connection, saving two SQL calls per statement.
   * </p>
   */
  private static Settings createSettings(@Nullable final String schema) {
    if (schema == null || schema.isEmpty()) {
      return (Settings) BASE_SETTINGS.clone();
    }
    return ((Settings) BASE_SETTINGS.clone())
        .withRenderSchema(true)
        .withRenderMapping(new RenderMapping()
            .withSchemata(new MappedSchema()
                .withInput(PLACEHOLDER_SCHEMA)
                .withOutput(schema)));
  }

  private final DataSource dataSource;

  // Non-null only while a transaction is active; null in per-query mode
  private Connection connection;

  private final DSLContext dslContext;

  private boolean committed = false;

  private List<Runnable> afterCommitHooks;

  /**
   * Create a TransactionContext from a DataSource.
   *
   * <p>
   * No connection is acquired at construction time. jOOQ is configured with the DataSource directly,
   * so connections are borrowed per-statement and returned immediately for read operations.
   * </p>
   *
   * @param dataSource the DataSource to borrow connections from
   * @param dialect the SQL dialect to use
   * @param schema the database schema used to qualify table names via jOOQ render mapping (may be null)
   */
  public TransactionContext(final DataSource dataSource, final SQLDialect dialect, final String schema) {
    this.dataSource = dataSource;
    DefaultConfiguration config = configureJooq(dialect, schema);
    config.set(dataSource);
    this.dslContext = DSL.using(config);
  }

  private static DefaultConfiguration configureJooq(final SQLDialect dialect, @Nullable final String schema) {
    DefaultConfiguration config = new DefaultConfiguration();
    config.set(dialect);
    config.set(createSettings(schema));
    config.set(new EnumAwareRecordUnmapperProvider(config));

    JooqSqlCounterListener sqlCounterListener = JooqSqlCounterListener.getInstance();
    if (sqlCounterListener.isEnabled()) {
      ExecuteListenerProvider listenerProvider = new DefaultExecuteListenerProvider(sqlCounterListener);
      config.set(listenerProvider);
    }

    return config;
  }

  /**
   * Begin a transaction. Acquires a connection from the pool and disables auto-commit. The DSLContext
   * is switched from per-query mode to single-connection mode for the duration of the transaction.
   * Schema routing continues to be handled by jOOQ's render mapping (no {@code SET search_path} needed).
   */
  public void begin() {
    if (connection != null) {
      return;
    }
    try {
      connection = dataSource.getConnection();
      connection.setAutoCommit(false);
      committed = false;

      dslContext.configuration().set(connection);
    }
    catch (SQLException e) {
      releaseConnection();
      throw new RuntimeException("Failed to begin transaction", e);
    }
  }

  /**
   * Commit the transaction and immediately return the connection to the pool.
   *
   * <p>
   * The connection is returned here, before {@link #close()} is called. The DSLContext is switched
   * back to per-query mode so subsequent reads do not hold a connection.
   * </p>
   */
  public void commit() {
    if (connection == null) {
      return;
    }
    try {
      connection.commit();
      committed = true;
    }
    catch (SQLException commitEx) {
      try {
        rollback();
      }
      catch (RuntimeException rollbackEx) {
        commitEx.addSuppressed(rollbackEx);
      }
      throw new RuntimeException("Failed to commit transaction", commitEx);
    }
    finally {
      releaseConnection();
    }
    runAfterCommitHooks();
  }

  /**
   * Register a hook to run after the transaction commits successfully.
   * Hooks execute in registration order after the connection is returned to the pool.
   * If the transaction is rolled back, hooks are not executed.
   * If a hook throws, remaining hooks still execute and exceptions are logged.
   */
  public void afterCommit(Runnable hook) {
    if (afterCommitHooks == null) {
      afterCommitHooks = new ArrayList<>();
    }
    afterCommitHooks.add(hook);
  }

  private void runAfterCommitHooks() {
    if (afterCommitHooks == null) {
      return;
    }
    for (Runnable hook : afterCommitHooks) {
      try {
        hook.run();
      }
      catch (RuntimeException e) {
        log.warn("After-commit hook failed", e);
      }
    }
    afterCommitHooks = null;
  }

  /**
   * Rollback the current transaction.
   */
  public void rollback() {
    if (connection == null) {
      return;
    }
    try {
      connection.rollback();
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to rollback transaction", e);
    }
    finally {
      afterCommitHooks = null;
      releaseConnection();
    }
  }

  /**
   * Check if a transaction is currently active.
   */
  public boolean isActive() {
    return connection != null;
  }

  /**
   * Check if the last transaction was committed successfully.
   */
  public boolean isCommitted() {
    return committed;
  }

  @Override
  public void close() {
    try {
      rollback();
    }
    catch (RuntimeException e) {
      log.warn("Error rolling back transaction on close: {}", e.getMessage());
    }
  }

  private void releaseConnection() {
    if (connection != null) {
      try {
        connection.close();
      }
      catch (SQLException e) {
        log.warn("Error closing connection: {}", e.getMessage());
      }
      connection = null;
      dslContext.configuration().set(dataSource);
    }
  }

  /**
   * Get the jOOQ DSLContext for type-safe SQL queries.
   *
   * @return a DSLContext configured with the appropriate connection source and dialect
   */
  public DSLContext dsl() {
    return dslContext;
  }

  /**
   * Get the underlying JDBC connection held for the current transaction.
   *
   * <p>
   * Returns {@code null} when no transaction is active (per-query mode), since no connection is held
   * between statements.
   * </p>
   *
   * @return the transactional connection, or {@code null} in per-query mode
   */
  @Nullable
  public Connection getConnection() {
    return connection;
  }
}
