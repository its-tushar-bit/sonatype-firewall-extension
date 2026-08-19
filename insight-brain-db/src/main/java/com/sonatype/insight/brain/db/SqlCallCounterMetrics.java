/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.jooq.JooqSqlCounterListener;

/**
 * Facade for SQL call counter metrics.
 *
 * <p>
 * This class provides access to SQL statement execution counts. Previously, this
 * class contained an OpenJPA JDBC listener for tracking SQL calls. That functionality
 * has been migrated to {@link JooqSqlCounterListener} for jOOQ-based data access.
 * </p>
 *
 * <p>
 * This class now delegates to {@link JooqSqlCounterListener} for SQL metrics
 * while maintaining backward compatibility with existing code that references
 * this class.
 * </p>
 *
 * @see JooqSqlCounterListener
 */
public class SqlCallCounterMetrics
{
  private static final SqlCallCounterMetrics instance = new SqlCallCounterMetrics();

  public static final String SQL_COUNT = "sqlcount";

  public static SqlCallCounterMetrics getInstance() {
    return instance;
  }

  private SqlCallCounterMetrics() {
    // Singleton
  }

  /**
   * Get the total count of SQL statements executed.
   *
   * <p>
   * This method now delegates to {@link JooqSqlCounterListener#getTotalCount()}.
   * </p>
   *
   * @return the total SQL statement count
   */
  public long getCount() {
    return JooqSqlCounterListener.getInstance().getTotalCount();
  }

  /**
   * Get the count of SELECT statements executed.
   *
   * @return the SELECT count
   */
  public long getSelectCount() {
    return JooqSqlCounterListener.getInstance().getSelectCount();
  }

  /**
   * Get the count of INSERT statements executed.
   *
   * @return the INSERT count
   */
  public long getInsertCount() {
    return JooqSqlCounterListener.getInstance().getInsertCount();
  }

  /**
   * Get the count of UPDATE statements executed.
   *
   * @return the UPDATE count
   */
  public long getUpdateCount() {
    return JooqSqlCounterListener.getInstance().getUpdateCount();
  }

  /**
   * Get the count of DELETE statements executed.
   *
   * @return the DELETE count
   */
  public long getDeleteCount() {
    return JooqSqlCounterListener.getInstance().getDeleteCount();
  }

  /**
   * Reset all counters.
   */
  public void reset() {
    JooqSqlCounterListener.getInstance().reset();
  }
}
