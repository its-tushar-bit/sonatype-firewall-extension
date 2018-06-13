/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;

public class SqlCallCounterMetrics
{
  private JDBCListener jdbcListener;

  private final AtomicLong statementCount = new AtomicLong();

  private static final SqlCallCounterMetrics instance = new SqlCallCounterMetrics();

  private static final String CUSTOM_METRICS = "customMetrics";

  public static final String SQL_COUNT = "sqlcount";

  public static SqlCallCounterMetrics getInstance() {
    return instance;
  }

  private SqlCallCounterMetrics() {
    String perfMetricsValue = System.getProperty(CUSTOM_METRICS, "");
    if (perfMetricsValue.toLowerCase(Locale.ENGLISH).contains(SQL_COUNT)) {
      jdbcListener = new AbstractJDBCListener()
      {
        @Override
        public void afterExecuteStatement(final JDBCEvent jdbcEvent) {
          statementCount.incrementAndGet();
        }
      };
    }
  }

  public JDBCListener getJDBCListener() {
    return jdbcListener;
  }

  public long getCount() {
    return statementCount.longValue();
  }
}
