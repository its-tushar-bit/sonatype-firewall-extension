/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useMemo } from 'react';
import { getLegalListUrl } from 'MainRoot/util/CLMLocation';
import { useTile, UseTileResult } from 'MainRoot/nosc/dashboard/useTile';
import {
  buildLegalListRequest,
  LEGAL_PAGE_SIZE,
  LegalListResponse,
} from 'MainRoot/nosc/legal/legalListApi';
import { ViolationsFilterState } from 'MainRoot/nosc/violations/violationListTypes';

export interface UseLegalListParams {
  readonly page: number;
  readonly pageSize?: number;
  readonly search?: string;
  readonly includeFacets?: boolean;
  readonly filters?: ViolationsFilterState;
  readonly enabled?: boolean;
}

/**
 * Fetches LEGAL_VIOLATION findings via POST /rest/dashboard/legal/list (CLM-43207).
 */
export function useLegalList(params: UseLegalListParams): UseTileResult<LegalListResponse> {
  const {
    page,
    pageSize = LEGAL_PAGE_SIZE,
    search,
    includeFacets = true,
    filters,
    enabled = true,
  } = params;

  const body = useMemo(
    () => buildLegalListRequest({ page, pageSize, search, includeFacets, filters }),
    [page, pageSize, search, includeFacets, filters],
  );

  return useTile<LegalListResponse>(getLegalListUrl(), undefined, {
    method: 'post',
    body,
    enabled,
  });
}
