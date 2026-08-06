/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * TypeScript types for IQ's global search.
 *
 * Two layers live here:
 *
 *   1. WIRE types — the exact JSON shapes returned by the dedicated
 *      global-search endpoints GET /rest/search/suggest and
 *      GET /rest/search/results. These mirror the Java DTOs in
 *      insight-brain-service/.../search/global/{SuggestResponse,SuggestGroup,
 *      SuggestRow,ResultsResponse,ResultRow}.java. Field names and enum casing
 *      match the wire exactly.
 *
 *   2. A stable internal RENDER model (SearchRow) that the omnibar and the
 *      /search results page consume. Adapter functions map wire rows onto it so
 *      the renderers stay decoupled from the wire shape.
 *
 * The five public entity types (Vulnerability, Component, Application,
 * Violation, Waiver) come straight from the backend SuggestItemType / Tab
 * enums; VIOLATION is a merged surface for policy + legal violations and there
 * is no Organization or Policy group. Organizations and policies remain
 * queryable via filter predicates inside the q= string.
 */

// -----------------------------------------------------------------------------
// Wire types — GET /rest/search/suggest and GET /rest/search/results
// -----------------------------------------------------------------------------

/**
 * The five public entity types surfaced by suggest groups and results tabs.
 * Serialized as the uppercase enum name on the wire (SuggestItemType for suggest
 * rows, Tab.name() for result rows). VIOLATION unions policy + legal violations.
 */
export type SearchEntityType = 'VULNERABILITY' | 'COMPONENT' | 'APPLICATION' | 'VIOLATION' | 'WAIVER';

/** Origin of a row / value of the ?source= query param. Lowercase on the wire. */
export type SearchSource = 'local' | 'catalog';

/** Tabs on the /rest/search/results endpoint. Uppercase on the wire. */
export type ResultsTab = 'ALL' | 'APPLICATION' | 'COMPONENT' | 'VULNERABILITY' | 'VIOLATION' | 'WAIVER';

/** One typeahead row from GET /rest/search/suggest. href is always null/absent on suggest. */
export interface SuggestRow {
  id: string;
  type: SearchEntityType;
  source: SearchSource;
  title: string;
  subtitle?: string | null;
  href?: string | null;
}

/**
 * One per-entity-type section of the suggest response. All rows share the group's
 * type + source. results is never null — an empty list is returned when nothing matched.
 */
export interface SuggestGroup {
  type: SearchEntityType;
  source: SearchSource;
  results: SuggestRow[];
}

/**
 * GET /rest/search/suggest response. bestMatch is the single highest-confidence
 * exact match (or null). groups is in fixed presentation order and always present,
 * including empty groups. catalogAvailable is tri-state: absent (catalog not
 * consulted), true (catalog usable), false (catalog requested but degraded).
 */
export interface SuggestResponse {
  bestMatch: SuggestRow | null;
  groups: SuggestGroup[];
  catalogAvailable?: boolean | null;
}

/**
 * One flat row from GET /rest/search/results. type is the uppercase entity type;
 * fields is an open per-row bag of entity-appropriate properties (license,
 * maxCvss, applicationPublicId, etc.). href may be null.
 */
export interface ResultRow {
  type: SearchEntityType;
  source: SearchSource;
  id: string;
  title: string;
  subtitle?: string | null;
  fields?: Record<string, unknown>;
  href?: string | null;
}

/**
 * One facet bucket. `value` round-trips as the filter value; `displayName` is a
 * human label when it differs from the value (absent otherwise). `count` is a
 * whole-corpus, RBAC-scoped count for the filtered result set.
 */
export interface FacetBucket {
  value: string;
  displayName?: string | null;
  count: number;
}

/**
 * GET /rest/search/results response. totalEstimate is exact below 10000 and
 * literally 10000 above it. tabCounts is emitted by the results endpoint; it stays
 * optional so an older server that omits it still deserializes.
 *
 * `facets` is the per-tab facet map (facet key → ordered buckets), populated only
 * for a single IQ-local entity-tab request made with includeFacets=true; null
 * for the ALL tab, count-only probes, and catalog-source responses.
 */
export interface ResultsResponse {
  tab: ResultsTab;
  page: number;
  pageSize: number;
  totalEstimate: number;
  results: ResultRow[];
  nextSearchAfter?: string | null;
  warnings?: string[];
  catalogAvailable: boolean;
  /** Per-tab badge counts; may be absent when talking to an older server. */
  tabCounts?: Partial<Record<ResultsTab, number>>;
  /** Optional per-tab facet buckets; absent on the ALL tab / catalog source / count-only probes. */
  facets?: Record<string, FacetBucket[]> | null;
}

// -----------------------------------------------------------------------------
// Internal render model — consumed by the omnibar + results page renderers
// -----------------------------------------------------------------------------

/**
 * Stable row shape the omnibar rows and results cards render. Adapter functions
 * below map SuggestRow / ResultRow onto it, so the renderers never touch the wire
 * shape directly. fields carries the open per-row bag from ResultRow (empty on
 * suggest rows, which have no fields).
 */
export interface SearchRow {
  readonly id: string;
  readonly type: SearchEntityType;
  readonly source: SearchSource;
  readonly title: string;
  readonly subtitle: string;
  readonly href: string | null;
  readonly fields: Record<string, unknown>;
}

/** Grouped suggest rows for the omnibar, mapped to the render model. */
export interface SuggestGroupRows {
  readonly type: SearchEntityType;
  readonly source: SearchSource;
  readonly rows: readonly SearchRow[];
}

// -----------------------------------------------------------------------------
// Adapters: wire -> render model
// -----------------------------------------------------------------------------

export function suggestRowToSearchRow(row: SuggestRow): SearchRow {
  return {
    id: row.id,
    type: row.type,
    source: row.source,
    title: row.title,
    subtitle: row.subtitle ?? '',
    href: row.href ?? null,
    fields: {},
  };
}

export function resultRowToSearchRow(row: ResultRow): SearchRow {
  return {
    id: row.id,
    type: row.type,
    source: row.source,
    title: row.title,
    subtitle: row.subtitle ?? '',
    href: row.href ?? null,
    fields: row.fields ?? {},
  };
}

// -----------------------------------------------------------------------------
// Type helpers on the render model
// -----------------------------------------------------------------------------

export function isApplication(r: SearchRow): boolean {
  return r.type === 'APPLICATION';
}

export function isComponent(r: SearchRow): boolean {
  return r.type === 'COMPONENT';
}

export function isVulnerability(r: SearchRow): boolean {
  return r.type === 'VULNERABILITY';
}

export function isViolation(r: SearchRow): boolean {
  return r.type === 'VIOLATION';
}

export function isWaiver(r: SearchRow): boolean {
  return r.type === 'WAIVER';
}

/**
 * The five entity types rendered as rows / tabs, in fixed presentation order
 * (matches the results-page tab order: Applications, Components, Vulnerabilities, Violations,
 * Waivers).
 */
export const RENDERED_ITEM_TYPES: readonly SearchEntityType[] = [
  'APPLICATION',
  'COMPONENT',
  'VULNERABILITY',
  'VIOLATION',
  'WAIVER',
];

/**
 * Narrows an arbitrary tab id / token to a SearchEntityType. Tab ids travel as plain
 * strings (a tab id may also be the All sentinel, and the tab-change callback hands back
 * `tab.id`), so a keyed lookup into a `Record<SearchEntityType, …>` needs the compiler to
 * be shown the key is one of the five rather than being told to assume it with a cast.
 */
export function isSearchEntityType(value: string): value is SearchEntityType {
  return (RENDERED_ITEM_TYPES as readonly string[]).includes(value);
}

/**
 * Human-readable label per entity type. Single source of truth shared by the omnibar section headers
 * (SearchOmnibar) and the /search results-page tabs (SearchResultsTabs) so the two surfaces can never
 * disagree on a label.
 */
export const ITEM_TYPE_LABEL: Record<SearchEntityType, string> = {
  VULNERABILITY: 'Vulnerabilities',
  COMPONENT: 'Components',
  APPLICATION: 'Applications',
  VIOLATION: 'Policy Violations',
  WAIVER: 'Waivers',
};

/**
 * The results tab id (uppercase) for a rendered entity type. Identity today since
 * the render type and the Tab enum share names, but kept as a function so the two
 * can diverge without touching call sites.
 */
export function tabIdForType(type: SearchEntityType): ResultsTab {
  return type;
}

/**
 * Single source-label helper for a vulnerability id, shared by the omnibar row and the /search results page so
 * the two surfaces can never label the same vulnerability differently. Mirrors the prefix logic in
 * VulnerabilityUrlBuilder.java (Source enum: CVE- / GHSA- / SONATYPE-); matching is case-insensitive and
 * unrecognized ids fall back to "Sonatype", matching that builder's default source.
 */
export function vulnerabilitySourceLabel(vulnerabilityId: string | undefined): string {
  const id = (vulnerabilityId ?? '').toUpperCase();
  if (id.startsWith('CVE-')) return 'CVE';
  if (id.startsWith('GHSA-')) return 'GHSA';
  // SONATYPE- needs no branch of its own: it shares the default. A prefix added to
  // VulnerabilityUrlBuilder without a branch here lands on "Sonatype" rather than
  // failing visibly, so a new source needs its own case adding above.
  return 'Sonatype';
}

/** Display name for a result row: the backend title, falling back to the id. */
export function displayNameFor(r: SearchRow): string {
  return r.title || r.id || '';
}

/**
 * Stable React key for a search result. The backend guarantees a unique id per
 * row within a response, scoped by type + source to avoid cross-source collisions.
 */
export function reactKeyFor(r: SearchRow): string {
  return `${r.type}:${r.source}:${r.id}`;
}
