/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useCurrentStateAndParams } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import ComponentsPage from 'MainRoot/nosc/componentsList/ComponentsPage';
import {
  buildComponentsListRouteParams,
  parseComponentsListParams,
  rawComponentsListParamsSnapshot,
} from 'MainRoot/nosc/componentsList/componentsListQuery';
import { NEXUS_ONE_COMPONENTS_STATE_NAME } from 'MainRoot/nosc/componentsList/componentsRoute';
import { useComponentsList } from 'MainRoot/nosc/componentsList/useComponentsList';

import '@radix-ui/themes/styles.css';

function routeStateKey(params: Record<string, unknown>): string {
  return JSON.stringify(buildComponentsListRouteParams(parseComponentsListParams(params)));
}

/**
 * Preview Components list page (CLM-42214).
 * <p>
 * Martha V1 layout: filter sidebar + My Scan Data / Sonatype Catalog tabs backed by
 * POST /rest/search/catalog. State persists in the hash query
 * ({@code source/q/page/org/ecosystem}).
 */
export default function ComponentsList(): JSX.Element {
  const { params } = useCurrentStateAndParams();
  const parsed = useMemo(() => parseComponentsListParams(params), [params]);
  const routeKey = useMemo(() => routeStateKey(params), [params]);
  const lastWrittenRouteKey = useRef(routeKey);
  const [fetchEnabled, setFetchEnabled] = useState(false);
  const fetchGateOpened = useRef(false);

  const {
    tab,
    components,
    facets,
    filters,
    hasActiveFilters,
    search,
    loading,
    error,
    info,
    retry,
    total,
    exactTotalEstimate,
    page,
    pageSize,
    hasNextPage,
    catalogAvailable,
    setPage,
    setTab,
    submitSearch,
    toggleFilter,
    resetFilters,
    syncQueryState,
  } = useComponentsList({ initialState: parsed, enabled: fetchEnabled });

  useLayoutEffect(() => {
    syncQueryState(parsed);
    const cleanedParams = buildComponentsListRouteParams(parsed);
    const cleanedKey = JSON.stringify(cleanedParams);
    lastWrittenRouteKey.current = cleanedKey;
    if (rawComponentsListParamsSnapshot(params) !== cleanedKey) {
      router.stateService.go(NEXUS_ONE_COMPONENTS_STATE_NAME, cleanedParams, {
        notify: false,
        location: 'replace',
      });
    }
    if (!fetchGateOpened.current) {
      fetchGateOpened.current = true;
      setFetchEnabled(true);
    }
  }, [routeKey, parsed, syncQueryState, params]);

  useEffect(() => {
    const nextParams = buildComponentsListRouteParams({
      tab,
      search,
      page,
      filters,
    });
    const nextKey = JSON.stringify(nextParams);
    if (nextKey === routeKey || nextKey === lastWrittenRouteKey.current) return;
    lastWrittenRouteKey.current = nextKey;
    router.stateService.go(NEXUS_ONE_COMPONENTS_STATE_NAME, nextParams, {
      notify: false,
      location: 'replace',
    });
  }, [tab, search, page, filters, routeKey]);

  return (
    <ComponentsPage
      tab={tab}
      onTabChange={setTab}
      components={components}
      facets={facets}
      filters={filters}
      hasActiveFilters={hasActiveFilters}
      onToggleFilter={toggleFilter}
      onResetFilters={resetFilters}
      loading={loading}
      error={error}
      info={info}
      onRetry={retry}
      totalCount={total}
      exactTotalEstimate={exactTotalEstimate}
      catalogAvailable={catalogAvailable}
      searchValue={search}
      onSearchSubmit={submitSearch}
      page={page + 1}
      pageSize={pageSize}
      hasNextPage={hasNextPage}
      onPageChange={(nextPage) => setPage(nextPage - 1)}
    />
  );
}
