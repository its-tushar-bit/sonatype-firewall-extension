/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, ApiError, API_PREFIX } from './apiFetch';
import { makeKeylessTtlCache } from './ttlCache';
import { parsePackageIdentifier, type Aggregations } from '@guide/ui-core/utils';
import type { ReadonlySearchParams } from '@guide/ui-core/adapters';
import { toStringArray } from '../utils/searchParams';
import type {
  ComponentSearchResponse,
  ComponentsFilters,
  ComponentsSearchOptions,
  VulnerabilitiesFilters,
  VulnerabilitiesSearchOptions,
  VersionsFilters,
  VulnerabilitySearchResponse,
  ComponentDetails,
  RecommendationResponse,
} from '@guide/ui-core/types';

function buildPurl(ecosystem: string, pkg: string, version: string): string {
  const { namespace, name } = parsePackageIdentifier(pkg);
  const normalizedFormat = ecosystem.trim().toLowerCase();
  // No pre-encoding here — URLSearchParams encodes the whole PURL string once when it's
  // used as a query param value. Pre-encoding with encodeURIComponent causes double-encoding
  // (%40 → %2540) and breaks scoped npm packages like @scope/name.
  const namespacePrefix = namespace ? `${namespace.trim()}/` : '';
  return `pkg:${normalizedFormat}/${namespacePrefix}${name.trim()}@${version.trim()}`;
}

/**
 * Calls GET /api/v2/guide/components/search with the supplied URL search params
 * forwarded verbatim. URL parameter keys are aligned with backend `@QueryParam`
 * names by design (see `@guide/ui-core`'s FILTER MAPPING REFERENCE), so no
 * remapping is needed.
 *
 * The caller (page) is responsible for setting any frontend-owned defaults
 * (e.g. `limit`) before invoking.
 */
export async function searchComponents(
  searchParams: ReadonlySearchParams
): Promise<ComponentSearchResponse> {
  return apiFetch<ComponentSearchResponse>(
    `${API_PREFIX}/components/search?${searchParams.toString()}`
  );
}

const BROWSE_AGGREGATIONS_TTL_MS = 10 * 60 * 1000;

/**
 * Fetches aggregations from a component browse request (no query, no filters).
 * Uses limit=1 to minimize the response payload while still returning aggregations.
 * Because this is an unfiltered match-all query, every bucket has count >= 1,
 * so the default minDocCount=1 returns the same buckets as minDocCount=0.
 *
 * Memoized in module scope for 10 minutes via {@link makeKeylessTtlCache}, so
 * navigation between filtered states does not refetch the facet universe.
 * Failed fetches resolve to null and are evicted so the next call retries.
 */
const browseAggregationsCache = makeKeylessTtlCache<Aggregations | null>(
  async () => {
    try {
      const response = await apiFetch<ComponentSearchResponse>(
        `${API_PREFIX}/components/search?limit=1`
      );
      return (response.aggregations as Aggregations | undefined) ?? null;
    } catch {
      return null;
    }
  },
  BROWSE_AGGREGATIONS_TTL_MS,
  (result) => result === null
);

export function fetchComponentBrowseAggregations(): Promise<Aggregations | null> {
  return browseAggregationsCache.fetch();
}

/** @internal Resets the in-memory browse-aggregations cache. Test-only. */
export function _resetBrowseAggregationsCacheForTests(): void {
  browseAggregationsCache.reset();
}

export interface ComponentVersionsResponse {
  hits: ComponentDetails[];
  total: number;
  offset: number;
  limit: number;
  aggregations: Record<string, Record<string, number>>;
}

export async function getComponentDetail(
  ecosystem: string,
  pkg: string,
  version: string
): Promise<ComponentDetails | null> {
  const purl = buildPurl(ecosystem, pkg, version);
  const params = new URLSearchParams({ purl });
  return apiFetch<ComponentDetails>(`${API_PREFIX}/components/detail?${params}`)
    .catch((e: unknown) => {
      if (e instanceof ApiError && e.status === 404) return null;
      throw e;
    });
}

export async function getRecommendations(
  ecosystem: string,
  pkg: string,
  version: string
): Promise<RecommendationResponse | null> {
  const purl = buildPurl(ecosystem, pkg, version);
  return apiFetch<RecommendationResponse>(`${API_PREFIX}/recommendations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ purl }),
  }).catch(() => {
    // Recommendations are supplementary — any failure (404, 500, network) should not block the component detail page.
    return null;
  });
}

export async function getComponentVulnerabilities(
  ecosystem: string,
  pkg: string,
  version: string,
  query: string | undefined,
  filters: VulnerabilitiesFilters,
  options: VulnerabilitiesSearchOptions
): Promise<VulnerabilitySearchResponse> {
  const purl = buildPurl(ecosystem, pkg, version);
  const params = new URLSearchParams({ purl });
  params.set('offset', String(options.offset ?? 0));
  params.set('limit', String(options.limit ?? 25));
  if (options.sortField) params.set('sortField', options.sortField);
  if (options.sortOrder) params.set('sortOrder', options.sortOrder);

  toStringArray(filters.affectedEcosystems).forEach((e) => params.append('affectedEcosystems', e));
  toStringArray(filters.severities).forEach((s) => params.append('severities', s));
  toStringArray(filters.cwes).forEach((c) => params.append('cwes', c));

  if (filters.minCvss !== undefined) params.set('minCvss', String(filters.minCvss));
  if (filters.maxCvss !== undefined) params.set('maxCvss', String(filters.maxCvss));
  if (filters.minEpss !== undefined) params.set('minEpss', String(filters.minEpss));
  if (filters.maxEpss !== undefined) params.set('maxEpss', String(filters.maxEpss));
  if (filters.exploitationKnown !== undefined) params.set('exploitationKnown', String(filters.exploitationKnown));
  if (filters.hasMalware !== undefined) params.set('hasMalware', String(filters.hasMalware));
  if (filters.publishedWindow) params.set('publishedWindow', filters.publishedWindow);

  return apiFetch<VulnerabilitySearchResponse>(`${API_PREFIX}/components/vulnerabilities?${params}`);
}

export async function getComponentVersions(
  ecosystem: string,
  pkg: string,
  version: string,
  query: string | undefined,
  filters: VersionsFilters,
  options: ComponentsSearchOptions
): Promise<ComponentVersionsResponse> {
  const { offset = 0, limit = 25, sortField, sortOrder } = options;
  const purl = buildPurl(ecosystem, pkg, version);
  const params = new URLSearchParams({ purl });
  params.set('offset', String(offset));
  params.set('limit', String(limit));
  if (query) params.set('versionQuery', query);
  if (sortField) params.set('sortField', sortField);
  if (sortOrder) params.set('sortOrder', sortOrder);
  if (filters.isStable !== undefined) params.set('isStable', String(filters.isStable));
  if (filters.hasMalware !== undefined) params.set('hasMalware', String(filters.hasMalware));
  toStringArray(filters.severities).forEach((s) => params.append('severities', s));
  if (filters.minVersionScore !== undefined) params.set('minVersionScore', String(filters.minVersionScore));
  if (filters.maxVersionScore !== undefined) params.set('maxVersionScore', String(filters.maxVersionScore));
  if (filters.publishedWindow) params.set('publishedWindow', filters.publishedWindow);
  return apiFetch<ComponentVersionsResponse>(`${API_PREFIX}/components/versions?${params}`)
    .catch((e: unknown) => {
      if (e instanceof ApiError && e.status === 404) {
        return { hits: [], total: 0, offset, limit, aggregations: {} } as ComponentVersionsResponse;
      }
      throw e;
    });
}

export async function getComponentDependencies(
  ecosystem: string,
  pkg: string,
  version: string,
  query: string | undefined,
  filters: ComponentsFilters,
  options: ComponentsSearchOptions
): Promise<ComponentSearchResponse> {
  const purl = buildPurl(ecosystem, pkg, version);
  const params = new URLSearchParams({ purl });
  params.set('offset', String(options.offset ?? 0));
  params.set('limit', String(options.limit ?? 25));
  if (query) params.set('query', query);
  if (options.sortField) params.set('sortField', options.sortField);
  if (options.sortOrder) params.set('sortOrder', options.sortOrder);

  toStringArray(filters.formats).forEach((f) => params.append('formats', f));
  toStringArray(filters.categories).forEach((c) => params.append('categories', c));
  toStringArray(filters.severities).forEach((s) => params.append('severities', s));
  toStringArray(filters.licenses).forEach((l) => params.append('licenses', l));
  toStringArray(filters.licenseFamilies).forEach((lf) => params.append('licenseFamilies', lf));

  if (filters.minVersionScore !== undefined) params.set('minVersionScore', String(filters.minVersionScore));
  if (filters.maxVersionScore !== undefined) params.set('maxVersionScore', String(filters.maxVersionScore));
  if (filters.hasMalware !== undefined) params.set('hasMalware', String(filters.hasMalware));
  if (filters.publishedWindow) params.set('publishedWindow', filters.publishedWindow);

  return apiFetch<ComponentSearchResponse>(`${API_PREFIX}/components/dependencies?${params}`);
}
