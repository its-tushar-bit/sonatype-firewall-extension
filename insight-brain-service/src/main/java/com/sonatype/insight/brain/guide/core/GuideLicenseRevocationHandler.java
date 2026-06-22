/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.core;

import java.util.EnumSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single-flight + 60s debounce around {@link CLMLicenseManager#loadLicense()}, triggered by
 * a Guide JAX-RS client receiving HTTP 402 from HDS. After a successful refresh, the existing
 * IQ-side filters ({@code SearchLicenseFilter} / {@code McpLicenseFilter}) short-circuit
 * subsequent Guide requests without further HDS round-trips.
 *
 * <p>
 * Concurrent 402s collapse to one refresh. 402s arriving inside a 60-second window after
 * a successful refresh are silently skipped — the deterministic {@code
 * GuideLicenseUnavailableException} is still thrown by the caller.
 */
@Named
@Singleton
public class GuideLicenseRevocationHandler
{
  private static final Logger log = LoggerFactory.getLogger(GuideLicenseRevocationHandler.class);

  private static final long DEBOUNCE_MILLIS = 60_000L;

  private final CLMLicenseManager clmLicenseManager;

  private final ProductLicense productLicense;

  private final AtomicReference<CompletableFuture<Void>> inFlight = new AtomicReference<>();

  private volatile long lastRefreshCompletedAtMillis = 0L;

  @Inject
  public GuideLicenseRevocationHandler(
      CLMLicenseManager clmLicenseManager,
      ProductLicense productLicense)
  {
    this.clmLicenseManager = clmLicenseManager;
    this.productLicense = productLicense;
  }

  /**
   * Called from {@code SearchApiClientImpl} when an HDS call returned HTTP 402. Triggers
   * a license refresh subject to single-flight and debounce. Returns when the refresh has
   * completed (or was skipped); the caller then throws the deterministic
   * {@code GuideLicenseUnavailableException}.
   *
   * @param endpoint relative HDS path that returned 402, used in the audit log
   */
  public void onPaymentRequired(String endpoint) {
    if (System.currentTimeMillis() - lastRefreshCompletedAtMillis < DEBOUNCE_MILLIS) {
      log.debug("license refresh debounced for endpoint {}", endpoint);
      return;
    }

    CompletableFuture<Void> mine = new CompletableFuture<>();
    CompletableFuture<Void> existing = inFlight.compareAndExchange(null, mine);
    if (existing != null) {
      existing.join();
      return;
    }
    try {
      // Re-check the debounce window now that we hold the single-flight slot. A concurrent
      // refresh may have completed (and cleared inFlight in its finally) between our debounce
      // check above and winning the CAS; without this re-check, a thread that sampled the
      // clock before that refresh would issue a redundant loadLicense() inside the window.
      if (System.currentTimeMillis() - lastRefreshCompletedAtMillis < DEBOUNCE_MILLIS) {
        log.debug("license refresh debounced after acquiring single-flight slot for endpoint {}", endpoint);
        mine.complete(null);
        return;
      }
      EnumSet<LicensedFeature> before = snapshotFeatures();
      clmLicenseManager.loadLicense();
      EnumSet<LicensedFeature> after = snapshotFeatures();
      logRefresh(endpoint, before, after);
      lastRefreshCompletedAtMillis = System.currentTimeMillis();
      mine.complete(null);
    }
    catch (Throwable t) {
      mine.completeExceptionally(t);
      throw t;
    }
    finally {
      inFlight.compareAndSet(mine, null);
    }
  }

  private EnumSet<LicensedFeature> snapshotFeatures() {
    EnumSet<LicensedFeature> features = EnumSet.noneOf(LicensedFeature.class);
    for (LicensedFeature feature : LicensedFeature.values()) {
      if (productLicense.hasFeature(feature)) {
        features.add(feature);
      }
    }
    return features;
  }

  private void logRefresh(String endpoint, EnumSet<LicensedFeature> before, EnumSet<LicensedFeature> after) {
    EnumSet<LicensedFeature> removed = before.isEmpty()
        ? EnumSet.noneOf(LicensedFeature.class)
        : EnumSet.copyOf(before);
    removed.removeAll(after);
    EnumSet<LicensedFeature> added = after.isEmpty()
        ? EnumSet.noneOf(LicensedFeature.class)
        : EnumSet.copyOf(after);
    added.removeAll(before);
    log.info(
        "Guide license refreshed in response to HTTP 402 from HDS: "
            + "endpoint={}, removedFeatures={}, addedFeatures={}, licenseFingerprint={}",
        endpoint, featureNames(removed), featureNames(added), productLicense.getFingerprint());
  }

  // LicensedFeature.toString() returns the kebab-case id ("guide-search"); for log analysis
  // we want the deterministic enum constant name ("GUIDE_SEARCH"), so convert explicitly.
  private static TreeSet<String> featureNames(EnumSet<LicensedFeature> features) {
    TreeSet<String> names = new TreeSet<>();
    for (LicensedFeature feature : features) {
      names.add(feature.name());
    }
    return names;
  }

  // Package-private test seam: the only way to advance the debounce clock without sleeping.
  // Not part of the public API; do not call from production code.
  void setLastRefreshCompletedAtMillisForTesting(long millis) {
    this.lastRefreshCompletedAtMillis = millis;
  }
}
