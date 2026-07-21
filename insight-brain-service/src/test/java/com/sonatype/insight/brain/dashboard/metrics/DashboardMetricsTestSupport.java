/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.google.common.cache.Cache;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.apache.commons.io.FileUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Shared fixtures for F15 dashboard metrics integration tests (CLM-40927).
 * <p>
 * Lucene {@code populateIndex()} rebuilds a process-wide {@code FSDirectory}; concurrent calls from
 * parallel test classes race and leave partial indexes. All metrics tests must rebuild through
 * {@link #populateIndex(SearchIndexClient)}.
 */
public final class DashboardMetricsTestSupport
{
  private static final Object INDEX_POPULATE_LOCK = new Object();

  private DashboardMetricsTestSupport() {
  }

  public static void populateIndex(final SearchIndexClient searchIndexClient) {
    synchronized (INDEX_POPULATE_LOCK) {
      searchIndexClient.populateIndex();
    }
  }

  static void deleteSearchIndexDir(final File searchIndexDir) throws IOException {
    synchronized (INDEX_POPULATE_LOCK) {
      FileUtils.deleteDirectory(searchIndexDir);
    }
  }

  static void runWithoutSearchIndex(final File searchIndexDir, final Runnable action) throws IOException {
    synchronized (INDEX_POPULATE_LOCK) {
      FileUtils.deleteDirectory(searchIndexDir);
      action.run();
    }
  }

  public static void clearDashboardMetricsCache(final DashboardMetricsService dashboardMetricsService) {
    @SuppressWarnings("unchecked")
    TenantReference<Cache<DashboardMetricsCacheKey, DashboardMetricsDTO>> caches =
        (TenantReference<Cache<DashboardMetricsCacheKey, DashboardMetricsDTO>>) ReflectionTestUtils.getField(
            dashboardMetricsService, "caches");
    if (caches == null) {
      return;
    }
    Cache<DashboardMetricsCacheKey, DashboardMetricsDTO> cache = caches.get();
    if (cache != null) {
      cache.invalidateAll();
    }
    caches.remove();
  }

  public static void resetTenantExecutor(final Object bean, final String fieldName) {
    @SuppressWarnings("unchecked")
    TenantReference<ExecutorService> executors =
        (TenantReference<ExecutorService>) ReflectionTestUtils.getField(bean, fieldName);
    if (executors == null) {
      return;
    }
    ExecutorService oldExecutor = executors.remove();
    if (oldExecutor != null) {
      oldExecutor.shutdownNow();
    }
  }

  static String violationComponentHash(String scanId) {
    String hash = "h" + Integer.toHexString(scanId.hashCode());
    return hash.length() <= 20 ? hash : hash.substring(0, 20);
  }
}
