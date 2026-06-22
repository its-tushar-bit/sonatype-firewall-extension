/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo, useState } from 'react';
import { useCurrentStateAndParams } from '@uirouter/react';
import { Box, Flex, Grid, Text, Theme } from '@radix-ui/themes';
import { Card, PageHeading, SectionHeading } from '@sonatype/nexus-one-components';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { useGlobalSearch } from 'MainRoot/nosc/search/useGlobalSearch';
import {
  RENDERED_ITEM_TYPES,
  SearchResultItemDTO,
} from 'MainRoot/nosc/search/searchTypes';
import { clickHrefFor } from 'MainRoot/nosc/search/searchClickTargets';
import router from 'MainRoot/router/routerInstance';
import { SearchResultsTabs } from 'MainRoot/nosc/searchResults/SearchResultsTabs';
import { SearchResultsFilters } from 'MainRoot/nosc/searchResults/SearchResultsFilters';
import { SearchResultsList } from 'MainRoot/nosc/searchResults/SearchResultsList';

// Radix styles and the Nexus One brand/Radix palette tokens are loaded once
// at the SPA bundle entry (`nexus-one/App.tsx`, via the side-effect import of
// `@sonatype/nexus-one-components`, which ships brand-colors.css + radix-palette.css)
// and the Classic bundle (`ClassicToggleButton.tsx`). They apply app-wide
// regardless of which route mounts first, so leaf pages must not re-import them.

/**
 * P1-F13: Full /search results page mounted by UI-Router at
 * `/search?q=<query>&tab=<itemType>`.
 *
 * Layout: 250px filter sidebar + 1fr main column (header + tabs + list).
 *
 * Phase 1 implementation notes:
 *   - Backend (/api/v2/search/advanced) doesn't accept itemType / ecosystem
 *     / severity filters. Tabs and filters are CLIENT-SIDE post-filters
 *     for now; we fetch a generous page (50) and filter in the browser.
 *   - URL state: ?q= (query), ?tab= (active entity type or omitted for 'all').
 *   - Pagination is implicit in pageSize for now; "Load more" is Phase 1.5.
 */

const RESULTS_PAGE_SIZE = 50;

export function SearchResultsPage(): JSX.Element {
  const { params } = useCurrentStateAndParams();
  const query = typeof params.q === 'string' ? params.q : '';
  const tabParam = typeof params.tab === 'string' && params.tab ? params.tab : 'all';
  const activeTab =
    tabParam === 'all' || (RENDERED_ITEM_TYPES as readonly string[]).includes(tabParam)
      ? tabParam
      : 'all';
  const [filterEcosystems, setFilterEcosystems] = useState<readonly string[]>([]);

  // Ecosystem (componentIdentifier.format) only exists on component results, so the
  // ecosystem filter is only meaningful on the "All" and Components tabs. On other
  // tabs (Applications, Vulnerabilities, Policy Violations, Waivers) it would silently
  // filter everything out, so we hide the sidebar AND skip the filter there.
  const showEcosystemFilter =
    activeTab === 'all' || activeTab === 'NON_VULNERABLE_COMPONENT';

  // Clear ecosystem filters when the query changes, not when switching tabs.
  useEffect(() => {
    setFilterEcosystems([]);
  }, [query]);

  const { loading, loadError, results, totalHits, hitsByType } = useGlobalSearch(query, {
    mode: 'full',
    pageSize: RESULTS_PAGE_SIZE,
  });

  const countsByType = useMemo(() => {
    const counts: Record<string, number> = { all: totalHits };
    for (const t of RENDERED_ITEM_TYPES) {
      counts[t] = hitsByType[t] ?? 0;
    }
    return counts;
  }, [totalHits, hitsByType]);

  const filteredResults = useMemo(() => {
    let out: readonly SearchResultItemDTO[] = results;
    if (activeTab !== 'all') {
      out = out.filter((r) => r.itemType === activeTab);
    }
    if (showEcosystemFilter && filterEcosystems.length > 0) {
      // Case-insensitive match: the selectable ECOSYSTEMS labels and the
      // backend's componentIdentifier.format can differ in case (e.g. "npm"
      // vs "NPM"), so normalize both sides before comparing.
      const selected = new Set(filterEcosystems.map((e) => e.toLowerCase()));
      out = out.filter((r) => {
        // Ecosystem is a component-only attribute. On the "All" tab the result set is
        // mixed, so the filter must only constrain component rows — apps, vulnerabilities,
        // policies, and waivers have no ecosystem and pass through unfiltered. (On the
        // Components tab every row is a component, so this behaves as a plain filter.)
        if (r.itemType !== 'NON_VULNERABLE_COMPONENT') return true;
        const eco = r.componentIdentifier?.format;
        return eco != null && selected.has(eco.toLowerCase());
      });
    }
    return out;
  }, [results, activeTab, filterEcosystems, showEcosystemFilter]);

  const relevantTotal =
    activeTab === 'all' ? totalHits : (hitsByType[activeTab] ?? 0);

  const handleTabChange = (newTab: string): void => {
    router.stateService.go('nexusOneSearch', {
      q: query || undefined,
      tab: newTab !== 'all' ? newTab : undefined,
    });
  };

  const handleResultClick = (resultDTO: SearchResultItemDTO): void => {
    window.location.assign(clickHrefFor(resultDTO));
  };

  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();

  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      hasBackground={false}
      data-testid="nosc-search-results-page"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        pointerEvents: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <Box p="6" style={{ maxWidth: '1400px', margin: '0 auto' }}>
        <Flex direction="column" gap="5">
          <PageHeading as="h1">
            {query ? `Search results for "${query}"` : 'Search'}
          </PageHeading>
          {query && relevantTotal > 0 && (
            <Text size="2" color="gray" data-testid="nosc-search-results-hit-summary">
              {filteredResults.length < relevantTotal
                ? `Showing ${filteredResults.length.toLocaleString()} of ${relevantTotal.toLocaleString()} matches`
                : `${relevantTotal.toLocaleString()} matches`}
            </Text>
          )}

          {!query && <EmptyQuery />}

          {query && (
            <>
              <SearchResultsTabs
                activeTab={activeTab}
                countsByType={countsByType}
                onTabChange={handleTabChange}
                loading={loading}
              />
              <Grid
                columns={showEcosystemFilter ? { initial: '1', sm: '250px 1fr' } : '1'}
                gap="6"
              >
                {showEcosystemFilter && (
                  <Box display={{ initial: 'none', sm: 'block' }}>
                    <SearchResultsFilters
                      ecosystems={filterEcosystems}
                      onEcosystemsChange={setFilterEcosystems}
                      onClearAll={() => setFilterEcosystems([])}
                    />
                  </Box>
                )}
                <Box minWidth="0">
                  <SearchResultsList
                    results={filteredResults}
                    loading={loading}
                    loadError={loadError}
                    query={query}
                    onResultClick={handleResultClick}
                  />
                </Box>
              </Grid>
            </>
          )}
        </Flex>
      </Box>
    </Theme>
  );
}

function EmptyQuery(): JSX.Element {
  return (
    <Card size="4" data-testid="nosc-search-results-empty-query">
      <Flex direction="column" align="center" gap="3" p="4">
        <ActionIcons.Search size={24} color="var(--gray-9)" />
        <SectionHeading as="h2" align="center">
          Search across all of IQ
        </SectionHeading>
        <Text size="2" color="gray" align="center" style={{ maxWidth: '480px' }}>
          Type in the search bar above to find applications, components,
          vulnerabilities, policies, and more across your IQ instance.
        </Text>
      </Flex>
    </Card>
  );
}
