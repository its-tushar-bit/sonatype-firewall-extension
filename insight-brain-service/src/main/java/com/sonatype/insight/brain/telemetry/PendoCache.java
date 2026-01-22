/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 *
 * Caches frontend telemetry files so that they do not need to be re-fetched on every page load. This cache
 * is local to the node as there isn't really a need for it to be perfectly synchronized across nodes, and putting
 * it on the filesystem contributes to performance problems as well as defeating the point of caching, when the FS
 * is a networked FS mount as it would be in a clustered environment.
 */
@Named
@Singleton
public class PendoCache
    implements ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(PendoCache.class);

  private static final String PENDO_JS_FILENAME = "user-telemetry.js";

  private static final String PENDO_CUSTOMER_TELEMETRY_FILENAME = TelemetrySender.RESOURCE_PATH;

  public static final Duration DEFAULT_CACHE_EXPIRATION = Duration.ofDays(1);

  private final Duration cacheExpiration;

  private final HdsClient hdsClient;

  // The JS file is the same for all tenants, so we can cache it globally
  private Supplier<byte[]> jsSupplier;

  // The segment config is tenant-specific, so we need to cache it per tenant
  private TenantReference<Supplier<CustomerTelemetryProperties>> segmentSupplier = new TenantReference<>();

  private final ObjectMapper objectMapper;

  @Inject
  public PendoCache(ObjectMapper objectMapper, HdsClient hdsClient) {
    this(objectMapper, hdsClient, DEFAULT_CACHE_EXPIRATION);
  }

  // visible for testing
  PendoCache(ObjectMapper objectMapper, HdsClient hdsClient, Duration cacheExpiration) {
    this.hdsClient = hdsClient;
    this.objectMapper = objectMapper;
    this.cacheExpiration = cacheExpiration;
    this.jsSupplier = createJsSupplier();
  }

  /**
   * Retrieve the user telemetry JavaScript as a byte[]. This byte[] is cached globally (across tenants) as it is the
   * same for all tenants. The cache expires once per day and the file will be (re-)fetched automatically when needed
   */
  public byte[] getJs() {
    try {
      CustomerTelemetryProperties customerTelemetryProperties = getCustomerTelemetryProperties();
      if (customerTelemetryProperties.disabled == null || !customerTelemetryProperties.disabled) {
        return jsSupplier.get();
      }
    }
    catch (Exception e) {
      log.error("Failed to retrieve {}.", PENDO_JS_FILENAME, e);
    }
    return null;
  }

  /**
   * Retrieve the tenant-specific segment configuration. This byte[] is cached per-tenant.
   * The caches expire once per day and the configuration will be (re-)fetched automatically when needed
   */
  public CustomerTelemetryProperties getCustomerTelemetryProperties() {
    try {
      return segmentSupplier.computeIfAbsent(tenant -> createTenantSegmentSupplier()).get();
    }
    catch (Exception e) {
      log.error("Failed to retrieve telemetry segment properties.", e);
      return new CustomerTelemetryProperties(false);
    }
  }

  /**
   * Invalidate the global JS cache and all per-tenant segment config caches
   */
  public void invalidateAll() {
    jsSupplier = createJsSupplier();
    segmentSupplier = new TenantReference<>();
  }

  /**
   * When a license change is detected, the segment configuration for the tenant who's license is changing is
   * invalidated. The globally-cached JS is not invalidated
   */
  @Override
  public void productLicenseChanged() {
    log.debug("Invalidating cache after update of product license");
    resetSegmentCacheForCurrentTenant();
  }

  private byte[] loadJs() {
    try (var jsInputStream = loadFromHds(PENDO_JS_FILENAME)) {
      return jsInputStream.readAllBytes();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private CustomerTelemetryProperties loadSegmentConfig() {
    try (var customerTelemtryInputStream = loadFromHds(PENDO_CUSTOMER_TELEMETRY_FILENAME)) {
      return objectMapper.readValue(customerTelemtryInputStream, CustomerTelemetryProperties.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private InputStream loadFromHds(String path) {
    return hdsClient.get(InputStream.class, path);
  }

  private void resetSegmentCacheForCurrentTenant() {
    segmentSupplier.set(createTenantSegmentSupplier());
  }

  private Supplier<CustomerTelemetryProperties> createTenantSegmentSupplier() {
    return Suppliers.memoizeWithExpiration(this::loadSegmentConfig, cacheExpiration);
  }

  private Supplier<byte[]> createJsSupplier() {
    return Suppliers.memoizeWithExpiration(this::loadJs, cacheExpiration);
  }
}
