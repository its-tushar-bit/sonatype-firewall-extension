/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import ViolationsPage from 'MainRoot/nosc/violations/ViolationsPage';
import { useViolationsList } from 'MainRoot/nosc/violations/useViolationsList';
import {
  ViolationFilterSetGroup,
  ViolationRow,
  ViolationsFilterState,
  ViolationsListFacets,
  ViolationThreatRange,
  ViolationWaiverType,
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  createDefaultViolationsFilterState,
  deriveViolationFacetLabels,
  VIOLATIONS_PAGE_SIZE,
} from 'MainRoot/nosc/violations/violationsListApi';
import {
  buildViolationsListRouteParams,
  parseViolationsListParams,
  rawViolationsListParamsSnapshot,
  violationsFiltersEqual,
} from 'MainRoot/nosc/violations/violationsListQuery';
import { NEXUS_ONE_VIOLATIONS_STATE_NAME } from 'MainRoot/nosc/violations/violationsRoute';
import { useNexusOneListUrlState } from 'MainRoot/nosc/list/useNexusOneListUrlState';

import '@radix-ui/themes/styles.css';

// Stable empty reference so the `?? EMPTY_ROWS` fallback keeps the same array identity across renders
// (e.g. during loading), instead of allocating a new `[]` that would re-run the facet-label useMemo.
const EMPTY_ROWS: ReadonlyArray<ViolationRow> = [];

/**
 * Preview Violations list page (CLM-42257 layout + CLM-42254 API wire + CLM-42258 filters +
 * CLM-42260 search/URL state/CSV).
 *
 * Martha V1: filter rail + violation cards inside the Nexus One Preview shell, fed by
 * POST /rest/dashboard/violations/list. Search + sidebar filters + page persist in the hash query so
 * the view is bookmarkable and back/forward moves between states; returning from an embedded detail
 * restores the prior state. PREVIEW_NEXUS_ONE_UI gates route registration; this component assumes the
 * Preview shell is active.
 */
export default function ViolationsList(): JSX.Element {
  const {
    search,
    page,
    filters,
    fetchEnabled,
    setSearch,
    setPage,
    setFilters,
    requestUrlWrite,
  } = useNexusOneListUrlState<ViolationsFilterState>({
    stateName: NEXUS_ONE_VIOLATIONS_STATE_NAME,
    parse: parseViolationsListParams,
    build: buildViolationsListRouteParams,
    rawSnapshot: rawViolationsListParamsSnapshot,
    filtersEqual: violationsFiltersEqual,
  });

  // Facets are scope-invariant across pages, so we normally aggregate them only on page 1 and reuse the
  // cache while paging. But a deep link can land directly on page 2+ with an empty cache, so also request
  // facets whenever none are cached yet — otherwise the filter rail would render count-less.
  const [cachedFacets, setCachedFacets] = useState<ViolationsListFacets | undefined>(undefined);
  const includeFacets = page === 1 || cachedFacets === undefined;

  const { status, data, error, retry } = useViolationsList({
    page: page - 1,
    pageSize: VIOLATIONS_PAGE_SIZE,
    search,
    includeFacets,
    filters,
    enabled: fetchEnabled,
  });

  useEffect(() => {
    if (data?.facets) {
      setCachedFacets(data.facets);
    }
  }, [data?.facets]);
  const facets = data?.facets ?? cachedFacets;

  useEffect(() => {
    if (!data) return;
    const total = data.total ?? 0;
    const maxPage = total > 0 ? Math.ceil(total / VIOLATIONS_PAGE_SIZE) : 1;
    if (page > maxPage) {
      setPage(maxPage);
      requestUrlWrite();
    }
  }, [data, page, setPage, requestUrlWrite]);

  const toggleFilter = useCallback((group: ViolationFilterSetGroup, id: string) => {
    setFilters((prev) => {
      const next = new Set(prev[group]);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return { ...prev, [group]: next };
    });
    setPage(1);
    requestUrlWrite();
  }, [setFilters, setPage, requestUrlWrite]);

  const changeThreatRange = useCallback((range: ViolationThreatRange) => {
    setFilters((prev) => ({ ...prev, threatRange: range }));
    setPage(1);
    requestUrlWrite();
  }, [setFilters, setPage, requestUrlWrite]);

  const changeWaiverType = useCallback((waiverType: ViolationWaiverType) => {
    setFilters((prev) => ({ ...prev, waiverType }));
    setPage(1);
    requestUrlWrite();
  }, [setFilters, setPage, requestUrlWrite]);

  const resetFilters = useCallback(() => {
    setFilters(createDefaultViolationsFilterState());
    setPage(1);
    requestUrlWrite();
  }, [setFilters, setPage, requestUrlWrite]);

  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(1);
    requestUrlWrite();
  }, [setSearch, setPage, requestUrlWrite]);

  const goToPage = useCallback((nextPage: number) => {
    setPage(nextPage);
    requestUrlWrite();
  }, [setPage, requestUrlWrite]);

  const violations = data?.violations ?? EMPTY_ROWS;
  const labels = useMemo(
    () =>
      deriveViolationFacetLabels(violations, {
        organizations: facets?.organizationNames,
        applications: facets?.applicationNames,
      }),
    [violations, facets?.organizationNames, facets?.applicationNames],
  );

  const loading = status === 'loading';
  const errorMessage = status === 'error' ? error?.message ?? 'Unable to load violations.' : null;

  return (
    <ViolationsPage
      violations={violations}
      facets={facets}
      labels={labels}
      filters={filters}
      onFilterToggle={toggleFilter}
      onWaiverTypeChange={changeWaiverType}
      onThreatRangeChange={changeThreatRange}
      onResetFilters={resetFilters}
      loading={loading}
      error={errorMessage}
      onRetry={retry}
      totalCount={data?.total ?? 0}
      searchValue={search}
      onSearchSubmit={submitSearch}
      page={page}
      pageSize={VIOLATIONS_PAGE_SIZE}
      onPageChange={goToPage}
    />
  );
}
