/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

/**
 * Processing strategies for entity filtering based on user permissions.
 * <p>
 * Strategy selection guidelines:
 * <ul>
 * <li><strong>SEQUENTIAL</strong> - &lt;10k entities, minimal overhead (default)</li>
 * <li><strong>PARALLEL</strong> - 10k-1M entities, good CPU utilization</li>
 * <li><strong>EXECUTOR_FIXED</strong> - &gt;1M entities, maximum throughput</li>
 * <li><strong>AUTO</strong> - Selects optimal strategy based on dataset size</li>
 * </ul>
 * <p>
 * Configured via {@code AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY} environment variable.
 */
public enum AuthorizationPermissionEntityFilterStrategy
{
  /**
   * Single-threaded processing with 256-entry HashMap cache. Optimal for small datasets (&lt;10k entities) with minimal
   * overhead.
   */
  SEQUENTIAL,

  /**
   * Parallel stream processing with ConcurrentHashMap cache. Optimal for medium datasets (10k-1M entities).
   */
  PARALLEL,

  /**
   * Custom thread pool with batch processing and thread-local caches. Optimal for very large datasets (&gt;1M entities)
   * with maximum throughput.
   */
  EXECUTOR_FIXED,

  /**
   * Automatically selects SEQUENTIAL, PARALLEL, or EXECUTOR_FIXED based on dataset size. Uses thresholds: &lt;10k →
   * SEQUENTIAL, 10k-1M → PARALLEL, &gt;1M → EXECUTOR_FIXED.
   */
  AUTO;

  public static final String AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY_ENV =
      "AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY";

  /**
   * Resolves the entity filter strategy from environment configuration.
   * <p>
   * Reads {@code AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY} environment variable. Returns {@code SEQUENTIAL} if
   * not set or invalid.
   *
   * @return the configured strategy, or SEQUENTIAL if not configured
   */
  public static AuthorizationPermissionEntityFilterStrategy getStrategyFromEnv() {
    try {
      String strategyValue = System.getenv(AUTHORIZATION_PERMISSION_ENTITY_FILTER_STRATEGY_ENV);
      return AuthorizationPermissionEntityFilterStrategy.valueOf(strategyValue.trim().toUpperCase());
    }
    catch (Exception ignore) {
      return SEQUENTIAL;
    }
  }
}
