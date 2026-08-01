/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import axios from 'axios';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import type { AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import { getIndexQueryUrl } from 'MainRoot/util/CLMLocation';
import {
  AnaWaiverRow,
  WaiversFilterFacetCounts,
} from 'MainRoot/nosc/waivers/waiversListTypes';
import {
  EMPTY_WAIVERS_LIST_FILTERS,
  WaiversFilterSetGroup,
  WaiversListFilterState,
  filtersEqual,
  hasActiveWaiversListFilters,
  toggleWaiversListFilterId,
} from 'MainRoot/nosc/waivers/waiversListFilters';
import {
  DEFAULT_WAIVERS_LIST_ORDER_BY,
  WaiversListOrderBy,
  WaiversListQueryState,
  buildWaiversListRouteParams,
} from 'MainRoot/nosc/waivers/waiversListQuery';
import {
  WAIVERS_LIST_PAGE_SIZE,
  WaiversIndexQueryResponse,
  buildWaiversIndexQueryRequest,
  mapWaiversIndexQueryResponse,
} from 'MainRoot/nosc/waivers/waiversListApi';

export const WAIVERS_INDEX_NOT_READY_MESSAGE =
  'The search index is still building. Please try again shortly.';

export const WAIVERS_INDEX_FORBIDDEN_MESSAGE =
  'No waivers are visible with your current permissions.';

/** GLOBAL_SEARCH off (or index-query route unavailable) — page hard-depends on Ana after cutover. */
export const WAIVERS_INDEX_UNAVAILABLE_MESSAGE =
  'Waiver search is not available in this environment. Ask an administrator to enable Global Search.';

const EMPTY_FACETS: WaiversFilterFacetCounts = {
  totalWaivers: 0,
  threatLevels: [],
  expiryStatuses: [],
  autoStatuses: [],
  organizations: [],
  applications: [],
  policies: [],
};

/**
 * Module-level cursor cache keyed by the URL-normalized query (search/sort/filters).
 * UI-Router remounts the list when hash query params change unless those params are marked
 * dynamic; even with dynamic params, this cache keeps Next working across an accidental remount.
 * Bounded LRU (insertion-order Map) so a long session cannot grow without bound.
 */
const MAX_CURSOR_CACHE_ENTRIES = 8;
const searchAfterCacheByQuery = new Map<string, Readonly<Record<number, string>>>();

function putSearchAfterCache(
  key: string,
  value: Readonly<Record<number, string>>,
): void {
  if (searchAfterCacheByQuery.has(key)) {
    searchAfterCacheByQuery.delete(key);
  }
  searchAfterCacheByQuery.set(key, value);
  while (searchAfterCacheByQuery.size > MAX_CURSOR_CACHE_ENTRIES) {
    const oldest = searchAfterCacheByQuery.keys().next().value;
    if (oldest === undefined) break;
    searchAfterCacheByQuery.delete(oldest);
  }
}

/** Clears the module cursor cache between Jest cases (not for production callers). */
export function _clearWaiversCursorCacheForTesting(): void {
  searchAfterCacheByQuery.clear();
}

function httpStatusOf(err: unknown): number | undefined {
  if (axios.isAxiosError(err)) return err.response?.status;
  return undefined;
}

function waiversQueryCacheKey(
  search: string,
  orderBy: WaiversListOrderBy,
  filters: WaiversListFilterState,
): string {
  return JSON.stringify(
    buildWaiversListRouteParams({
      search,
      orderBy,
      page: 1,
      filters,
    }),
  );
}

export interface UseAnaWaiversListResult {
  readonly waivers: ReadonlyArray<AnaWaiverRow>;
  readonly facets: WaiversFilterFacetCounts;
  readonly filters: WaiversListFilterState;
  readonly hasActiveFilters: boolean;
  readonly search: string;
  readonly orderBy: WaiversListOrderBy;
  readonly loading: boolean;
  readonly error: string | null;
  readonly info: AsyncPageStateInfoProps | null;
  readonly retry: () => void;
  readonly total: number;
  readonly exactTotalEstimate: boolean;
  /** 1-based page index — matches the index-query contract. */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
  readonly warnings: ReadonlyArray<string>;
  readonly setPage: (page: number) => void;
  readonly submitSearch: (term: string) => void;
  readonly changeOrderBy: (orderBy: WaiversListOrderBy) => void;
  readonly toggleFilter: (group: WaiversFilterSetGroup, id: string) => void;
  readonly resetFilters: () => void;
  readonly syncQueryState: (state: WaiversListQueryState) => void;
}

export interface UseAnaWaiversListOptions {
  readonly pageSize?: number;
  readonly includeFacets?: boolean;
  readonly initialState?: WaiversListQueryState;
  /** Defer the POST until the container hydrates route params (default true). */
  readonly enabled?: boolean;
}

/**
 * Ana IQ-index waivers list hook (CLM-43204). Fetches
 * {@code POST /rest/search/index-query} with {@code entityType: WAIVER}. WAIVER
 * pagination is cursor-based — the hook records the {@code nextSearchAfter} from
 * each response and only advances when a cached cursor for the target page exists.
 */
export function useAnaWaiversList(
  options: UseAnaWaiversListOptions = {},
): UseAnaWaiversListResult {
  const {
    pageSize = WAIVERS_LIST_PAGE_SIZE,
    includeFacets = true,
    initialState,
    enabled = true,
  } = options;

  const [page, setPage] = useState<number>(() => Math.max(1, initialState?.page ?? 1));
  const [search, setSearch] = useState(() => initialState?.search ?? '');
  const [orderBy, setOrderBy] = useState<WaiversListOrderBy>(
    () => initialState?.orderBy ?? DEFAULT_WAIVERS_LIST_ORDER_BY,
  );
  const [filters, setFilters] = useState<WaiversListFilterState>(
    () => initialState?.filters ?? EMPTY_WAIVERS_LIST_FILTERS,
  );
  const initialQueryKey = waiversQueryCacheKey(
    initialState?.search ?? '',
    initialState?.orderBy ?? DEFAULT_WAIVERS_LIST_ORDER_BY,
    initialState?.filters ?? EMPTY_WAIVERS_LIST_FILTERS,
  );
  /** 1-based page → searchAfter cursor. Hydrated from the module cache so a route remount
   * (non-dynamic page param) does not forget the cursor needed for page 2+. */
  const [searchAfterByPage, setSearchAfterByPage] = useState<Readonly<Record<number, string>>>(
    () => searchAfterCacheByQuery.get(initialQueryKey) ?? {},
  );
  const queryRef = useRef({ search, orderBy, filters });
  queryRef.current = { search, orderBy, filters };
  const clearCursors = useCallback(() => {
    const key = waiversQueryCacheKey(
      queryRef.current.search,
      queryRef.current.orderBy,
      queryRef.current.filters,
    );
    searchAfterCacheByQuery.delete(key);
    setSearchAfterByPage({});
  }, []);

  // Deep links to page > 1 without a stored cursor clamp back to page 1 so the request cannot
  // fail with DEEP_PAGINATION_NOT_SUPPORTED; the user then walks forward from page 1. Same
  // trade-off componentsList makes for the cursor-only local source.
  const effectivePage = page > 1 && !searchAfterByPage[page] ? 1 : page;
  useEffect(() => {
    if (effectivePage !== page) {
      setPage(effectivePage);
    }
  }, [effectivePage, page]);

  const requestBody = useMemo(
    () =>
      buildWaiversIndexQueryRequest({
        page: effectivePage,
        pageSize,
        includeFacets,
        search,
        sort: orderBy,
        filters,
        searchAfter: searchAfterByPage[effectivePage],
      }),
    [effectivePage, pageSize, includeFacets, search, orderBy, filters, searchAfterByPage],
  );

  const { status, data, error, retry } = useTile<WaiversIndexQueryResponse>(
    getIndexQueryUrl(),
    undefined,
    {
      method: 'post',
      body: requestBody,
      // 409 = index building; 403 = no readable context; 404 = GLOBAL_SEARCH off / route absent.
      // All three are expected product states after the Ana cutover — surface as info, not a crash.
      mapErrorStatus: (statusCode) => (
        statusCode === 409 || statusCode === 403 || statusCode === 404 ? 'not-ready' : 'error'
      ),
      enabled,
    },
  );

  const mapped = useMemo(() => (data ? mapWaiversIndexQueryResponse(data) : null), [data]);

  // Store the cursor in layout (and the module cache synchronously) so a UI-Router remount
  // on page= can rehydrate the cursor before the deep-link clamp runs. Key off queryRef so a
  // search/sort/filter change that arrives before mapped updates does not write under a stale key.
  useLayoutEffect(() => {
    if (!mapped?.nextSearchAfter) return;
    const nextPage = mapped.page + 1;
    const { search: s, orderBy: o, filters: f } = queryRef.current;
    const key = waiversQueryCacheKey(s, o, f);
    setSearchAfterByPage((current) => {
      if (current[nextPage] === mapped.nextSearchAfter) {
        putSearchAfterCache(key, current);
        return current;
      }
      const next = { ...current, [nextPage]: mapped.nextSearchAfter! };
      putSearchAfterCache(key, next);
      return next;
    });
  }, [mapped?.nextSearchAfter, mapped?.page]);

  const goToPage = useCallback((nextPage: number) => {
    setPage(Math.max(1, nextPage));
  }, []);

  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(1);
    clearCursors();
  }, [clearCursors]);

  const changeOrderBy = useCallback((nextOrderBy: WaiversListOrderBy) => {
    setOrderBy(nextOrderBy);
    setPage(1);
    clearCursors();
  }, [clearCursors]);

  const toggleFilter = useCallback((group: WaiversFilterSetGroup, id: string) => {
    setFilters((current) => {
      const next = toggleWaiversListFilterId(current, group, id);
      if (next === current) return current;
      setPage(1);
      clearCursors();
      return next;
    });
  }, [clearCursors]);

  const resetFilters = useCallback(() => {
    setFilters(EMPTY_WAIVERS_LIST_FILTERS);
    setPage(1);
    clearCursors();
  }, [clearCursors]);

  const syncQueryState = useCallback((state: WaiversListQueryState) => {
    const prev = queryRef.current;
    const queryChanged =
      prev.search !== state.search
      || prev.orderBy !== state.orderBy
      || !filtersEqual(prev.filters, state.filters);
    if (queryChanged) {
      // Drop in-memory cursors for the outgoing query, then rehydrate from the module cache for
      // the inbound query so a remount mid-pagination can resume without another page-1 walk.
      const oldKey = waiversQueryCacheKey(prev.search, prev.orderBy, prev.filters);
      searchAfterCacheByQuery.delete(oldKey);
      const newKey = waiversQueryCacheKey(state.search, state.orderBy, state.filters);
      setSearchAfterByPage(searchAfterCacheByQuery.get(newKey) ?? {});
    }
    setSearch((current) => (current === state.search ? current : state.search));
    setOrderBy((current) => (current === state.orderBy ? current : state.orderBy));
    setPage((current) => (current === state.page ? current : Math.max(1, state.page)));
    setFilters((current) => (filtersEqual(current, state.filters) ? current : state.filters));
  }, []);

  const info: AsyncPageStateInfoProps | null = (() => {
    if (status !== 'not-ready') return null;
    const code = httpStatusOf(error);
    if (code === 403) {
      return {
        title: 'No waivers visible',
        message: WAIVERS_INDEX_FORBIDDEN_MESSAGE,
        testId: 'waivers-list-forbidden',
      };
    }
    if (code === 404) {
      return {
        title: 'Waiver search unavailable',
        message: WAIVERS_INDEX_UNAVAILABLE_MESSAGE,
        testId: 'waivers-list-unavailable',
      };
    }
    return {
      title: 'Search index building',
      message: WAIVERS_INDEX_NOT_READY_MESSAGE,
      testId: 'waivers-list-not-ready',
    };
  })();

  return {
    waivers: mapped?.waivers ?? [],
    facets: mapped?.facets ?? EMPTY_FACETS,
    filters,
    hasActiveFilters: hasActiveWaiversListFilters(filters),
    search,
    orderBy,
    loading: status === 'loading',
    error: status === 'error' ? (error?.message ?? null) : null,
    info,
    retry,
    total: mapped?.total ?? 0,
    exactTotalEstimate: mapped?.exactTotalEstimate ?? true,
    // effectivePage is the source of truth when a deep link asks for page>1 without a cursor
    // (clamped to 1) or when a stale later-page response arrives after a filter reset.
    page: effectivePage,
    pageSize: mapped?.pageSize ?? pageSize,
    hasNextPage: mapped?.hasNextPage ?? false,
    warnings: mapped?.warnings ?? [],
    setPage: goToPage,
    submitSearch,
    changeOrderBy,
    toggleFilter,
    resetFilters,
    syncQueryState,
  };
}
