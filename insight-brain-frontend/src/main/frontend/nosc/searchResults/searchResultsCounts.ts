/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { RENDERED_ITEM_TYPES, ResultsTab, tabIdForType } from 'MainRoot/nosc/search/searchTypes';

/**
 * Count formatting + per-tab badge derivation for the /search results page.
 * Lives apart from SearchResultsPage so the tab strip can share the formatter
 * without importing the page (which renders the tab strip).
 */

/** The backend caps a total / tab count at this value, exposing it as "10,000+". */
export const TOTAL_COUNT_CAP = 10000;

/**
 * Renders the total-match count for the hit summary. The backend caps
 * totalEstimate at TOTAL_COUNT_CAP, exposing isExactTotal=false when capped; a
 * capped count renders as "10,000+" so it doesn't read as an exact total.
 */
export function formatTotalLabel(total: number, isExactTotal: boolean): string {
  return isExactTotal ? total.toLocaleString() : `${total.toLocaleString()}+`;
}

/**
 * Formats a per-tab badge count, treating a count at the backend cap as capped so
 * the badge and the hit summary read the same way.
 */
export function formatTabCount(count: number): string {
  return formatTotalLabel(count, count < TOTAL_COUNT_CAP);
}

/**
 * Per-tab badge counts. Counts supplied by the backend win for the tabs they
 * name, but they are overlaid on the last-seen counts (priorFallback) rather than
 * replacing them: sibling tabs are only probed on page 1, so page 2+ reports the
 * active tab alone and the badges learned on page 1 must survive paging. When no
 * tabCounts arrive at all, only the active tab's total is known this fetch.
 * Tabs whose count has not been learned are left absent (not 0) so the badge is
 * suppressed rather than asserting a misleading zero.
 * Returns both the counts to render and the next fallback cache to persist.
 */
export function computeCountsByType(
  tabCounts: Partial<Record<ResultsTab, number>> | undefined,
  totalEstimate: number,
  activeTab: string,
  priorFallback: Record<string, number>,
): { counts: Record<string, number>; nextFallback: Record<string, number> } {
  if (tabCounts) {
    const counts: Record<string, number> = { ...priorFallback };
    if (typeof tabCounts.ALL === 'number') counts.all = tabCounts.ALL;
    for (const t of RENDERED_ITEM_TYPES) {
      const c = tabCounts[tabIdForType(t)];
      if (typeof c === 'number') counts[t] = c;
    }
    return { counts, nextFallback: counts };
  }
  const nextFallback = { ...priorFallback, [activeTab]: totalEstimate };
  return { counts: { ...nextFallback }, nextFallback };
}
