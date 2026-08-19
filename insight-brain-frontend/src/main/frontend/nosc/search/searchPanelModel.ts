/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  RENDERED_ITEM_TYPES,
  ResultsTab,
  SearchEntityType,
  SearchRow,
  SearchSource,
  SuggestGroupRows,
} from 'MainRoot/nosc/search/searchTypes';
import { isTypeVisibleForSource } from 'MainRoot/nosc/search/searchDataSource';

/**
 * Pure state + selection model for the global-search suggest panel. Kept free of
 * React so the panel-state transitions and per-tab row selection are testable
 * without rendering, and so the omnibar component stays presentation-only.
 */

/**
 * Minimum query length before the panel leaves the Recent Searches view. Also
 * gates when useGlobalSearch fires a request, so the two thresholds are one value.
 */
export const MIN_QUERY_LENGTH = 2;

/**
 * Hard cap on result rows rendered for the active tab. Kept deliberately small so
 * the panel stays compact under the nav rather than covering the page; the
 * "View more results" row is the path to the full, unbounded result list.
 */
export const ROWS_VISIBLE_PER_TAB = 5;

/** The tab id shown first, which mixes one row per entity type. */
export const ALL_TAB_ID = 'ALL';

/**
 * Tab labels for the suggest panel's strip. Deliberately separate from the
 * shared ITEM_TYPE_LABEL: the panel's strip has to fit inside a 790px card, so
 * it uses the short "Violations" wording rather than "Policy Violations".
 */
export const PANEL_TAB_LABEL: Record<SearchEntityType, string> = {
  VULNERABILITY: 'Vulnerabilities',
  COMPONENT: 'Components',
  APPLICATION: 'Applications',
  VIOLATION: 'Violations',
  WAIVER: 'Waivers',
};

/**
 * Which view the panel body renders. Derived state — never assigned directly —
 * so the panel can only ever be in one of these observable states.
 */
export type SearchPanelState =
  | 'closed'
  | 'focused-empty'
  | 'focused-short'
  | 'loading'
  | 'loaded'
  | 'loaded-empty';

export interface SearchPanelStateInput {
  readonly panelOpen: boolean;
  /** Query with surrounding whitespace already removed. */
  readonly trimmedQuery: string;
  readonly loading: boolean;
  readonly rowCount: number;
}

/**
 * Single source of truth for the panel view. Order matters: an open panel with a
 * short query stays on Recent Searches regardless of any in-flight request, so a
 * stale fetch can never flip the body to a results view.
 */
export function derivePanelState({
  panelOpen,
  trimmedQuery,
  loading,
  rowCount,
}: SearchPanelStateInput): SearchPanelState {
  if (!panelOpen) return 'closed';
  if (trimmedQuery.length === 0) return 'focused-empty';
  if (trimmedQuery.length < MIN_QUERY_LENGTH) return 'focused-short';
  if (loading) return 'loading';
  if (rowCount === 0) return 'loaded-empty';
  return 'loaded';
}

/** A tab in the suggest panel's horizontal strip. */
export interface SearchPanelTab {
  /** Tab id: ALL_TAB_ID, or the uppercase entity type. */
  readonly id: string;
  readonly label: string;
  /** Total matches for the tab; undefined while counts are unknown. */
  readonly count?: number;
  /** Entity types the tab admits. Undefined on the All tab, which admits every type. */
  readonly entityTypes?: readonly SearchEntityType[];
}

/**
 * Build the tab list for a data source, dropping tabs the source cannot serve
 * (the catalog has no applications / violations / waivers) so the strip never
 * offers a tab that must come back empty.
 */
export function buildPanelTabs(
  source: SearchSource,
  countsByType: Partial<Record<SearchEntityType, number>>,
  allCount: number | undefined
): SearchPanelTab[] {
  const tabs: SearchPanelTab[] = [{ id: ALL_TAB_ID, label: 'All', count: allCount }];
  for (const type of RENDERED_ITEM_TYPES) {
    if (!isTypeVisibleForSource(type, source)) continue;
    tabs.push({
      id: type,
      label: PANEL_TAB_LABEL[type],
      count: countsByType[type],
      entityTypes: [type],
    });
  }
  return tabs;
}

/**
 * Flatten suggest groups into one relevance-ordered row list, promoting the best
 * match to the front and dropping rows the active source cannot serve.
 *
 * The backend returns groups per entity type; the panel's All tab shows a MIXED
 * list rather than per-type sections, so this interleaves by taking rows in group
 * order after the best match. De-duplicates against the best match so a promoted
 * row cannot also appear in its group.
 */
export function flattenSuggestRows(
  bestMatch: SearchRow | null,
  groups: readonly SuggestGroupRows[],
  source: SearchSource
): SearchRow[] {
  const rows: SearchRow[] = [];
  const seen = new Set<string>();

  const keyOf = (row: SearchRow): string => `${row.type}:${row.source}:${row.id}`;

  if (bestMatch && isTypeVisibleForSource(bestMatch.type, source)) {
    rows.push(bestMatch);
    seen.add(keyOf(bestMatch));
  }

  for (const group of groups) {
    if (!isTypeVisibleForSource(group.type, source)) continue;
    for (const row of group.rows) {
      const key = keyOf(row);
      if (seen.has(key)) continue;
      seen.add(key);
      rows.push(row);
    }
  }

  return rows;
}

/**
 * Rows to render for the active tab.
 *
 * The All tab shows a mixed best-match-ranked list: the first row (the promoted
 * best match, when present) leads, then one row per remaining entity type in
 * presentation order, so every type that matched is represented instead of one
 * type dominating. A type tab filters to that type alone.
 *
 * A non-All tab id with no matching tab returns no rows: falling through to the
 * mixed list would render a list contradicting the requested narrowing.
 */
export function selectTabRows(
  rows: readonly SearchRow[],
  activeTabId: string,
  tabs: readonly SearchPanelTab[]
): SearchRow[] {
  const activeTab = tabs.find((tab) => tab.id === activeTabId);
  if (activeTab?.entityTypes) {
    const allowed = new Set(activeTab.entityTypes);
    return rows.filter((row) => allowed.has(row.type)).slice(0, ROWS_VISIBLE_PER_TAB);
  }
  if (activeTabId !== ALL_TAB_ID) return [];

  const mixed: SearchRow[] = [];
  const representedTypes = new Set<SearchEntityType>();
  // The leading row keeps its position regardless of type so the best match is
  // never displaced by presentation order.
  const [leading, ...remaining] = rows;
  if (leading) {
    mixed.push(leading);
    representedTypes.add(leading.type);
  }
  for (const type of RENDERED_ITEM_TYPES) {
    if (mixed.length >= ROWS_VISIBLE_PER_TAB) break;
    if (representedTypes.has(type)) continue;
    const next = remaining.find((row) => row.type === type);
    if (next) {
      mixed.push(next);
      representedTypes.add(type);
    }
  }
  return mixed.slice(0, ROWS_VISIBLE_PER_TAB);
}

/**
 * The results-page tab id for a panel tab id. The All tab maps to the results
 * page's ALL tab; every other panel tab id is already the uppercase entity type.
 */
export function resultsTabForPanelTab(panelTabId: string): ResultsTab {
  return panelTabId === ALL_TAB_ID ? 'ALL' : (panelTabId as ResultsTab);
}

/**
 * Map an `itemType:` token value onto the tab id that renders it. The query
 * grammar's types are finer-grained than the panel's tabs: both violation kinds
 * merge onto VIOLATION, and SECURITY_VULNERABILITY is the Vulnerabilities tab.
 */
function normalizeItemTypeToken(value: string): string {
  if (value === 'POLICY_VIOLATION' || value === 'LEGAL_VIOLATION') return 'VIOLATION';
  if (value === 'SECURITY_VULNERABILITY') return 'VULNERABILITY';
  return value;
}

/**
 * Extract `itemType:VALUE` tokens from a raw query. One or more tokens means the
 * user has already narrowed by type in the query itself, so the tab strip is
 * hidden and a single token selects its tab.
 */
export function itemTypeTokens(query: string): SearchEntityType[] {
  const matches = query.matchAll(/\bitemType:([A-Za-z_]+)/g);
  const tokens: SearchEntityType[] = [];
  for (const match of matches) {
    const value = (match[1] ?? '').toUpperCase();
    const normalized = normalizeItemTypeToken(value);
    if ((RENDERED_ITEM_TYPES as readonly string[]).includes(normalized)) {
      tokens.push(normalized as SearchEntityType);
    }
  }
  return tokens;
}
