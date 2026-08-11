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
  SecurityEventResultsList,
  SecurityEventsHeader,
  Pagination,
  EmptySecurityEventsResults,
  useAdapterSearchParams,
} from '@guide/ui-core';
import {
  securityEventSortOptions,
  SECURITY_EVENT_FILTER_ORDER,
  mergeAggregations,
  type Aggregations,
} from '@guide/ui-core/utils';
import {
  searchSecurityEvents,
  fetchSecurityEventBrowseAggregations,
  type ApiSearchResponse,
} from 'GuideRoot/api/securityEventsBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import { FilteredPageSkeleton } from 'GuideRoot/layout/FilteredPageSkeleton';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { reloadPage, clearErrorRetries } from 'GuideRoot/utils/navigation';
import type { SecurityEventDocument } from '@guide/ui-core/types';

const LIMIT = 25;

export function SecurityEventsPage() {
  const searchParams = useAdapterSearchParams();
  const [response, setResponse] = useState<ApiSearchResponse<SecurityEventDocument> | null>(null);
  const [browseAggregations, setBrowseAggregations] = useState<Aggregations | null>(null);
  const [error, setError] = useState<string | null>(null);
  // Start pending so the first paint is the full-page skeleton, not a flash of the
  // empty-results state before the mount effect kicks off the initial fetch.
  const [isPending, setIsPending] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const params = new URLSearchParams(searchParams.toString());
    if (!params.has('limit')) params.set('limit', String(LIMIT));
    if (!params.has('sortField')) params.set('sortField', 'publishedDate');
    if (!params.has('sortOrder')) params.set('sortOrder', 'desc');

    setError(null);
    setIsPending(true);

    Promise.all([searchSecurityEvents(params), fetchSecurityEventBrowseAggregations()])
      .then(([data, browse]) => {
        if (!cancelled) {
          setResponse(data);
          setBrowseAggregations(browse);
          clearErrorRetries();
        }
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e));
      })
      .finally(() => {
        if (!cancelled) setIsPending(false);
      });

    return () => {
      cancelled = true;
    };
  }, [searchParams]);

  if (isPending && response === null) return <FilteredPageSkeleton variant="security-events" />;

  if (error) {
    return <ErrorPage showGoBack={false} onRetry={reloadPage} />;
  }

  const events = response?.hits ?? [];
  const total = response?.total ?? 0;
  const aggregations =
    mergeAggregations(browseAggregations, response?.aggregations as Aggregations | undefined) ?? {};
  const offset = response?.offset ?? 0;
  const limit = response?.limit ?? LIMIT;

  const paramsRecord = toParamsRecord(searchParams);

  return (
    <PageLayout>
      <FilteredPageLayout
        aggregations={aggregations}
        searchParams={paramsRecord}
        customFilterConfigs={SECURITY_EVENT_FILTER_ORDER}
        formAction="/security-events"
        searchPlaceholder="Filter security events"
        sortOptions={securityEventSortOptions}
        totalResults={total}
        header={<SecurityEventsHeader total={total} />}
        clearRemovesQuery
      >
        {/* Empty state (only show when not loading and no results) */}
        {!isPending && events.length === 0 && <EmptySecurityEventsResults />}

        {/* Results list (shows skeleton or actual results) */}
        {(isPending || events.length > 0) && (
          <SecurityEventResultsList
            events={events}
            isPending={isPending}
            limit={limit}
            moduleName="security-events-page"
            renderLinkWrapper={({ event, children }) => (
              <Link to={`/security-event/${event.eventId}`} className="unstyled-link">
                {children}
              </Link>
            )}
          />
        )}

        {total > limit && (
          <Pagination
            formAction="/security-events"
            searchParams={paramsRecord}
            total={total}
            offset={offset}
            limit={limit}
          />
        )}
      </FilteredPageLayout>
    </PageLayout>
  );
}
