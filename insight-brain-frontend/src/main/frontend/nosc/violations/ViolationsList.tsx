/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
 * CLM-42260 search/URL state/CSV + CLM-42912 server-side facet search).
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

  // Facet-rail org/app name search — not URL-persisted; debounced before the list POST (CLM-42912).
  const [organizationFacetSearch, setOrganizationFacetSearch] = useState('');
  const [applicationFacetSearch, setApplicationFacetSearch] = useState('');
  const [debouncedOrganizationFacetSearch, setDebouncedOrganizationFacetSearch] = useState('');
  const [debouncedApplicationFacetSearch, setDebouncedApplicationFacetSearch] = useState('');

  useEffect(() => {
    const handle = window.setTimeout(
      () => setDebouncedOrganizationFacetSearch(organizationFacetSearch.trim()),
      300,
    );
    return () => window.clearTimeout(handle);
  }, [organizationFacetSearch]);

  useEffect(() => {
    const handle = window.setTimeout(
      () => setDebouncedApplicationFacetSearch(applicationFacetSearch.trim()),
      300,
    );
    return () => window.clearTimeout(handle);
  }, [applicationFacetSearch]);

  // Facet search changes the owner facet maps; reset to page 1 so includeFacets stays true and the
  // rail refreshes (clearing search restores top-by-count facets on page 1). Skip the mount pass so
  // a deep-linked page > 1 is not clobbered before hydration.
  const skipFacetSearchPageReset = useRef(true);
  useEffect(() => {
    if (skipFacetSearchPageReset.current) {
      skipFacetSearchPageReset.current = false;
      return;
    }
    setPage(1);
  }, [debouncedOrganizationFacetSearch, debouncedApplicationFacetSearch, setPage]);

  // Facets are scope-invariant across pages, so we normally aggregate them only on page 1 and reuse the
  // cache while paging. But a deep link can land directly on page 2+ with an empty cache, so also request
  // facets whenever none are cached yet — otherwise the filter rail would render count-less.
  const [cachedFacets, setCachedFacets] = useState<ViolationsListFacets | undefined>(undefined);
  const facetSearchActive =
    Boolean(debouncedOrganizationFacetSearch) || Boolean(debouncedApplicationFacetSearch);
  // Facet search must always request facets (page 2+ normally reuses the cache).
  const includeFacets = page === 1 || cachedFacets === undefined || facetSearchActive;

  const { status, data, error, retry } = useViolationsList({
    page: page - 1,
    pageSize: VIOLATIONS_PAGE_SIZE,
    search,
    includeFacets,
    filters,
    organizationFacetSearch: debouncedOrganizationFacetSearch || undefined,
    applicationFacetSearch: debouncedApplicationFacetSearch || undefined,
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
    const selected = filters[group];
    const removingLast = selected.has(id) && selected.size === 1;
    setFilters((prev) => {
      const next = new Set(prev[group]);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return { ...prev, [group]: next };
    });
    // Clear facet search outside the updater (pure) and eagerly clear debounce so page-1
    // refetch does not carry a stale search term.
    if (removingLast && group === 'organizationIds') {
      setOrganizationFacetSearch('');
      setDebouncedOrganizationFacetSearch('');
    }
    if (removingLast && group === 'applicationIds') {
      setApplicationFacetSearch('');
      setDebouncedApplicationFacetSearch('');
    }
    setPage(1);
    requestUrlWrite();
  }, [filters, setFilters, setPage, requestUrlWrite]);

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
    setOrganizationFacetSearch('');
    setDebouncedOrganizationFacetSearch('');
    setApplicationFacetSearch('');
    setDebouncedApplicationFacetSearch('');
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
      organizationFacetSearch={organizationFacetSearch}
      onOrganizationFacetSearchChange={setOrganizationFacetSearch}
      applicationFacetSearch={applicationFacetSearch}
      onApplicationFacetSearchChange={setApplicationFacetSearch}
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
