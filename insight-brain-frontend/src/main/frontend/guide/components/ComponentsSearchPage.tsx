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
  getStringParam,
  getOffsetFromParams,
  getLimitFromParams,
  getSortFromParams,
  buildComponentFilters,
  getComponentDetailUrl,
  COMPONENT_FILTER_ORDER,
} from '@guide/ui-core/utils';
import { searchComponents } from 'GuideRoot/api/componentsBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import { FilteredPageSkeleton } from 'GuideRoot/layout/FilteredPageSkeleton';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { reloadPage, clearErrorRetries } from 'GuideRoot/utils/navigation';
import type { ComponentSearchResponse, ComponentsSearchOptions } from '@guide/ui-core/types';
import type { ReadonlySearchParams } from '@guide/ui-core/adapters';

const LIMIT = 25;

function buildParams(searchParams: ReadonlySearchParams) {
  const params = toParamsRecord(searchParams);

  const query = getStringParam(params, 'query', '') || undefined;
  const filters = buildComponentFilters(params);
  const offset = getOffsetFromParams(params);
  const limit = getLimitFromParams(params, LIMIT);
  const { sortField, sortOrder } = getSortFromParams(params);

  const options: ComponentsSearchOptions = { offset, limit };
  if (sortField) options.sortField = sortField;
  if (sortOrder) options.sortOrder = sortOrder;

  return {
    query,
    filters: Object.keys(filters).length ? filters : undefined,
    options,
  };
}

export function ComponentsSearchPage() {
  const searchParams = useAdapterSearchParams();
  const [data, setData] = useState<ComponentSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterPending, setFilterPending] = useState(false);
  const [toolbarPending, setToolbarPending] = useState(false);
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    searchComponents(buildParams(searchParams))
      .then((res) => { if (!cancelled) { setData(res); clearErrorRetries(); } })
      .catch((e) => { if (!cancelled) setError(e instanceof Error ? e.message : String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [searchParams]);

  if (loading && data === null) return <FilteredPageSkeleton variant="components" />;

  if (error) {
    return <ErrorPage showGoBack={false} onRetry={reloadPage} />;
  }

  const isPending = loading || filterPending || toolbarPending;
  const components = data?.hits ?? [];
  const total = data?.total ?? 0;
  const aggregations = data?.aggregations ?? {};
  const rawOffset = parseInt(searchParams.get('offset') ?? '0', 10);
  const offset = Number.isNaN(rawOffset) ? 0 : rawOffset;
  const hasQuery = !!searchParams.get('query');
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
        clearRemovesQuery={true}
        hasQuery={hasQuery}
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
