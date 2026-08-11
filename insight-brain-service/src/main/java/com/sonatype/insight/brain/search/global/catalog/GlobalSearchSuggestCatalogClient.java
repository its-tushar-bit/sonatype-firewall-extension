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
   * Indicates whether the catalog source is available. When this returns {@code false} the service skips
   * the catalog leg entirely and reports {@code catalogAvailable: false}.
   * <p>
   * Catalog federation is base Nexus One functionality, so implementations MUST NOT gate on a license
   * feature or on tenancy: it is available with any valid IQ license on both single-tenant and MTIQ
   * deployments. The {@code PREVIEW_NEXUS_ONE_UI} feature checked upstream in the resource is the sole
   * kill-switch for this surface.
   */
  boolean isEnabled();
}
