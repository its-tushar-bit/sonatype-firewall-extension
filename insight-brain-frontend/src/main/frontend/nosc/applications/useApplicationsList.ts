/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import type { AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import {
  APPLICATIONS_LIST_PAGE_SIZE,
  ApplicationsListApiResponse,
  ApplicationsListOrderBy,
  mapApplicationsListResponse,
} from 'MainRoot/nosc/applications/applicationsListApi';
import {
  APPLICATIONS_POLICY_TYPES,
  APPLICATIONS_VIOLATION_STATES,
  ApplicationRiskScore,
  ApplicationsFilterFacetCounts,
  zeroCountFacetEntries,
} from 'MainRoot/nosc/applications/applicationListTypes';
import {
  ApplicationsListFilterSetField,
  ApplicationsListFilterState,
  ApplicationsThreatRange,
  EMPTY_APPLICATIONS_LIST_FILTERS,
  applicationsListFiltersToRequest,
  hasActiveApplicationsListFilters,
  normalizeApplicationsThreatRange,
  toggleApplicationsListFilterId,
  type ApplicationsAgeInDays,
} from 'MainRoot/nosc/applications/applicationsListFilters';
import { DEFAULT_APPLICATIONS_LIST_ORDER_BY, filtersEqual } from 'MainRoot/nosc/applications/applicationsListQuery';
import { getApplicationsListUrl } from 'MainRoot/util/CLMLocation';

export interface ApplicationsListQueryState {
  readonly search: string;
  readonly orderBy: ApplicationsListOrderBy;
  /** 0-based page index for the list API. */
  readonly page: number;
  readonly filters: ApplicationsListFilterState;
}

export interface UseApplicationsListResult {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
  readonly facets: ApplicationsFilterFacetCounts;
  readonly filters: ApplicationsListFilterState;
  readonly hasActiveFilters: boolean;
  readonly search: string;
  readonly orderBy: ApplicationsListOrderBy;
  readonly loading: boolean;
  readonly error: string | null;
  readonly info: AsyncPageStateInfoProps | null;
  readonly retry: () => void;
  readonly total: number;
  /** 0-based page index (API contract). */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage: boolean;
  readonly setPage: (page: number) => void;
  readonly submitSearch: (term: string) => void;
  readonly changeOrderBy: (orderBy: ApplicationsListOrderBy) => void;
  readonly toggleFilter: (field: ApplicationsListFilterSetField, id: string) => void;
  readonly setThreatRange: (range: ApplicationsThreatRange) => void;
  readonly setAgeInDays: (ageInDays: ApplicationsAgeInDays | undefined) => void;
  readonly resetFilters: () => void;
  readonly syncQueryState: (state: {
    readonly search: string;
    readonly orderBy: ApplicationsListOrderBy;
    readonly page: number;
    readonly filters: ApplicationsListFilterState;
  }) => void;
}

const EMPTY_FACETS: ApplicationsFilterFacetCounts = {
  totalApplications: 0,
  stages: [],
  organizations: [],
  applications: [],
  policyTypes: zeroCountFacetEntries(APPLICATIONS_POLICY_TYPES),
  violationStates: zeroCountFacetEntries(APPLICATIONS_VIOLATION_STATES),
};

export const APPLICATIONS_INDEX_NOT_READY_MESSAGE = 'The search index is still building. Please try again shortly.';

export interface UseApplicationsListOptions {
  readonly pageSize?: number;
  readonly includeFacets?: boolean;
  /** Seed list state from the route so the first fetch matches deep-linked hash params. */
  readonly initialState?: ApplicationsListQueryState;
  /** When false, defers the list POST until route state is hydrated (default true). */
  readonly enabled?: boolean;
}

/**
 * Martha V1 Applications list data hook.
 * Fetches POST /rest/dashboard/applications/list with server pagination, search, sort, and filters.
 */
export function useApplicationsList(options: UseApplicationsListOptions = {}): UseApplicationsListResult {
  const { pageSize = APPLICATIONS_LIST_PAGE_SIZE, includeFacets = true, initialState, enabled = true } = options;
  const [page, setPage] = useState(() => initialState?.page ?? 0);
  const [search, setSearch] = useState(() => initialState?.search ?? '');
  const [orderBy, setOrderBy] = useState<ApplicationsListOrderBy>(
    () => initialState?.orderBy ?? DEFAULT_APPLICATIONS_LIST_ORDER_BY
  );
  const [filters, setFilters] = useState<ApplicationsListFilterState>(
    () => initialState?.filters ?? EMPTY_APPLICATIONS_LIST_FILTERS
  );
  // Per mount: first request omits facet aggregations so the card list paints, then a follow-up
  // fills the filter rail. Later filter/search/page changes keep includeFacets true. Navigating
  // away and back remounts the hook and repeats the two-step paint.
  const [facetsReady, setFacetsReady] = useState(false);

  const requestBody = useMemo(
    () => ({
      page,
      pageSize,
      includeFacets: includeFacets && facetsReady,
      orderBy,
      ...(search.trim() ? { search: search.trim() } : {}),
      ...applicationsListFiltersToRequest(filters),
    }),
    [page, pageSize, includeFacets, facetsReady, search, orderBy, filters]
  );

  const { status, data, error, retry } = useTile<ApplicationsListApiResponse>(getApplicationsListUrl(), undefined, {
    method: 'post',
    body: requestBody,
    mapErrorStatus: (statusCode) => (statusCode === 409 ? 'not-ready' : 'error'),
    enabled,
  });

  const mapped = useMemo(() => (data ? mapApplicationsListResponse(data) : null), [data]);

  useEffect(() => {
    if (includeFacets && mapped && !facetsReady) {
      setFacetsReady(true);
    }
  }, [includeFacets, mapped, facetsReady]);

  // Stale-while-revalidate: a filter/search/page change refetches server-side, and useTile
  // may clear `data` while the request is in flight. Retaining the last rows during that window
  // keeps the card list on screen instead of blanking to the loading skeleton and back (the "flash").
  // Only the very first load (no prior data) shows the loading state.
  const lastGoodData = useRef<typeof mapped>(null);
  useEffect(() => {
    if (mapped) lastGoodData.current = mapped;
  }, [mapped]);
  const effectiveData = mapped ?? lastGoodData.current;

  // Preserve facets during loading to prevent filter rail from flashing empty.
  const [cachedFacets, setCachedFacets] = useState<ApplicationsFilterFacetCounts>(EMPTY_FACETS);

  // Intentional "keep last known facets" contract: we only overwrite the cache when a response
  // carries facets (page 1 / cache-miss). Page 2+ responses omit facets, and the list API does not
  // return a facet set that shrinks per query, so there is no stale-facet case to invalidate here.
  useEffect(() => {
    if (effectiveData?.facets) {
      setCachedFacets(effectiveData.facets);
    }
  }, [effectiveData?.facets]);

  // Only show loading skeleton on initial load, not during refetch.
  // This prevents the content from flashing when filters/search change.
  const loading = status === 'loading' && !effectiveData;

  // Prefer local page while a newer request is in flight. Trusting mapped.page from a stale
  // response (still page 0 with hasNextPage=true) was snapping pagination back to page 1.
  const resolvedPage = useMemo(() => {
    if (!mapped) return page;
    const maxPage = mapped.total <= 0 ? 0 : Math.max(0, Math.ceil(mapped.total / mapped.pageSize) - 1);
    if (!mapped.hasNextPage && page > maxPage) {
      return maxPage;
    }
    return page;
  }, [mapped, page]);

  useEffect(() => {
    if (resolvedPage !== page) {
      setPage(resolvedPage);
    }
  }, [resolvedPage, page]);

  const goToPage = useCallback((nextPage: number) => {
    setPage(Math.max(0, nextPage));
  }, []);

  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(0);
  }, []);

  const changeOrderBy = useCallback((nextOrderBy: ApplicationsListOrderBy) => {
    setOrderBy(nextOrderBy);
    setPage(0);
  }, []);

  const toggleFilter = useCallback((field: ApplicationsListFilterSetField, id: string) => {
    setPage(0);
    setFilters((current) => toggleApplicationsListFilterId(current, field, id));
  }, []);

  const setThreatRange = useCallback(
    (range: ApplicationsThreatRange) => {
      const normalized = normalizeApplicationsThreatRange(range);
      if (filters.threatRange[0] === normalized[0] && filters.threatRange[1] === normalized[1]) {
        return;
      }
      setPage(0);
      setFilters((current) => ({ ...current, threatRange: normalized }));
    },
    [filters.threatRange]
  );

  const setAgeInDays = useCallback(
    (ageInDays: ApplicationsAgeInDays | undefined) => {
      if (filters.ageInDays === ageInDays) {
        return;
      }
      setPage(0);
      setFilters((current) => ({ ...current, ageInDays }));
    },
    [filters.ageInDays]
  );

  const resetFilters = useCallback(() => {
    setFilters(EMPTY_APPLICATIONS_LIST_FILTERS);
    setPage(0);
  }, []);

  const syncQueryState = useCallback(
    (state: {
      readonly search: string;
      readonly orderBy: ApplicationsListOrderBy;
      readonly page: number;
      readonly filters: ApplicationsListFilterState;
    }) => {
      setSearch((current) => (current === state.search ? current : state.search));
      setOrderBy((current) => (current === state.orderBy ? current : state.orderBy));
      setPage((current) => (current === state.page ? current : state.page));
      setFilters((current) => (filtersEqual(current, state.filters) ? current : state.filters));
    },
    []
  );

  const info: AsyncPageStateInfoProps | null =
    status === 'not-ready'
      ? {
          title: 'Search index building',
          message: APPLICATIONS_INDEX_NOT_READY_MESSAGE,
          testId: 'applications-list-not-ready',
        }
      : null;

  return {
    applications: effectiveData?.applications ?? [],
    facets: effectiveData?.facets ?? cachedFacets,
    filters,
    hasActiveFilters: hasActiveApplicationsListFilters(filters),
    search,
    orderBy,
    loading,
    error: status === 'error' ? error?.message ?? null : null,
    info,
    retry,
    total: effectiveData?.total ?? 0,
    page: resolvedPage,
    pageSize: effectiveData?.pageSize ?? pageSize,
    hasNextPage: effectiveData?.hasNextPage ?? false,
    setPage: goToPage,
    submitSearch,
    changeOrderBy,
    toggleFilter,
    setThreatRange,
    setAgeInDays,
    resetFilters,
    syncQueryState,
  };
}
