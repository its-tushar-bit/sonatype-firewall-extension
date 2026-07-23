/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.SuggestItemType;

/**
 * SPI for the Global Search suggest-endpoint catalog leg, serving the catalog-backed suggest types
 * ({@link SuggestItemType#COMPONENT} and {@link SuggestItemType#VULNERABILITY}).
 *
 * <p>
 * The suggest service consumes this interface so it can be wired and tested in isolation from the
 * concrete implementation (which carries its own dedicated
 * {@link GlobalSearchCatalogHdsClient} connection pool).
 *
 * <p>
 * Implementations MUST tag every row's source with {@link SearchSource#CATALOG} and translate every
 * failure mode (catalog off, HTTP 5xx, HTTP 429, timeout) into
 * {@link CatalogSuggestResult#unavailable()} so the service never fails the whole response. Throwing
 * is reserved for programmer errors.
 */
public interface GlobalSearchSuggestCatalogClient
{
  /**
   * Executes a catalog suggest call.
   *
   * @param request caller-supplied parameters (already validated at the controller layer)
   * @return the catalog outcome; never {@code null}, never throws for upstream failures
   */
  CatalogSuggestResult suggest(CatalogSuggestRequest request);

  /**
   * Indicates whether the catalog source is entitled and feature-enabled. When this returns
   * {@code false} the service skips the catalog leg entirely and reports {@code catalogAvailable:
   * false}. Implementations MUST apply the same entitlement gate as the catalog list endpoint (deny
   * multi-tenant, require the licensed catalog search feature).
   */
  boolean isEnabled();
}
