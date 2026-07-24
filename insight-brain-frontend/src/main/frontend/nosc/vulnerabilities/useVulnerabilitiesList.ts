/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { getVulnerabilitiesListUrl } from 'MainRoot/util/CLMLocation';
import { useTile, UseTileResult } from 'MainRoot/nosc/dashboard/useTile';
import {
  buildVulnerabilitiesListRequest,
  createDefaultVulnerabilitiesFilterState,
  VULNERABILITIES_DEFAULT_ORDER_BY,
  VULNERABILITIES_PAGE_SIZE,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListOrderBy,
  VulnerabilitiesListResponse,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import type { VulnerabilitiesTab } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import { DEFAULT_VULNERABILITIES_TAB } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';

/** Stable default so callers that omit `filters` do not bust `useMemo` every render. */
const DEFAULT_VULNERABILITIES_FILTERS = createDefaultVulnerabilitiesFilterState();

export interface UseVulnerabilitiesListParams {
  readonly tab?: VulnerabilitiesTab;
  /** 0-based page index sent to the API. */
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly orderBy?: VulnerabilitiesListOrderBy;
  readonly filters?: VulnerabilitiesFilterState;
  readonly includeFacets?: boolean;
  /** When false, defers the list POST until route state is hydrated (default true). */
  readonly enabled?: boolean;
}

/**
 * Fetches a page of Nexus One vulnerabilities via POST /rest/dashboard/vulnerabilities/list.
 */
export function useVulnerabilitiesList(
  params: UseVulnerabilitiesListParams,
): UseTileResult<VulnerabilitiesListResponse> {
  const {
    tab = DEFAULT_VULNERABILITIES_TAB,
    page,
    pageSize = VULNERABILITIES_PAGE_SIZE,
    search,
    orderBy = VULNERABILITIES_DEFAULT_ORDER_BY,
    filters = DEFAULT_VULNERABILITIES_FILTERS,
    includeFacets = true,
    enabled = true,
  } = params;

  const body = useMemo(
    () =>
      buildVulnerabilitiesListRequest({
        tab,
        page,
        pageSize,
        search,
        orderBy,
        includeFacets,
        filters,
      }),
    [tab, page, pageSize, search, orderBy, includeFacets, filters],
  );

  return useTile<VulnerabilitiesListResponse>(getVulnerabilitiesListUrl(), undefined, {
    method: 'post',
    body,
    enabled,
  });
}
