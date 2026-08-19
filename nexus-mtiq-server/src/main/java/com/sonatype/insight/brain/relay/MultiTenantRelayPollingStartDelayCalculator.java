/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.api.admin.service.TenantService;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.springframework.context.annotation.Primary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MTIQ implementation that spreads tenant start times evenly across the polling interval so that
 * hundreds of tenants don't fire their first relay poll at the same instant. Tenant N (after
 * sorting all slugs) starts at offset {@code N * pollIntervalSeconds / numberOfTenants} seconds.
 *
 * <p>
 * Mirrors the {@link com.sonatype.insight.brain.git.MultiTenantDefaultBranchMonitorExecutor}
 * staggering pattern, just at the seconds-scale needed for relay polling.
 */
// @Primary makes this win over DefaultRelayPollingStartDelayCalculator (insight-brain-service)
// when both are on the classpath in the MTIQ runtime. Single-tenant IQ doesn't ship this class
// so the Default impl is the only candidate there.
@Named
@Singleton
@Primary
public class MultiTenantRelayPollingStartDelayCalculator
    implements RelayPollingStartDelayCalculator
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantRelayPollingStartDelayCalculator.class);

  private static final long CACHE_TTL_NANOS = TimeUnit.SECONDS.toNanos(60);

  private final TenantService tenantService;

  // Cold-start cache: hundreds of tenants registering in a tight burst would otherwise issue
  // N tenant-list fetches and sorts. The cache is short-lived; staleness only affects new
  // tenants registering inside the TTL, which fall back to the default initial delay.
  private volatile List<String> cachedSortedTenants;

  private volatile long cacheExpiryNanos;

  @Inject
  public MultiTenantRelayPollingStartDelayCalculator(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @Override
  public int computeInitialDelaySeconds(int pollIntervalSeconds, int defaultInitialDelaySeconds) {
    String tenantSlug = tenantService.getTenantSlug();
    List<String> allTenants = new ArrayList<>(getCachedSortedTenants());
    // Chicken-and-egg: register() runs per-tenant, but TenantService.getAllTenantsNames()
    // may not yet include the tenant currently registering (cache miss, eventual
    // consistency, or the tenant row hasn't propagated yet). Without this defensive
    // merge, every tenant whose slug isn't in the cached snapshot falls through to
    // defaultInitialDelaySeconds and stampedes the relay at +30s on cold boot.
    if (tenantSlug != null && !allTenants.contains(tenantSlug)) {
      allTenants.add(tenantSlug);
      Collections.sort(allTenants);
    }
    return computeOffset(tenantSlug, allTenants, pollIntervalSeconds, defaultInitialDelaySeconds);
  }

  private List<String> getCachedSortedTenants() {
    long now = System.nanoTime();
    List<String> cached = cachedSortedTenants;
    if (cached != null && now < cacheExpiryNanos) {
      return cached;
    }
    synchronized (this) {
      cached = cachedSortedTenants;
      if (cached != null && System.nanoTime() < cacheExpiryNanos) {
        return cached;
      }
      // Defensive: tenantService may transiently return null on cold start before the tenant
      // table is loaded. Treat that as "no tenants yet" so the next call re-queries instead of
      // NPE-ing through register() and silently delaying first poll by defaultInitialDelaySeconds.
      // Also drop any null elements before List.copyOf (which is null-hostile and throws on a
      // null entry); a transient DB row without a schema name should not poison the snapshot.
      Collection<String> names = tenantService.getAllTenantsNames();
      List<String> snapshot = new ArrayList<>();
      if (names != null) {
        for (String name : names) {
          if (name != null) {
            snapshot.add(name);
          }
        }
      }
      Collections.sort(snapshot);
      cachedSortedTenants = List.copyOf(snapshot);
      cacheExpiryNanos = System.nanoTime() + CACHE_TTL_NANOS;
      return cachedSortedTenants;
    }
  }

  @VisibleForTesting
  int computeOffset(
      String tenantSlug,
      List<String> allTenants,
      int pollIntervalSeconds,
      int defaultInitialDelaySeconds)
  {
    // tenantSlug may be null when register() runs without an established tenant context
    // (a transient state on cold start). Guard explicitly to avoid NPE through contains()
    // and to make the "unknown tenant" code path explicit.
    if (tenantSlug == null || allTenants.isEmpty() || !allTenants.contains(tenantSlug)) {
      log.debug("Tenant {} not in tenant list; using default initial delay {}s", tenantSlug,
          defaultInitialDelaySeconds);
      return defaultInitialDelaySeconds;
    }
    // Caller guarantees the list is sorted (the production entry point caches a sorted
    // snapshot). Tests pass a sorted list directly.
    int tenantIndex = allTenants.indexOf(tenantSlug);
    int numberOfTenants = allTenants.size();
    // Multiply before dividing so staggering survives when numberOfTenants >
    // pollIntervalSeconds (e.g. 61 tenants, 60s interval): the previous order
    // truncated (60/61)=0 and collapsed all tenants to the same start instant.
    int offset = (int) ((long) pollIntervalSeconds * tenantIndex / numberOfTenants);
    // Add the default initial delay so we don't kick off polling at second 0 for the first tenant.
    return defaultInitialDelaySeconds + offset;
  }
}
