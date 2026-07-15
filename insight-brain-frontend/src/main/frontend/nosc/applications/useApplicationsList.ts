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
  ApplicationsListRequest,
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
import { getApplicationsListUrl } from 'MainRoot/util/CLMLocation';

export interface UseApplicationsListResult {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
  readonly facets: ApplicationsFilterFacetCounts;
  readonly filters: ApplicationsListFilterState;
  readonly hasActiveFilters: boolean;
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
  readonly toggleFilter: (
    field: keyof ApplicationsListFilterState,
    id: string,
  ) => void;
  readonly resetFilters: () => void;
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

/**
 * Martha V1 Applications list data hook.
 * Fetches POST /rest/dashboard/applications/list with server pagination and sidebar filters.
 */
export function useApplicationsList(
  options: ApplicationsListRequest = {},
): UseApplicationsListResult {
  const {
    page: initialPage = 0,
    pageSize = APPLICATIONS_LIST_PAGE_SIZE,
    includeFacets = true,
  } = options;
  const [page, setPage] = useState(initialPage);
  const [filters, setFilters] = useState<ApplicationsListFilterState>(EMPTY_APPLICATIONS_LIST_FILTERS);

  const requestBody = useMemo(
    () => ({
      page,
      pageSize,
      includeFacets,
      ...applicationsListFiltersToRequest(filters),
    }),
    [page, pageSize, includeFacets, filters],
  );

  const { status, data, error, retry } = useTile<ApplicationsListApiResponse>(
    getApplicationsListUrl(),
    undefined,
    {
      method: 'post',
      body: requestBody,
      mapErrorStatus: (statusCode) => (statusCode === 409 ? 'not-ready' : 'error'),
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
    loading: status === 'loading',
    error: status === 'error' ? (error?.message ?? null) : null,
    info,
    retry,
    total: mapped?.total ?? 0,
    page: resolvedPage,
    pageSize: mapped?.pageSize ?? pageSize,
    hasNextPage: mapped?.hasNextPage ?? false,
    setPage: goToPage,
    toggleFilter,
    resetFilters,
  };
}
