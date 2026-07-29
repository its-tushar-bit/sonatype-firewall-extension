/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useCurrentStateAndParams } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import WaiversAnaPage from 'MainRoot/nosc/waivers/WaiversAnaPage';
import { useAnaWaiversList } from 'MainRoot/nosc/waivers/useAnaWaiversList';
import {
  buildWaiversListRouteParams,
  parseWaiversListParams,
} from 'MainRoot/nosc/waivers/waiversListQuery';

import '@radix-ui/themes/styles.css';

function routeStateKey(params: Record<string, unknown>): string {
  return JSON.stringify(buildWaiversListRouteParams(parseWaiversListParams(params)));
}

/** Raw hash-query snapshot for the list URL fields (before parse/normalize). */
function rawRouteParamsSnapshot(params: Record<string, unknown>): string {
  const asOptionalString = (value: unknown): string | undefined =>
    (typeof value === 'string' && value.length > 0 ? value : undefined);
  return JSON.stringify({
    q: asOptionalString(params.q),
    sort: asOptionalString(params.sort),
    page: asOptionalString(params.page),
    threat: asOptionalString(params.threat),
    expiry: asOptionalString(params.expiry),
    auto: asOptionalString(params.auto),
    org: asOptionalString(params.org),
    app: asOptionalString(params.app),
    policy: asOptionalString(params.policy),
  });
}

/**
 * Ana Waivers list container (CLM-43204).
 *
 * Hard cutover away from the Classic {@code /rest/dashboard/policy/policyWaivers} list —
 * this page now reads exclusively from {@code POST /rest/search/index-query} with
 * {@code entityType: WAIVER}. Detail (WaiverDetailPage) still uses the v2 API. Toolbar
 * search/sort and sidebar filters round-trip in the hash query for bookmarks/back-forward.
 */
export default function WaiversListPage(): JSX.Element {
  const { params } = useCurrentStateAndParams();
  const parsed = useMemo(() => parseWaiversListParams(params), [params]);
  const routeKey = useMemo(() => routeStateKey(params), [params]);
  const lastWrittenRouteKey = useRef(routeKey);
  const [fetchEnabled, setFetchEnabled] = useState(false);
  const fetchGateOpened = useRef(false);
  // Keep latest parse/params in refs so inbound sync can depend only on routeKey. Re-running
  // syncQueryState on every params/parsed identity change (same URL) was stomping a local
  // page advance back to the still-stale hash page=1 and aborting the page-2 index-query.
  const parsedRef = useRef(parsed);
  const paramsRef = useRef(params);
  parsedRef.current = parsed;
  paramsRef.current = params;

  const {
    waivers,
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
    exactTotalEstimate,
    page,
    pageSize,
    hasNextPage,
    warnings,
    setPage,
    submitSearch,
    changeOrderBy,
    toggleFilter,
    resetFilters,
    syncQueryState,
  } = useAnaWaiversList({ initialState: parsed, enabled: fetchEnabled });

  useLayoutEffect(() => {
    const currentParsed = parsedRef.current;
    syncQueryState(currentParsed);
    const cleanedParams = buildWaiversListRouteParams(currentParsed);
    const cleanedKey = JSON.stringify(cleanedParams);
    lastWrittenRouteKey.current = cleanedKey;
    // Drop invalid/unrecognised tokens from the URL before the first fetch so the address
    // bar stays canonical after a deep link (e.g. threat=Bogus or expiry=WhoKnows).
    if (rawRouteParamsSnapshot(paramsRef.current) !== cleanedKey) {
      router.stateService.go('nexusOneWaivers', cleanedParams, { notify: false, location: 'replace' });
    }
    if (!fetchGateOpened.current) {
      fetchGateOpened.current = true;
      setFetchEnabled(true);
    }
  }, [routeKey, syncQueryState]);

  useEffect(() => {
    const nextParams = buildWaiversListRouteParams({
      search,
      orderBy,
      page,
      filters,
    });
    const nextKey = JSON.stringify(nextParams);
    if (nextKey === routeKey || nextKey === lastWrittenRouteKey.current) return;
    lastWrittenRouteKey.current = nextKey;
    router.stateService.go('nexusOneWaivers', nextParams, { notify: false, location: 'replace' });
  }, [search, orderBy, page, filters, routeKey]);

  return (
    <WaiversAnaPage
      waivers={waivers}
      facets={facets}
      filters={filters}
      hasActiveFilters={hasActiveFilters}
      onToggleFilter={toggleFilter}
      onResetFilters={resetFilters}
      loading={loading}
      error={error}
      info={info}
      onRetry={retry}
      searchValue={search}
      onSearchSubmit={submitSearch}
      orderBy={orderBy}
      onOrderByChange={changeOrderBy}
      totalCount={total}
      exactTotalEstimate={exactTotalEstimate}
      page={page}
      pageSize={pageSize}
      hasNextPage={hasNextPage}
      onPageChange={setPage}
      warnings={warnings}
    />
  );
}
