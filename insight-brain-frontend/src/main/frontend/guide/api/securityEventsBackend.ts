/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, API_PREFIX, ApiError } from './apiFetch';
import { makeKeylessTtlCache } from './ttlCache';
import type { SecurityEventDocument, SecurityEventDetailDocument } from '@guide/ui-core/types';
import type { ReadonlySearchParams } from '@guide/ui-core/adapters';

/** Aggregations type matching ui-core's Record<string, Record<string, number>>. */
type Aggregations = Record<string, Record<string, number>>;

/** Generic paginated search response returned by the Guide search endpoints. */
export interface ApiSearchResponse<T> {
  hits: T[];
  total: number;
  offset: number;
  limit: number;
  aggregations?: Aggregations;
}

/**
 * Calls GET /api/v2/guide/security-events/search with the supplied URL search params forwarded
 * verbatim. URL parameter keys are aligned with backend `@QueryParam` names by design — ui-core's
 * SECURITY_EVENT_FILTER_ORDER emits `severities`, `threatTypes`, `knownExploited`, and
 * `affectedEcosystems`, matching {@code GuideSecurityEventsResource} — so no remapping is needed.
 *
 * The caller (page) sets any frontend-owned defaults (e.g. `limit`, `sortField`) before invoking.
 */
export async function searchSecurityEvents(
  searchParams: ReadonlySearchParams
): Promise<ApiSearchResponse<SecurityEventDocument>> {
  return apiFetch<ApiSearchResponse<SecurityEventDocument>>(
    `${API_PREFIX}/security-events/search?${searchParams.toString()}`
  );
}

const BROWSE_AGGREGATIONS_TTL_MS = 10 * 60 * 1000;

/**
 * Fetches aggregations from an unfiltered browse request (no query, no filters) so filtered views
 * can restore zero-count facet buckets. Uses limit=1 to minimize the payload while still returning
 * aggregations.
 *
 * Memoized in module scope for 10 minutes via {@link makeKeylessTtlCache}, so navigation between
 * filtered states does not refetch the facet universe. Failed fetches resolve to null and are
 * evicted so the next call retries — this also yields a graceful empty facet rail on air-gapped
 * installs where the search server is unreachable.
 */
const browseAggregationsCache = makeKeylessTtlCache<Aggregations | null>(
  async () => {
    try {
      const response = await apiFetch<ApiSearchResponse<SecurityEventDocument>>(
        `${API_PREFIX}/security-events/search?limit=1`
      );
      return response.aggregations ?? null;
    } catch {
      return null;
    }
  },
  BROWSE_AGGREGATIONS_TTL_MS,
  (result) => result === null
);

export function fetchSecurityEventBrowseAggregations(): Promise<Aggregations | null> {
  return browseAggregationsCache.fetch();
}

/** @internal Resets the in-memory browse-aggregations cache. Test-only. */
export function _resetBrowseAggregationsCacheForTests(): void {
  browseAggregationsCache.reset();
}

/**
 * Fetches a single security event's full advisory from GET /api/v2/guide/security-events/{id}.
 * Resolves null for a blank id or an HTTP 404 (unknown/removed event) so callers can render a
 * not-found state; other errors propagate for the page's error boundary. Mirrors
 * {@code getVulnerabilityDetails}.
 */
export async function getSecurityEventDetails(
  eventId: string
): Promise<SecurityEventDetailDocument | null> {
  if (!eventId.trim()) return null;
  return apiFetch<SecurityEventDetailDocument | null>(
    `${API_PREFIX}/security-events/${encodeURIComponent(eventId)}`
  ).catch((e: unknown) => {
    if (e instanceof ApiError && e.status === 404) return null;
    throw e;
  });
}
