/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { Box, Button, Dialog, Flex, Heading, Text } from '@radix-ui/themes';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import VulnerabilitiesFilterRail from 'MainRoot/nosc/vulnerabilities/VulnerabilitiesFilterRail';
import VulnerabilityCardGrid from 'MainRoot/nosc/vulnerabilities/VulnerabilityCardGrid';
import { hasActiveVulnerabilityFilters } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';
import type {
  VulnerabilitiesFilterState,
  VulnerabilitiesListFacets,
  VulnerabilitiesListOrderBy,
  VulnerabilityCvssRange,
  VulnerabilityEpssRange,
  VulnerabilityFilterSetGroup,
  VulnerabilityPublishedWindow,
  VulnerabilityRow,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import type { VulnerabilitiesTab } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesRoute';
import VulnerabilitiesToolbar from 'MainRoot/nosc/vulnerabilities/VulnerabilitiesToolbar';

import './vulnerabilitiesPageLayout.css';

export interface VulnerabilitiesPageProps {
  readonly tab: VulnerabilitiesTab;
  readonly onTabChange: (tab: VulnerabilitiesTab) => void;
  readonly vulnerabilities: ReadonlyArray<VulnerabilityRow>;
  readonly facets?: VulnerabilitiesListFacets | null;
  readonly filters: VulnerabilitiesFilterState;
  readonly onFilterToggle: (group: VulnerabilityFilterSetGroup, id: string) => void;
  readonly onCvssRangeChange: (range: VulnerabilityCvssRange) => void;
  readonly onKnownExploitedChange: (value: boolean) => void;
  readonly onMalwareChange: (value: boolean) => void;
  readonly onEpssRangeChange: (range: VulnerabilityEpssRange) => void;
  readonly onPublishedWindowChange: (value: '' | VulnerabilityPublishedWindow) => void;
  readonly onFiltersReset: () => void;
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly onRetry?: () => void;
  readonly totalCount: number;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  readonly orderBy: VulnerabilitiesListOrderBy;
  readonly onOrderByChange: (orderBy: VulnerabilitiesListOrderBy) => void;
  /** 1-based current page for {@link Pagination}. */
  readonly page: number;
  readonly pageSize: number;
  readonly onPageChange: (nextPage: number) => void;
}

/**
 * Martha V1 Vulnerabilities page shell: filter rail (desktop + mobile drawer), tabs, toolbar, cards.
 */
export default function VulnerabilitiesPage({
  tab,
  onTabChange,
  vulnerabilities,
  facets = null,
  filters,
  onFilterToggle,
  onCvssRangeChange,
  onKnownExploitedChange,
  onMalwareChange,
  onEpssRangeChange,
  onPublishedWindowChange,
  onFiltersReset,
  loading = false,
  error = null,
  onRetry,
  totalCount,
  searchValue,
  onSearchSubmit,
  orderBy,
  onOrderByChange,
  page,
  pageSize,
  onPageChange,
}: VulnerabilitiesPageProps): JSX.Element {
  const offsets = usePreviewShellOffsets();
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  const showPagination = (totalCount > pageSize || page > 1) && !error;
  const hasSearch = Boolean(searchValue);
  const filtersActive = hasActiveVulnerabilityFilters(filters, tab);

  const railProps = {
    tab,
    facets,
    selected: filters,
    onToggle: onFilterToggle,
    onCvssRangeChange,
    onKnownExploitedChange,
    onMalwareChange,
    onEpssRangeChange,
    onPublishedWindowChange,
    onReset: onFiltersReset,
  };

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
      <main data-testid="preview-vulnerabilities-page">
        <Flex direction="column" gap="2" mb="5">
          <Flex align="center" gap="3">
            <DomainIcons.Vulnerability size={28} color="var(--accent-9)" />
            <Heading size="6">Vulnerabilities</Heading>
          </Flex>
          <Text size="2" color="gray">
            Estate CVEs from your scans, plus the Sonatype Catalog — highest CVSS first.
          </Text>
        </Flex>

        <Flex gap="4" align="start" wrap="wrap" data-testid="vulnerabilities-page-layout">
          {/* Desktop rail; on small screens it is replaced by the mobile drawer below. */}
          <Box
            className="vulnerabilities-page__filter-rail"
            display={{ initial: 'none', sm: 'block' }}
            data-testid="vulnerabilities-filter-rail-host"
          >
            <VulnerabilitiesFilterRail {...railProps} idPrefix="desktop" />
          </Box>

          <Box className="vulnerabilities-page__content" data-testid="vulnerabilities-page-content">
            <Flex direction="column" gap="4">
              {/* Mobile-only trigger for the filter drawer (same rail, different id namespace). */}
              <Box display={{ initial: 'block', sm: 'none' }}>
                <Dialog.Root open={mobileFiltersOpen} onOpenChange={setMobileFiltersOpen}>
                  <Dialog.Trigger>
                    <Button
                      variant="outline"
                      color="gray"
                      size="2"
                      data-testid="vulnerabilities-filters-mobile-trigger"
                      aria-label={filtersActive ? 'Filters (active)' : 'Filters'}
                    >
                      <ActionIcons.Filter size={14} aria-hidden />
                      Filters
                      {filtersActive && (
                        <Box
                          data-testid="vulnerabilities-filters-mobile-active-dot"
                          style={{
                            width: 8,
                            height: 8,
                            borderRadius: '50%',
                            backgroundColor: 'var(--accent-9)',
                          }}
                        />
                      )}
                    </Button>
                  </Dialog.Trigger>
                  <Dialog.Content
                    maxWidth="360px"
                    className="vulnerabilities-page__filter-drawer"
                    data-testid="vulnerabilities-filters-mobile-drawer"
                  >
                    <Dialog.Title size="3">Filters</Dialog.Title>
                    <Dialog.Description size="1" color="gray" mb="3">
                      Narrow vulnerabilities by severity, CVSS score, and ecosystem.
                    </Dialog.Description>
                    <VulnerabilitiesFilterRail {...railProps} idPrefix="mobile" />
                    <Flex justify="end" mt="4">
                      <Dialog.Close>
                        <Button size="2" data-testid="vulnerabilities-filters-mobile-apply">
                          Done
                        </Button>
                      </Dialog.Close>
                    </Flex>
                  </Dialog.Content>
                </Dialog.Root>
              </Box>

              <VulnerabilitiesToolbar
                tab={tab}
                onTabChange={onTabChange}
                totalCount={totalCount}
                searchValue={searchValue}
                onSearchSubmit={onSearchSubmit}
                orderBy={orderBy}
                onOrderByChange={onOrderByChange}
                filters={filters}
              />

              <AsyncPageState
                loading={loading}
                error={error}
                onRetry={onRetry}
                loadingTestId="vulnerabilities-list-loading"
                errorTestId="vulnerabilities-list-error"
                errorTitle="Failed to load vulnerabilities"
                errorVariant="banner"
              >
                {vulnerabilities.length === 0 ? (
                  <Flex
                    direction="column"
                    align="center"
                    gap="2"
                    py="8"
                    data-testid="vulnerabilities-list-empty"
                  >
                    <DomainIcons.Vulnerability size={32} color="var(--gray-9)" />
                    <Text size="3" color="gray">
                      {totalCount > 0 && page > 1
                        ? 'No results on this page.'
                        : tab === 'catalog'
                          ? hasSearch && filtersActive
                            ? 'No catalog vulnerabilities match your search and filters.'
                            : hasSearch
                              ? 'No catalog vulnerabilities match your search.'
                              : filtersActive
                                ? 'No catalog vulnerabilities match your filters.'
                                : 'No catalog vulnerabilities found.'
                          : hasSearch && filtersActive
                            ? 'No vulnerabilities match your search and filters.'
                            : hasSearch
                              ? 'No vulnerabilities match your search.'
                              : filtersActive
                                ? 'No vulnerabilities match your filters.'
                                : 'No vulnerabilities in scope.'}
                    </Text>
                  </Flex>
                ) : (
                  <VulnerabilityCardGrid vulnerabilities={vulnerabilities} />
                )}
              </AsyncPageState>

              {showPagination && (
                <Pagination
                  page={page}
                  pageSize={pageSize}
                  totalItems={totalCount}
                  onPageChange={onPageChange}
                  data-testid="vulnerabilities-pagination"
                />
              )}
            </Flex>
          </Box>
        </Flex>
      </main>
    </Box>
  );
}
