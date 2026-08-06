/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import { computeCountsByType, formatTotalLabel } from 'MainRoot/nosc/searchResults/SearchResultsPage';
import { formatTabCount } from 'MainRoot/nosc/searchResults/searchResultsCounts';

describe('formatTotalLabel', () => {
  it('renders an exact total with grouping separators', () => {
    expect(formatTotalLabel(1234, true)).toBe('1,234');
  });

  it('appends "+" when the total is capped (not exact)', () => {
    // Backend caps totalEstimate at 10,000 and reports isExactTotal=false.
    expect(formatTotalLabel(10000, false)).toBe('10,000+');
  });
});

describe('computeCountsByType', () => {
  it('lets tabCounts win for the tabs it names, over the remembered counts', () => {
    const { counts, nextFallback } = computeCountsByType({ ALL: 12, COMPONENT: 4, VULNERABILITY: 8 }, 12, 'COMPONENT', {
      WAIVER: 99,
    });
    expect(counts.all).toBe(12);
    expect(counts.COMPONENT).toBe(4);
    expect(counts.VULNERABILITY).toBe(8);
    // A tab this response omits keeps the count learned earlier rather than losing
    // its badge: sibling tabs are only probed on page 1 of a query.
    expect(counts.WAIVER).toBe(99);
    // The merged counts become the cache, so the next fetch can overlay onto them.
    expect(nextFallback).toEqual({ all: 12, COMPONENT: 4, VULNERABILITY: 8, WAIVER: 99 });
  });

  it('never invents a 0 badge for a tab no response has counted', () => {
    const { counts } = computeCountsByType({ ALL: 12, COMPONENT: 4 }, 12, 'COMPONENT', {});
    // Absent, not 0 -- the badge is suppressed rather than asserting an empty tab.
    expect(counts.VULNERABILITY).toBeUndefined();
    expect(counts.WAIVER).toBeUndefined();
  });

  it('does not carry stale fallback counts across a query change (empty priorFallback)', () => {
    // On a new query the page resets the fallback cache to {}, so only the active
    // tab's freshly-fetched total is known; other tabs stay absent (no badge).
    const { counts } = computeCountsByType(undefined, 3, 'VULNERABILITY', {});
    expect(counts).toEqual({ VULNERABILITY: 3 });
    expect(counts.COMPONENT).toBeUndefined();
  });

  it('falls back to the active tab total when tabCounts is absent', () => {
    const { counts, nextFallback } = computeCountsByType(undefined, 5, 'COMPONENT', {});
    expect(counts.COMPONENT).toBe(5);
    expect(nextFallback).toEqual({ COMPONENT: 5 });
  });

  it('preserves prior per-tab counts across a tab switch when tabCounts is absent', () => {
    // First fetch on the Components tab.
    const first = computeCountsByType(undefined, 5, 'COMPONENT', {});
    expect(first.counts).toEqual({ COMPONENT: 5 });

    // Switch to the Vulnerabilities tab: its count is learned, and the previously
    // counted Components tab keeps its badge instead of being cleared.
    const second = computeCountsByType(undefined, 8, 'VULNERABILITY', first.nextFallback);
    expect(second.counts).toEqual({ COMPONENT: 5, VULNERABILITY: 8 });

    // Switch back to Components with a new total: it updates, others persist.
    const third = computeCountsByType(undefined, 6, 'COMPONENT', second.nextFallback);
    expect(third.counts).toEqual({ COMPONENT: 6, VULNERABILITY: 8 });
  });

  it('keeps sibling tab badges when paging past page 1 narrows tabCounts to the active tab', () => {
    // Page 1 is probed for every sibling tab, so the backend sends a full tabCounts.
    const page1 = computeCountsByType({ ALL: 30, COMPONENT: 4, VULNERABILITY: 8, WAIVER: 18 }, 4, 'COMPONENT', {});
    expect(page1.counts).toEqual({ all: 30, COMPONENT: 4, VULNERABILITY: 8, WAIVER: 18 });

    // Page 2 skips the sibling probes (they cannot change between pages of the same
    // query) and reports only the active tab. The badges learned on page 1 must
    // survive rather than vanishing from the tab strip on Next.
    const page2 = computeCountsByType({ COMPONENT: 4 }, 4, 'COMPONENT', page1.nextFallback);
    expect(page2.counts).toEqual({ all: 30, COMPONENT: 4, VULNERABILITY: 8, WAIVER: 18 });
  });

  it('lets a fresh tabCounts value overwrite the remembered count for the same tab', () => {
    const first = computeCountsByType({ ALL: 30, COMPONENT: 4, VULNERABILITY: 8 }, 4, 'COMPONENT', {});
    // A re-fetch reporting a different count for a tab must win over the remembered one.
    const second = computeCountsByType({ ALL: 26, COMPONENT: 2 }, 2, 'COMPONENT', first.nextFallback);
    expect(second.counts.all).toBe(26);
    expect(second.counts.COMPONENT).toBe(2);
    expect(second.counts.VULNERABILITY).toBe(8);
  });
});

describe('formatTabCount', () => {
  it('renders an under-cap count with thousands separators', () => {
    expect(formatTabCount(1234)).toBe('1,234');
  });

  it('renders a capped count the same way the hit summary does', () => {
    // The badge and the "N matches" summary describe the same number, so a capped
    // count must not read as an exact 10000 in one place and 10,000+ in the other.
    expect(formatTabCount(10000)).toBe('10,000+');
    expect(formatTabCount(10000)).toBe(formatTotalLabel(10000, false));
  });
});
