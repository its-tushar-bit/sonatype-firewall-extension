/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.jooq;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteType;

/**
 * jOOQ ExecuteListener implementation for tracking SQL call metrics.
 *
 * <p>
 * This listener tracks the count of SQL statements executed through jOOQ,
 * categorized by type (SELECT, INSERT, UPDATE, DELETE, OTHER).
 * </p>
 *
 * <p>
 * The listener is enabled when the system property {@code customMetrics} contains
 * {@code sqlcount}. This maintains consistency with the OpenJPA JDBC listener behavior in
 * {@link com.sonatype.insight.brain.db.SqlCallCounterMetrics}.
 * </p>
 *
 * <p>
 * Usage: Register this listener with jOOQ's Configuration when creating a DSLContext.
 * </p>
 */
public class JooqSqlCounterListener
    implements ExecuteListener
{
  private static final String CUSTOM_METRICS = "customMetrics";

  public static final String SQL_COUNT = "sqlcount";

  private static final JooqSqlCounterListener instance = new JooqSqlCounterListener();

  private final AtomicLong selectCount = new AtomicLong();

  private final AtomicLong insertCount = new AtomicLong();

  private final AtomicLong updateCount = new AtomicLong();

  private final AtomicLong deleteCount = new AtomicLong();

  private final AtomicLong otherCount = new AtomicLong();

  private final boolean enabled;

  private JooqSqlCounterListener() {
    String perfMetricsValue = System.getProperty(CUSTOM_METRICS, "");
    this.enabled = perfMetricsValue.toLowerCase(Locale.ENGLISH).contains(SQL_COUNT);
  }

  /**
   * Get the singleton instance of the JooqSqlCounterListener.
   *
   * @return the singleton instance
   */
  public static JooqSqlCounterListener getInstance() {
    return instance;
  }

  /**
   * Check if SQL counting is enabled.
   *
   * @return true if SQL counting is enabled via the customMetrics system property
   */
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void executeEnd(final ExecuteContext ctx) {
    if (!enabled) {
      return;
    }

    ExecuteType type = ctx.type();
    if (type == null) {
      otherCount.incrementAndGet();
      return;
    }

    switch (type) {
      case READ:
        selectCount.incrementAndGet();
        break;
      case WRITE:
        // For WRITE type, we need to inspect the SQL to determine INSERT/UPDATE/DELETE
        String sql = ctx.sql();
        if (sql != null) {
          String sqlUpper = sql.trim().toUpperCase(Locale.ENGLISH);
          if (sqlUpper.startsWith("INSERT")) {
            insertCount.incrementAndGet();
          }
          else if (sqlUpper.startsWith("UPDATE")) {
            updateCount.incrementAndGet();
          }
          else if (sqlUpper.startsWith("DELETE")) {
            deleteCount.incrementAndGet();
          }
          else {
            otherCount.incrementAndGet();
          }
        }
        else {
          otherCount.incrementAndGet();
        }
        break;
      case DDL:
      case BATCH:
      case ROUTINE:
      case OTHER:
      default:
        otherCount.incrementAndGet();
        break;
    }
  }

  /**
   * Get the count of SELECT statements executed.
   *
   * @return the SELECT count
   */
  public long getSelectCount() {
    return selectCount.longValue();
  }

  /**
   * Get the count of INSERT statements executed.
   *
   * @return the INSERT count
   */
  public long getInsertCount() {
    return insertCount.longValue();
  }

  /**
   * Get the count of UPDATE statements executed.
   *
   * @return the UPDATE count
   */
  public long getUpdateCount() {
    return updateCount.longValue();
  }

  /**
   * Get the count of DELETE statements executed.
   *
   * @return the DELETE count
   */
  public long getDeleteCount() {
    return deleteCount.longValue();
  }

  /**
   * Get the count of other statements executed (DDL, batch, etc.).
   *
   * @return the other count
   */
  public long getOtherCount() {
    return otherCount.longValue();
  }

  /**
   * Get the total count of all SQL statements executed.
   *
   * @return the total count
   */
  public long getTotalCount() {
    return selectCount.longValue() + insertCount.longValue() + updateCount.longValue() +
        deleteCount.longValue() + otherCount.longValue();
  }

  /**
   * Reset all counters to zero.
   *
   * <p>
   * This is useful for testing or for resetting metrics between measurement periods.
   * </p>
   */
  public void reset() {
    selectCount.set(0);
    insertCount.set(0);
    updateCount.set(0);
    deleteCount.set(0);
    otherCount.set(0);
  }
}
