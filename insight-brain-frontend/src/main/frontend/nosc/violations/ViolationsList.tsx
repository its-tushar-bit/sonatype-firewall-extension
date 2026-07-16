/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
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
  // Read and write through the same router (context) — not the module singleton — so a URL write
  // round-trips back through useCurrentStateAndParams and stays consistent under back/forward.
  const router = useRouter();
  const { params } = useCurrentStateAndParams();
  const parsed = useMemo(() => parseViolationsListParams(params), [params]);
  const routeKey = useMemo(() => JSON.stringify(buildViolationsListRouteParams(parsed)), [parsed]);
  const rawKey = useMemo(() => rawViolationsListParamsSnapshot(params), [params]);
  // Load-bearing: UI-Router can mount this component before the route params are populated, so the
  // first render's `parsed` may be the empty default. The gate defers the initial POST until the
  // hydrate effect below runs, so we fetch once with the restored deep-link state instead of firing a
  // throwaway default request first. The ref opens the gate exactly once (not on every URL change).
  const fetchGateOpened = useRef(false);
  const [fetchEnabled, setFetchEnabled] = useState(false);

  // Page is 1-based in the UI (Pagination contract); the API and the query codec are 0-based.
  const [page, setPage] = useState(() => parsed.page + 1);
  const [search, setSearch] = useState(() => parsed.search);
  const [filters, setFilters] = useState<ViolationsFilterState>(() => parsed.filters);

  // URL writes are one-directional: only *user* actions flip this flag, so the write effect below never
  // fires for URL-driven (deep-link / back-forward) state changes. That removes the stale-closure race
  // where a passive write running against pre-hydration state could clobber a real navigation.
  const pendingUrlWrite = useRef(false);

  // Hydrate list state from the route: first mount and every URL change (deep links, back/forward,
  // returning from the embedded detail). Runs in the layout phase so state is applied before paint; it
  // never navigates (URL normalization + write-back are the passive effects below). Opens the fetch gate
  // once so the first POST carries the restored state instead of the pre-hydration default.
  useLayoutEffect(() => {
    setSearch((current) => (current === parsed.search ? current : parsed.search));
    setPage((current) => (current === parsed.page + 1 ? current : parsed.page + 1));
    setFilters((current) => (violationsFiltersEqual(current, parsed.filters) ? current : parsed.filters));
    if (!fetchGateOpened.current) {
      fetchGateOpened.current = true;
      setFetchEnabled(true);
    }
  }, [parsed]);

  // Normalize the address bar when the URL carried tokens that parse dropped/clamped (e.g. threat=abc or
  // an unsupported state) so a deep link settles on its canonical form. Idempotent: after the replace,
  // rawKey matches routeKey and this no-ops.
  useEffect(() => {
    if (rawKey === routeKey) return;
    router.stateService.go(NEXUS_ONE_VIOLATIONS_STATE_NAME, buildViolationsListRouteParams(parsed), {
      notify: false,
      location: 'replace',
    });
  }, [router, rawKey, routeKey, parsed]);

  // Write list state back to the hash query (replace, no transition) — only in response to a user action
  // (guarded by pendingUrlWrite). The routeKey check then skips a redundant write when state already
  // matches the URL, so there is no hydrate→write feedback loop.
  useEffect(() => {
    if (!pendingUrlWrite.current) return;
    pendingUrlWrite.current = false;
    const nextParams = buildViolationsListRouteParams({ search, page: page - 1, filters });
    if (JSON.stringify(nextParams) === routeKey) return;
    router.stateService.go(NEXUS_ONE_VIOLATIONS_STATE_NAME, nextParams, {
      notify: false,
      location: 'replace',
    });
  }, [router, search, page, filters, routeKey]);

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

  // Retain the last facet map so the filter rail keeps its counts while paging, even though page 2+
  // responses omit facets.
  useEffect(() => {
    if (data?.facets) {
      setCachedFacets(data.facets);
    }
  }, [data?.facets]);
  const facets = data?.facets ?? cachedFacets;

  // Clamp an out-of-range page (a stale bookmark past the last page, or a page that no longer exists
  // after the result set shrank) to the last page that has rows, and persist the correction to the URL.
  // Absurd deep links are also soft-capped in parseViolationsListParams (MAX_DEEP_LINK_PAGE) so the
  // first POST never carries a million-scale page index.
  useEffect(() => {
    if (!data) return;
    const total = data.total ?? 0;
    const maxPage = total > 0 ? Math.ceil(total / VIOLATIONS_PAGE_SIZE) : 1;
    if (page > maxPage) {
      setPage(maxPage);
      pendingUrlWrite.current = true;
    }
  }, [data, page]);

  // Any filter change resets to page 1 (the current page may not exist under a narrowed result set) and
  // flags a URL write so the selection is persisted to the hash.
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
    pendingUrlWrite.current = true;
  }, []);

  const changeThreatRange = useCallback((range: ViolationThreatRange) => {
    setFilters((prev) => ({ ...prev, threatRange: range }));
    setPage(1);
    pendingUrlWrite.current = true;
  }, []);

  const changeWaiverType = useCallback((waiverType: ViolationWaiverType) => {
    setFilters((prev) => ({ ...prev, waiverType }));
    setPage(1);
    pendingUrlWrite.current = true;
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(createDefaultViolationsFilterState());
    setPage(1);
    pendingUrlWrite.current = true;
  }, []);

  // Submitting a new search term resets to page 1, mirroring the filter handlers above.
  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(1);
    pendingUrlWrite.current = true;
  }, []);

  const goToPage = useCallback((nextPage: number) => {
    setPage(nextPage);
    pendingUrlWrite.current = true;
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
      // Pagination must use the page size the list was actually fetched with. The request always
      // sends VIOLATIONS_PAGE_SIZE, so drive the display from the same constant; trusting a
      // server-echoed data.pageSize could disagree with the requested size and skip/duplicate rows.
      pageSize={VIOLATIONS_PAGE_SIZE}
      onPageChange={goToPage}
    />
  );
}
