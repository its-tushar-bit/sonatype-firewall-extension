/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { getViolationsListUrl } from 'MainRoot/util/CLMLocation';
import { useTile, UseTileResult } from 'MainRoot/nosc/dashboard/useTile';
import {
  ViolationsFilterState,
  ViolationsListResponse,
} from 'MainRoot/nosc/violations/violationListTypes';
import { buildViolationsListRequest, VIOLATIONS_PAGE_SIZE } from 'MainRoot/nosc/violations/violationsListApi';

export interface UseViolationsListParams {
  /** 0-based page index sent to the API. */
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly orderBy?: string;
  readonly includeFacets?: boolean;
  readonly filters?: ViolationsFilterState;
  /** Debounced Organizations facet search (server-side name match; not URL-persisted). */
  readonly organizationFacetSearch?: string;
  /** Debounced Applications facet search (server-side name match; not URL-persisted). */
  readonly applicationFacetSearch?: string;
  /** When false, defers the list POST until route state is hydrated from the URL (default true). */
  readonly enabled?: boolean;
}

/**
 * Fetches a page of Nexus One violations via POST /rest/dashboard/violations/list.
 *
 * Wraps the shared {@link useTile} POST state machine (loading / ready / error / retry) so the
 * page component stays presentational. useTile keys its refetch on the stringified request body, so
 * a fetch happens whenever any of page / pageSize / search / includeFacets / filters changes; the
 * useMemo here only stabilizes the body's object identity across renders (it does not gate refetch).
 * {@code enabled} lets the container defer the first fetch until deep-linked URL params are applied, so
 * a bookmarked search/filter view fetches with the restored state rather than the pre-hydration default
 * (avoiding a throwaway default request in the common case where params are present at mount).
 */
export function useViolationsList(
  params: UseViolationsListParams,
): UseTileResult<ViolationsListResponse> {
  const {
    page,
    pageSize = VIOLATIONS_PAGE_SIZE,
    search,
    orderBy,
    includeFacets = true,
    filters,
    organizationFacetSearch,
    applicationFacetSearch,
    enabled = true,
  } = params;

  const body = useMemo(
    () =>
      buildViolationsListRequest({
        page,
        pageSize,
        search,
        orderBy,
        includeFacets,
        filters,
        organizationFacetSearch,
        applicationFacetSearch,
      }),
    [
      page,
      pageSize,
      search,
      orderBy,
      includeFacets,
      filters,
      organizationFacetSearch,
      applicationFacetSearch,
    ],
  );

  return useTile<ViolationsListResponse>(getViolationsListUrl(), undefined, {
    method: 'post',
    body,
    enabled,
  });
}
