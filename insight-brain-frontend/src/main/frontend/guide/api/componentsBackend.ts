/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, API_PREFIX } from './apiFetch';
import { mockComponents } from './mocks/mockComponentsData';
import { filterVulnerabilities, computeVulnerabilityAggregations } from './vulnerabilitiesBackend';
import {
  mockComponentDetail,
  mockVulnerabilities,
  mockVersions,
  mockDependencies,
  makeDts,
} from './mocks/mockComponentDetailData';
import { getCVSSSeverity } from '@guide/ui-core';
import { parsePackageIdentifier } from '@guide/ui-core/utils';
import type { ReadonlySearchParams } from '@guide/ui-core/adapters';
import type {
  Component,
  ComponentSearchResponse,
  ComponentsFilters,
  ComponentsSearchOptions,
  VulnerabilitiesFilters,
  VulnerabilitiesSearchOptions,
  VersionsFilters,
  VulnerabilitySearchResponse,
  ComponentDetails,
} from '@guide/ui-core/types';

/** Aggregations type matching ui-core's Record<string, Record<string, number>> */
type Aggregations = Record<string, Record<string, number>>;

/**
 * Sorts components by field and order.
 */
function sortComponents(
  components: Component[],
  sortField?: string,
  sortOrder?: 'asc' | 'desc'
): Component[] {
  if (!sortField) return components;

  const order = sortOrder === 'desc' ? -1 : 1;

  return [...components].sort((a, b) => {
    let aVal: number | string;
    let bVal: number | string;

    switch (sortField) {
      case 'name':
        aVal = (a.name ?? '').toLowerCase();
        bVal = (b.name ?? '').toLowerCase();
        break;
      case 'version':
        aVal = (a.version ?? '').toLowerCase();
        bVal = (b.version ?? '').toLowerCase();
        break;
      case 'maxCvss':
        aVal = a.maxCvss ?? -1;
        bVal = b.maxCvss ?? -1;
        break;
      case 'versionScore':
        aVal = a.versionScore ?? 0;
        bVal = b.versionScore ?? 0;
        break;
      case 'publishedDate':
        aVal = a.publishedDate ?? '';
        bVal = b.publishedDate ?? '';
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
 * Filters components based on query and filter params.
 */
function filterComponents(
  components: Component[],
  query?: string,
  filters?: ComponentsFilters
): Component[] {
  let result = components;

  // Text query filter (searches name, namespace)
  if (query && query.trim() && query !== '*') {
    const q = query.toLowerCase().trim();
    result = result.filter((c) => {
      const name = (c.name ?? '').toLowerCase();
      const ns = (c.namespace ?? '').toLowerCase();
      return name.includes(q) || ns.includes(q);
    });
  }

  if (!filters) return result;

  // Format filter
  if (filters.formats) {
    const formats = Array.isArray(filters.formats)
      ? filters.formats
      : [filters.formats];
    result = result.filter((c) => formats.includes(c.format));
  }

  // Category filter
  if (filters.categories) {
    const categories = Array.isArray(filters.categories)
      ? filters.categories
      : [filters.categories];
    result = result.filter(
      (c) => c.categories?.some((cat) => categories.includes(cat))
    );
  }

  // Severity filter
  if (filters.severities) {
    const severities = Array.isArray(filters.severities)
      ? filters.severities
      : [filters.severities];
    result = result.filter((c) =>
      severities.includes(getCVSSSeverity(c.maxCvss ?? 0))
    );
  }

  // License filter
  if (filters.licenses) {
    const licenses = Array.isArray(filters.licenses)
      ? filters.licenses
      : [filters.licenses];
    result = result.filter((c) =>
      (c.licenses ?? []).some((l) => licenses.includes(l.licenseName))
    );
  }

  // Published window filter (date range)
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
    result = result.filter((c) => {
      if (!c.publishedDate) return false;
      return new Date(c.publishedDate) >= startDate;
    });
  }

  // Malware filter
  if (filters.hasMalware !== undefined) {
    result = result.filter(
      (c) => (c.isMalware ?? false) === filters.hasMalware
    );
  }

  // Version score range
  if (filters.minVersionScore !== undefined) {
    result = result.filter(
      (c) => (c.versionScore ?? 0) >= filters.minVersionScore!
    );
  }
  if (filters.maxVersionScore !== undefined) {
    result = result.filter(
      (c) => (c.versionScore ?? 100) <= filters.maxVersionScore!
    );
  }

  return result;
}

/**
 * Computes aggregations from filtered results.
 */
function computeAggregations(components: Component[]): Aggregations {
  const formats: Record<string, number> = {};
  const categories: Record<string, number> = {};
  const severities: Record<string, number> = { critical: 0, high: 0, medium: 0, low: 0, none: 0 };
  const licenses: Record<string, number> = {};
  const malware: Record<string, number> = { true: 0, false: 0 };

  for (const c of components) {
    formats[c.format] = (formats[c.format] ?? 0) + 1;

    for (const cat of c.categories ?? []) {
      categories[cat] = (categories[cat] ?? 0) + 1;
    }

    const sev = getCVSSSeverity(c.maxCvss ?? 0);
    severities[sev] = (severities[sev] ?? 0) + 1;

    for (const lic of c.licenses ?? []) {
      licenses[lic.licenseName] = (licenses[lic.licenseName] ?? 0) + 1;
    }

    const malwareKey = c.isMalware ? 'true' : 'false';
    malware[malwareKey] = (malware[malwareKey] ?? 0) + 1;
  }

  return {
    byFormat: formats,
    byCategory: categories,
    bySeverity: severities,
    byLicense: licenses,
    byMalware: malware,
  };
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

export interface ComponentVersionsResponse {
  hits: ComponentDetails[];
  total: number;
  offset: number;
  limit: number;
  aggregations: Record<string, Record<string, number>>;
}

function mockComponentToDetails(ecosystem: string, pkg: string, version: string): ComponentDetails | null {
  const { name } = parsePackageIdentifier(pkg);
  // Try exact match (name + ecosystem + version)
  const exactMatch = mockComponents.find(
    (c) => c.name === name && c.format === ecosystem && c.version === version
  );
  if (exactMatch) {
    return {
      ...mockComponentDetail,
      format: exactMatch.format,
      originId: exactMatch.originId,
      namespace: exactMatch.namespace ?? '',
      name: exactMatch.name,
      version: exactMatch.version,
      registryLink: exactMatch.registryLink,
      maxCvss: exactMatch.maxCvss ?? 0,
      licenses: exactMatch.licenses ?? [],
      categories: exactMatch.categories,
      versionScore: exactMatch.versionScore ?? 0,
      isMalware: exactMatch.isMalware ?? false,
      dts: makeDts(exactMatch.versionScore ?? 0),
    };
  }
  // Return null if no exact match (component not found at that version)
  return null;
}

export async function getComponentDetail(
  ecosystem: string,
  pkg: string,
  version: string
): Promise<ComponentDetails | null> {
  return apiFetch<ComponentDetails>(
    `${API_PREFIX}/components/${encodeURIComponent(ecosystem)}/${encodeURIComponent(pkg)}/${encodeURIComponent(version)}`,
    { mockHandler: () => mockComponentToDetails(ecosystem, pkg, version) }
  );
}

export async function getComponentVulnerabilities(
  ecosystem: string,
  pkg: string,
  version: string,
  query: string | undefined,
  filters: VulnerabilitiesFilters,
  options: VulnerabilitiesSearchOptions
): Promise<VulnerabilitySearchResponse> {
  const { offset = 0, limit = 25, sortField, sortOrder } = options;
  return apiFetch<VulnerabilitySearchResponse>(
    `${API_PREFIX}/components/${encodeURIComponent(ecosystem)}/${encodeURIComponent(pkg)}/${encodeURIComponent(version)}/vulnerabilities`,
    {
      mockHandler: () => {
        const component = mockComponentToDetails(ecosystem, pkg, version);
        if (!component) {
          return { hits: [], total: 0, offset, limit, aggregations: {} };
        }
        const all = component.maxCvss > 0 ? mockVulnerabilities : [];
        const filtered = filterVulnerabilities(all, query, filters);
        const sorted = sortField
          ? [...filtered].sort((a, b) => {
              const av = sortField === 'publishedDate' ? (a.publishedAt ?? '') : (a.cvssSeverity ?? 0);
              const bv = sortField === 'publishedDate' ? (b.publishedAt ?? '') : (b.cvssSeverity ?? 0);
              return (typeof av === 'string' ? av.localeCompare(bv as string) : (av as number) - (bv as number)) *
                (sortOrder === 'asc' ? 1 : -1);
            })
          : filtered;
        return {
          hits: sorted.slice(offset, offset + limit),
          total: filtered.length,
          offset,
          limit,
          aggregations: computeVulnerabilityAggregations(filtered),
        };
      },
    }
  );
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
  return apiFetch<ComponentVersionsResponse>(
    `${API_PREFIX}/components/${encodeURIComponent(ecosystem)}/${encodeURIComponent(pkg)}/${encodeURIComponent(version)}/versions`,
    {
      mockHandler: () => {
        const { name } = parsePackageIdentifier(pkg);
        const samePackage = mockComponents
          .filter((c) => c.name === name && c.format === ecosystem)
          .map((c) => ({
            ...mockComponentDetail,
            format: c.format,
            originId: c.originId,
            namespace: c.namespace ?? '',
            name: c.name,
            version: c.version,
            registryLink: c.registryLink,
            maxCvss: c.maxCvss ?? 0,
            licenses: c.licenses ?? [],
            categories: c.categories,
            versionScore: c.versionScore ?? 0,
            isMalware: c.isMalware ?? false,
            latestStable: c.version === version,
            dts: makeDts(c.versionScore ?? 0),
          }));
        const all = samePackage.length > 0 ? samePackage : mockVersions;
        const f = filters as Record<string, unknown>;

        // Text query: filter by version string
        let filtered = query
          ? all.filter((v) => v.version.toLowerCase().includes(query.toLowerCase()))
          : all;

        const isPreRelease = (ver: string) => /[-.](?:alpha|beta|rc|snapshot|preview|dev|nightly|m\d)/i.test(ver) || /-[a-zA-Z]/.test(ver);
        if (f.isStable === true || f.isStable === 'true') {
          filtered = filtered.filter((v) => !isPreRelease(v.version));
        } else if (f.isStable === false || f.isStable === 'false') {
          filtered = filtered.filter((v) => isPreRelease(v.version));
        }

        // hasMalware filter
        if (f.hasMalware !== undefined) {
          const wantMalware = f.hasMalware === true || f.hasMalware === 'true';
          filtered = filtered.filter((v) => (v.isMalware ?? false) === wantMalware);
        }

        // severity filter
        if (f.severities) {
          const sevs = Array.isArray(f.severities) ? f.severities : [f.severities];
          filtered = filtered.filter((v) => sevs.includes(getCVSSSeverity(v.maxCvss ?? 0)));
        }

        // versionScore range filter
        if (f.minVersionScore !== undefined) {
          filtered = filtered.filter((v) => (v.versionScore ?? 0) >= Number(f.minVersionScore));
        }
        if (f.maxVersionScore !== undefined) {
          filtered = filtered.filter((v) => (v.versionScore ?? 100) <= Number(f.maxVersionScore));
        }

        // publishedWindow filter (reads from components[0].publishedDate)
        if (f.publishedWindow) {
          const now = Date.now();
          const windows: Record<string, number> = {
            '7d': 7, '30d': 30, '60d': 60, '90d': 90,
            '6m': 180, '1y': 365, '2y': 730,
          };
          const days = windows[String(f.publishedWindow)];
          if (days) {
            const cutoff = now - days * 24 * 60 * 60 * 1000;
            filtered = filtered.filter((v) => {
              const pub = v.components[0]?.publishedDate;
              return pub ? new Date(pub).getTime() >= cutoff : false;
            });
          }
        }

        const sorted = sortField
          ? [...filtered].sort((a, b) => {
              const av = String((a as unknown as Record<string, unknown>)[sortField] ?? '');
              const bv = String((b as unknown as Record<string, unknown>)[sortField] ?? '');
              return av.localeCompare(bv) * (sortOrder === 'asc' ? 1 : -1);
            })
          : filtered;
        return {
          hits: sorted.slice(offset, offset + limit),
          total: filtered.length,
          offset,
          limit,
          aggregations: (({ bySeverity, byMalware }) => ({ bySeverity, byMalware }))(
            computeAggregations(filtered as Component[])
          ),
        };
      },
    }
  );
}

export async function getComponentDependencies(
  ecosystem: string,
  pkg: string,
  version: string,
  query: string | undefined,
  filters: ComponentsFilters,
  options: ComponentsSearchOptions
): Promise<ComponentSearchResponse> {
  const { offset = 0, limit = 25, sortField, sortOrder } = options;
  return apiFetch<ComponentSearchResponse>(
    `${API_PREFIX}/components/${encodeURIComponent(ecosystem)}/${encodeURIComponent(pkg)}/${encodeURIComponent(version)}/dependencies`,
    {
      mockHandler: () => {
        const filtered = filterComponents(mockDependencies as Component[], query, filters);
        const sorted = sortComponents(filtered, sortField, sortOrder);
        return {
          hits: sorted.slice(offset, offset + limit),
          total: filtered.length,
          offset,
          limit,
          aggregations: computeAggregations(filtered),
        };
      },
    }
  );
}
