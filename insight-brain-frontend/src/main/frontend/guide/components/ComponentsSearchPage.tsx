/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import {
  PageLayout,
  FilteredPageLayout,
  ComponentsHeader,
  ComponentsResultsList,
  EmptyComponentsResults,
  Pagination,
  useAdapterSearchParams,
} from '@guide/ui-core';
import {
  componentSortOptions,
  componentFilterDefinitions,
  getComponentDetailUrl,
  mergeAggregations,
  COMPONENT_FILTER_ORDER,
  type Aggregations,
} from '@guide/ui-core/utils';
import {
  searchComponents,
  fetchComponentBrowseAggregations,
} from 'GuideRoot/api/componentsBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import { FilteredPageSkeleton } from 'GuideRoot/layout/FilteredPageSkeleton';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { reloadPage, clearErrorRetries } from 'GuideRoot/utils/navigation';
import { useFeatureFlags } from 'GuideRoot/feature-flags/FeatureFlagProvider';
import { FEATURE_FLAGS } from 'GuideRoot/feature-flags/featureFlags';
import type { ComponentSearchResponse } from '@guide/ui-core/types';

const LIMIT = 25;

export function ComponentsSearchPage() {
  const searchParams = useAdapterSearchParams();
  const { isFeatureEnabled, isLoading: flagsLoading } = useFeatureFlags();
  const [data, setData] = useState<ComponentSearchResponse | null>(null);
  const [browseAggregations, setBrowseAggregations] = useState<Aggregations | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterPending, setFilterPending] = useState(false);
  const [toolbarPending, setToolbarPending] = useState(false);
  useEffect(() => {
    if (flagsLoading) return;
    let cancelled = false;
    setLoading(true);
    setError(null);

    const params = new URLSearchParams(searchParams.toString());
    if (!isFeatureEnabled(FEATURE_FLAGS.GUIDE_SEARCH)) params.delete('query');
    if (!params.has('limit')) params.set('limit', String(LIMIT));

    const query = params.get('query') ?? '';
    const sortField = params.get('sortField') ?? '';
    const sortOrder = params.get('sortOrder') ?? '';
    const effectiveSortField = sortField === '_score' && !query
      ? 'trending'
      : sortField || (query ? '_score' : 'trending');
    params.set('sortField', effectiveSortField);
    params.set('sortOrder', sortOrder || 'desc');

    Promise.all([
      searchComponents(params),
      fetchComponentBrowseAggregations(),
    ])
      .then(([res, browse]) => {
        if (!cancelled) { setData(res); setBrowseAggregations(browse); clearErrorRetries(); }
      })
      .catch((e) => { if (!cancelled) setError(e instanceof Error ? e.message : String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [searchParams, flagsLoading]);

  if (loading && data === null) return <FilteredPageSkeleton variant="components" />;

  if (error) {
    return <ErrorPage showGoBack={false} onRetry={reloadPage} />;
  }

  const isPending = loading || filterPending || toolbarPending;
  const components = data?.hits ?? [];
  const total = data?.total ?? 0;
  const aggregations =
    mergeAggregations(browseAggregations, data?.aggregations as Aggregations | undefined) ?? {};
  const rawOffset = parseInt(searchParams.get('offset') ?? '0', 10);
  const offset = Number.isNaN(rawOffset) ? 0 : rawOffset;
  const query = searchParams.get('query') ?? '';
  const hasQuery = !!query;
  const sortField = searchParams.get('sortField') ?? '';
  const sortOrder = searchParams.get('sortOrder') ?? '';
  const defaultSortValue = sortField && sortOrder
    ? (sortField === '_score' && !query ? 'trending:desc' : `${sortField}:${sortOrder}`)
    : (query ? '_score:desc' : 'trending:desc');
  const showPagination = components.length > 0 && total > LIMIT;

  return (
    <PageLayout>
      <FilteredPageLayout
        aggregations={aggregations}
        formAction="/components"
        searchParams={toParamsRecord(searchParams)}
        customFilterConfigs={{ ...COMPONENT_FILTER_ORDER, ...componentFilterDefinitions }}
        searchPlaceholder="Search components..."
        sortOptions={componentSortOptions}
        defaultSortValue={defaultSortValue}
        clearRemovesQuery={true}
        hasQuery={hasQuery}
        hideSearch={!isFeatureEnabled(FEATURE_FLAGS.GUIDE_SEARCH)}
        totalResults={total}
        header={<ComponentsHeader total={total} />}
        onFilterSidebarPendingChange={setFilterPending}
        onToolbarPendingChange={setToolbarPending}
      >
        {!isPending && components.length === 0 ? (
          <EmptyComponentsResults />
        ) : (
          <ComponentsResultsList
            components={components}
            isPending={isPending}
            limit={LIMIT}
            renderLink={(component, _index, children) => (
              <Link to={getComponentDetailUrl(component)} className="unstyled-link">
                {children}
              </Link>
            )}
          />
        )}
        {showPagination && (
          <Pagination
            formAction="/components"
            searchParams={toParamsRecord(searchParams)}
            total={total}
            limit={LIMIT}
            offset={offset}
            onPendingChange={setToolbarPending}
          />
        )}
      </FilteredPageLayout>
    </PageLayout>
  );
}
