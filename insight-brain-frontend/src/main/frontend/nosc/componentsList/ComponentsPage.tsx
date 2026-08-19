/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Box, Button, Flex, Heading, Text } from '@radix-ui/themes';
import { AsyncPageState, AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import ComponentCardGrid from 'MainRoot/nosc/componentsList/ComponentCardGrid';
import ComponentsFilterRail from 'MainRoot/nosc/componentsList/ComponentsFilterRail';
import ComponentsToolbar from 'MainRoot/nosc/componentsList/ComponentsToolbar';
import {
  ComponentListRow,
  ComponentsFilterFacetCounts,
} from 'MainRoot/nosc/componentsList/componentListTypes';
import {
  ComponentsFilterSetGroup,
  ComponentsListFilterState,
  ComponentsThreatRange,
} from 'MainRoot/nosc/componentsList/componentsListFilters';
import type { ComponentsTab } from 'MainRoot/nosc/componentsList/componentsRoute';

import './componentsPageLayout.css';

export interface ComponentsPageProps {
  readonly tab: ComponentsTab;
  readonly onTabChange: (tab: ComponentsTab) => void;
  readonly components: ReadonlyArray<ComponentListRow>;
  readonly facets: ComponentsFilterFacetCounts;
  readonly filters: ComponentsListFilterState;
  readonly hasActiveFilters: boolean;
  readonly onToggleFilter: (group: ComponentsFilterSetGroup, id: string) => void;
  readonly onThreatRangeChange: (range: ComponentsThreatRange) => void;
  readonly onResetFilters: () => void;
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly info?: AsyncPageStateInfoProps | null;
  readonly onRetry?: () => void;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  /** RBAC-scoped total from the catalog API (may exceed the current page length). */
  readonly totalCount: number;
  /** False when the backend capped the estimate (toolbar shows N+). */
  readonly exactTotalEstimate?: boolean;
  /** False when Sonatype Catalog federation is unavailable. */
  readonly catalogAvailable?: boolean;
  /** 1-based page index for {@link Pagination}. */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage?: boolean;
  readonly onPageChange: (nextPage: number) => void;
}

/**
 * Martha V1 Components page shell (CLM-42214).
 * Filter rail + My Scan Data / Sonatype Catalog toolbar + catalog-backed card grid.
 */
export default function ComponentsPage({
  tab,
  onTabChange,
  components,
  facets,
  filters,
  hasActiveFilters,
  onToggleFilter,
  onThreatRangeChange,
  onResetFilters,
  loading = false,
  error = null,
  info = null,
  onRetry,
  searchValue,
  onSearchSubmit,
  totalCount,
  exactTotalEstimate = true,
  catalogAvailable = true,
  page,
  pageSize,
  hasNextPage = false,
  onPageChange,
}: ComponentsPageProps): JSX.Element {
  const offsets = usePreviewShellOffsets();

  return (
    <Box
      asChild
      p="6"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <main data-testid="preview-components-page">
        <Flex direction="column" gap="2" mb="5">
          <Flex align="center" gap="3">
            <DomainIcons.Component size={28} color="var(--accent-9)" />
            <Heading size="6">Components</Heading>
          </Flex>
          <Text size="2" color="gray">
            Estate components from your scans, plus the Sonatype Catalog.
          </Text>
        </Flex>

        <Flex gap="4" align="start" wrap="wrap" data-testid="components-page-layout">
          <ComponentsFilterRail
            tab={tab}
            facets={facets}
            filters={filters}
            hasActiveFilters={hasActiveFilters}
            onToggleFilter={onToggleFilter}
            onThreatRangeChange={onThreatRangeChange}
            onResetFilters={onResetFilters}
          />
          <Box className="components-page__content" data-testid="components-page-content">
            <Flex direction="column" gap="4">
              <ComponentsToolbar
                tab={tab}
                onTabChange={onTabChange}
                totalCount={totalCount}
                exactTotalEstimate={exactTotalEstimate}
                catalogAvailable={catalogAvailable}
                searchValue={searchValue}
                onSearchSubmit={onSearchSubmit}
                filters={filters}
              />

              <AsyncPageState
                loading={loading}
                error={error}
                info={info}
                onRetry={onRetry}
                loadingTestId="components-list-loading"
                errorTestId="components-list-error"
                errorTitle="Failed to load components"
                errorVariant="banner"
                infoVariant="banner"
              >
                {components.length === 0 ? (
                  <Flex
                    direction="column"
                    align="center"
                    gap="2"
                    py="8"
                    data-testid="components-list-empty"
                  >
                    <DomainIcons.Component size={32} color="var(--gray-9)" />
                    <Text size="3" color="gray">
                      {searchValue && hasActiveFilters
                        ? 'No components match your search and filters.'
                        : searchValue
                          ? 'No components match your search.'
                          : hasActiveFilters
                            ? 'No components match the selected filters.'
                            : tab === 'catalog'
                              ? 'No catalog components match this view.'
                              : 'No components in scope'}
                    </Text>
                    <Text size="2" color="gray">
                      {searchValue || hasActiveFilters
                        ? 'Try adjusting or clearing your search and filters.'
                        : tab === 'catalog'
                          ? 'Browse the Sonatype Catalog once federation is available.'
                          : 'Components visible to your account will appear here once scans exist.'}
                    </Text>
                    <Flex gap="2" mt="1">
                      {searchValue && (
                        <Button
                          variant="soft"
                          size="2"
                          onClick={() => onSearchSubmit('')}
                          data-testid="components-empty-clear-search"
                        >
                          <ActionIcons.Refresh size={14} aria-hidden />
                          Clear search
                        </Button>
                      )}
                      {hasActiveFilters && (
                        <Button
                          variant="soft"
                          size="2"
                          onClick={onResetFilters}
                          data-testid="components-empty-reset-filters"
                        >
                          <ActionIcons.Refresh size={14} aria-hidden />
                          Reset filters
                        </Button>
                      )}
                    </Flex>
                  </Flex>
                ) : (
                  <>
                    <ComponentCardGrid components={components} />
                    {(totalCount > pageSize || page > 1 || hasNextPage) && (
                      <Pagination
                        page={page}
                        pageSize={pageSize}
                        totalItems={totalCount}
                        hasNextPage={hasNextPage}
                        onPageChange={onPageChange}
                        data-testid="components-list-pagination"
                      />
                    )}
                  </>
                )}
              </AsyncPageState>
            </Flex>
          </Box>
        </Flex>
      </main>
    </Box>
  );
}
