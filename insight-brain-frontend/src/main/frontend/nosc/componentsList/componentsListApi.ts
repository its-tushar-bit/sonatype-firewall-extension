/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  ComponentListRow,
  ComponentsFilterFacetCounts,
  ComponentsFilterFacetEntry,
} from 'MainRoot/nosc/componentsList/componentListTypes';
import {
  ComponentsListFilterState,
  componentsListFiltersToCatalogFilters,
} from 'MainRoot/nosc/componentsList/componentsListFilters';
import {
  ComponentsTab,
  DEFAULT_COMPONENTS_TAB,
  componentsTabToSource,
} from 'MainRoot/nosc/componentsList/componentsRoute';

export const COMPONENTS_LIST_PAGE_SIZE = 50;

/** Mirrors backend {@code CatalogRow}. */
export type ApiCatalogRow = {
  readonly entityType?: string;
  readonly source?: string;
  readonly id?: string;
  readonly title?: string;
  readonly subtitle?: string | null;
  readonly fields?: Readonly<Record<string, unknown>> | null;
  readonly href?: string | null;
};

/** Mirrors backend {@code CatalogResponse.CatalogFacetBucket}. */
export type ApiCatalogFacetBucket = {
  readonly value?: string;
  readonly count?: number;
};

/** Mirrors backend {@code CatalogResponse}. */
export type ComponentsCatalogApiResponse = {
  readonly entityType?: string;
  readonly source?: string;
  readonly catalogAvailable?: boolean;
  readonly page?: number;
  readonly pageSize?: number;
  readonly totalEstimate?: number;
  readonly exactTotalEstimate?: boolean;
  readonly rows?: ReadonlyArray<ApiCatalogRow> | null;
  readonly facets?: Readonly<Record<string, ReadonlyArray<ApiCatalogFacetBucket>>> | null;
  readonly nextSearchAfter?: string | null;
  readonly warnings?: ReadonlyArray<string> | null;
};

export type ComponentsCatalogRequest = {
  readonly entityType: 'COMPONENT';
  readonly source: 'local' | 'catalog';
  readonly filters?: Readonly<Record<string, unknown>>;
  /** 1-based page index (catalog API). */
  readonly page?: number;
  readonly pageSize?: number;
  readonly searchAfter?: string;
  readonly includeFacets?: boolean;
};

export function buildComponentsCatalogRequest(params: {
  readonly tab?: ComponentsTab;
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly includeFacets?: boolean;
  readonly filters?: ComponentsListFilterState;
  readonly searchAfter?: string;
}): ComponentsCatalogRequest {
  const tab = params.tab ?? DEFAULT_COMPONENTS_TAB;
  const source = componentsTabToSource(tab);
  const filters = params.filters;
  const catalogFilters = {
    ...(params.search?.trim() ? { query: params.search.trim() } : {}),
    ...(filters
      ? componentsListFiltersToCatalogFilters(filters, {
          includeOrganizations: source === 'local',
        })
      : {}),
  };

  // Catalog API page is 1-based; callers pass 0-based like other Martha list pages.
  const page = Math.max(0, params.page) + 1;
  const searchAfter =
    source === 'local' && page > 1 && params.searchAfter?.trim()
      ? params.searchAfter.trim()
      : undefined;

  return {
    entityType: 'COMPONENT',
    source,
    page,
    pageSize: params.pageSize ?? COMPONENTS_LIST_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    ...(Object.keys(catalogFilters).length > 0 ? { filters: catalogFilters } : {}),
    ...(searchAfter ? { searchAfter } : {}),
  };
}

function asStringField(fields: Readonly<Record<string, unknown>> | null | undefined, key: string): string | undefined {
  const value = fields?.[key];
  if (typeof value === 'string' && value.trim()) return value.trim();
  return undefined;
}

export function mapCatalogComponentRow(row: ApiCatalogRow): ComponentListRow | null {
  const id = row.id?.trim();
  if (!id) return null;
  const source = row.source === 'catalog' ? 'catalog' : 'local';
  const fields = row.fields ?? undefined;
  const href = row.href?.trim() || undefined;
  return {
    id,
    name: row.title?.trim() || id,
    subtitle: row.subtitle?.trim() || undefined,
    ecosystem: asStringField(fields, 'ecosystem'),
    organization: asStringField(fields, 'organization'),
    source,
    ...(href ? { href } : {}),
  };
}

function facetEntriesFromBuckets(
  buckets: ReadonlyArray<ApiCatalogFacetBucket> | null | undefined,
): ReadonlyArray<ComponentsFilterFacetEntry> {
  if (!buckets?.length) return [];
  return buckets
    .filter((bucket) => typeof bucket.value === 'string' && bucket.value.trim().length > 0)
    .map((bucket) => {
      const id = bucket.value!.trim();
      return {
        id,
        // Catalog facet values are already friendly labels (org name / ecosystem id).
        label: id,
        count: typeof bucket.count === 'number' ? bucket.count : 0,
      };
    })
    .sort((left, right) => left.label.localeCompare(right.label));
}

export function mapCatalogFacets(
  response: ComponentsCatalogApiResponse,
  totalComponents: number,
): ComponentsFilterFacetCounts {
  const facets = response.facets ?? {};
  return {
    totalComponents,
    organizations: facetEntriesFromBuckets(facets.organization),
    ecosystems: facetEntriesFromBuckets(facets.ecosystem),
  };
}

export function mapComponentsCatalogResponse(response: ComponentsCatalogApiResponse): {
  readonly components: ReadonlyArray<ComponentListRow>;
  readonly facets: ComponentsFilterFacetCounts;
  readonly total: number;
  /** False when the backend capped/truncated the estimate (show as N+). */
  readonly exactTotalEstimate: boolean;
  /** 0-based page index for UI pagination. */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
  readonly nextSearchAfter: string | null;
  readonly catalogAvailable: boolean;
  readonly source: 'local' | 'catalog';
  readonly warnings: ReadonlyArray<string>;
} {
  const components = (response.rows ?? [])
    .map(mapCatalogComponentRow)
    .filter((row): row is ComponentListRow => row != null);
  const pageSize = response.pageSize ?? COMPONENTS_LIST_PAGE_SIZE;
  const apiPage = typeof response.page === 'number' && response.page >= 1 ? response.page : 1;
  const catalogAvailable = response.catalogAvailable !== false;
  const source = response.source === 'catalog' ? 'catalog' : 'local';
  // When catalog federation is down, totalEstimate is unknown — do not treat missing/zero as a real count.
  const total =
    source === 'catalog' && !catalogAvailable
      ? 0
      : typeof response.totalEstimate === 'number'
        ? response.totalEstimate
        : components.length;
  const exactTotalEstimate = response.exactTotalEstimate !== false;
  const nextSearchAfter = response.nextSearchAfter?.trim() || null;
  const hasNextPage =
    source === 'local'
      ? Boolean(nextSearchAfter)
      : !catalogAvailable
        ? false
        : exactTotalEstimate
          ? apiPage * pageSize < total
          : components.length >= pageSize || apiPage * pageSize < total;

  return {
    components,
    facets: mapCatalogFacets(response, total),
    total,
    exactTotalEstimate,
    page: apiPage - 1,
    pageSize,
    hasNextPage,
    nextSearchAfter,
    catalogAvailable,
    source,
    warnings: response.warnings ?? [],
  };
}

/** Mirrors backend {@code ComponentRiskDTO} for My Scan Data hybrid list. */
export type ApiComponentRiskRow = {
  readonly hash?: string | null;
  readonly scoreCritical?: number;
  readonly scoreSevere?: number;
  readonly scoreModerate?: number;
  readonly scoreLow?: number;
  readonly affectedApplications?: number;
  readonly derivedComponentName?: string | null;
  readonly filename?: string | null;
  readonly displayName?: {
    readonly name?: string | null;
    readonly toString?: unknown;
  } | null;
};

/** Mirrors backend {@code ComponentsListResponseDTO}. */
export type ComponentsDashboardApiResponse = {
  readonly components?: ReadonlyArray<ApiComponentRiskRow> | null;
  readonly facets?: {
    readonly totalComponents?: number;
    readonly organizations?: Readonly<Record<string, number>> | null;
    readonly applications?: Readonly<Record<string, number>> | null;
    readonly stages?: Readonly<Record<string, number>> | null;
    readonly organizationNames?: Readonly<Record<string, string>> | null;
    readonly applicationNames?: Readonly<Record<string, string>> | null;
  } | null;
  readonly total?: number;
  readonly page?: number;
  readonly pageSize?: number;
  readonly hasNextPage?: boolean;
  readonly source?: string;
};

export type ComponentsDashboardRequest = {
  readonly page: number;
  readonly pageSize: number;
  readonly includeFacets: boolean;
  readonly search?: string;
  readonly organizationIds?: ReadonlyArray<string>;
};

export function buildComponentsDashboardRequest(params: {
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly includeFacets?: boolean;
  readonly filters?: ComponentsListFilterState;
}): ComponentsDashboardRequest {
  const organizationIds =
    params.filters && params.filters.organizations.size > 0
      ? Array.from(params.filters.organizations).sort()
      : undefined;
  return {
    page: Math.max(0, params.page),
    pageSize: params.pageSize ?? COMPONENTS_LIST_PAGE_SIZE,
    includeFacets: params.includeFacets ?? true,
    ...(params.search?.trim() ? { search: params.search.trim() } : {}),
    ...(organizationIds ? { organizationIds } : {}),
  };
}

function componentRiskDisplayName(row: ApiComponentRiskRow): string {
  const derived = row.derivedComponentName?.trim();
  if (derived) return derived;
  const named = row.displayName?.name?.trim();
  if (named) return named;
  const filename = row.filename?.trim();
  if (filename) return filename;
  return row.hash?.trim() || 'Unknown component';
}

export function mapComponentRiskRow(row: ApiComponentRiskRow): ComponentListRow | null {
  const id = row.hash?.trim();
  if (!id) return null;
  // Preserve numeric zeros from SQL enrich (Applications-parity badge chrome). Only omit fields when
  // the API left them absent so catalog/stub rows do not invent risk chrome.
  return {
    id,
    name: componentRiskDisplayName(row),
    subtitle: id,
    source: 'local',
    ...(row.scoreCritical != null ? { scoreCritical: row.scoreCritical } : {}),
    ...(row.scoreSevere != null ? { scoreSevere: row.scoreSevere } : {}),
    ...(row.scoreModerate != null ? { scoreModerate: row.scoreModerate } : {}),
    ...(row.scoreLow != null ? { scoreLow: row.scoreLow } : {}),
    ...(row.affectedApplications != null ? { affectedApplications: row.affectedApplications } : {}),
  };
}

function facetEntriesFromCountMap(
  counts: Readonly<Record<string, number>> | null | undefined,
  names?: Readonly<Record<string, string>> | null,
): ReadonlyArray<ComponentsFilterFacetEntry> {
  if (!counts) return [];
  return Object.entries(counts)
    .filter(([id, count]) => id.trim().length > 0 && typeof count === 'number')
    .map(([id, count]) => {
      const trimmed = id.trim();
      return {
        id: trimmed,
        label: names?.[trimmed]?.trim() || trimmed,
        count,
      };
    })
    .sort((left, right) => left.label.localeCompare(right.label));
}

export function mapComponentsDashboardResponse(response: ComponentsDashboardApiResponse): {
  readonly components: ReadonlyArray<ComponentListRow>;
  readonly facets: ComponentsFilterFacetCounts;
  readonly total: number;
  readonly exactTotalEstimate: boolean;
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
  readonly nextSearchAfter: string | null;
  readonly catalogAvailable: boolean;
  readonly source: 'local' | 'catalog';
  readonly warnings: ReadonlyArray<string>;
} {
  const components = (response.components ?? [])
    .map(mapComponentRiskRow)
    .filter((row): row is ComponentListRow => row != null);
  const pageSize = response.pageSize ?? COMPONENTS_LIST_PAGE_SIZE;
  const page = typeof response.page === 'number' && response.page >= 0 ? response.page : 0;
  const total = typeof response.total === 'number' ? response.total : components.length;
  const facets = response.facets;
  return {
    components,
    facets: {
      totalComponents: typeof facets?.totalComponents === 'number' ? facets.totalComponents : total,
      organizations: facetEntriesFromCountMap(facets?.organizations, facets?.organizationNames),
      // My Scan Data dashboard list has no ecosystem facet buckets yet (Catalog keeps them).
      ecosystems: [],
    },
    total,
    exactTotalEstimate: true,
    page,
    pageSize,
    hasNextPage: Boolean(response.hasNextPage),
    nextSearchAfter: null,
    catalogAvailable: true,
    source: 'local',
    warnings: [],
  };
}
