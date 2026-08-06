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
   * Indicates whether the catalog source is available. When this returns {@code false} the dispatcher
   * skips the catalog leg entirely and treats the response as catalog-unavailable.
   * <p>
   * Catalog federation is base Nexus One functionality, so implementations MUST NOT gate on a license
   * feature or on tenancy: it is available with any valid IQ license on both single-tenant and MTIQ
   * deployments. The {@code PREVIEW_NEXUS_ONE_UI} feature checked upstream in the resource is the kill-switch for
   * this surface; the default-off {@code CATALOG_FEDERATION} toggle gates the catalog browse endpoint
   * only and MUST NOT be read here.
   */
  boolean isEnabled();
}
