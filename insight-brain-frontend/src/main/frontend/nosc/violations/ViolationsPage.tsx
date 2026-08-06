/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Button, Flex, Text } from '@radix-ui/themes';
import { FilteredListLayout } from 'MainRoot/nosc/components/FilteredListLayout';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
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
 * Martha V1 Violations page — now a thin config over the shared FilteredListLayout skeleton
 * (CLM-42562). The skeleton owns the shell (header, filter rail + mobile drawer, async states,
 * pagination); this page supplies the violation-specific slots: the custom toolbar (search + sort
 * + CSV export), the filter rail, the card grid, and a filter/search-aware empty state.
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
  // Legal reuses this page with filtersActive overridden (its default-state predicate differs).
  const filtersActive = filtersActiveProp ?? hasActiveViolationFilters(filters);
  const hasSearch = Boolean(searchValue);
  const nounCapitalized = `${emptyResultNoun.charAt(0).toUpperCase()}${emptyResultNoun.slice(1)}`;
  const idleEmptyDescription =
    emptyIdleDescription ?? `${nounCapitalized} visible to your account will appear here once data is loaded.`;

  const renderFilterRail = (idPrefix?: string) => (
    <ViolationsFilterRail
      facets={facets}
      labels={labels}
      selected={filters}
      onToggle={onFilterToggle}
      onWaiverTypeChange={onWaiverTypeChange}
      onThreatRangeChange={onThreatRangeChange}
      onReset={onResetFilters}
      organizationFacetSearch={organizationFacetSearch}
      onOrganizationFacetSearchChange={onOrganizationFacetSearchChange}
      applicationFacetSearch={applicationFacetSearch}
      onApplicationFacetSearchChange={onApplicationFacetSearchChange}
      applicationCategoryOptions={applicationCategoryOptions}
      applicationCategorySearch={applicationCategorySearch}
      onApplicationCategorySearchChange={onApplicationCategorySearchChange}
      idPrefix={idPrefix}
      hideStateFilter={hideStateFilter}
      hideWaiverTypeFilter={hideWaiverTypeFilter}
      threatCategorySectionTitle={threatCategorySectionTitle}
      threatCategoryUseIdentityLabels={threatCategoryUseIdentityLabels}
      filtersActive={filtersActive}
    />
  );

  // Filter/search-aware empty state with recovery actions (preserved from the pre-skeleton page).
  const renderEmpty = () => (
    <Flex direction="column" align="center" gap="2" py="8" data-testid="violations-list-empty">
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
            <Button variant="soft" size="2" onClick={onResetFilters} data-testid="violations-empty-reset-filters">
              <ActionIcons.Refresh size={14} aria-hidden />
              Reset filters
            </Button>
          )}
        </Flex>
      )}
    </Flex>
  );

  return (
    <FilteredListLayout<ViolationRow>
      title={title}
      slug="violations"
      pageTestId={pageTestId}
      description={description}
      icon={HeadingIcon}
      // countNoun.plural also drives the skeleton's error title ("Failed to load {plural}"),
      // so feeding it the branded noun keeps Legal's error copy correct.
      countNoun={{
        singular: emptyResultNounSingular ?? 'violation',
        plural: emptyResultNoun,
      }}
      items={violations}
      totalCount={totalCount}
      loading={loading}
      error={error}
      errorTitle={errorTitle}
      onRetry={onRetry}
      searchable={false}
      page={page}
      pageSize={pageSize}
      onPageChange={onPageChange}
      hasActiveFilters={filtersActive}
      onResetFilters={onResetFilters}
      filterDrawerDescription={filterDrawerDescription}
      renderFilterRail={() => renderFilterRail()}
      renderMobileFilterDrawer={() => renderFilterRail('violations-filter-mobile')}
      renderToolbar={() => (
        <ViolationsToolbar
          totalCount={totalCount}
          searchValue={searchValue}
          onSearchSubmit={onSearchSubmit}
          filters={filters}
          hideCsvExport={hideCsvExport}
          resultNoun={emptyResultNoun === 'violations' ? undefined : emptyResultNoun}
          resultNounSingular={emptyResultNoun === 'violations' ? undefined : emptyResultNounSingular}
        />
      )}
      renderCardGrid={(items) => (
        <ViolationCardGrid
          violations={items as ViolationRow[]}
          getCardHref={getCardHref}
          hideStateBadges={hideCardStateBadges}
        />
      )}
      renderEmpty={renderEmpty}
    />
  );
}
