/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ResultsTab, SearchEntityType, SearchSource } from 'MainRoot/nosc/search/searchTypes';

/**
 * Shared data-source model for the global-search omnibar and the /search results
 * page. The data source selects which corpus the backend searches:
 *
 *   - `local`   — the tenant's own IQ index: scanned components, vulnerabilities,
 *                 applications, policy violations, and waivers ("My Scan Data").
 *   - `catalog` — the shared Sonatype catalog corpus ("Sonatype Catalog"), which
 *                 only serves Component + Vulnerability. Application / Violation /
 *                 Waiver are IQ-local concepts with no catalog counterpart, so the
 *                 backend never returns them for source=catalog.
 *
 * The single source of truth for which entity types a data source cannot serve
 * lives in UNSERVABLE_TYPES_FOR_DATA_SOURCE, so the omnibar dropdown grouping and
 * the results-page tab list stay in lock-step and can never disagree on what a
 * source is capable of returning.
 */

/** The default data source when the user has not chosen one. */
export const DEFAULT_SEARCH_SOURCE: SearchSource = 'local';

/** User-facing label per data source, shown in the omnibar's source select. */
export const SEARCH_SOURCE_LABEL: Record<SearchSource, string> = {
  local: 'My Scan Data',
  catalog: 'Sonatype Catalog',
};

/**
 * Parse a URL/state `source` param into a SearchSource. Anything other than
 * the explicit catalog value (including missing/unknown) is the local default
 * so bookmarked or mistyped URLs never invent a third corpus.
 */
export function parseSearchSource(raw: unknown): SearchSource {
  return raw === 'catalog' ? 'catalog' : DEFAULT_SEARCH_SOURCE;
}

/**
 * Entity types a given data source cannot serve, and which therefore must be
 * hidden from both the omnibar's suggest groups and the results-page tabs. Named
 * source-neutrally because it drives both surfaces, not tabs alone.
 *
 * `local` serves all five types; `catalog` serves only Component + Vulnerability,
 * so Application / Violation / Waiver are unservable. Kept as an exhaustive record
 * so a new SearchSource forces a compile-time decision here.
 */
export const UNSERVABLE_TYPES_FOR_DATA_SOURCE: Record<SearchSource, readonly SearchEntityType[]> = {
  local: [],
  catalog: ['APPLICATION', 'VIOLATION', 'WAIVER'],
};

/** True when the given entity type cannot be served by the given data source. */
export function isTypeHiddenForSource(type: SearchEntityType, source: SearchSource): boolean {
  return UNSERVABLE_TYPES_FOR_DATA_SOURCE[source].includes(type);
}

/** True when the given entity type is visible (i.e. servable) for the given data source. */
export function isTypeVisibleForSource(type: SearchEntityType, source: SearchSource): boolean {
  return !isTypeHiddenForSource(type, source);
}

/**
 * Every entity type is also a results tab (ResultsTab adds only the synthetic
 * 'ALL'), which is what lets the tab-level record below reuse the entity-level one
 * without a cast. Stated as a constraint so adding a SearchEntityType that is not a
 * ResultsTab fails to compile here rather than silently widening the tab helpers.
 */
type AssertEntityTypesAreTabs<T extends ResultsTab> = T;
export type SearchEntityTypeIsResultsTab = AssertEntityTypesAreTabs<SearchEntityType>;

/**
 * Results-page tabs hidden per data source, derived from the unservable entity
 * types so the tab list and the omnibar groups cannot drift apart. The synthetic
 * 'ALL' tab is never hidden — it is always available as the landing tab.
 *
 * Deliberately the same object as UNSERVABLE_TYPES_FOR_DATA_SOURCE rather than a
 * copy: one runtime value viewed at the tab type, which is what keeps the two
 * surfaces in lock-step. Both are readonly, and the assertion above is what makes
 * the wider type sound.
 */
export const HIDDEN_TABS_FOR_DATA_SOURCE: Record<SearchSource, readonly ResultsTab[]> =
  UNSERVABLE_TYPES_FOR_DATA_SOURCE;

/** True when a results-page tab is hidden for the given source. */
export function isTabHiddenForSource(tab: ResultsTab, source: SearchSource): boolean {
  return HIDDEN_TABS_FOR_DATA_SOURCE[source].includes(tab);
}
