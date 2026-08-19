/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.catalog;

/**
 * Request DTO for {@link GlobalSearchSuggestCatalogClient#suggest(CatalogSuggestRequest)}.
 *
 * <p>
 * Wrapping the user-entered query in a dedicated type keeps the Guide-usage-event telemetry extractor
 * away from the raw query string. {@code GuideUsageIdentifiers.extract} only walks {@code purl()} /
 * {@code id()} accessors and the first non-blank {@code String} argument; a record exposing only
 * {@link #query()} and {@link #limit()} contributes no such accessor and no leaked string argument.
 *
 * @param query the plain-text user query, validated by the caller. Must be non-blank.
 * @param limit per-call upstream row limit (&ge; 1).
 */
public record CatalogSuggestRequest(String query, int limit)
{
  public CatalogSuggestRequest {
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("query must not be blank");
    }
    if (limit < 1) {
      throw new IllegalArgumentException("limit must be >= 1");
    }
  }
}
