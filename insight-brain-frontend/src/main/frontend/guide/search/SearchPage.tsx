/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { Search } from 'lucide-react';
import { Box, Callout, Flex } from '@radix-ui/themes';
import {
  PageLayout,
  PageHeading,
  BodyText,
  FilteredPageLayout,
  FilterSidebar,
  SearchTabs,
  SearchResultsList,
  ComponentsResultsList,
  VulnerabilitiesResultsList,
  SecurityEventResultsList,
  EmptyResultsCard,
  Pagination,
  useAdapterSearchParams,
} from '@guide/ui-core';
import {
  globalSearchFilterDefinitions,
  globalSearchSortOptions,
  componentFilterDefinitions,
  componentSortOptions,
  COMPONENT_FILTER_ORDER,
  vulnerabilityFilterDefinitions,
  vulnerabilitySortOptions,
  VULNERABILITY_FILTER_ORDER,
  securityEventSortOptions,
  SECURITY_EVENT_FILTER_ORDER,
  extractTabCounts,
  getOffsetFromParams,
  getLimitFromParams,
  getStringParam,
  getComponentDetailUrl,
  formatNumber,
  mergeAggregations,
  tokens,
  type Aggregations,
} from '@guide/ui-core/utils';
import { searchAll, fetchGlobalSearchTotals } from 'GuideRoot/api/searchBackend';
import {
  searchComponents,
  fetchComponentBrowseAggregations,
} from 'GuideRoot/api/componentsBackend';
import {
  searchVulnerabilities,
  fetchVulnerabilityBrowseAggregations,
} from 'GuideRoot/api/vulnerabilitiesBackend';
import {
  searchSecurityEvents,
  fetchSecurityEventBrowseAggregations,
} from 'GuideRoot/api/securityEventsBackend';
import type { ApiSearchResponse } from 'GuideRoot/api/securityEventsBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import { FilteredPageSkeleton } from 'GuideRoot/layout/FilteredPageSkeleton';
import { PolicyContextBar } from 'GuideRoot/components/navigation/context-picker/PolicyContextBar';
import type {
  SearchResponse,
  ComponentSearchResponse,
  VulnerabilitySearchResponse,
  Component,
  Vulnerability,
  SecurityEventDocument,
} from '@guide/ui-core/types';

const LIMIT = 25;

type ActiveTab = 'all' | 'components' | 'vulnerabilities' | 'securityEvents';
type TabResponse =
  | SearchResponse
  | ComponentSearchResponse
  | VulnerabilitySearchResponse
  | ApiSearchResponse<SecurityEventDocument>;

function readActiveTab(params: Record<string, string | string[]>): ActiveTab {
  const t = getStringParam(params, 'tab', 'all');
  return t === 'components' || t === 'vulnerabilities' || t === 'securityEvents' ? t : 'all';
}

function buildTabConfig(activeTab: ActiveTab) {
  switch (activeTab) {
    case 'components':
      return {
        filterConfigs: { ...COMPONENT_FILTER_ORDER, ...componentFilterDefinitions },
        sortOptions: componentSortOptions,
        placeholder: 'Search components...',
      };
    case 'vulnerabilities':
      return {
        filterConfigs: { ...VULNERABILITY_FILTER_ORDER, ...vulnerabilityFilterDefinitions },
        sortOptions: vulnerabilitySortOptions,
        placeholder: 'Search vulnerabilities...',
      };
    case 'securityEvents':
      return {
        filterConfigs: SECURITY_EVENT_FILTER_ORDER,
        sortOptions: securityEventSortOptions,
        placeholder: 'Filter security events',
      };
    default:
      return {
        // Override fieldNames to match the GuideGlobalSearchResource @QueryParam keys —
        // the upstream sharedFilterDefinitions uses "affectedEcosystems"/"lastUpdated"
        // but the backend reads "formats"/"publishedWindow".
        filterConfigs: {
          ...globalSearchFilterDefinitions,
          byEcosystem: {
            ...globalSearchFilterDefinitions.byEcosystem,
            fieldName: 'formats',
          },
          byLastUpdated: {
            ...globalSearchFilterDefinitions.byLastUpdated,
            fieldName: 'publishedWindow',
          },
        },
        sortOptions: globalSearchSortOptions,
        placeholder: 'Search components and vulnerabilities...',
      };
  }
}

export function SearchPage() {
  const searchParams = useAdapterSearchParams();
  const [tabData, setTabData] = useState<TabResponse | null>(null);
  const [tabDataFor, setTabDataFor] = useState<ActiveTab | null>(null);
  const [allData, setAllData] = useState<SearchResponse | null>(null);
  const [browseAggregations, setBrowseAggregations] = useState<Aggregations | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterPending, setFilterPending] = useState(false);
  const [toolbarPending, setToolbarPending] = useState(false);

  const paramsRecord = toParamsRecord(searchParams);
  const activeTab = readActiveTab(paramsRecord);
  const query = getStringParam(paramsRecord, 'query', '') || undefined;
  const hasQuery = !!query;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    // Clear stale browse aggregations so a tab switch (e.g. components ->
    // vulnerabilities) doesn't briefly merge the previous tab's facet universe
    // (byFormat/byCategory vs byEcosystem/bySeverity) into the new sidebar.
    setBrowseAggregations(null);

    let combinedPromise: Promise<{ tab: TabResponse; all: SearchResponse; browse: Aggregations | null }>;
    if (activeTab === 'components') {
      const params = new URLSearchParams(searchParams.toString());
      if (!params.has('limit')) params.set('limit', String(LIMIT));
      combinedPromise = Promise.all([
        searchComponents(params),
        fetchGlobalSearchTotals(query),
        fetchComponentBrowseAggregations(),
      ]).then(([tab, all, browse]) => ({ tab, all, browse }));
    } else if (activeTab === 'vulnerabilities') {
      const params = new URLSearchParams(searchParams.toString());
      if (!params.has('limit')) params.set('limit', String(LIMIT));
      combinedPromise = Promise.all([
        searchVulnerabilities(params),
        fetchGlobalSearchTotals(query),
        fetchVulnerabilityBrowseAggregations(),
      ]).then(([tab, all, browse]) => ({ tab, all, browse }));
    } else if (activeTab === 'securityEvents') {
      const params = new URLSearchParams(searchParams.toString());
      if (!params.has('limit')) params.set('limit', String(LIMIT));
      // Dedicated Security Events tab: query the /security-events endpoint directly so the
      // SE-only filters (severity, threat type, known-exploited, ecosystem) and sort apply.
      // Other-tab badge counts still come from the shared global-search byType aggregation.
      combinedPromise = Promise.all([
        searchSecurityEvents(params),
        fetchGlobalSearchTotals(query),
        fetchSecurityEventBrowseAggregations(),
      ]).then(([tab, all, browse]) => ({ tab, all, browse }));
    } else {
      const params = new URLSearchParams(searchParams.toString());
      if (!params.has('limit')) params.set('limit', String(LIMIT));
      // On the All tab, the tab response IS the global SearchResponse — no need
      // to issue a second searchAll call for cross-type totals. No browse
      // aggregations either: GuideGlobalSearchResource doesn't return the
      // per-type facet universe and minDocCount isn't supported there.
      combinedPromise = searchAll(params).then((tab) => ({ tab, all: tab, browse: null }));
    }

    combinedPromise
      .then(({ tab, all, browse }) => {
        if (cancelled) return;
        setTabData(tab);
        setTabDataFor(activeTab);
        setAllData(all);
        setBrowseAggregations(browse);
      })
      .catch((e) => { if (!cancelled) setError(e instanceof Error ? e.message : String(e)); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [searchParams]);

  if (loading && tabData === null) return <FilteredPageSkeleton variant="search" />;

  if (error && !tabData) {
    return (
      <PageLayout>
        <Callout.Root color="red" role="alert">
          <Callout.Text>Error loading search: {error}</Callout.Text>
        </Callout.Root>
      </PageLayout>
    );
  }

  // After a tab switch, `tabData` still holds the previous tab's response (e.g. component-shape
  // hits from the All tab) until the new fetch completes. Rendering that into the new tab's list
  // would produce cards with undefined fields and links like `/vulnerability/undefined`, which
  // `@guide/ui-core` 1.10 leaves as orphan DOM because every duplicate key collapses in the
  // reconciler. Treat the cross-tab data as still-pending so the list shows skeletons.
  const tabDataMatchesActiveTab = tabDataFor === activeTab;
  const effectiveTabData = tabDataMatchesActiveTab ? tabData : null;
  const isPending = loading || filterPending || toolbarPending || !tabDataMatchesActiveTab;
  const total = effectiveTabData?.total ?? 0;
  const aggregations =
    mergeAggregations(browseAggregations, effectiveTabData?.aggregations as Aggregations | undefined) ?? {};
  const offset = getOffsetFromParams(paramsRecord);

  // Per-tab badge counts come from the shared global-search byType aggregation. extractTabCounts
  // centralizes the plural component/vulnerability keys and the hyphenated `security-events` key so
  // they can't silently drift. totalAll falls back to the summed type counts when the response
  // omits its own total.
  const {
    totalComponents,
    totalVulnerabilities,
    totalSecurityEvents: byTypeTotalSecurityEvents,
  } = extractTabCounts(allData?.aggregations?.byType, allData?.total ?? 0);
  const totalAll =
    allData?.total ?? totalComponents + totalVulnerabilities + byTypeTotalSecurityEvents;
  // On the dedicated Security Events tab the badge reflects the fully-filtered SE total from the
  // /security-events endpoint; other tabs fall back to the byType-derived count (0 if the IQ
  // global-search backend doesn't emit a security-events bucket).
  const totalSecurityEvents =
    activeTab === 'securityEvents' ? total : byTypeTotalSecurityEvents;

  const tabConfig = buildTabConfig(activeTab);
  const limit = getLimitFromParams(paramsRecord, LIMIT);
  const showPagination = total > limit;

  const emptyResultsCard = (
    <EmptyResultsCard
      icon={Search}
      title="No results found"
      description="Try adjusting your search or filters to find what you're looking for."
    />
  );

  let resultsNode: ReactNode;
  if (activeTab === 'components') {
    const components = (effectiveTabData as ComponentSearchResponse | null)?.hits ?? [];
    resultsNode = !isPending && components.length === 0
      ? emptyResultsCard
      : <ComponentsResultsList components={components} isPending={isPending} limit={limit} />;
  } else if (activeTab === 'vulnerabilities') {
    const vulnerabilities = (effectiveTabData as VulnerabilitySearchResponse | null)?.hits ?? [];
    resultsNode = !isPending && vulnerabilities.length === 0
      ? emptyResultsCard
      : (
        <VulnerabilitiesResultsList
          vulnerabilities={vulnerabilities}
          isPending={isPending}
          limit={limit}
          moduleName="search-page"
          renderLinkWrapper={({ vulnerability, children }: { vulnerability: Vulnerability; children: ReactNode }) => (
            <Link to={`/vulnerability/${vulnerability.vulnId}`} className="unstyled-link">{children}</Link>
          )}
        />
      );
  } else if (activeTab === 'securityEvents') {
    const events = (effectiveTabData as ApiSearchResponse<SecurityEventDocument> | null)?.hits ?? [];
    resultsNode = !isPending && events.length === 0
      ? emptyResultsCard
      : (
        <SecurityEventResultsList
          events={events}
          isPending={isPending}
          limit={limit}
          moduleName="search-page"
          renderLinkWrapper={({ event, children }: { event: SecurityEventDocument; children: ReactNode }) => (
            <Link to={`/security-event/${encodeURIComponent(event.eventId)}`} className="unstyled-link">{children}</Link>
          )}
        />
      );
  } else {
    const hits = (effectiveTabData as SearchResponse | null)?.hits ?? [];
    resultsNode = !isPending && hits.length === 0
      ? emptyResultsCard
      : (
        <SearchResultsList
          results={hits}
          isPending={isPending}
          limit={limit}
          activeTab={activeTab}
          renderComponentLink={(component: Component, _index: number, children: ReactNode) => (
            <Link to={getComponentDetailUrl(component)} className="unstyled-link">{children}</Link>
          )}
          renderVulnerabilityLink={(vulnerability: Vulnerability, _index: number, children: ReactNode) => (
            <Link to={`/vulnerability/${vulnerability.vulnId}`} className="unstyled-link">{children}</Link>
          )}
          renderSecurityEventLink={(event: SecurityEventDocument, _index: number, children: ReactNode) => (
            <Link to={`/security-event/${encodeURIComponent(event.eventId)}`} className="unstyled-link">{children}</Link>
          )}
        />
      );
  }

  return (
    <PageLayout>
      <PolicyContextBar />
      <FilteredPageLayout
        aggregations={aggregations}
        formAction="/search"
        searchParams={paramsRecord}
        customFilterConfigs={tabConfig.filterConfigs}
        searchPlaceholder={tabConfig.placeholder}
        sortOptions={tabConfig.sortOptions}
        clearRemovesQuery
        hasQuery={hasQuery}
        totalResults={total}
        hideSidebar
        stackHeaderToolbar
        header={
          <Flex align="center" gap={tokens.space.inline} flexShrink="0">
            <PageHeading>Search Results</PageHeading>
            {query && (
              <BodyText tone="subtle" asChild>
                <output aria-live="polite">
                  {formatNumber(totalAll)} for &quot;<strong>{query}</strong>&quot;
                </output>
              </BodyText>
            )}
          </Flex>
        }
        subheader={
          <SearchTabs
            activeTab={activeTab}
            totalAll={totalAll}
            totalComponents={totalComponents}
            totalVulnerabilities={totalVulnerabilities}
            totalSecurityEvents={totalSecurityEvents}
            showSecurityEventsTab
            searchParams={paramsRecord}
          />
        }
        onFilterSidebarPendingChange={setFilterPending}
        onToolbarPendingChange={setToolbarPending}
      >
        <Flex gap={tokens.space.section}>
          <Box flexShrink="0" width="280px" display={{ initial: 'none', md: 'block' }}>
            <FilterSidebar
              aggregations={aggregations}
              formAction="/search"
              searchParams={paramsRecord}
              customFilterConfigs={tabConfig.filterConfigs}
              onPendingChange={setFilterPending}
            />
          </Box>
          <Box flexBasis="1" minWidth="0" width="100%">
            <Flex direction="column" gap={tokens.space.section}>
              {error && tabData && (
                <Callout.Root color="red" role="alert">
                  <Callout.Text>Error loading search: {error}</Callout.Text>
                </Callout.Root>
              )}
              {resultsNode}
              {showPagination && (
                <Pagination
                  formAction="/search"
                  searchParams={paramsRecord}
                  total={total}
                  offset={offset}
                  limit={limit}
                  onPendingChange={setToolbarPending}
                />
              )}
            </Flex>
          </Box>
        </Flex>
      </FilteredPageLayout>
    </PageLayout>
  );
}
