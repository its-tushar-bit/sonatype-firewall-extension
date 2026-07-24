/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import VulnerabilitiesPage from 'MainRoot/nosc/vulnerabilities/VulnerabilitiesPage';
import { useVulnerabilitiesList } from 'MainRoot/nosc/vulnerabilities/useVulnerabilitiesList';
import {
  createDefaultVulnerabilitiesFilterState,
  mapVulnerabilitiesListResponse,
  VULNERABILITIES_PAGE_SIZE,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import {
  buildVulnerabilitiesListRouteParams,
  parseVulnerabilitiesListParams,
  rawVulnerabilitiesListParamsSnapshot,
  vulnerabilitiesFiltersEqual,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListQuery';
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListFacets,
  VulnerabilitiesListOrderBy,
  VulnerabilityCvssRange,
  VulnerabilityFilterSetGroup,
  VulnerabilityRow,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import {
  NEXUS_ONE_VULNERABILITIES_STATE_NAME,
  type VulnerabilitiesTab,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';

import '@radix-ui/themes/styles.css';

const EMPTY_ROWS: ReadonlyArray<VulnerabilityRow> = [];

type UrlWriteState = {
  readonly tab: VulnerabilitiesTab;
  readonly page: number;
  readonly search: string;
  readonly orderBy: VulnerabilitiesListOrderBy;
  readonly filters: VulnerabilitiesFilterState;
};

/**
 * Preview Vulnerabilities list: My Scan Data / Catalog tabs, filter rail, cards, and URL state for
 * tab + search + page + sort + severity/CVSS/ecosystem filters.
 */
export default function VulnerabilitiesList(): JSX.Element {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();

  const routeState = useMemo(
    () => parseVulnerabilitiesListParams(params as Record<string, unknown>),
    [params],
  );
  const routeKey = useMemo(
    () => JSON.stringify(buildVulnerabilitiesListRouteParams(routeState)),
    [routeState],
  );
  const rawKey = useMemo(
    () => rawVulnerabilitiesListParamsSnapshot(params as Record<string, unknown>),
    [params],
  );

  const fetchGateOpened = useRef(false);
  const [fetchEnabled, setFetchEnabled] = useState(false);
  const [facetCache, setFacetCache] = useState<VulnerabilitiesListFacets | null>(null);

  const [tab, setTab] = useState<VulnerabilitiesTab>(() => routeState.tab);
  const [page, setPage] = useState(() => routeState.page + 1);
  const [search, setSearch] = useState(() => routeState.search);
  const [orderBy, setOrderBy] = useState<VulnerabilitiesListOrderBy>(() => routeState.orderBy);
  const [filters, setFilters] = useState<VulnerabilitiesFilterState>(() => routeState.filters);

  // Hydrate from the route before paint. UI-Router can mount with empty params for a tick, so the
  // fetch gate opens on a microtask after this layout flush — the first POST then carries restored
  // deep-link state instead of a throwaway default request (same race Violations/Apps guard against).
  useLayoutEffect(() => {
    setTab((current) => (current === routeState.tab ? current : routeState.tab));
    setPage((current) => (current === routeState.page + 1 ? current : routeState.page + 1));
    setSearch((current) => (current === routeState.search ? current : routeState.search));
    setOrderBy((current) => (current === routeState.orderBy ? current : routeState.orderBy));
    setFilters((current) =>
      vulnerabilitiesFiltersEqual(current, routeState.filters) ? current : routeState.filters,
    );
    if (!fetchGateOpened.current) {
      fetchGateOpened.current = true;
      queueMicrotask(() => setFetchEnabled(true));
    }
  }, [routeState]);

  // Normalize the address bar when the URL carried tokens that parse dropped/clamped (e.g. cvss=abc
  // or severity=bogus) so a deep link settles on its canonical form. Idempotent: after the replace,
  // rawKey matches routeKey and this no-ops.
  useEffect(() => {
    if (rawKey === routeKey) return;
    router.stateService.go(
      NEXUS_ONE_VULNERABILITIES_STATE_NAME,
      buildVulnerabilitiesListRouteParams(routeState),
      { notify: false, location: 'replace' },
    );
  }, [router, rawKey, routeKey, routeState]);

  const apiPage = Math.max(0, page - 1);
  const includeFacets = apiPage === 0 || facetCache == null;
  const { status, data, error, retry } = useVulnerabilitiesList({
    tab,
    page: apiPage,
    pageSize: VULNERABILITIES_PAGE_SIZE,
    search,
    orderBy,
    filters,
    includeFacets,
    enabled: fetchEnabled,
  });

  const mapped = mapVulnerabilitiesListResponse(data);
  const vulnerabilities = mapped.vulnerabilities.length > 0 ? mapped.vulnerabilities : EMPTY_ROWS;

  useLayoutEffect(() => {
    if (mapped.facets) {
      setFacetCache(mapped.facets);
    }
  }, [mapped.facets]);

  const writeUrl = useCallback(
    (next: UrlWriteState) => {
      router.stateService.go(
        NEXUS_ONE_VULNERABILITIES_STATE_NAME,
        buildVulnerabilitiesListRouteParams(next),
        { location: 'replace', notify: false },
      );
    },
    [router],
  );

  // Clamp an out-of-range page (stale bookmark past the last page, or a page that no longer exists
  // after the result set shrank) to the last page that has rows, and persist the correction.
  useEffect(() => {
    if (status !== 'ready' || data == null) return;
    const total = mapped.total;
    const maxPage = total > 0 ? Math.ceil(total / VULNERABILITIES_PAGE_SIZE) : 1;
    if (page > maxPage) {
      setPage(maxPage);
      writeUrl({ tab, page: maxPage - 1, search, orderBy, filters });
    }
  }, [status, data, mapped.total, page, tab, search, orderBy, filters, writeUrl]);

  const handleTabChange = (nextTab: VulnerabilitiesTab) => {
    setTab(nextTab);
    setPage(1);
    setFacetCache(null);
    writeUrl({ tab: nextTab, page: 0, search, orderBy, filters });
  };

  const handleSearchSubmit = (term: string) => {
    setSearch(term);
    setPage(1);
    setFacetCache(null);
    writeUrl({ tab, page: 0, search: term, orderBy, filters });
  };

  const handleOrderByChange = (nextOrderBy: VulnerabilitiesListOrderBy) => {
    setOrderBy(nextOrderBy);
    setPage(1);
    setFacetCache(null);
    writeUrl({ tab, page: 0, search, orderBy: nextOrderBy, filters });
  };

  const handlePageChange = (nextPage: number) => {
    setPage(nextPage);
    writeUrl({ tab, page: Math.max(0, nextPage - 1), search, orderBy, filters });
  };

  const handleFilterToggle = (group: VulnerabilityFilterSetGroup, id: string) => {
    const nextSet = new Set(filters[group]);
    if (nextSet.has(id)) {
      nextSet.delete(id);
    } else {
      nextSet.add(id);
    }
    const next = { ...filters, [group]: nextSet };
    setFilters(next);
    setPage(1);
    setFacetCache(null);
    writeUrl({ tab, page: 0, search, orderBy, filters: next });
  };

  const handleCvssRangeChange = (range: VulnerabilityCvssRange) => {
    const next = { ...filters, cvssRange: range };
    setFilters(next);
    setPage(1);
    setFacetCache(null);
    writeUrl({ tab, page: 0, search, orderBy, filters: next });
  };

  const handleFiltersReset = () => {
    const next = createDefaultVulnerabilitiesFilterState();
    setFilters(next);
    setPage(1);
    setFacetCache(null);
    writeUrl({ tab, page: 0, search, orderBy, filters: next });
  };

  return (
    <VulnerabilitiesPage
      tab={tab}
      onTabChange={handleTabChange}
      vulnerabilities={vulnerabilities}
      facets={facetCache ?? mapped.facets}
      filters={filters}
      onFilterToggle={handleFilterToggle}
      onCvssRangeChange={handleCvssRangeChange}
      onFiltersReset={handleFiltersReset}
      loading={status === 'loading'}
      error={error?.message ?? null}
      onRetry={retry}
      totalCount={mapped.total}
      searchValue={search}
      onSearchSubmit={handleSearchSubmit}
      orderBy={orderBy}
      onOrderByChange={handleOrderByChange}
      page={page}
      pageSize={VULNERABILITIES_PAGE_SIZE}
      onPageChange={handlePageChange}
    />
  );
}
