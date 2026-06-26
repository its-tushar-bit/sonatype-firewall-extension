/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.tenancy.TenantReference;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

@Named
@Singleton
public class DashboardMetricsService
{
  static final String METRIC_SOURCE_INDEX = "index";

  private static final int CACHE_MAXIMUM_SIZE = 128;

  /**
   * Coalescing window ({@link CacheBuilder#expireAfterWrite}) for per-principal metric responses.
   * Entries older than this TTL are refreshed on the next request; concurrent callers within the
   * window share one index load.
   */
  private static final Duration CACHE_TTL = Duration.ofSeconds(5);

  /**
   * Non-printable separator (SOH) between the principal's username and realm in the cache key,
   * so a username that happens to contain the delimiter can't forge another principal's cache
   * entry. A printable {@code :} would collide with {@code user:realm}-style values.
   */
  private static final char CACHE_KEY_DELIMITER = '\u0001';

  private final SearchIndexClient searchIndexClient;

  private final MetricFilterValidator metricFilterValidator;

  private final CurrentUser currentUser;

  private final TenantReference<Cache<String, DashboardMetricsDTO>> caches;

  @Inject
  public DashboardMetricsService(
      SearchIndexClient searchIndexClient,
      MetricFilterValidator metricFilterValidator,
      CurrentUser currentUser)
  {
    this.searchIndexClient = searchIndexClient;
    this.metricFilterValidator = metricFilterValidator;
    this.currentUser = currentUser;
    this.caches = new TenantReference<>(this::createCache);
  }

  public DashboardMetricsDTO getMetrics(DashboardMetricsRequestDTO request) {
    metricFilterValidator.validate(request);
    metricFilterValidator.rejectUnsupportedFilters(request);
    String cacheKey = buildCacheKey();
    try {
      return getCache().get(cacheKey, () -> loadMetrics(request));
    }
    catch (ExecutionException | UncheckedExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      throw new RuntimeException("Failed to load dashboard metrics", cause);
    }
  }

  /**
   * Loads metrics from the search index. During the Hybrid OpenSearch migration, {@code count} and
   * {@code getLastIndexTime} each try primary then secondary independently — when primary is
   * partially available the two values may originate from different backends.
   */
  private DashboardMetricsDTO loadMetrics(DashboardMetricsRequestDTO request) {
    String metricQuery = buildApplicationsMetricQuery();
    long applications = searchIndexClient.count(metricQuery);
    Long lastUpdatedAt = searchIndexClient.getLastIndexTime();

    MetricValueDTO applicationsMetric = new MetricValueDTO(applications, null, METRIC_SOURCE_INDEX);
    return new DashboardMetricsDTO(applicationsMetric, lastUpdatedAt);
  }

  /**
   * Cache key is the principal identity alone. This is correct <em>only</em> while
   * {@link MetricFilterValidator#rejectUnsupportedFilters} rejects every non-empty filter (PR1):
   * with no filters, two requests from the same principal are genuinely equivalent. PR2 MUST
   * encode the filter identity into this key before lifting the filter restriction, otherwise a
   * request scoped to orgA and one scoped to orgB from the same user would share a cache entry
   * (data leakage).
   */
  private String buildCacheKey() {
    return principalCacheIdentity();
  }

  private String principalCacheIdentity() {
    UserPrincipal principal = currentUser.getUserPrincipal();
    if (principal == null) {
      return CurrentUser.ANONYMOUS;
    }
    String realmId = principal.getRealmId();
    return principal.getUsername() + CACHE_KEY_DELIMITER + (realmId != null ? realmId : "");
  }

  /**
   * PR1 walking skeleton returns the RBAC-scoped applications count. RBAC already scopes to the
   * user's readable organization/application hierarchy (descendant-expanded) inside
   * {@link SearchIndexClient#count}. Request-level filters are rejected until hierarchy-inclusive
   * filtering is implemented in PR2.
   */
  private static String buildApplicationsMetricQuery() {
    return "itemType:" + ItemType.APPLICATION.searchFieldName();
  }

  private Cache<String, DashboardMetricsDTO> createCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(CACHE_TTL.toMillis(), TimeUnit.MILLISECONDS)
        .maximumSize(CACHE_MAXIMUM_SIZE)
        .build();
  }

  private Cache<String, DashboardMetricsDTO> getCache() {
    return caches.get();
  }
}
