/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.sonatype.insight.brain.search.global.SearchSource;

/**
 * {@code catalogAvailable} is meaningful only for {@code source=catalog}: {@code false} means the
 * catalog source (Guide/HDS) was unavailable and {@code rows} is empty. No fall-through to local rows.
 * Clients MUST branch on {@code catalogAvailable} before reading {@code totalEstimate}/
 * {@code exactTotalEstimate}: on the unavailable path the count is unknown, not exactly zero.
 *
 * <p>
 * On {@code source=local} responses this field is always {@code true} and carries no information
 * about catalog (Guide/HDS) health — a local response never probes the catalog. Clients MUST NOT
 * read {@code catalogAvailable} as a catalog-up signal unless {@code source} is {@code catalog}.
 * (It is a primitive here, so it cannot be nulled out on the local path.)
 *
 * <p>
 * The two sources use different paging models. The catalog source reads {@code page}
 * (offset-from-page) and never returns a {@code nextSearchAfter}; the local source reads the opaque
 * {@code searchAfter} cursor and echoes the next one in {@code nextSearchAfter}. Paging params with
 * no effect for the chosen source are reported in {@code warnings} rather than dropped silently.
 */
@JsonPropertyOrder({
  "entityType", "source", "catalogAvailable", "page", "pageSize", "totalEstimate", "exactTotalEstimate",
  "rows", "facets", "nextSearchAfter", "warnings"})
@JsonInclude(Include.NON_NULL)
public record CatalogResponse(
    String entityType,
    SearchSource source,
    boolean catalogAvailable,
    int page,
    int pageSize,
    long totalEstimate,
    boolean exactTotalEstimate,
    List<CatalogRow> rows,
    Map<String, List<CatalogFacetBucket>> facets,
    String nextSearchAfter,
    List<String> warnings)
{
  public CatalogResponse {
    rows = rows == null ? List.of() : List.copyOf(rows);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  @JsonInclude(Include.NON_NULL)
  public record CatalogFacetBucket(String value, long count)
  {
  }

  public static CatalogResponse catalogUnavailable(
      final CatalogEntityType entityType,
      final int page,
      final int pageSize,
      final List<String> warnings)
  {
    final List<String> merged = new ArrayList<>();
    merged.add(CatalogService.CatalogWarnings.CATALOG_UNAVAILABLE);
    if (warnings != null) {
      merged.addAll(warnings);
    }
    return new CatalogResponse(
        entityType.name(),
        SearchSource.CATALOG,
        false,
        page,
        pageSize,
        0L,
        // Count is unknown when the catalog source is unavailable, not exactly zero.
        false,
        List.of(),
        null,
        null,
        merged);
  }
}
