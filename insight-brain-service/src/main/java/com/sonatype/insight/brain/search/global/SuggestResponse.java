/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Top-level shape for {@code GET /rest/search/suggest}.
 *
 * <p>
 * The response carries up to 10 rows total: a single optional BEST MATCH plus per-type groups with
 * up to {@code perTypeLimit} rows each. BEST MATCH (when present) counts against the 10-row total.
 * Group order is fixed (see {@link SuggestItemType}).
 *
 * @param bestMatch the single highest-confidence exact match (vulnerability id, application public
 *          id, or component coordinate equal to the query), or {@code null} when nothing exactly
 *          matched. Promoted regardless of which leg produced it.
 * @param groups the per-entity-type sections in fixed presentation order. Never {@code null}.
 *          Sections with no rows are still included with an empty {@code results} list so the UI
 *          can render the section headers consistently.
 * @param catalogAvailable tri-state catalog signal: {@code null} when the catalog was not consulted
 *          for this request (i.e. {@code source=local}), so the field is omitted from the JSON;
 *          {@code true} when {@code source=catalog} was requested and the catalog returned a usable
 *          response; {@code false} when {@code source=catalog} was requested but the catalog was
 *          unentitled or the call timed out / failed. This lets a consumer distinguish "catalog never
 *          consulted" (absent) from "catalog requested but unavailable" ({@code false}); the UI renders
 *          the subtle "catalog unavailable" indicator only on the {@code false} case.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuggestResponse(
    SuggestRow bestMatch,
    List<SuggestGroup> groups,
    Boolean catalogAvailable)
{
  public SuggestResponse {
    groups = groups == null ? List.of() : List.copyOf(groups);
  }
}
