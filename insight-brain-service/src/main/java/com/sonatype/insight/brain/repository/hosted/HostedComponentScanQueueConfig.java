/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for {@link HostedComponentScanQueueConsumer}.
 * <p>
 * Stored as JSON under {@code SystemConfigurationProperty.HOSTED_SCAN_QUEUE_CONFIG}
 * and live-updated via {@link com.sonatype.insight.brain.api.v2.service.ConfigurationListener}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HostedComponentScanQueueConfig(
    boolean enabled,
    int workerThreadsPerTenant,
    long pollIntervalMilliseconds,
    int maxQueuedRows,
    int maxRetries)
{

  static final int MAX_WORKER_THREADS_PER_TENANT = 20;

  public HostedComponentScanQueueConfig {
    if (workerThreadsPerTenant <= 0 || workerThreadsPerTenant > MAX_WORKER_THREADS_PER_TENANT) {
      throw new IllegalArgumentException(
          "workerThreadsPerTenant must be between 1 and " + MAX_WORKER_THREADS_PER_TENANT
              + ", got: " + workerThreadsPerTenant);
    }
    if (pollIntervalMilliseconds <= 0) {
      throw new IllegalArgumentException(
          "pollIntervalMilliseconds must be positive, got: " + pollIntervalMilliseconds);
    }
    if (maxQueuedRows <= 0) {
      throw new IllegalArgumentException(
          "maxQueuedRows must be positive, got: " + maxQueuedRows);
    }
    if (maxRetries < 0) {
      throw new IllegalArgumentException(
          "maxRetries must be non-negative, got: " + maxRetries);
    }
  }

  public static final boolean DEFAULT_ENABLED = true;

  /**
   * 1 thread per tenant: jobs processed serially within a tenant.
   * Tenants are isolated so they never block each other.
   */
  public static final int DEFAULT_WORKER_THREADS_PER_TENANT = 1;

  public static final long DEFAULT_POLL_INTERVAL_MILLISECONDS = Duration.ofSeconds(30).toMillis();

  public static final int DEFAULT_MAX_QUEUED_ROWS = 10;

  /** Maximum retry attempts before a job is permanently marked FAILED. */
  public static final int DEFAULT_MAX_RETRIES = 3;

  public static HostedComponentScanQueueConfig defaultConfig() {
    return new HostedComponentScanQueueConfig(
        DEFAULT_ENABLED,
        DEFAULT_WORKER_THREADS_PER_TENANT,
        DEFAULT_POLL_INTERVAL_MILLISECONDS,
        DEFAULT_MAX_QUEUED_ROWS,
        DEFAULT_MAX_RETRIES);
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static HostedComponentScanQueueConfig merge(
      final HostedComponentScanQueueConfig base,
      final Map<String, Object> overrides)
  {
    return new HostedComponentScanQueueConfig(
        overrides.containsKey("enabled") && overrides.get("enabled") != null
            ? MAPPER.convertValue(overrides.get("enabled"), Boolean.class)
            : base.enabled(),
        overrides.containsKey("workerThreadsPerTenant") && overrides.get("workerThreadsPerTenant") != null
            ? MAPPER.convertValue(overrides.get("workerThreadsPerTenant"), Integer.class)
            : base.workerThreadsPerTenant(),
        overrides.containsKey("pollIntervalMilliseconds") && overrides.get("pollIntervalMilliseconds") != null
            ? MAPPER.convertValue(overrides.get("pollIntervalMilliseconds"), Long.class)
            : base.pollIntervalMilliseconds(),
        overrides.containsKey("maxQueuedRows") && overrides.get("maxQueuedRows") != null
            ? MAPPER.convertValue(overrides.get("maxQueuedRows"), Integer.class)
            : base.maxQueuedRows(),
        overrides.containsKey("maxRetries") && overrides.get("maxRetries") != null
            ? MAPPER.convertValue(overrides.get("maxRetries"), Integer.class)
            : base.maxRetries());
  }
}
