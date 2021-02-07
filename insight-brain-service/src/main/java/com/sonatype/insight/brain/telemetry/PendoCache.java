/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 */
@Named
@Singleton
public class PendoCache
{
  public static final String HDS_PENDO_JS_PATH = "user-telemetry.js";

  private static final String CUSTOMER_TELEMETRY_KEY = "segment";

  private static final Logger log = LoggerFactory.getLogger(PendoCache.class);

  private final LoadingCache<String, File> jsCache;

  private final LoadingCache<String, CustomerTelemetryProperties> propertiesCache;

  @Inject
  public PendoCache(HdsClient hdsClient) {
    propertiesCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
        .build(new CustomerTelemetryPropertiesCacheLoader(hdsClient));

    jsCache = CacheBuilder.newBuilder().removalListener(n -> FileUtils.deleteQuietly((File) n.getValue()))
        .expireAfterWrite(1, TimeUnit.DAYS).build(new JsCacheLoader(hdsClient));
  }

  public File getJs() {
    try {
      CustomerTelemetryProperties customerTelemetryProperties = getCustomerTelemetryProperties();
      if (customerTelemetryProperties.disabled == null || !customerTelemetryProperties.disabled) {
        return jsCache.get(HDS_PENDO_JS_PATH);
      }
    }
    catch (Exception e) {
      log.debug("Failed to download {}.", HDS_PENDO_JS_PATH, e);
    }
    return null;
  }

  public CustomerTelemetryProperties getCustomerTelemetryProperties() {
    try {
      return propertiesCache.get(CUSTOMER_TELEMETRY_KEY);
    }
    catch (Exception e) {
      log.debug("Failed to retrieve telemetry segment properties.", e);
      return new CustomerTelemetryProperties(false);
    }
  }

  @VisibleForTesting
  public void invalidate() {
    jsCache.invalidateAll();
    propertiesCache.invalidateAll();
  }

  private static class JsCacheLoader extends CacheLoader<String, File>
  {
    private HdsClient hdsClient;

    JsCacheLoader(HdsClient hdsClient) {
      this.hdsClient = hdsClient;
    }

    @Override
    public File load(String key) throws Exception {
      log.debug("Retrieving {} from HDS", key);
      try (InputStream in = hdsClient.get(InputStream.class, key)) {
        File javascript = Files.createTempFile("iq-cache", "js").toFile();
        javascript.deleteOnExit();

        FileUtils.copyToFile(in, javascript);

        return javascript;
      }
    }
  }

  private static class CustomerTelemetryPropertiesCacheLoader extends CacheLoader<String, CustomerTelemetryProperties>
  {
    private HdsClient hdsClient;

    CustomerTelemetryPropertiesCacheLoader(HdsClient hdsClient) {
      this.hdsClient = hdsClient;
    }

    @Override
    public CustomerTelemetryProperties load(String key) throws Exception {
      PendoCache.log.debug("Retrieving customer telemetry properties from HDS");

      return hdsClient.get(CustomerTelemetryProperties.class, TelemetrySender.RESOURCE_PATH);
    }
  }
}
