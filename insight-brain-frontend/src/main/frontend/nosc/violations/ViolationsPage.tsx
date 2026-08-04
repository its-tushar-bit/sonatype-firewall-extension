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
import {
  ApplicationCategoryOption,
  ViolationFilterSetGroup,
  ViolationRow,
  ViolationsFilterState,
  ViolationsListFacets,
  ViolationThreatRange,
  ViolationWaiverType,
} from 'MainRoot/nosc/violations/violationListTypes';
import { hasActiveViolationFilters } from 'MainRoot/nosc/violations/violationsListApi';
import ViolationsFilterRail from 'MainRoot/nosc/violations/ViolationsFilterRail';
import ViolationsToolbar from 'MainRoot/nosc/violations/ViolationsToolbar';
import ViolationCardGrid from 'MainRoot/nosc/violations/ViolationCardGrid';

import './violationsPageLayout.css';

export interface ViolationsPageProps {
  readonly violations: ReadonlyArray<ViolationRow>;
  readonly facets?: ViolationsListFacets;
  readonly labels?: {
    readonly organizations: Readonly<Record<string, string>>;
    readonly applications: Readonly<Record<string, string>>;
  };
  readonly filters: ViolationsFilterState;
  readonly onFilterToggle: (group: ViolationFilterSetGroup, id: string) => void;
  readonly onWaiverTypeChange: (waiverType: ViolationWaiverType) => void;
  readonly onThreatRangeChange: (range: ViolationThreatRange) => void;
  readonly onResetFilters: () => void;
  readonly organizationFacetSearch?: string;
  readonly onOrganizationFacetSearchChange?: (query: string) => void;
  readonly applicationFacetSearch?: string;
  readonly onApplicationFacetSearchChange?: (query: string) => void;
  readonly applicationCategoryOptions?: ReadonlyArray<ApplicationCategoryOption>;
  readonly applicationCategorySearch?: string;
  readonly onApplicationCategorySearchChange?: (query: string) => void;
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly onRetry?: () => void;
  /** Total matching violations across all pages (for the toolbar count + pagination). */
  readonly totalCount: number;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  /** 1-based current page. */
  readonly page: number;
  readonly pageSize: number;
  readonly onPageChange: (nextPage: number) => void;
  /** Page heading (default: Violations). Legal V1 passes "Legal". */
  readonly title?: string;
  /** Subtitle under the heading. */
  readonly description?: string;
  readonly hideStateFilter?: boolean;
  readonly hideWaiverTypeFilter?: boolean;
  /** When true, hide the Classic violations CSV export control. */
  readonly hideCsvExport?: boolean;
  /** Relabel Policy Type section (Legal: License Threat Group). */
  readonly threatCategorySectionTitle?: string;
  /**
   * When true, threat-category facet options use identity labels (LTG names).
   * Passed through to {@link ViolationsFilterRail}.
   */
  readonly threatCategoryUseIdentityLabels?: boolean;
  /** Override card href / hide waiver-state chrome (Legal findings). */
  readonly getCardHref?: (violation: ViolationRow) => string;
  readonly hideCardStateBadges?: boolean;
  /** Override Reset / mobile active-dot narrowing. */
  readonly filtersActive?: boolean;
  /** Root main data-testid (default: preview-violations-page). */
  readonly pageTestId?: string;
  /** Optional heading icon; defaults to Vulnerability. Legal passes DomainIcons.Legal. */
  readonly HeadingIcon?: typeof DomainIcons.Vulnerability;
  /** AsyncPageState error banner title (default: Failed to load violations). */
  readonly errorTitle?: string;
  /**
   * Plural noun used in filter/search-aware empty copy (default: "violations").
   * Legal passes "license risk findings" so narrowing messages stay branded without hard-overrides.
   */
  readonly emptyResultNoun?: string;
  /**
   * Singular form for the toolbar count when {@link emptyResultNoun} is customized
   * (e.g. Legal: "license risk finding").
   */
  readonly emptyResultNounSingular?: string;
  /**
   * Idle empty-state secondary copy (no search / no filters). Filter/search-aware descriptions
   * still win when narrowing is active.
   */
  readonly emptyIdleDescription?: string;
  /**
   * Mobile filter drawer helper text. Defaults to Violations copy; Legal passes LTG-aware wording
   * (state/waiver are hidden on that page).
   */
  readonly filterDrawerDescription?: string;
}

/**
 * Martha V1 Violations page shell: filter rail + toolbar + card list + pagination inside the Nexus One
 * Preview shell. Data is wired to POST /rest/dashboard/violations/list by {@link ViolationsList}; the
 * toolbar's CSV export posts the active filters to the Classic violations export. The same filter rail
 * is reused inside a mobile drawer on small screens.
 */
export default function ViolationsPage({
  violations,
  facets,
  labels,
  filters,
  onFilterToggle,
  onWaiverTypeChange,
  onThreatRangeChange,
  onResetFilters,
  organizationFacetSearch,
  onOrganizationFacetSearchChange,
  applicationFacetSearch,
  onApplicationFacetSearchChange,
  applicationCategoryOptions,
  applicationCategorySearch,
  onApplicationCategorySearchChange,
  loading = false,
  error = null,
  onRetry,
  totalCount,
  searchValue,
  onSearchSubmit,
  page,
  pageSize,
  onPageChange,
  title = 'Violations',
  description = 'Policy violations across every application visible to your account, highest threat first.',
  hideStateFilter = false,
  hideWaiverTypeFilter = false,
  hideCsvExport = false,
  threatCategorySectionTitle,
  threatCategoryUseIdentityLabels = false,
  getCardHref,
  hideCardStateBadges = false,
  filtersActive: filtersActiveProp,
  pageTestId = 'preview-violations-page',
  HeadingIcon = DomainIcons.Vulnerability,
  errorTitle = 'Failed to load violations',
  emptyResultNoun = 'violations',
  emptyResultNounSingular,
  emptyIdleDescription,
  filterDrawerDescription = 'Narrow violations by state, threat, stage, organization, and application.',
}: ViolationsPageProps): JSX.Element {
  const offsets = usePreviewShellOffsets();
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);
  const showPagination = totalCount > pageSize || page > 1;
  const filtersActive = filtersActiveProp ?? hasActiveViolationFilters(filters);
  const hasSearch = Boolean(searchValue);
  const idleEmptyDescription =
    emptyIdleDescription ??
    `${emptyResultNoun.charAt(0).toUpperCase()}${emptyResultNoun.slice(1)} visible to your account will appear here once data is loaded.`;

  const railProps = {
    facets,
    labels,
    selected: filters,
    onToggle: onFilterToggle,
    onWaiverTypeChange,
    onThreatRangeChange,
    onReset: onResetFilters,
    organizationFacetSearch,
    onOrganizationFacetSearchChange,
    applicationFacetSearch,
    onApplicationFacetSearchChange,
    applicationCategoryOptions,
    applicationCategorySearch,
    onApplicationCategorySearchChange,
    hideStateFilter,
    hideWaiverTypeFilter,
    threatCategorySectionTitle,
    threatCategoryUseIdentityLabels,
    filtersActive,
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
      <main data-testid={pageTestId}>
        <Flex direction="column" gap="2" mb="5">
          <Flex align="center" gap="3">
            <HeadingIcon size={28} color="var(--accent-9)" />
            <Heading size="6">{title}</Heading>
          </Flex>
          <Text size="2" color="gray">
            {description}
          </Text>
        </Flex>

        <Flex gap="4" align="start" wrap="wrap" data-testid="violations-page-layout">
          {/* Desktop rail; on small screens it is replaced by the mobile drawer below.
              Width / containment live on `.nosc-violations-filter-rail` (SCSS). */}
          <Box display={{ initial: 'none', sm: 'block' }} style={{ flexShrink: 0, minWidth: 0 }}>
            <ViolationsFilterRail {...railProps} />
          </Box>
          <Box className="violations-page__content" data-testid="violations-page-content">
            <Flex direction="column" gap="4">
              {/* Mobile-only trigger for the filter drawer (same rail, different id namespace). */}
              <Box display={{ initial: 'block', sm: 'none' }}>
                <Dialog.Root open={mobileFiltersOpen} onOpenChange={setMobileFiltersOpen}>
                  <Dialog.Trigger>
                    <Button
                      variant="outline"
                      color="gray"
                      size="2"
                      data-testid="violations-filters-mobile-trigger"
                      aria-label={filtersActive ? 'Filters (active)' : 'Filters'}
                    >
                      <ActionIcons.Filter size={14} aria-hidden />
                      Filters
                      {filtersActive && (
                        <Box
                          data-testid="violations-filters-mobile-active-dot"
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
                    className="violations-page__filter-drawer"
                    data-testid="violations-filters-mobile-drawer"
                  >
                    <Dialog.Title size="3">Filters</Dialog.Title>
                    <Dialog.Description size="1" color="gray" mb="3">
                      {filterDrawerDescription}
                    </Dialog.Description>
                    <ViolationsFilterRail {...railProps} idPrefix="violations-filter-mobile" />
                    <Flex justify="end" mt="4">
                      <Dialog.Close>
                        <Button size="2" data-testid="violations-filters-mobile-apply">
                          Done
                        </Button>
                      </Dialog.Close>
                    </Flex>
                  </Dialog.Content>
                </Dialog.Root>
              </Box>

              <ViolationsToolbar
                totalCount={totalCount}
                searchValue={searchValue}
                onSearchSubmit={onSearchSubmit}
                filters={filters}
                hideCsvExport={hideCsvExport}
                resultNoun={emptyResultNoun === 'violations' ? undefined : emptyResultNoun}
                resultNounSingular={
                  emptyResultNoun === 'violations' ? undefined : emptyResultNounSingular
                }
              />

              <AsyncPageState
                loading={loading}
                error={error}
                onRetry={onRetry}
                loadingTestId="violations-list-loading"
                errorTestId="violations-list-error"
                errorTitle={errorTitle}
                errorVariant="banner"
              >
                {violations.length === 0 ? (
                  <Flex
                    direction="column"
                    align="center"
                    gap="2"
                    py="8"
                    data-testid="violations-list-empty"
                  >
                    <HeadingIcon size={32} color="var(--gray-9)" />
                    <Text size="3" color="gray">
                      {hasSearch && filtersActive
                        ? `No ${emptyResultNoun} match your search and filters.`
                        : hasSearch
                          ? `No ${emptyResultNoun} match your search.`
                          : filtersActive
                            ? `No ${emptyResultNoun} match your filters.`
                            : `No ${emptyResultNoun} to display.`}
                    </Text>
                    <Text size="2" color="gray">
                      {hasSearch && filtersActive
                        ? 'Try adjusting or clearing your search and filters.'
                        : hasSearch
                          ? 'Try adjusting or clearing your search.'
                          : filtersActive
                            ? 'Try adjusting or resetting your filters.'
                            : idleEmptyDescription}
                    </Text>
                    {/* Give the empty state actionable recovery controls: clear the committed search
                        and/or reset the filter selection so a zero-result narrowing isn't a dead end. */}
                    {(searchValue || filtersActive) && (
                      <Flex gap="2" mt="1">
                        {searchValue && (
                          <Button
                            variant="soft"
                            size="2"
                            onClick={() => onSearchSubmit('')}
                            data-testid="violations-empty-clear-search"
                          >
                            <ActionIcons.Refresh size={14} aria-hidden />
                            Clear search
                          </Button>
                        )}
                        {filtersActive && (
                          <Button
                            variant="soft"
                            size="2"
                            onClick={onResetFilters}
                            data-testid="violations-empty-reset-filters"
                          >
                            <ActionIcons.Refresh size={14} aria-hidden />
                            Reset filters
                          </Button>
                        )}
                      </Flex>
                    )}
                  </Flex>
                ) : (
                  <ViolationCardGrid
                    violations={violations}
                    getCardHref={getCardHref}
                    hideStateBadges={hideCardStateBadges}
                  />
                )}

                {/* Rendered independent of the empty/grid branch so an out-of-range page (page > 1
                    with an empty result after a shrunk total) can still page back. Both this control
                    and the empty state sit inside AsyncPageState, which returns only the skeleton while
                    loading — so the initial (totalCount=0) fetch shows no empty-state or pagination
                    flash before data arrives. */}
                {showPagination && (
                  <Pagination
                    page={page}
                    pageSize={pageSize}
                    totalItems={totalCount}
                    onPageChange={onPageChange}
                    data-testid="violations-pagination"
                  />
                )}
              </AsyncPageState>
            </Flex>
          </Box>
        </Flex>
      </main>
    </Box>
  );
}
