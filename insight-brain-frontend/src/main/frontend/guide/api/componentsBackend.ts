/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { apiFetch, API_PREFIX } from './apiFetch';
import { mockComponents } from './mocks/mockComponentsData';
import { getCVSSSeverity } from '@guide/ui-core';
import type {
  Component,
  ComponentSearchResponse,
  ComponentsFilters,
  ComponentsSearchOptions,
} from '@guide/ui-core/types';

/** Aggregations type matching ui-core's Record<string, Record<string, number>> */
type Aggregations = Record<string, Record<string, number>>;

/** Combined params for component search */
export interface ComponentsSearchParams {
  query?: string;
  filters?: ComponentsFilters;
  options?: ComponentsSearchOptions;
}

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
  }

  return {
    byFormat: formats,
    byCategory: categories,
    bySeverity: severities,
    byLicense: licenses,
  };
}

/**
 * Mock handler that implements realistic filter/sort/pagination.
 */
function mockSearchHandler(params: ComponentsSearchParams): ComponentSearchResponse {
  const { query, filters, options } = params;
  const { offset = 0, limit = 25, sortField, sortOrder } = options ?? {};

  // Filter
  const filtered = filterComponents(mockComponents, query, filters);

  // Sort
  const sorted = sortComponents(filtered, sortField, sortOrder);

  // Paginate
  const paginated = sorted.slice(offset, offset + limit);

  // Compute aggregations from filtered set
  const aggregations = computeAggregations(filtered);

  return {
    hits: paginated,
    total: filtered.length,
    offset,
    limit,
    aggregations,
  };
}

/**
 * Searches for components matching the given parameters.
 *
 * When USE_MOCKS is true, returns mock data with realistic
 * filtering, sorting, and pagination.
 *
 * @param params - Search parameters including query, filters, and options
 * @returns Search response with hits and aggregations
 */
export async function searchComponents(
  params: ComponentsSearchParams = {}
): Promise<ComponentSearchResponse> {
  return apiFetch<ComponentSearchResponse>(
    `${API_PREFIX}/components/search`,
    {
      mockHandler: () => mockSearchHandler(params),
    }
  );
}
