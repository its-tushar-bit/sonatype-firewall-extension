/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, API_PREFIX } from './apiFetch';
import { makeKeyedTtlCache } from './ttlCache';
import type { ReadonlySearchParams } from '@guide/ui-core/adapters';
import type { SearchResponse } from '@guide/ui-core/types';

export async function searchAll(searchParams: ReadonlySearchParams): Promise<SearchResponse> {
  return apiFetch<SearchResponse>(`${API_PREFIX}/global/search?${searchParams.toString()}`);
}

const TOTALS_TTL_MS = 10 * 60 * 1000;

/**
 * Fetches cross-tab totals from the global search endpoint with `limit=1`.
 * Used by the search page's components/vulnerabilities tabs to populate badge
 * counts for the All / Components / Vulnerabilities tabs without paying for a
 * full page of hits.
 *
 * Cached in module scope by query for 10 minutes via {@link makeKeyedTtlCache}.
 * Filter changes on those tabs (which do not affect the totals — totals are
 * query-only) do not refetch. On query change the previous entry is replaced.
 * Failed fetches are evicted so the next call retries.
 */
const totalsCache = makeKeyedTtlCache<string | undefined, SearchResponse>(
  (query) => {
    const params = new URLSearchParams();
    if (query) params.set('query', query);
    params.set('offset', '0');
    params.set('limit', '1');
    return searchAll(params);
  },
  TOTALS_TTL_MS,
  (query) => query ?? ''
);

export function fetchGlobalSearchTotals(query: string | undefined): Promise<SearchResponse> {
  return totalsCache.fetch(query);
}

/** @internal Resets the in-memory totals cache. Test-only. */
export function _resetGlobalSearchTotalsCacheForTests(): void {
  totalsCache.reset();
}
