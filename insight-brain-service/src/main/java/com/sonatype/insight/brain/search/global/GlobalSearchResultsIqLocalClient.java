/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.Optional;

/**
 * SPI for issuing plain-text, builder-constructed queries against the IQ search index for IQ-local
 * tabs.
 *
 * <p>
 * There is intentionally no cross-source fall-through: catalog tabs are served by the catalog leg
 * and degrade to an empty section on failure rather than falling back to this IQ-local index.
 *
 * <p>
 * The {@code /results} endpoint consumes this interface so the dispatcher can be wired and tested
 * in isolation from the concrete IQ-local implementation, which is provided by Spring.
 *
 * <p>
 * Implementations MUST tag every row's {@link ResultRow#getSource()} with {@link SearchSource#LOCAL}, apply
 * the {@code allowedContextIds} permission filter, and cap {@link SectionResult#totalEstimate()} at
 * {@code AbstractSearchIndexClient.GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP}.
 *
 * <p>
 * <b>Permission scoping on ID-list filters.</b> Implementations MUST intersect any caller-supplied ID
 * list filter ({@code applicationPublicIds}, {@code organizationIds}, {@code policyIds},
 * {@code licenseThreatGroupIds}, etc.) with the user's org-permission-filtered readable set and DROP
 * any unpermitted IDs. Do NOT AND caller-supplied ID lists directly into the query.
 */
public interface GlobalSearchResultsIqLocalClient
{
  /**
   * Executes a per-tab IQ-local search for the request's tab against its native IQ-local entity type.
   * Called for IQ-local-only tabs (Applications, Organizations, Policies, PolicyViolations,
   * LegalViolations).
   *
   * @return a populated {@link SectionResult} or {@link SectionResult#empty(Tab)} when there are no matches
   */
  Optional<SectionResult> searchNative(ResultsRequest request);
}
