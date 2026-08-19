/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Response body for {@code POST /rest/search/index-query}. {@code totalEstimate} is capped at the
 * shared total-hits cap; {@code exactTotalEstimate} is false when capped so the frontend renders "10,000+".
 * {@code facets} is a (possibly empty) map when facets were requested and omitted (null) otherwise.
 * {@code facetsOverPageOnly} is {@code false}: bucket counts are whole-corpus, RBAC-scoped counts over
 * the same active structured filters + item type, so they may be shown as filter-wide totals. The flag
 * is retained for wire compatibility.
 */
@JsonPropertyOrder({
  "entityType", "page", "pageSize", "totalEstimate", "exactTotalEstimate", "rows", "facets",
  "facetsOverPageOnly", "nextSearchAfter", "warnings"})
@JsonInclude(Include.NON_NULL)
public record IndexQueryResponse(
    String entityType,
    int page,
    int pageSize,
    long totalEstimate,
    boolean exactTotalEstimate,
    List<IndexQueryRow> rows,
    Map<String, List<IndexQueryFacetBucket>> facets,
    boolean facetsOverPageOnly,
    String nextSearchAfter,
    List<String> warnings)
{
  public IndexQueryResponse {
    rows = rows == null ? List.of() : List.copyOf(rows);
    warnings = warnings == null ? List.of() : List.copyOf(warnings);
  }

  /**
   * One facet bucket. {@code value} is the filter key the matching filter consumes (the indexed term:
   * a name for app/org facets, the term for everything else), so a bucket value round-trips directly
   * back as a filter value; {@code displayName} is an optional human-readable label when it differs
   * from the value (null otherwise, so simple facets stay a flat value+count pair on the wire).
   */
  @JsonInclude(Include.NON_NULL)
  public record IndexQueryFacetBucket(String value, String displayName, long count)
  {
    /** Value-only bucket (no distinct display name). */
    public IndexQueryFacetBucket(final String value, final long count) {
      this(value, null, count);
    }
  }
}
