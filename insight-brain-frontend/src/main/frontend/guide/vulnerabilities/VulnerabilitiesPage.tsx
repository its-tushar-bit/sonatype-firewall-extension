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
  VulnerabilitiesResultsList,
  VulnerabilitiesHeader,
  Pagination,
  EmptyVulnerabilitiesResults,
  useAdapterSearchParams,
} from '@guide/ui-core';
import {
  vulnerabilitySortOptions,
  vulnerabilityFilterDefinitions,
  buildVulnerabilityFilters,
  getOffsetFromParams,
  getLimitFromParams,
  getSortFromParams,
  getStringParam,
  VULNERABILITY_FILTER_ORDER,
} from '@guide/ui-core/utils';
import { searchVulnerabilities } from 'GuideRoot/api/vulnerabilitiesBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import { FilteredPageSkeleton } from 'GuideRoot/layout/FilteredPageSkeleton';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { reloadPage, clearErrorRetries } from 'GuideRoot/utils/navigation';
import type { VulnerabilitySearchResponse, VulnerabilitiesSearchOptions } from '@guide/ui-core/types';

export function VulnerabilitiesPage() {
  const searchParams = useAdapterSearchParams();
  const [response, setResponse] = useState<VulnerabilitySearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, setIsPending] = useState(false);
  useEffect(() => {
    let cancelled = false;

    // Convert to Record once for all utility functions (they expect Record<string, string | string[]>)
    const paramsRecord = toParamsRecord(searchParams);

    const query = getStringParam(paramsRecord, 'query');
    const filters = buildVulnerabilityFilters(paramsRecord);
    const sortParam = getSortFromParams(paramsRecord);
    const options: VulnerabilitiesSearchOptions = {
      offset: getOffsetFromParams(paramsRecord),
      limit: getLimitFromParams(paramsRecord),
      ...sortParam,
    };

    setError(null);
    setIsPending(true);

    searchVulnerabilities({ query, filters, options })
      .then((data) => {
        if (!cancelled) { setResponse(data); clearErrorRetries(); }
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

  if (isPending && response === null) return <FilteredPageSkeleton variant="vulnerabilities" />;

  if (error) {
    return <ErrorPage showGoBack={false} onRetry={reloadPage} />;
  }

  // Use defaults while pending or if no response
  const vulnerabilities = response?.hits ?? [];
  const total = response?.total ?? 0;
  const aggregations = response?.aggregations ?? {};
  const offset = response?.offset ?? 0;
  const limit = response?.limit ?? 25;

  // Convert search params to Record once for all components
  const paramsRecord = toParamsRecord(searchParams);

  return (
    <PageLayout>
      <FilteredPageLayout
        aggregations={aggregations}
        searchParams={paramsRecord}
        customFilterConfigs={{ ...VULNERABILITY_FILTER_ORDER, ...vulnerabilityFilterDefinitions }}
        formAction="/vulnerabilities"
        searchPlaceholder="Search vulnerabilities..."
        sortOptions={vulnerabilitySortOptions}
        totalResults={total}
        header={<VulnerabilitiesHeader total={total} />}
        clearRemovesQuery
      >
        {/* Empty state (only show when not loading and no results) */}
        {!isPending && vulnerabilities.length === 0 && (
          <EmptyVulnerabilitiesResults formAction="/vulnerabilities" />
        )}

        {/* Results list (shows skeleton or actual results) */}
        {(isPending || vulnerabilities.length > 0) && (
          <VulnerabilitiesResultsList
            vulnerabilities={vulnerabilities}
            isPending={isPending}
            limit={limit}
            moduleName="vulnerabilities-page"
            renderLinkWrapper={({ vulnerability, children }) => (
              <Link to={`/vulnerability/${vulnerability.vulnId}`} className="unstyled-link">
                {children}
              </Link>
            )}
          />
        )}
        {total > limit && (
          <Pagination
            formAction="/vulnerabilities"
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
