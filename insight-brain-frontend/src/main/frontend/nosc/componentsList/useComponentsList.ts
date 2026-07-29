/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import type { AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import {
  COMPONENTS_LIST_PAGE_SIZE,
  ComponentsCatalogApiResponse,
  ComponentsDashboardApiResponse,
  buildComponentsCatalogRequest,
  buildComponentsDashboardRequest,
  mapComponentsCatalogResponse,
  mapComponentsDashboardResponse,
} from 'MainRoot/nosc/componentsList/componentsListApi';
import {
  ComponentListRow,
  ComponentsFilterFacetCounts,
} from 'MainRoot/nosc/componentsList/componentListTypes';
import {
  ComponentsFilterSetGroup,
  ComponentsListFilterState,
  EMPTY_COMPONENTS_LIST_FILTERS,
  filtersEqual,
  hasActiveComponentsListFilters,
  toggleComponentsListFilterId,
} from 'MainRoot/nosc/componentsList/componentsListFilters';
import {
  ComponentsTab,
  DEFAULT_COMPONENTS_TAB,
} from 'MainRoot/nosc/componentsList/componentsRoute';
import type { ComponentsListQueryState } from 'MainRoot/nosc/componentsList/componentsListQuery';
import { getComponentsListUrl, getSearchCatalogUrl } from 'MainRoot/util/CLMLocation';

export type { ComponentsListQueryState };

export interface UseComponentsListResult {
  readonly tab: ComponentsTab;
  readonly components: ReadonlyArray<ComponentListRow>;
  readonly facets: ComponentsFilterFacetCounts;
  readonly filters: ComponentsListFilterState;
  readonly hasActiveFilters: boolean;
  readonly search: string;
  readonly loading: boolean;
  readonly error: string | null;
  readonly info: AsyncPageStateInfoProps | null;
  readonly retry: () => void;
  readonly total: number;
  readonly exactTotalEstimate: boolean;
  /** 0-based page index (API contract for UI). */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
  readonly catalogAvailable: boolean;
  readonly setPage: (page: number) => void;
  readonly setTab: (tab: ComponentsTab) => void;
  readonly submitSearch: (term: string) => void;
  readonly toggleFilter: (group: ComponentsFilterSetGroup, id: string) => void;
  readonly resetFilters: () => void;
  readonly syncQueryState: (state: ComponentsListQueryState) => void;
}

const EMPTY_FACETS: ComponentsFilterFacetCounts = {
  totalComponents: 0,
  organizations: [],
  ecosystems: [],
};

export const COMPONENTS_INDEX_NOT_READY_MESSAGE =
  'The search index is still building. Please try again shortly.';

export const COMPONENTS_CATALOG_UNAVAILABLE_MESSAGE =
  'Sonatype Catalog is temporarily unavailable. Try My Scan Data, or retry shortly.';

export interface UseComponentsListOptions {
  readonly pageSize?: number;
  readonly includeFacets?: boolean;
  /** Seed list state from the route so the first fetch matches deep-linked hash params. */
  readonly initialState?: ComponentsListQueryState;
  /** When false, defers the list POST until route state is hydrated (default true). */
  readonly enabled?: boolean;
}

type MappedComponentsList = ReturnType<typeof mapComponentsCatalogResponse>;

/** Matches ComponentsListDistinctPageFetcher.MAX_DISTINCT_PAGE on the dashboard list API. */
export const COMPONENTS_DASHBOARD_MAX_PAGE = 200;

/**
 * Martha V1 Components list data hook.
 * My Scan Data → POST /rest/dashboard/components/list (index + SQL risk enrich).
 * Sonatype Catalog → POST /rest/search/catalog with {@code source: catalog}.
 */
export function useComponentsList(
  options: UseComponentsListOptions = {},
): UseComponentsListResult {
  const {
    pageSize = COMPONENTS_LIST_PAGE_SIZE,
    includeFacets = true,
    initialState,
    enabled = true,
  } = options;
  const [tab, setTabState] = useState<ComponentsTab>(() => initialState?.tab ?? DEFAULT_COMPONENTS_TAB);
  const [page, setPage] = useState(() => initialState?.page ?? 0);
  const [search, setSearch] = useState(() => initialState?.search ?? '');
  const [filters, setFilters] = useState<ComponentsListFilterState>(
    () => initialState?.filters ?? EMPTY_COMPONENTS_LIST_FILTERS,
  );

  const isCatalog = tab === 'catalog';
  const listUrl = isCatalog ? getSearchCatalogUrl() : getComponentsListUrl();
  const requestPage = isCatalog ? page : Math.min(page, COMPONENTS_DASHBOARD_MAX_PAGE);

  const requestBody = useMemo(
    () =>
      isCatalog
        ? buildComponentsCatalogRequest({
            tab,
            page,
            pageSize,
            includeFacets,
            search,
            filters,
          })
        : buildComponentsDashboardRequest({
            page: requestPage,
            pageSize,
            includeFacets,
            search,
            filters,
          }),
    [isCatalog, tab, page, requestPage, pageSize, includeFacets, search, filters],
  );

  const { status, data, error, retry } = useTile<
    ComponentsCatalogApiResponse | ComponentsDashboardApiResponse
  >(listUrl, undefined, {
    method: 'post',
    body: requestBody,
    mapErrorStatus: (statusCode) => (statusCode === 409 ? 'not-ready' : 'error'),
    enabled,
  });

  const mapped = useMemo((): MappedComponentsList | null => {
    if (!data) return null;
    if (isCatalog) {
      return mapComponentsCatalogResponse(data as ComponentsCatalogApiResponse);
    }
    return mapComponentsDashboardResponse(data as ComponentsDashboardApiResponse);
  }, [data, isCatalog]);

  const resolvedPage = useMemo(() => {
    if (!mapped) {
      return isCatalog ? page : Math.min(page, COMPONENTS_DASHBOARD_MAX_PAGE);
    }
    if (isCatalog && mapped.catalogAvailable) {
      const maxPage =
        mapped.total <= 0 ? 0 : Math.max(0, Math.ceil(mapped.total / mapped.pageSize) - 1);
      if (!mapped.hasNextPage && page > maxPage) {
        return maxPage;
      }
      return page;
    }
    // My Scan Data: clamp deep pages to the dashboard API estate-scale guard.
    const maxPage = Math.min(
      COMPONENTS_DASHBOARD_MAX_PAGE,
      mapped.total <= 0 ? 0 : Math.max(0, Math.ceil(mapped.total / mapped.pageSize) - 1),
    );
    if (!mapped.hasNextPage && page > maxPage) {
      return maxPage;
    }
    return Math.min(page, COMPONENTS_DASHBOARD_MAX_PAGE);
  }, [mapped, page, isCatalog]);

  useEffect(() => {
    if (resolvedPage !== page) {
      setPage(resolvedPage);
    }
  }, [resolvedPage, page]);

  const goToPage = useCallback((nextPage: number) => {
    setPage(Math.max(0, Math.min(nextPage, COMPONENTS_DASHBOARD_MAX_PAGE)));
  }, []);

  const setTab = useCallback((nextTab: ComponentsTab) => {
    setTabState(nextTab);
    if (nextTab === 'catalog') {
      // Organizations are My Scan Data–only; drop them when switching to Catalog.
      setFilters((current) =>
        current.organizations.size === 0
          ? current
          : { ...current, organizations: new Set<string>() },
      );
    }
    else if (nextTab === 'myScanData') {
      // Ecosystem facets are Catalog-only; drop silent selections on My Scan Data.
      setFilters((current) =>
        current.ecosystems.size === 0
          ? current
          : { ...current, ecosystems: new Set<string>() },
      );
    }
    setPage(0);
  }, []);

  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(0);
  }, []);

  const toggleFilter = useCallback((group: ComponentsFilterSetGroup, id: string) => {
    setFilters((current) => toggleComponentsListFilterId(current, group, id));
    setPage(0);
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(EMPTY_COMPONENTS_LIST_FILTERS);
    setPage(0);
  }, []);

  const syncQueryState = useCallback((state: ComponentsListQueryState) => {
    setTabState((current) => (current === state.tab ? current : state.tab));
    setSearch((current) => (current === state.search ? current : state.search));
    setPage((current) => (current === state.page ? current : state.page));
    setFilters((current) => (filtersEqual(current, state.filters) ? current : state.filters));
  }, []);

  const info: AsyncPageStateInfoProps | null =
    status === 'not-ready'
      ? {
          title: 'Search index building',
          message: COMPONENTS_INDEX_NOT_READY_MESSAGE,
          testId: 'components-list-not-ready',
        }
      : mapped && mapped.source === 'catalog' && !mapped.catalogAvailable
        ? {
            title: 'Catalog unavailable',
            message: COMPONENTS_CATALOG_UNAVAILABLE_MESSAGE,
            testId: 'components-list-catalog-unavailable',
          }
        : null;

  return {
    tab,
    components: mapped?.components ?? [],
    facets: mapped?.facets ?? EMPTY_FACETS,
    filters,
    hasActiveFilters: hasActiveComponentsListFilters(filters),
    search,
    loading: status === 'loading',
    error: status === 'error' ? (error?.message ?? null) : null,
    info,
    retry,
    total: mapped?.total ?? 0,
    exactTotalEstimate: mapped?.exactTotalEstimate ?? true,
    page: resolvedPage,
    pageSize: mapped?.pageSize ?? pageSize,
    hasNextPage: mapped?.hasNextPage ?? false,
    catalogAvailable: mapped?.catalogAvailable ?? true,
    setPage: goToPage,
    setTab,
    submitSearch,
    toggleFilter,
    resetFilters,
    syncQueryState,
  };
}
