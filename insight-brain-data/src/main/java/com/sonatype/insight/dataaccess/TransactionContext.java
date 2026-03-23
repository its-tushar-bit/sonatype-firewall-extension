/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-db-common
package com.sonatype.insight.dataaccess;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.sonatype.insight.brain.db.jooq.EnumAwareRecordUnmapperProvider;
import com.sonatype.insight.brain.db.jooq.JooqSqlCounterListener;

import org.jooq.DSLContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.SQLDialect;
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
 * @since 2.1.2
 */
public class TransactionContext
    implements AutoCloseable
{
  private static final Logger log = LoggerFactory.getLogger(TransactionContext.class);

  private static final Settings DEFAULT_SETTINGS = new Settings()
      .withRenderFormatted(false)
      .withRenderSchema(false)
      .withMapJPAAnnotations(true);

  private final Connection connection;

  private final DSLContext dslContext;

  private final boolean connectionOwned;

  private boolean transactionActive = false;

  private boolean committed = false;

  /**
   * Create a TransactionContext from a DataSource.
   *
   * @param dataSource the DataSource to obtain a connection from
   * @param dialect the SQL dialect to use
   * @param schema the database schema (may be null)
   * @throws SQLException if unable to obtain a connection
   */
  public TransactionContext(DataSource dataSource, SQLDialect dialect, String schema) throws SQLException {
    this.connection = dataSource.getConnection();
    this.connectionOwned = true;

    // Set the schema on the connection if provided
    if (schema != null && !schema.isEmpty()) {
      connection.setSchema(schema);
    }

    DefaultConfiguration config = configureJooq(dialect);
    config.set(connection);
    this.dslContext = DSL.using(config);
  }

  /**
   * Create a TransactionContext with an existing connection.
   *
   * @param connection the JDBC connection (not owned, will not be closed)
   * @param dialect the SQL dialect to use
   */
  public TransactionContext(Connection connection, SQLDialect dialect) {
    this.connection = connection;
    this.connectionOwned = false;

    DefaultConfiguration config = configureJooq(dialect);
    config.set(connection);
    this.dslContext = DSL.using(config);
  }

  private static DefaultConfiguration configureJooq(SQLDialect dialect) {
    DefaultConfiguration config = new DefaultConfiguration();
    config.set(dialect);
    config.set(DEFAULT_SETTINGS);
    config.set(new EnumAwareRecordUnmapperProvider(config));

    // Register SQL counter listener if enabled
    JooqSqlCounterListener sqlCounterListener = JooqSqlCounterListener.getInstance();
    if (sqlCounterListener.isEnabled()) {
      ExecuteListenerProvider listenerProvider = new DefaultExecuteListenerProvider(sqlCounterListener);
      config.set(listenerProvider);
    }

    return config;
  }

  /**
   * Begin a transaction. Sets autocommit to false on the connection.
   */
  public void begin() {
    if (transactionActive) {
      return;
    }
    try {
      connection.setAutoCommit(false);
      transactionActive = true;
      committed = false;
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to begin transaction", e);
    }
  }

  /**
   * Commit the transaction.
   */
  public void commit() {
    if (!transactionActive) {
      return;
    }
    try {
      connection.commit();
      transactionActive = false;
      committed = true;
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to commit transaction", e);
    }
  }

  /**
   * Rollback the current transaction.
   */
  public void rollback() {
    if (!transactionActive) {
      return;
    }
    try {
      connection.rollback();
      transactionActive = false;
    }
    catch (SQLException e) {
      throw new RuntimeException("Failed to rollback transaction", e);
    }
  }

  /**
   * Check if a transaction is currently active.
   */
  public boolean isActive() {
    return transactionActive;
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
      // If transaction is still active (not committed), rollback
      if (transactionActive) {
        try {
          connection.rollback();
        }
        catch (SQLException e) {
          log.warn("Error rolling back transaction on close: {}", e.getMessage());
        }
      }
    }
    finally {
      // Only close the connection if we own it
      if (connectionOwned) {
        try {
          connection.close();
        }
        catch (SQLException e) {
          log.warn("Error closing connection: {}", e.getMessage());
        }
      }
    }
  }

  /**
   * Get the jOOQ DSLContext for type-safe SQL queries.
   *
   * @return a DSLContext configured with the underlying connection and appropriate dialect
   */
  public DSLContext dsl() {
    return dslContext;
  }

  /**
   * Get the underlying JDBC connection.
   *
   * @return the JDBC connection
   */
  public Connection getConnection() {
    return connection;
  }
}
