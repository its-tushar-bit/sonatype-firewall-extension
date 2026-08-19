/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export type DashboardTabResultsSlice = {
  readonly results: readonly unknown[] | null;
  readonly hasNextPage?: boolean;
};

/**
 * Format a dashboard tab-strip badge from a Classic `dashboard.*`
 * results slice.
 *
 * Semantics (per tab, under the active dashboard filter):
 *   - Violations  → open policy violations
 *   - Components  → components with matching violations
 *   - Applications → applications with policy-risk rows
 *   - Waivers     → waivers / auto-waivers in scope
 *
 * Returns `null` while `results` is still unloaded so the badge stays
 * hidden during the initial fetch (no "0" flash). When the backend
 * signals another page (`hasNextPage`), suffix "+" so "100" reads as
 * "at least 100" — the dashboard APIs are page-based (100 rows) and
 * do not return a grand total.
 *
 * An empty page is always hidden (returns `null`), even if the backend
 * inconsistently sets `hasNextPage: true` on a 0-row page — otherwise the
 * badge would read "0+", which is meaningless.
 */
export function formatDashboardTabBadge(
  slice: DashboardTabResultsSlice | undefined,
): string | null {
  if (!Array.isArray(slice?.results)) return null;
  const n = slice.results.length;
  if (n === 0) return null;
  if (slice.hasNextPage) return `${n}+`;
  return String(n);
}
