/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTile } from 'MainRoot/nosc/dashboard/useTile';
import type { AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import {
  APPLICATIONS_LIST_PAGE_SIZE,
  ApplicationsListApiResponse,
  ApplicationsListOrderBy,
  mapApplicationsListResponse,
} from 'MainRoot/nosc/applications/applicationsListApi';
import {
  ApplicationRiskScore,
  ApplicationsFilterFacetCounts,
} from 'MainRoot/nosc/applications/applicationListTypes';
import {
  ApplicationsListFilterState,
  EMPTY_APPLICATIONS_LIST_FILTERS,
  applicationsListFiltersToRequest,
  hasActiveApplicationsListFilters,
  toggleApplicationsListFilterId,
} from 'MainRoot/nosc/applications/applicationsListFilters';
import {
  DEFAULT_APPLICATIONS_LIST_ORDER_BY,
  filtersEqual,
} from 'MainRoot/nosc/applications/applicationsListQuery';
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
  readonly toggleFilter: (
    field: keyof ApplicationsListFilterState,
    id: string,
  ) => void;
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
  threatLevels: [],
  stages: [],
  organizations: [],
  applications: [],
};

export const APPLICATIONS_INDEX_NOT_READY_MESSAGE =
  'The search index is still building. Please try again shortly.';

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
export function useApplicationsList(
  options: UseApplicationsListOptions = {},
): UseApplicationsListResult {
  const {
    pageSize = APPLICATIONS_LIST_PAGE_SIZE,
    includeFacets = true,
    initialState,
    enabled = true,
  } = options;
  const [page, setPage] = useState(() => initialState?.page ?? 0);
  const [search, setSearch] = useState(() => initialState?.search ?? '');
  const [orderBy, setOrderBy] = useState<ApplicationsListOrderBy>(
    () => initialState?.orderBy ?? DEFAULT_APPLICATIONS_LIST_ORDER_BY,
  );
  const [filters, setFilters] = useState<ApplicationsListFilterState>(
    () => initialState?.filters ?? EMPTY_APPLICATIONS_LIST_FILTERS,
  );

  const requestBody = useMemo(
    () => ({
      page,
      pageSize,
      includeFacets,
      orderBy,
      ...(search.trim() ? { search: search.trim() } : {}),
      ...applicationsListFiltersToRequest(filters),
    }),
    [page, pageSize, includeFacets, search, orderBy, filters],
  );

  const { status, data, error, retry } = useTile<ApplicationsListApiResponse>(
    getApplicationsListUrl(),
    undefined,
    {
      method: 'post',
      body: requestBody,
      mapErrorStatus: (statusCode) => (statusCode === 409 ? 'not-ready' : 'error'),
      enabled,
    },
  );

  const mapped = useMemo(
    () => (data ? mapApplicationsListResponse(data) : null),
    [data],
  );

  const resolvedPage = useMemo(() => {
    if (!mapped) return page;
    const apiPage = mapped.page ?? page;
    if (mapped.hasNextPage) return apiPage;
    const maxPage = mapped.total <= 0 ? 0 : Math.max(0, Math.ceil(mapped.total / mapped.pageSize) - 1);
    return Math.min(apiPage, maxPage);
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

  const toggleFilter = useCallback((
    field: keyof ApplicationsListFilterState,
    id: string,
  ) => {
    setFilters((current) => {
      const next = toggleApplicationsListFilterId(current, field, id);
      // No-op toggles (unknown/None threat ids) return the same object — skip page reset/refetch.
      if (next === current) {
        return current;
      }
      setPage(0);
      return next;
    });
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(EMPTY_APPLICATIONS_LIST_FILTERS);
    setPage(0);
  }, []);

  const syncQueryState = useCallback((state: {
    readonly search: string;
    readonly orderBy: ApplicationsListOrderBy;
    readonly page: number;
    readonly filters: ApplicationsListFilterState;
  }) => {
    setSearch((current) => (current === state.search ? current : state.search));
    setOrderBy((current) => (current === state.orderBy ? current : state.orderBy));
    setPage((current) => (current === state.page ? current : state.page));
    setFilters((current) => (filtersEqual(current, state.filters) ? current : state.filters));
  }, []);

  const info: AsyncPageStateInfoProps | null =
    status === 'not-ready'
      ? {
          title: 'Search index building',
          message: APPLICATIONS_INDEX_NOT_READY_MESSAGE,
          testId: 'applications-list-not-ready',
        }
      : null;

  return {
    applications: mapped?.applications ?? [],
    facets: mapped?.facets ?? EMPTY_FACETS,
    filters,
    hasActiveFilters: hasActiveApplicationsListFilters(filters),
    search,
    orderBy,
    loading: status === 'loading',
    error: status === 'error' ? (error?.message ?? null) : null,
    info,
    retry,
    total: mapped?.total ?? 0,
    page: resolvedPage,
    pageSize: mapped?.pageSize ?? pageSize,
    hasNextPage: mapped?.hasNextPage ?? false,
    setPage: goToPage,
    submitSearch,
    changeOrderBy,
    toggleFilter,
    resetFilters,
    syncQueryState,
  };
}
