/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, API_PREFIX } from './apiFetch';
import { mockComponents } from './mocks/mockComponentsData';
import { mockVulnerabilities } from './mocks/mockVulnerabilitiesData';
import { searchComponents } from './componentsBackend';
import { searchVulnerabilities } from './vulnerabilitiesBackend';
import type {
  SearchResponse,
  SearchResult,
  GlobalSearchFilters,
  GlobalSearchOptions,
} from '@guide/ui-core/types';

export interface GlobalSearchParams {
  query?: string;
  filters?: GlobalSearchFilters;
  options?: GlobalSearchOptions;
}

const DEFAULT_LIMIT = 25;

function matchesQuery(hit: SearchResult, query: string): boolean {
  const q = query.toLowerCase().trim();
  if (!q) return true;
  if ('name' in hit) {
    return (hit.name ?? '').toLowerCase().includes(q) ||
           (hit.namespace ?? '').toLowerCase().includes(q);
  }
  return (hit.vulnId ?? '').toLowerCase().includes(q) ||
         (hit.summary ?? '').toLowerCase().includes(q);
}

function computeGlobalAggregations(hits: SearchResult[]): Record<string, Record<string, number>> {
  const byType: Record<string, number> = { component: 0, vulnerability: 0 };
  // Pre-populate with known ecosystems so the filter renders even when counts are zero.
  const byEcosystem: Record<string, number> = {
    maven: 0,
    npm: 0,
    pypi: 0,
    go: 0,
    nuget: 0,
    cargo: 0,
    rubygems: 0,
    composer: 0,
    cocoapods: 0,
    conan: 0,
    conda: 0,
    cran: 0,
    helm: 0,
    oci: 0,
  };
  for (const h of hits) {
    if ('name' in h) {
      byType.component += 1;
      if (h.format) byEcosystem[h.format] = (byEcosystem[h.format] ?? 0) + 1;
    } else {
      byType.vulnerability += 1;
      for (const eco of h.affectedEcosystems ?? []) {
        byEcosystem[eco] = (byEcosystem[eco] ?? 0) + 1;
      }
    }
  }
  return { byType, byEcosystem };
}

function matchesEcosystem(hit: SearchResult, formats: string[]): boolean {
  if (formats.length === 0) return true;
  if ('name' in hit) {
    return hit.format ? formats.includes(hit.format) : false;
  }
  return (hit.affectedEcosystems ?? []).some((eco) => formats.includes(eco));
}

const LAST_UPDATED_WINDOW_MS: Record<string, number> = {
  '7d': 7 * 24 * 60 * 60 * 1000,
  '30d': 30 * 24 * 60 * 60 * 1000,
  '60d': 60 * 24 * 60 * 60 * 1000,
  '90d': 90 * 24 * 60 * 60 * 1000,
  '6m': 182 * 24 * 60 * 60 * 1000,
  '1y': 365 * 24 * 60 * 60 * 1000,
  '2y': 2 * 365 * 24 * 60 * 60 * 1000,
};

function matchesLastUpdated(hit: SearchResult, lastUpdated: string | undefined): boolean {
  if (!lastUpdated) return true;
  const windowMs = LAST_UPDATED_WINDOW_MS[lastUpdated];
  if (!windowMs) return true;
  const dateStr = 'name' in hit ? hit.publishedDate : hit.publishedAt;
  if (!dateStr) return false;
  const published = new Date(dateStr).getTime();
  return !Number.isNaN(published) && Date.now() - published <= windowMs;
}

function mockSearchAll(params: GlobalSearchParams): SearchResponse {
  const { query, filters, options } = params;
  const offset = options?.offset ?? 0;
  const limit = options?.limit ?? DEFAULT_LIMIT;

  const formats = filters?.formats
    ? Array.isArray(filters.formats) ? filters.formats : [filters.formats]
    : [];
  const lastUpdated = filters?.lastUpdated;

  const pool: SearchResult[] = [...mockComponents, ...mockVulnerabilities];
  const filtered = pool.filter(
    (h) =>
      matchesQuery(h, query ?? '') &&
      matchesEcosystem(h, formats) &&
      matchesLastUpdated(h, lastUpdated),
  );
  const aggregations = computeGlobalAggregations(filtered);
  const hits = filtered.slice(offset, offset + limit);

  return { hits, total: filtered.length, offset, limit, aggregations };
}

export async function searchAll(
  params: GlobalSearchParams = {}
): Promise<SearchResponse> {
  return apiFetch<SearchResponse>(`${API_PREFIX}/global/search`, {
    mockHandler: () => mockSearchAll(params),
  });
}

export { searchComponents, searchVulnerabilities };
