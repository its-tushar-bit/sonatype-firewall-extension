/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, API_PREFIX } from './apiFetch';
import { mockVulnerabilities } from './mocks/mockVulnerabilitiesData';
import { getCVSSSeverity } from '@guide/ui-core';
import type {
  Vulnerability,
  VulnerabilitySearchResponse,
  VulnerabilitiesFilters,
  VulnerabilitiesSearchOptions,
  AffectedComponentVersion,
} from '@guide/ui-core/types';
import { getMockVulnerabilityDetail } from './mocks/mockVulnerabilityDetailData';
import { getMockAffectedComponents } from './mocks/mockAffectedComponentsData';

/** Aggregations type matching ui-core's Record<string, Record<string, number>> */
type Aggregations = Record<string, Record<string, number>>;

/** Generic paginated API response */
export interface ApiSearchResponse<T> {
  hits: T[];
  total: number;
  offset: number;
  limit: number;
}

/** Combined params for vulnerability search */
export interface VulnerabilitiesSearchParams {
  query?: string;
  filters?: VulnerabilitiesFilters;
  options?: VulnerabilitiesSearchOptions;
}

/**
 * Sorts vulnerabilities by field and order.
 */
function sortVulnerabilities(
  vulnerabilities: Vulnerability[],
  sortField?: string,
  sortOrder?: 'asc' | 'desc'
): Vulnerability[] {
  if (!sortField) return vulnerabilities;

  const order = sortOrder === 'desc' ? -1 : 1;

  return [...vulnerabilities].sort((a, b) => {
    let aVal: number | string;
    let bVal: number | string;

    switch (sortField) {
      case 'refid':
        aVal = (a.vulnId ?? '').toLowerCase();
        bVal = (b.vulnId ?? '').toLowerCase();
        break;
      case 'sonatypeCvssSeverity':
        aVal = a.sonatypeCvssSeverity ?? a.cvssSeverity ?? 0;
        bVal = b.sonatypeCvssSeverity ?? b.cvssSeverity ?? 0;
        break;
      case 'publishedDate':
        aVal = a.publishedAt ?? '';
        bVal = b.publishedAt ?? '';
        break;
      case 'epss':
        aVal = a.epss ?? 0;
        bVal = b.epss ?? 0;
        break;
      case 'kev':
        aVal = a.kev ? 1 : 0;
        bVal = b.kev ? 1 : 0;
        break;
      case 'isMalware':
        aVal = a.isMalware ? 1 : 0;
        bVal = b.isMalware ? 1 : 0;
        break;
      default:
        return 0;
    }

    if (typeof aVal === 'string' && typeof bVal === 'string') {
      return aVal.localeCompare(bVal) * order;
    }
    return ((aVal as number) - (bVal as number)) * order;
  });
}

/**
 * Filters vulnerabilities based on query and filter params.
 */
export function filterVulnerabilities(
  vulnerabilities: Vulnerability[],
  query?: string,
  filters?: VulnerabilitiesFilters
): Vulnerability[] {
  let result = vulnerabilities;

  // Text query filter (searches vulnId and summary)
  if (query && query.trim() && query !== '*') {
    const q = query.toLowerCase().trim();
    result = result.filter((v) => {
      const vulnId = (v.vulnId ?? '').toLowerCase();
      const summary = (v.summary ?? '').toLowerCase();
      return vulnId.includes(q) || summary.includes(q);
    });
  }

  if (!filters) return result;

  // affectedEcosystems filter
  if (filters.affectedEcosystems) {
    const ecosystems = Array.isArray(filters.affectedEcosystems)
      ? filters.affectedEcosystems
      : [filters.affectedEcosystems];
    result = result.filter((v) => v.affectedEcosystems?.some((eco) => ecosystems.includes(eco)));
  }

  // severities filter (uses Sonatype-adjusted severity for consistency with display)
  if (filters.severities) {
    const severities = Array.isArray(filters.severities) ? filters.severities : [filters.severities];
    result = result.filter((v) => severities.includes(getCVSSSeverity(v.sonatypeCvssSeverity ?? v.cvssSeverity ?? 0)));
  }

  // cwes filter
  if (filters.cwes) {
    const cwes = Array.isArray(filters.cwes) ? filters.cwes : [filters.cwes];
    result = result.filter((v) => v.cwes?.some((cwe) => cwes.includes(cwe)));
  }

  // exploitationKnown filter (kev)
  if (filters.exploitationKnown !== undefined) {
    result = result.filter((v) => v.kev === filters.exploitationKnown);
  }

  // hasMalware filter
  if (filters.hasMalware !== undefined) {
    result = result.filter((v) => (v.isMalware ?? false) === filters.hasMalware);
  }

  // CVSS range filter (uses Sonatype-adjusted severity for consistency with display)
  // Note: Missing CVSS defaults to 0 for min (excluded from min threshold)
  // and 10 for max (always passes max threshold), treating unscored as worst-case.
  if (filters.minCvss !== undefined) {
    const minCvss = filters.minCvss;
    result = result.filter((v) => (v.sonatypeCvssSeverity ?? v.cvssSeverity ?? 0) >= minCvss);
  }
  if (filters.maxCvss !== undefined) {
    const maxCvss = filters.maxCvss;
    result = result.filter((v) => (v.sonatypeCvssSeverity ?? v.cvssSeverity ?? 10) <= maxCvss);
  }

  // EPSS range filter
  // Note: Missing EPSS defaults to 0 for min (excluded from min threshold)
  // and 1 for max (always passes max threshold), treating unscored as worst-case.
  if (filters.minEpss !== undefined) {
    const minEpss = filters.minEpss;
    result = result.filter((v) => (v.epss ?? 0) >= minEpss);
  }
  if (filters.maxEpss !== undefined) {
    const maxEpss = filters.maxEpss;
    result = result.filter((v) => (v.epss ?? 1) <= maxEpss);
  }

  // publishedWindow filter (date range)
  if (filters.publishedWindow) {
    const now = new Date();
    let startDate: Date;
    switch (filters.publishedWindow) {
      case '7d':
        startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        break;
      case '30d':
        startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
        break;
      case '60d':
        startDate = new Date(now.getTime() - 60 * 24 * 60 * 60 * 1000);
        break;
      case '90d':
        startDate = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
        break;
      case '6m':
        startDate = new Date(now.getTime() - 182 * 24 * 60 * 60 * 1000);
        break;
      case '1y':
        startDate = new Date(now.getTime() - 365 * 24 * 60 * 60 * 1000);
        break;
      case '2y':
        startDate = new Date(now.getTime() - 730 * 24 * 60 * 60 * 1000);
        break;
      default:
        startDate = new Date(0); // All time
    }
    result = result.filter((v) => {
      if (!v.publishedAt) return false;
      const publishedAt = new Date(v.publishedAt);
      return publishedAt >= startDate;
    });
  }

  return result;
}

/**
 * Computes aggregations from filtered results.
 */
export function computeVulnerabilityAggregations(vulnerabilities: Vulnerability[]): Aggregations {
  // Pre-populate with known ecosystems to ensure filter shows even when empty
  const affectedEcosystems: Record<string, number> = {
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
    helm: 0,
    oci: 0,
  };
  const severities: Record<string, number> = { critical: 0, high: 0, medium: 0, low: 0, none: 0 };
  const kev: Record<string, number> = { true: 0, false: 0 };
  const isMalware: Record<string, number> = { true: 0, false: 0 };

  for (const v of vulnerabilities) {
    // Affected ecosystems
    for (const eco of v.affectedEcosystems ?? []) {
      affectedEcosystems[eco] = (affectedEcosystems[eco] ?? 0) + 1;
    }

    // Severities (uses Sonatype-adjusted severity for consistency with display)
    const sev = getCVSSSeverity(v.sonatypeCvssSeverity ?? v.cvssSeverity ?? 0);
    severities[sev] = (severities[sev] ?? 0) + 1;

    // KEV
    const kevKey = v.kev ? 'true' : 'false';
    kev[kevKey] = (kev[kevKey] ?? 0) + 1;

    // Malware
    const malwareKey = v.isMalware ? 'true' : 'false';
    isMalware[malwareKey] = (isMalware[malwareKey] ?? 0) + 1;
  }

  return {
    byEcosystem: affectedEcosystems,
    bySeverity: severities,
    byKev: kev,
    byMalware: isMalware,
  };
}

/**
 * Mock handler that implements realistic filter/sort/pagination.
 */
function mockSearchHandler(params: VulnerabilitiesSearchParams): VulnerabilitySearchResponse {
  const { query, filters, options } = params;
  const { offset = 0, limit = 25, sortField, sortOrder } = options ?? {};

  // Filter
  const filtered = filterVulnerabilities(mockVulnerabilities, query, filters);

  // Sort
  const sorted = sortVulnerabilities(filtered, sortField, sortOrder);

  // Paginate
  const paginated = sorted.slice(offset, offset + limit);

  // Compute aggregations from filtered set
  const aggregations = computeVulnerabilityAggregations(filtered);

  return {
    hits: paginated,
    total: filtered.length,
    offset,
    limit,
    aggregations,
  };
}

/**
 * Searches for vulnerabilities matching the given parameters.
 *
 * When USE_MOCKS is true, returns mock data with realistic
 * filtering, sorting, and pagination.
 *
 * @param params - Search parameters including query, filters, and options
 * @returns Search response with hits and aggregations
 */
export async function searchVulnerabilities(
  params: VulnerabilitiesSearchParams = {}
): Promise<VulnerabilitySearchResponse> {
  return apiFetch<VulnerabilitySearchResponse>(`${API_PREFIX}/vulnerabilities/search`, {
    mockHandler: () => mockSearchHandler(params),
  });
}

/**
 * Get vulnerability details by ID.
 *
 * @param vulnId - The vulnerability identifier (CVE, Sonatype ID, etc.)
 * @returns Vulnerability details, or null if not found
 */
export async function getVulnerabilityDetails(vulnId: string): Promise<Vulnerability | null> {
  if (!vulnId.trim()) return null;

  return apiFetch<Vulnerability | null>(`${API_PREFIX}/vulnerabilities/${encodeURIComponent(vulnId)}`, {
    mockHandler: () => getMockVulnerabilityDetail(vulnId) ?? null,
  });
}

/**
 * Mock handler for affected components with realistic filter/sort/pagination.
 */
function mockAffectedComponentsHandler(
  vulnId: string,
  params?: {
    query?: string;
    offset?: number;
    limit?: number;
    sortField?: string;
    sortOrder?: 'asc' | 'desc';
  }
): ApiSearchResponse<AffectedComponentVersion> {
  const allComponents = getMockAffectedComponents(vulnId);
  if (allComponents.length === 0) {
    return { hits: [], total: 0, offset: params?.offset ?? 0, limit: params?.limit ?? 50 };
  }

  const query = params?.query?.toLowerCase()?.trim();
  const offset = params?.offset ?? 0;
  const limit = params?.limit ?? 50;
  const sortField = params?.sortField ?? 'packageName';
  const sortOrder = params?.sortOrder ?? 'asc';

  // Filter by query
  let filtered = allComponents;
  if (query) {
    filtered = allComponents.filter((c) => {
      const pkgName = c.packageName.toLowerCase();
      const namespace = c.namespace?.toLowerCase() ?? '';
      return pkgName.includes(query) || namespace.includes(query);
    });
  }

  // Sort
  filtered = [...filtered].sort((a, b) => {
    let aVal: string;
    let bVal: string;
    switch (sortField) {
      case 'version':
        aVal = a.version ?? '';
        bVal = b.version ?? '';
        break;
      case 'ecosystem':
        aVal = a.ecosystem ?? '';
        bVal = b.ecosystem ?? '';
        break;
      case 'packageName':
      default:
        // Sort by package name (including namespace as secondary key)
        aVal = `${a.namespace ?? ''}:${a.packageName}`;
        bVal = `${b.namespace ?? ''}:${b.packageName}`;
    }
    const cmp = aVal.localeCompare(bVal);
    return sortOrder === 'desc' ? -cmp : cmp;
  });

  // Paginate
  const paginated = filtered.slice(offset, offset + limit);

  return {
    hits: paginated,
    total: filtered.length,
    offset,
    limit,
  };
}

/** Accepted sortField values for affected-components queries. Single source of truth shared with ComponentsImpactedTab. */
export const AFFECTED_COMPONENTS_SORT_FIELDS = new Set(['packageName', 'version', 'ecosystem']);

/**
 * Get paginated affected components for a vulnerability.
 *
 * @param vulnId - The vulnerability identifier
 * @param params - Optional search parameters
 * @param params.sortField - Field to sort by. Accepted values: see {@link AFFECTED_COMPONENTS_SORT_FIELDS}.
 *   Callers must validate sortField before passing it here; the API layer does not enforce the allowlist.
 * @returns Paginated affected components response, or null if not found
 */
export async function getVulnerabilityAffectedComponents(
  vulnId: string,
  params?: {
    query?: string;
    offset?: number;
    limit?: number;
    sortField?: string;
    sortOrder?: 'asc' | 'desc';
  }
): Promise<ApiSearchResponse<AffectedComponentVersion> | null> {
  if (!vulnId.trim()) return null;

  const queryString = new URLSearchParams();
  if (params?.query) queryString.set('query', params.query);
  if (params?.offset !== undefined) queryString.set('offset', String(params.offset));
  if (params?.limit !== undefined) queryString.set('limit', String(params.limit));
  if (params?.sortField) queryString.set('sortField', params.sortField);
  if (params?.sortOrder) queryString.set('sortOrder', params.sortOrder);

  const separator = queryString.toString() ? '?' : '';
  return apiFetch<ApiSearchResponse<AffectedComponentVersion> | null>(
    `${API_PREFIX}/vulnerabilities/${encodeURIComponent(vulnId)}/affected-components${separator}${queryString.toString()}`,
    {
      mockHandler: () => mockAffectedComponentsHandler(vulnId, params),
    }
  );
}
