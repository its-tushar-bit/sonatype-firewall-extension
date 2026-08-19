/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;

import com.sonatype.insight.brain.model.security.UserPrincipal;

/**
 * SPI for the Global Search suggest-endpoint IQ-local leg: a typeahead-shaped facade over the IQ
 * search index. Serves APPLICATION, VIOLATION, and WAIVER rows always, and COMPONENT / VULNERABILITY
 * rows under {@code source=local}.
 *
 * <p>
 * Distinct from the full-results IQ-local SPI ({@link GlobalSearchResultsIqLocalClient}) so the two
 * can evolve independently: this one is shaped around the typeahead row contract, whereas the results
 * SPI owns the paginated full-search path. Rows returned by any implementation are pre-tagged
 * {@link SearchSource#LOCAL}.
 *
 * <p>
 * There is intentionally no cross-source fall-through: catalog types are served by the catalog leg
 * under {@code source=catalog} and degrade to an empty section on failure rather than falling back to
 * this IQ-local index.
 *
 * <p>
 * <b>Authorization contract.</b> Implementations MUST filter returned rows to entities the current
 * user is permitted to READ. The controller performs only a coarse read-context gate; per-row
 * filtering is this method's responsibility. The {@code principal} parameter is the request-thread
 * {@link UserPrincipal}, or {@code null} for system-context callers. Implementations that receive a
 * {@code null} principal MUST return an empty list rather than serving unfiltered results.
 */
public interface GlobalSearchSuggestIqLocalClient
{
  /**
   * Fetches up to {@code perTypeLimit} rows for each requested type. Rows are pre-tagged
   * {@link SearchSource#LOCAL}. Returned list is never {@code null} and may be empty.
   *
   * <p>
   * The service typically calls this with a slightly larger limit than the visible per-type cap
   * ({@code perTypeLimit + BEST_MATCH_LOOKAHEAD}), so exact-id matches sitting just outside the
   * visible window can still be promoted to BEST MATCH before per-type capping. Implementations MUST
   * honour {@code perTypeLimit} as the actual fetch size, not silently truncate it back to a smaller
   * visible cap.
   *
   * @param query plain-text, non-blank, length-validated by the caller. Never interpreted as a
   *          Lucene query-string.
   * @param types the public-facing types to look up. Internally, {@link SuggestItemType#COMPONENT}
   *          maps to {@code NON_VULNERABLE_COMPONENT} and {@link SuggestItemType#VULNERABILITY} to
   *          {@code SECURITY_VULNERABILITY}.
   * @param perTypeLimit per-type row cap (&ge; 0). A cap of 0 still validates input but yields no
   *          rows.
   * @param principal request-thread principal used to filter rows down to what the caller is
   *          permitted to READ. May be {@code null} for system-context callers; implementations
   *          receiving {@code null} MUST return an empty list.
   * @return the rows, grouped by type in the requested order. Empty list when nothing matched.
   */
  List<SuggestRow> suggest(String query, List<SuggestItemType> types, int perTypeLimit, UserPrincipal principal);
}
