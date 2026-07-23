/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

import java.util.Optional;

import com.sonatype.insight.brain.search.global.ResultRow;
import com.sonatype.insight.brain.search.global.ResultsRequest;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.global.SectionResult;
import com.sonatype.insight.brain.search.global.Tab;

/**
 * SPI for the Global Search results-endpoint catalog leg, serving the catalog-backed entity types
 * ({@link Tab#COMPONENT} and {@link Tab#VULNERABILITY}).
 *
 * <p>
 * The {@code /results} dispatcher consumes this interface so it can be wired and tested in isolation
 * from the concrete implementation (which carries its own dedicated {@link GlobalSearchCatalogHdsClient}
 * connection pool).
 *
 * <p>
 * Implementations MUST tag every row's {@link ResultRow#getSource()} with {@link SearchSource#CATALOG} and
 * translate every failure mode (catalog off, HTTP 5xx, HTTP 429, timeout) into an empty {@link Optional}
 * or a degraded empty {@link SectionResult} so the dispatcher never fails the whole response. Throwing is
 * reserved for programmer errors.
 */
public interface GlobalSearchResultsCatalogClient
{
  /**
   * Executes a catalog results call for the request's tab.
   *
   * @param request caller-supplied parameters (already validated at the controller layer)
   * @return the catalog section, or {@link Optional#empty()} when the catalog source was unavailable for
   *         any reason
   */
  Optional<SectionResult> searchResults(ResultsRequest request);

  /**
   * Indicates whether the catalog source is entitled and feature-enabled. When this returns {@code false}
   * the dispatcher skips the catalog leg entirely and treats the response as catalog-unavailable.
   * Implementations MUST apply the same entitlement gate as the catalog list endpoint (deny multi-tenant,
   * require the licensed catalog search feature).
   */
  boolean isEnabled();
}
