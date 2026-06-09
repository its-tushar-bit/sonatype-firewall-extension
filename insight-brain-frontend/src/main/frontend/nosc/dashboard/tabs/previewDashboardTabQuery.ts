/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { Dispatch } from 'redux';
import { toggleFilter } from 'MainRoot/dashboard/filter/dashboardFilterActions';

/**
 * Shared hash-query helpers for Preview dashboard tabs (CLM-39992 / S2-PR-D-3..D-4).
 *
 * Tiles deep-link into tabs via `?key=value` on the hash. Each tab reads whitelisted
 * keys on first mount and dispatches the matching Redux filter action. Query params
 * remain in the URL so bookmarks and back/forward preserve the deep-link state.
 */

/** Map severity slug → policy threat level [min, max] tuple matching Classic's slice. */
export const SEVERITY_TO_THREAT_RANGE: Record<string, readonly [number, number]> = {
  critical: [8, 10],
  severe: [4, 7],
  moderate: [2, 3],
  low: [0, 1],
};

/** Parse whitelisted keys from the query portion (after `?`) of a location hash. */
export function parseHashQueryParams<K extends string>(
  hash: string,
  keys: readonly K[],
): Record<K, string | null> {
  const out = Object.fromEntries(keys.map((key) => [key, null])) as Record<K, string | null>;
  const qIdx = hash.indexOf('?');
  if (qIdx < 0) {
    return out;
  }
  const query = hash.slice(qIdx + 1);
  if (!query) {
    return out;
  }
  const params = new URLSearchParams(query);
  for (const key of keys) {
    out[key] = params.get(key);
  }
  return out;
}

export function parseViolationsTabQuery(hash: string) {
  return parseHashQueryParams(hash, ['severity', 'ltg', 'policy'] as const);
}

export function parseComponentsTabQuery(hash: string) {
  return parseHashQueryParams(hash, ['severity', 'policy'] as const);
}

export function parseApplicationsTabQuery(hash: string) {
  return parseHashQueryParams(hash, ['org', 'stage', 'policy'] as const);
}

export function applySeverityFilterFromQuery(dispatch: Dispatch, severity: string | null): void {
  if (severity && SEVERITY_TO_THREAT_RANGE[severity]) {
    const range = SEVERITY_TO_THREAT_RANGE[severity];
    dispatch(toggleFilter('policyThreatLevels', [range[0], range[1]]));
  }
}
