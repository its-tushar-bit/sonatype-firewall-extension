/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock.LockType;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicenseListener;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.CustomerTelemetryProperties;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.50
 */
@Named
@Singleton
public class PendoCache
    implements ProductLicenseListener
{
  private static final Logger log = LoggerFactory.getLogger(PendoCache.class);

  // Visible for testing
  public static final String PENDO_JS_FILENAME = "user-telemetry.js";

  // Visible for testing
  public static final String PENDO_CUSTOMER_TELEMETRY_FILENAME = "segment";

  private static final Map<String, String> FILENAME_TO_HDS_PATH = ImmutableMap.of(
      PENDO_JS_FILENAME, PENDO_JS_FILENAME,
      PENDO_CUSTOMER_TELEMETRY_FILENAME, TelemetrySender.RESOURCE_PATH
  );

  private final HdsClient hdsClient;

  private final InsightWork insightWork;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public PendoCache(HdsClient hdsClient, InsightWork insightWork, final ClusterLockManager clusterLockManager) {
    this.hdsClient = hdsClient;
    this.insightWork = insightWork;
    this.clusterLockManager = clusterLockManager;
  }

  // Visible for testing
  byte[] loadFile(String filename) throws IOException {
    try (ClusterLock clusterLock = clusterLockManager.createForFilename(filename)) {
      clusterLock.lock(LockType.SHARED);
      return doLoadFile(filename);
    }
  }

  // Visible for testing
  byte[] doLoadFile(String filename) throws IOException {
    File file = new File(insightWork.getCacheDir(), filename);
    if (fileNeedsUpdating(file)) {
      try (InputStream in = hdsClient.get(InputStream.class, PendoCache.FILENAME_TO_HDS_PATH.get(file.getName()))) {
        FileUtils.copyToFile(in, file);
        log.debug("Updated {}.", file.getName());
      }
    }
    log.debug("Loaded {}.", file.getName());
    return Files.readAllBytes(file.toPath());
  }

  private boolean fileNeedsUpdating(File file) {
    return !file.exists() || getCurrentTimeMillis() - getLastModifiedTime(file) >= Duration.ofDays(1).toMillis();
  }

  // Visible for testing
  long getCurrentTimeMillis() {
    return System.currentTimeMillis();
  }

  // Visible for testing
  long getLastModifiedTime(File file) {
    return file.lastModified();
  }

  public byte[] getJs() {
    try {
      CustomerTelemetryProperties customerTelemetryProperties = getCustomerTelemetryProperties();
      if (customerTelemetryProperties.disabled == null || !customerTelemetryProperties.disabled) {
        return loadFile(PENDO_JS_FILENAME);
      }
    }
    catch (Exception e) {
      log.debug("Failed to retrieve {}.", PENDO_JS_FILENAME, e);
    }
    return null;
  }

  public CustomerTelemetryProperties getCustomerTelemetryProperties() {
    try {
      return JsonUtils.parse(loadFile(PENDO_CUSTOMER_TELEMETRY_FILENAME), CustomerTelemetryProperties.class);
    }
    catch (Exception e) {
      log.debug("Failed to retrieve telemetry segment properties.", e);
      return new CustomerTelemetryProperties(false);
    }
  }

  @Override
  public void productLicenseChanged() {
    log.debug("Invalidating cache after update of product license");
    invalidate();
  }

  // Visible for testing
  public void invalidate() {
    for (String filename : FILENAME_TO_HDS_PATH.keySet()) {
      deleteFileIfExists(filename);
    }
  }

  // Visible for testing
  void deleteFileIfExists(String filename) {
    File file = new File(insightWork.getCacheDir(), filename);
    if (file.exists()) {
      try (ClusterLock clusterLock = clusterLockManager.createForFilename(filename)) {
        clusterLock.lock();
        doDeleteFile(file);
      }
    }
  }

  // Visible for testing
  void doDeleteFile(File file) {
    FileUtils.deleteQuietly(file);
    log.debug("Deleted {}.", file.getName());
  }
}
