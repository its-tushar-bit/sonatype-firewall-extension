/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { getViolationsListUrl } from 'MainRoot/util/CLMLocation';
import { useTile, UseTileResult } from 'MainRoot/nosc/dashboard/useTile';
import { ViolationsListResponse } from 'MainRoot/nosc/violations/violationListTypes';
import { buildViolationsListRequest, VIOLATIONS_PAGE_SIZE } from 'MainRoot/nosc/violations/violationsListApi';

export interface UseViolationsListParams {
  /** 0-based page index sent to the API. */
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly includeFacets?: boolean;
}

/**
 * Fetches a page of Nexus One violations via POST /rest/dashboard/violations/list.
 *
 * Wraps the shared {@link useTile} POST state machine (loading / ready / error / retry) so the
 * page component stays presentational. useTile keys its refetch on the stringified request body, so
 * a fetch happens whenever any of page / pageSize / search / includeFacets changes; the useMemo here
 * only stabilizes the body's object identity across renders (it does not gate the refetch itself).
 */
export function useViolationsList(
  params: UseViolationsListParams,
): UseTileResult<ViolationsListResponse> {
  const { page, pageSize = VIOLATIONS_PAGE_SIZE, search, includeFacets = true } = params;

  const body = useMemo(
    () => buildViolationsListRequest({ page, pageSize, search, includeFacets }),
    [page, pageSize, search, includeFacets],
  );

  return useTile<ViolationsListResponse>(getViolationsListUrl(), undefined, {
    method: 'post',
    body,
  });
}
