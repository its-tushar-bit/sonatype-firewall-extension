/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { useRouter } from '@uirouter/react';
import { Badge, Box, Button, Flex, Heading, Text } from '@radix-ui/themes';
import { AsyncPageState, AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import {
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  DEFAULT_AUTO_WAIVER_OWNER_ID,
  DEFAULT_AUTO_WAIVER_OWNER_TYPE,
} from 'MainRoot/nosc/waivers/autoWaiversApi';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import WaiversFilterRail from 'MainRoot/nosc/waivers/WaiversFilterRail';
import WaiversToolbar from 'MainRoot/nosc/waivers/WaiversToolbar';
import WaiversAnaCardList from 'MainRoot/nosc/waivers/WaiversAnaCardList';
import type { AnaWaiverRow, WaiversFilterFacetCounts } from 'MainRoot/nosc/waivers/waiversListTypes';
import type {
  WaiversFilterSetGroup,
  WaiversListFilterState,
} from 'MainRoot/nosc/waivers/waiversListFilters';
import type { WaiversListOrderBy } from 'MainRoot/nosc/waivers/waiversListQuery';

import './waiversPageLayout.css';

export interface WaiversAnaPageProps {
  readonly waivers: ReadonlyArray<AnaWaiverRow>;
  readonly facets: WaiversFilterFacetCounts;
  readonly filters: WaiversListFilterState;
  readonly hasActiveFilters: boolean;
  readonly onToggleFilter: (group: WaiversFilterSetGroup, id: string) => void;
  readonly onResetFilters: () => void;
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly info?: AsyncPageStateInfoProps | null;
  readonly onRetry?: () => void;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  readonly orderBy: WaiversListOrderBy;
  readonly onOrderByChange: (orderBy: WaiversListOrderBy) => void;
  readonly totalCount: number;
  readonly exactTotalEstimate: boolean;
  /** 1-based page index for the {@link Pagination} bar (matches the wire contract). */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage?: boolean;
  readonly onPageChange: (nextPage: number) => void;
  /** Optional API warnings surfaced above the table (e.g. facet truncation). */
  readonly warnings?: ReadonlyArray<string>;
}

/**
 * Presentational Ana Waivers page shell (CLM-43204): filter rail + toolbar + card list.
 * Wired by {@code WaiversListPage} (router hydration) against
 * {@code POST /rest/search/index-query} with {@code entityType: WAIVER}. Mirrors the
 * Applications page contract so future filter/toolbar improvements stay symmetric.
 */
export default function WaiversAnaPage({
  waivers,
  facets,
  filters,
  hasActiveFilters,
  onToggleFilter,
  onResetFilters,
  loading = false,
  error = null,
  info = null,
  onRetry,
  searchValue,
  onSearchSubmit,
  orderBy,
  onOrderByChange,
  totalCount,
  exactTotalEstimate,
  page,
  pageSize,
  hasNextPage = false,
  onPageChange,
  warnings = [],
}: WaiversAnaPageProps): JSX.Element {
  const offsets = usePreviewShellOffsets();
  const { stateService } = useRouter();
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const showAutoWaiversEntry = isAutoWaiversEnabled && isDeveloperDashboardEnabled;
  const headerCount = facets.totalWaivers > 0 ? facets.totalWaivers : totalCount;
  const headerCountLabel = exactTotalEstimate
    ? String(headerCount)
    : `${headerCount}+`;

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
      <main data-testid="preview-waivers-page">
        <Flex direction="column" gap="2" mb="5">
          <Flex align="center" justify="between" gap="3" wrap="wrap">
            <Flex align="center" gap="3">
              <DomainIcons.Waivers size={28} color="var(--accent-9)" />
              <Heading size="6">Waivers</Heading>
              <Badge
                size="2"
                color="gray"
                variant="soft"
                radius="full"
                data-testid="waivers-page-count-badge"
              >
                {headerCountLabel}
              </Badge>
            </Flex>
            {showAutoWaiversEntry && (
              <Button
                size="2"
                variant="solid"
                onClick={() =>
                  stateService.go('nexusOneAutoWaivers', {
                    ownerType: DEFAULT_AUTO_WAIVER_OWNER_TYPE,
                    ownerId: DEFAULT_AUTO_WAIVER_OWNER_ID,
                  })
                }
                data-testid="nosc-waivers-auto-waivers-button"
              >
                <DomainIcons.AutoWaiver size={14} aria-hidden /> Auto-Waivers
              </Button>
            )}
          </Flex>
          <Text size="2" color="gray">
            Review and manage policy waivers across applications and organizations you can access.
          </Text>
          {warnings.length > 0 && (
            <Flex direction="column" gap="1" data-testid="waivers-list-warnings">
              {warnings.map((warning) => (
                <Text key={warning} size="1" color="amber">
                  {warning}
                </Text>
              ))}
            </Flex>
          )}
        </Flex>

        <Flex gap="4" align="start" wrap="wrap" data-testid="waivers-page-layout">
          <WaiversFilterRail
            facets={facets}
            filters={filters}
            hasActiveFilters={hasActiveFilters}
            onToggleFilter={onToggleFilter}
            onResetFilters={onResetFilters}
          />
          <Box className="waivers-page__content" data-testid="waivers-page-content">
            <Flex direction="column" gap="4">
              <WaiversToolbar
                totalCount={totalCount}
                exactTotalEstimate={exactTotalEstimate}
                searchValue={searchValue}
                onSearchSubmit={onSearchSubmit}
                orderBy={orderBy}
                onOrderByChange={onOrderByChange}
                currentPageWaivers={waivers}
              />

              <AsyncPageState
                loading={loading}
                error={error}
                info={info}
                onRetry={onRetry}
                loadingTestId="waivers-list-loading"
                errorTestId="waivers-list-error"
                errorTitle="Failed to load waivers"
                errorVariant="banner"
                infoVariant="banner"
              >
                {waivers.length === 0 ? (
                  <Flex
                    direction="column"
                    align="center"
                    gap="2"
                    py="8"
                    data-testid="waivers-list-empty"
                  >
                    <DomainIcons.Waivers size={32} color="var(--gray-9)" />
                    {(() => {
                      const emptyStateKind =
                        searchValue && hasActiveFilters
                          ? 'searchAndFilters'
                          : searchValue
                            ? 'search'
                            : hasActiveFilters
                              ? 'filters'
                              : 'none';
                      const emptyCopy = {
                        searchAndFilters: {
                          title: 'No waivers match your search and filters.',
                          description: 'Try adjusting or clearing your search and filters.',
                        },
                        search: {
                          title: 'No waivers match your search.',
                          description: 'Try a different search term or clear the search.',
                        },
                        filters: {
                          title: 'No waivers match the selected filters.',
                          description: 'Adjust the sidebar filters or reset them to see more waivers.',
                        },
                        none: {
                          title: 'No waivers in scope',
                          description:
                            'Waivers you can see will appear here as they are created. New waivers can take a moment to appear while the search index catches up.',
                        },
                      }[emptyStateKind];
                      return (
                        <>
                          <Text size="3" color="gray">
                            {emptyCopy.title}
                          </Text>
                          <Text size="2" color="gray" align="center" style={{ maxWidth: 480 }}>
                            {emptyCopy.description}
                          </Text>
                        </>
                      );
                    })()}
                    <Flex gap="2" mt="1">
                      {searchValue && (
                        <Button
                          variant="soft"
                          size="2"
                          onClick={() => onSearchSubmit('')}
                          data-testid="waivers-empty-clear-search"
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
                          data-testid="waivers-empty-reset-filters"
                        >
                          <ActionIcons.Refresh size={14} aria-hidden />
                          Reset filters
                        </Button>
                      )}
                    </Flex>
                  </Flex>
                ) : (
                  <>
                    <WaiversAnaCardList waivers={waivers} linkFrom="waivers-list" />
                    {(totalCount > pageSize || page > 1 || hasNextPage) && (
                      <Pagination
                        page={page}
                        pageSize={pageSize}
                        totalItems={totalCount}
                        hasNextPage={hasNextPage}
                        onPageChange={onPageChange}
                        data-testid="waivers-list-pagination"
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
