/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import ViolationsPage from 'MainRoot/nosc/violations/ViolationsPage';
import { useViolationsList } from 'MainRoot/nosc/violations/useViolationsList';
import { ViolationRow, ViolationsListFacets } from 'MainRoot/nosc/violations/violationListTypes';
import {
  deriveViolationFacetLabels,
  VIOLATIONS_PAGE_SIZE,
} from 'MainRoot/nosc/violations/violationsListApi';

import '@radix-ui/themes/styles.css';

// Stable empty reference so the `?? EMPTY_ROWS` fallback keeps the same array identity across renders
// (e.g. during loading), instead of allocating a new `[]` that would re-run the facet-label useMemo.
const EMPTY_ROWS: ReadonlyArray<ViolationRow> = [];

/**
 * Preview Violations list page (CLM-42257 layout + CLM-42254 API wire).
 *
 * Martha V1: filter rail + violation cards inside the Nexus One Preview shell, fed by
 * POST /rest/dashboard/violations/list. PREVIEW_NEXUS_ONE_UI gates route registration; this
 * component assumes the Preview shell is active.
 */
export default function ViolationsList(): JSX.Element {
  // Page is 1-based in the UI (Pagination contract); the API is 0-based.
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');

  // Facets are scope-invariant across pages, so we only ask the backend to re-aggregate them when the
  // scope changes (initial load and every search change, both of which land on page 1) — not on each
  // pagination step. Paging past page 1 sends includeFacets:false and reuses the cached counts below.
  const includeFacets = page === 1;

  const { status, data, error, retry } = useViolationsList({
    page: page - 1,
    pageSize: VIOLATIONS_PAGE_SIZE,
    search,
    includeFacets,
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
      loading={loading}
      error={errorMessage}
      onRetry={retry}
      totalCount={data?.total ?? 0}
      searchValue={search}
      onSearchSubmit={(term) => {
        setSearch(term);
        setPage(1);
      }}
      page={page}
      // Pagination must use the page size the list was actually fetched with. The request always
      // sends VIOLATIONS_PAGE_SIZE, so drive the display from the same constant; trusting a
      // server-echoed data.pageSize could disagree with the requested size and skip/duplicate rows.
      pageSize={VIOLATIONS_PAGE_SIZE}
      onPageChange={setPage}
    />
  );
}
