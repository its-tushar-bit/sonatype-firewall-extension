/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useCurrentStateAndParams } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import ApplicationsPage from 'MainRoot/nosc/applications/ApplicationsPage';
import { useApplicationsList } from 'MainRoot/nosc/applications/useApplicationsList';
import {
  buildApplicationsListRouteParams,
  parseApplicationsListParams,
} from 'MainRoot/nosc/applications/applicationsListQuery';

import '@radix-ui/themes/styles.css';

function routeStateKey(params: Record<string, unknown>): string {
  return JSON.stringify(buildApplicationsListRouteParams(parseApplicationsListParams(params)));
}

/** Raw hash-query snapshot for the list URL fields (before parse/normalize). */
function rawRouteParamsSnapshot(params: Record<string, unknown>): string {
  const asOptionalString = (value: unknown): string | undefined =>
    (typeof value === 'string' && value.length > 0 ? value : undefined);
  return JSON.stringify({
    q: asOptionalString(params.q),
    sort: asOptionalString(params.sort),
    page: asOptionalString(params.page),
    stage: asOptionalString(params.stage),
    org: asOptionalString(params.org),
    app: asOptionalString(params.app),
    policyType: asOptionalString(params.policyType),
    violationState: asOptionalString(params.violationState),
    threat: asOptionalString(params.threat),
  });
}

/**
 * Preview Applications list page (CLM-42223 / CLM-42224 / CLM-42226).
 *
 * Martha V1 layout: filter sidebar + evaluation card grid backed by
 * POST /rest/dashboard/applications/list inside the Nexus One Preview shell.
 * Toolbar search/sort and sidebar filters persist in the hash query for bookmarks/back-forward.
 */
export default function ApplicationsList() {
  const { params } = useCurrentStateAndParams();
  const parsed = useMemo(() => parseApplicationsListParams(params), [params]);
  const routeKey = useMemo(() => routeStateKey(params), [params]);
  const lastWrittenRouteKey = useRef(routeKey);
  const [fetchEnabled, setFetchEnabled] = useState(false);
  const fetchGateOpened = useRef(false);

  const {
    applications,
    facets,
    filters,
    hasActiveFilters,
    search,
    orderBy,
    loading,
    error,
    info,
    retry,
    total,
    page,
    pageSize,
    hasNextPage,
    setPage,
    submitSearch,
    changeOrderBy,
    toggleFilter,
    setThreatRange,
    resetFilters,
    syncQueryState,
  } = useApplicationsList({ initialState: parsed, enabled: fetchEnabled });

  useLayoutEffect(() => {
    syncQueryState(parsed);
    const cleanedParams = buildApplicationsListRouteParams(parsed);
    const cleanedKey = JSON.stringify(cleanedParams);
    lastWrittenRouteKey.current = cleanedKey;
    // Parse drops invalid tokens (e.g. threat=Bogus) from state; replace the URL so they
    // do not linger in the address bar when routeKey already matches the cleaned form.
    if (rawRouteParamsSnapshot(params) !== cleanedKey) {
      router.stateService.go('nexusOneApplications', cleanedParams, { notify: false, location: 'replace' });
    }
    if (!fetchGateOpened.current) {
      fetchGateOpened.current = true;
      setFetchEnabled(true);
    }
  }, [routeKey, parsed, syncQueryState, params]);

  useEffect(() => {
    // Guard relies on stable key ordering from buildApplicationsListRouteParams; integration
    // tests cover hydration and write-back so a param-order regression is caught in CI.
    const nextParams = buildApplicationsListRouteParams({
      search,
      orderBy,
      page,
      filters,
    });
    const nextKey = JSON.stringify(nextParams);
    if (nextKey === routeKey || nextKey === lastWrittenRouteKey.current) return;
    lastWrittenRouteKey.current = nextKey;
    router.stateService.go('nexusOneApplications', nextParams, { notify: false, location: 'replace' });
  }, [search, orderBy, page, filters, routeKey]);

  return (
    <ApplicationsPage
      applications={applications}
      facets={facets}
      filters={filters}
      hasActiveFilters={hasActiveFilters}
      onToggleFilter={toggleFilter}
      onThreatRangeChange={setThreatRange}
      onResetFilters={resetFilters}
      loading={loading}
      error={error}
      info={info}
      onRetry={retry}
      totalCount={total}
      searchValue={search}
      onSearchSubmit={submitSearch}
      orderBy={orderBy}
      onOrderByChange={changeOrderBy}
      page={page + 1}
      pageSize={pageSize}
      hasNextPage={hasNextPage}
      onPageChange={(nextPage) => setPage(nextPage - 1)}
    />
  );
}
