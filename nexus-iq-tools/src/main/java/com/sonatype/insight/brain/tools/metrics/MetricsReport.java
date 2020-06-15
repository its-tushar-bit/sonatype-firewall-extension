/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.metrics;

import java.util.Optional;

import com.sonatype.insight.brain.metrics.CustomMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsReport
{
  private static final Logger log = LoggerFactory.getLogger(MetricsReport.class);

  private final MetricsResult beforeResult;

  private final MetricsResult afterResult;

  public MetricsReport(MetricsResult beforeResult, MetricsResult afterResult) {
    this.beforeResult = beforeResult;
    this.afterResult = afterResult;
  }

  public void printMetrics() {
    Long jpaCalls = calculateDifferenceLong(CustomMetrics.JPA_SQL_CALLS_KEY);
    if (jpaCalls != null) {
      log.info("Number of SQL Calls: {}", jpaCalls);
    }

    Long diskBytesRead = calculateDifferenceLong(CustomMetrics.DISK_BYTES_READ_KEY);
    if (diskBytesRead != null) {
      log.info("Disk Bytes Read: {}", diskBytesRead);
    }
  }

  private Long calculateDifferenceLong(String key) {
    Optional<Long> beforeValue = beforeResult.getGaugeValue(key, Long.class);
    Optional<Long> afterValue = afterResult.getGaugeValue(key, Long.class);
    if (afterValue.isPresent() && beforeValue.isPresent()) {
      return afterValue.get() - beforeValue.get();
    }
    return null;
  }
}
