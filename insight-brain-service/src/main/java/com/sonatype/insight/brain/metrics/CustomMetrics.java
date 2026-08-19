/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.metrics;

import java.util.Locale;

import com.sonatype.insight.brain.db.SqlCallCounterMetrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;

public class CustomMetrics
{
  public static final String DISK_BYTES_READ_KEY = "diskBytesRead";

  public static final String JPA_SQL_CALLS_KEY = "jpaSqlCalls";

  public static void registerMetrics(MetricRegistry registry) {
    String perfMetricsValue = System.getProperty("customMetrics", "").toLowerCase(Locale.ENGLISH);

    if (perfMetricsValue.contains(SqlCallCounterMetrics.SQL_COUNT)) {
      Metric sqlMetric = (Gauge<Long>) () -> SqlCallCounterMetrics.getInstance().getCount();
      registry.register(JPA_SQL_CALLS_KEY, sqlMetric);
    }
    if (perfMetricsValue.contains("diskmetrics")) {
      Metric bytesReadMetric = (Gauge<Long>) OsMetrics::getBytesRead;
      registry.register(DISK_BYTES_READ_KEY, bytesReadMetric);
    }
  }
}
