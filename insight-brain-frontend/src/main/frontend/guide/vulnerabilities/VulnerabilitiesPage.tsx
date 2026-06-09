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
  mergeAggregations,
  VULNERABILITY_FILTER_ORDER,
  type Aggregations,
} from '@guide/ui-core/utils';
import {
  searchVulnerabilities,
  fetchVulnerabilityBrowseAggregations,
} from 'GuideRoot/api/vulnerabilitiesBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import { FilteredPageSkeleton } from 'GuideRoot/layout/FilteredPageSkeleton';
import { ErrorPage } from 'GuideRoot/layout/ErrorPage';
import { reloadPage, clearErrorRetries } from 'GuideRoot/utils/navigation';
import { useFeatureFlags } from 'GuideRoot/feature-flags/FeatureFlagProvider';
import { FEATURE_FLAGS } from 'GuideRoot/feature-flags/featureFlags';
import type { VulnerabilitySearchResponse } from '@guide/ui-core/types';

const LIMIT = 25;

export function VulnerabilitiesPage() {
  const searchParams = useAdapterSearchParams();
  const { isFeatureEnabled, isLoading: flagsLoading } = useFeatureFlags();
  const [response, setResponse] = useState<VulnerabilitySearchResponse | null>(null);
  const [browseAggregations, setBrowseAggregations] = useState<Aggregations | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isPending, setIsPending] = useState(false);
  useEffect(() => {
    if (flagsLoading) return;
    let cancelled = false;

    const params = new URLSearchParams(searchParams.toString());
    if (!isFeatureEnabled(FEATURE_FLAGS.GUIDE_SEARCH)) params.delete('query');
    if (!params.has('limit')) params.set('limit', String(LIMIT));

    if (!params.has('sortField')) params.set('sortField', 'publishedDate');
    if (!params.has('sortOrder')) params.set('sortOrder', 'desc');

    setError(null);
    setIsPending(true);

    Promise.all([
      searchVulnerabilities(params),
      fetchVulnerabilityBrowseAggregations(),
    ])
      .then(([data, browse]) => {
        if (!cancelled) { setResponse(data); setBrowseAggregations(browse); clearErrorRetries(); }
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
  }, [searchParams, flagsLoading]);

  if ((isPending || flagsLoading) && response === null) return <FilteredPageSkeleton variant="vulnerabilities" />;

  if (error) {
    return <ErrorPage showGoBack={false} onRetry={reloadPage} />;
  }

  // Use defaults while pending or if no response
  const vulnerabilities = response?.hits ?? [];
  const total = response?.total ?? 0;
  const aggregations =
    mergeAggregations(browseAggregations, response?.aggregations as Aggregations | undefined) ?? {};
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
        hideSearch={!isFeatureEnabled(FEATURE_FLAGS.GUIDE_SEARCH)}
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
