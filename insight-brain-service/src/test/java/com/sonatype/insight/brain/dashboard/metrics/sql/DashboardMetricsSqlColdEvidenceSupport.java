/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class DashboardMetricsSqlColdEvidenceSupport
{
  private DashboardMetricsSqlColdEvidenceSupport() {
  }

  static long nearestRank(final List<Long> samples, final int percentile) {
    if (samples.isEmpty()) {
      throw new IllegalArgumentException("Samples must not be empty");
    }
    if (percentile < 1 || percentile > 100) {
      throw new IllegalArgumentException("Percentile must be between 1 and 100");
    }
    List<Long> sorted = samples.stream().sorted().toList();
    int rank = (int) Math.ceil(percentile / 100.0d * sorted.size());
    return sorted.get(rank - 1);
  }

  static String thresholdVerdict(final long percentileNanos, final long thresholdMillis) {
    return percentileNanos <= thresholdMillis * 1_000_000L ? "PASS" : "FAIL";
  }

  static <T> T withAdvisoryLock(
      final LockLifecycle lock,
      final CheckedSupplier<T> work) throws Exception
  {
    lock.acquire();
    try {
      return work.get();
    }
    finally {
      lock.release();
    }
  }

  static <T> List<T> runSequentiallyUntil(
      final List<? extends CheckedSupplier<T>> executions,
      final Predicate<T> stopAfter) throws Exception
  {
    List<T> completed = new ArrayList<>();
    for (CheckedSupplier<T> execution : executions) {
      T result = execution.get();
      completed.add(result);
      if (stopAfter.test(result)) {
        break;
      }
    }
    return List.copyOf(completed);
  }

  interface LockLifecycle
  {
    void acquire() throws Exception;

    void release() throws Exception;
  }

  @FunctionalInterface
  interface CheckedSupplier<T>
  {
    T get() throws Exception;
  }
}
