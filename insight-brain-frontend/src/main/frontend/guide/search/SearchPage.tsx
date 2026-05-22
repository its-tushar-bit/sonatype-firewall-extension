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
  buildSearchFilters,
  buildComponentFilters,
  buildVulnerabilityFilters,
  getOffsetFromParams,
  getLimitFromParams,
  getSortFromParams,
  getStringParam,
  getComponentDetailUrl,
  formatNumber,
  tokens,
} from '@guide/ui-core/utils';
import { searchAll, searchComponents, searchVulnerabilities } from 'GuideRoot/api/searchBackend';
import { toParamsRecord } from 'GuideRoot/utils/searchParams';
import { FilteredPageSkeleton } from 'GuideRoot/layout/FilteredPageSkeleton';
import type {
  SearchResponse,
  ComponentSearchResponse,
  VulnerabilitySearchResponse,
  GlobalSearchOptions,
  ComponentsSearchOptions,
  VulnerabilitiesSearchOptions,
  Component,
  Vulnerability,
} from '@guide/ui-core/types';

const LIMIT = 25;

type ActiveTab = 'all' | 'components' | 'vulnerabilities';
type TabResponse = SearchResponse | ComponentSearchResponse | VulnerabilitySearchResponse;

function readActiveTab(params: Record<string, string | string[]>): ActiveTab {
  const t = getStringParam(params, 'tab', 'all');
  return t === 'components' || t === 'vulnerabilities' ? t : 'all';
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
    default:
      return {
        // Override byEcosystem.fieldName to "formats" to match Guide SaaS — buildSearchFilters
        // reads params.formats, but the upstream sharedFilterDefinitions uses "affectedEcosystems".
        filterConfigs: {
          ...globalSearchFilterDefinitions,
          byEcosystem: {
            ...globalSearchFilterDefinitions.byEcosystem,
            fieldName: 'formats',
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
  const [allData, setAllData] = useState<SearchResponse | null>(null);
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

    const { sortField, sortOrder } = getSortFromParams(paramsRecord);
    const offset = getOffsetFromParams(paramsRecord);
    const limit = getLimitFromParams(paramsRecord, LIMIT);

    let combinedPromise: Promise<{ tab: TabResponse; all: SearchResponse }>;
    if (activeTab === 'components') {
      const filters = buildComponentFilters(paramsRecord);
      const options: ComponentsSearchOptions = { offset, limit };
      if (sortField) options.sortField = sortField;
      if (sortOrder) options.sortOrder = sortOrder;
      const tabPromise = searchComponents({
        query,
        filters: Object.keys(filters).length ? filters : undefined,
        options,
      });
      const allPromise = searchAll({ query, options: { offset: 0, limit: 1 } });
      combinedPromise = Promise.all([tabPromise, allPromise])
        .then(([tab, all]) => ({ tab, all }));
    } else if (activeTab === 'vulnerabilities') {
      const filters = buildVulnerabilityFilters(paramsRecord);
      const options: VulnerabilitiesSearchOptions = { offset, limit };
      if (sortField) options.sortField = sortField;
      if (sortOrder) options.sortOrder = sortOrder;
      const tabPromise = searchVulnerabilities({
        query,
        filters: Object.keys(filters).length ? filters : undefined,
        options,
      });
      const allPromise = searchAll({ query, options: { offset: 0, limit: 1 } });
      combinedPromise = Promise.all([tabPromise, allPromise])
        .then(([tab, all]) => ({ tab, all }));
    } else {
      const filters = buildSearchFilters(paramsRecord);
      const options: GlobalSearchOptions = { offset, limit };
      if (sortField) options.sortField = sortField;
      if (sortOrder) options.sortOrder = sortOrder;
      // On the All tab, the tab response IS the global SearchResponse — no need
      // to issue a second searchAll call for cross-type totals.
      combinedPromise = searchAll({
        query,
        filters: Object.keys(filters).length ? filters : undefined,
        options,
      }).then((tab) => ({ tab, all: tab }));
    }

    combinedPromise
      .then(({ tab, all }) => {
        if (cancelled) return;
        setTabData(tab);
        setAllData(all);
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

  const isPending = loading || filterPending || toolbarPending;
  const total = tabData?.total ?? 0;
  const aggregations = tabData?.aggregations ?? {};
  const offset = getOffsetFromParams(paramsRecord);

  const totalsSource = allData?.aggregations?.byType ?? {};
  const totalComponents = totalsSource.component ?? 0;
  const totalVulnerabilities = totalsSource.vulnerability ?? 0;
  const totalAll = allData?.total ?? (totalComponents + totalVulnerabilities);

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
    const components = (tabData as ComponentSearchResponse | null)?.hits ?? [];
    resultsNode = !isPending && components.length === 0
      ? emptyResultsCard
      : <ComponentsResultsList components={components} isPending={isPending} limit={limit} />;
  } else if (activeTab === 'vulnerabilities') {
    const vulnerabilities = (tabData as VulnerabilitySearchResponse | null)?.hits ?? [];
    resultsNode = !isPending && vulnerabilities.length === 0
      ? emptyResultsCard
      : (
        <VulnerabilitiesResultsList
          vulnerabilities={vulnerabilities}
          isPending={isPending}
          limit={limit}
          moduleName="search-page"
          renderLinkWrapper={({ vulnerability, children }: { vulnerability: Vulnerability; children: ReactNode }) => (
            <Link to={`/vulnerabilities/${vulnerability.vulnId}`} className="unstyled-link">{children}</Link>
          )}
        />
      );
  } else {
    const hits = (tabData as SearchResponse | null)?.hits ?? [];
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
            <Link to={`/vulnerabilities/${vulnerability.vulnId}`} className="unstyled-link">{children}</Link>
          )}
        />
      );
  }

  return (
    <PageLayout>
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
