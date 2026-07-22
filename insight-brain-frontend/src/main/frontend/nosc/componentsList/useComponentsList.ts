/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import type { AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import {
  COMPONENTS_LIST_PAGE_SIZE,
  ComponentsCatalogApiResponse,
  buildComponentsCatalogRequest,
  mapComponentsCatalogResponse,
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
import { getSearchCatalogUrl } from 'MainRoot/util/CLMLocation';

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

/**
 * Martha V1 Components list data hook.
 * Fetches POST /rest/search/catalog with {@code entityType: COMPONENT} and source local|catalog.
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
  /** 1-based page → searchAfter cursor for local source paging. */
  const [searchAfterByPage, setSearchAfterByPage] = useState<Readonly<Record<number, string>>>({});
  const queryRef = useRef({ tab, search, filters });
  queryRef.current = { tab, search, filters };

  const clearCursors = useCallback(() => {
    setSearchAfterByPage({});
  }, []);

  // Local source is cursor-only: deep links / bookmarks to page > 1 without a stored cursor
  // cannot be honored — clamp to page 0 until the user walks forward from the first page.
  const effectivePage =
    tab === 'myScanData' && page > 0 && !searchAfterByPage[page + 1] ? 0 : page;

  useEffect(() => {
    if (effectivePage !== page) {
      setPage(effectivePage);
    }
  }, [effectivePage, page]);

  const requestBody = useMemo(
    () =>
      buildComponentsCatalogRequest({
        tab,
        page: effectivePage,
        pageSize,
        includeFacets,
        search,
        filters,
        searchAfter: searchAfterByPage[effectivePage + 1],
      }),
    [tab, effectivePage, pageSize, includeFacets, search, filters, searchAfterByPage],
  );

  const { status, data, error, retry } = useTile<ComponentsCatalogApiResponse>(
    getSearchCatalogUrl(),
    undefined,
    {
      method: 'post',
      body: requestBody,
      mapErrorStatus: (statusCode) => (statusCode === 409 ? 'not-ready' : 'error'),
      enabled,
    },
  );

  const mapped = useMemo(
    () => (data ? mapComponentsCatalogResponse(data) : null),
    [data],
  );

  useEffect(() => {
    if (!mapped?.nextSearchAfter) return;
    const nextPageOneBased = mapped.page + 2;
    setSearchAfterByPage((current) => {
      if (current[nextPageOneBased] === mapped.nextSearchAfter) return current;
      return { ...current, [nextPageOneBased]: mapped.nextSearchAfter! };
    });
  }, [mapped?.nextSearchAfter, mapped?.page]);

  // Prefer local page while a newer request is in flight. Trusting mapped.page from a stale
  // response was snapping URL/pagination back after filter/search resets (Applications pattern).
  const resolvedPage = useMemo(() => {
    if (!mapped) return page;
    if (tab === 'catalog' && mapped.catalogAvailable) {
      const maxPage =
        mapped.total <= 0 ? 0 : Math.max(0, Math.ceil(mapped.total / mapped.pageSize) - 1);
      if (!mapped.hasNextPage && page > maxPage) {
        return maxPage;
      }
    }
    return page;
  }, [mapped, page, tab]);

  useEffect(() => {
    if (resolvedPage !== page) {
      setPage(resolvedPage);
    }
  }, [resolvedPage, page]);

  const goToPage = useCallback((nextPage: number) => {
    setPage(Math.max(0, nextPage));
  }, []);

  const setTab = useCallback((nextTab: ComponentsTab) => {
    setTabState(nextTab);
    // Organizations are local-only; drop them when switching to Catalog so active-filter state matches the rail.
    if (nextTab === 'catalog') {
      setFilters((current) =>
        current.organizations.size === 0
          ? current
          : { ...current, organizations: new Set<string>() },
      );
    }
    setPage(0);
    clearCursors();
  }, [clearCursors]);

  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(0);
    clearCursors();
  }, [clearCursors]);

  const toggleFilter = useCallback((group: ComponentsFilterSetGroup, id: string) => {
    setFilters((current) => toggleComponentsListFilterId(current, group, id));
    setPage(0);
    clearCursors();
  }, [clearCursors]);

  const resetFilters = useCallback(() => {
    setFilters(EMPTY_COMPONENTS_LIST_FILTERS);
    setPage(0);
    clearCursors();
  }, [clearCursors]);

  const syncQueryState = useCallback((state: ComponentsListQueryState) => {
    const prev = queryRef.current;
    const queryChanged =
      prev.tab !== state.tab
      || prev.search !== state.search
      || !filtersEqual(prev.filters, state.filters);
    if (queryChanged) {
      clearCursors();
    }
    setTabState((current) => (current === state.tab ? current : state.tab));
    setSearch((current) => (current === state.search ? current : state.search));
    setPage((current) => (current === state.page ? current : state.page));
    setFilters((current) => (filtersEqual(current, state.filters) ? current : state.filters));
  }, [clearCursors]);

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
