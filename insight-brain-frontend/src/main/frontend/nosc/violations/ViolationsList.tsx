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
} from 'MainRoot/nosc/violations/violationListTypes';
import {
  createDefaultViolationsFilterState,
  deriveViolationFacetLabels,
  VIOLATIONS_PAGE_SIZE,
} from 'MainRoot/nosc/violations/violationsListApi';

import '@radix-ui/themes/styles.css';

// Stable empty reference so the `?? EMPTY_ROWS` fallback keeps the same array identity across renders
// (e.g. during loading), instead of allocating a new `[]` that would re-run the facet-label useMemo.
const EMPTY_ROWS: ReadonlyArray<ViolationRow> = [];

/**
 * Preview Violations list page (CLM-42257 layout + CLM-42254 API wire + CLM-42258 filters).
 *
 * Martha V1: filter rail + violation cards inside the Nexus One Preview shell, fed by
 * POST /rest/dashboard/violations/list. PREVIEW_NEXUS_ONE_UI gates route registration; this
 * component assumes the Preview shell is active.
 */
export default function ViolationsList(): JSX.Element {
  // Page is 1-based in the UI (Pagination contract); the API is 0-based.
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<ViolationsFilterState>(createDefaultViolationsFilterState);

  // Facets are scope-invariant across pages, so we only ask the backend to re-aggregate them when the
  // scope changes (initial load, search changes, and filter changes — all of which land on page 1) —
  // not on each pagination step. Paging past page 1 sends includeFacets:false and reuses the cache.
  const includeFacets = page === 1;

  const { status, data, error, retry } = useViolationsList({
    page: page - 1,
    pageSize: VIOLATIONS_PAGE_SIZE,
    search,
    includeFacets,
    filters,
  });

  // Retain the last facet map fetched on page 1 so the filter rail keeps its counts while paging,
  // even though page 2+ responses omit facets.
  const [cachedFacets, setCachedFacets] = useState<ViolationsListFacets | undefined>(undefined);
  useEffect(() => {
    if (data?.facets) {
      setCachedFacets(data.facets);
    }
  }, [data?.facets]);
  const facets = data?.facets ?? cachedFacets;

  // Any filter change resets to page 1: the current page may not exist under the narrowed result set.
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
  }, []);

  const changeThreatRange = useCallback((range: ViolationThreatRange) => {
    setFilters((prev) => ({ ...prev, threatRange: range }));
    setPage(1);
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(createDefaultViolationsFilterState());
    setPage(1);
  }, []);

  // Submitting a new search term resets to page 1, mirroring the filter handlers above.
  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(1);
  }, []);

  const violations = data?.violations ?? EMPTY_ROWS;
  const labels = useMemo(() => deriveViolationFacetLabels(violations), [violations]);

  const loading = status === 'loading';
  const errorMessage = status === 'error' ? error?.message ?? 'Unable to load violations.' : null;
  // useTile also has a 'not-ready' status, but it is only produced when a caller supplies a
  // mapErrorStatus (e.g. to surface a 409 index-building state). useViolationsList never does, so
  // 'not-ready' is unreachable here and deliberately unhandled. If this hook later surfaces 409s, add
  // an `info` panel path like ApplicationsList rather than letting it fall through to the empty state.

  return (
    <ViolationsPage
      violations={violations}
      facets={facets}
      labels={labels}
      filters={filters}
      onFilterToggle={toggleFilter}
      onThreatRangeChange={changeThreatRange}
      onResetFilters={resetFilters}
      loading={loading}
      error={errorMessage}
      onRetry={retry}
      totalCount={data?.total ?? 0}
      searchValue={search}
      onSearchSubmit={submitSearch}
      page={page}
      // Pagination must use the page size the list was actually fetched with. The request always
      // sends VIOLATIONS_PAGE_SIZE, so drive the display from the same constant; trusting a
      // server-echoed data.pageSize could disagree with the requested size and skip/duplicate rows.
      pageSize={VIOLATIONS_PAGE_SIZE}
      onPageChange={setPage}
    />
  );
}
