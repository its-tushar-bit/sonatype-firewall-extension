/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { FilteredListLayout } from 'MainRoot/nosc/components/FilteredListLayout';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import ApplicationsFilterRail from 'MainRoot/nosc/applications/ApplicationsFilterRail';
import ApplicationsToolbar from 'MainRoot/nosc/applications/ApplicationsToolbar';
import EvaluationCardGrid from 'MainRoot/nosc/applications/EvaluationCardGrid';
import { ApplicationRiskScore, ApplicationsFilterFacetCounts } from 'MainRoot/nosc/applications/applicationListTypes';
import {
  ApplicationsListFilterSetField,
  ApplicationsListFilterState,
  ApplicationsThreatRange,
  type ApplicationsAgeInDays,
} from 'MainRoot/nosc/applications/applicationsListFilters';
import { ApplicationsListOrderBy } from 'MainRoot/nosc/applications/applicationsListApi';
import { AsyncPageStateInfoProps } from 'MainRoot/nosc/components/AsyncPageState';

export interface ApplicationsPageProps {
  readonly applications: ReadonlyArray<ApplicationRiskScore>;
  readonly facets: ApplicationsFilterFacetCounts;
  readonly filters: ApplicationsListFilterState;
  readonly hasActiveFilters: boolean;
  readonly onToggleFilter: (field: ApplicationsListFilterSetField, id: string) => void;
  readonly onThreatRangeChange: (range: ApplicationsThreatRange) => void;
  readonly onAgeInDaysChange: (ageInDays: ApplicationsAgeInDays | undefined) => void;
  readonly onResetFilters: () => void;
  readonly loading?: boolean;
  readonly error?: string | null;
  readonly info?: AsyncPageStateInfoProps | null;
  readonly onRetry?: () => void;
  readonly searchValue: string;
  readonly onSearchSubmit: (term: string) => void;
  readonly orderBy: ApplicationsListOrderBy;
  readonly onOrderByChange: (orderBy: ApplicationsListOrderBy) => void;
  /** RBAC-scoped total from the list API (may exceed the current page length). */
  readonly totalCount: number;
  /** 1-based page index for {@link Pagination}. */
  readonly page: number;
  readonly pageSize: number;
  readonly hasNextPage?: boolean;
  readonly onPageChange: (nextPage: number) => void;
}

/**
 * Martha V1 Applications page — now a thin config over the shared FilteredListLayout skeleton
 * (CLM-42562). The skeleton owns the shell (header, filter rail + mobile drawer, async states,
 * pagination); this page supplies the application-specific slots: the custom toolbar (search + sort),
 * the filter rail, the card grid, and a filter/search-aware empty state.
 */
export default function ApplicationsPage({
  applications,
  facets,
  filters,
  hasActiveFilters,
  onToggleFilter,
  onThreatRangeChange,
  onAgeInDaysChange,
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
  page,
  pageSize,
  hasNextPage = false,
  onPageChange,
}: ApplicationsPageProps): JSX.Element {
  const renderFilterRail = (idPrefix?: string) => (
    <ApplicationsFilterRail
      facets={facets}
      filters={filters}
      hasActiveFilters={hasActiveFilters}
      onToggleFilter={onToggleFilter}
      onThreatRangeChange={onThreatRangeChange}
      onAgeInDaysChange={onAgeInDaysChange}
      onResetFilters={onResetFilters}
      idPrefix={idPrefix}
    />
  );

  const renderToolbar = () => (
    <ApplicationsToolbar
      totalCount={totalCount}
      searchValue={searchValue}
      onSearchSubmit={onSearchSubmit}
      orderBy={orderBy}
      onOrderByChange={onOrderByChange}
      filters={filters}
    />
  );

  const renderCardGrid = (items: ReadonlyArray<ApplicationRiskScore>) => <EvaluationCardGrid applications={items} />;

  return (
    <FilteredListLayout
      title="Applications"
      slug="applications"
      description="Evaluation history and policy risk across every application visible to your account."
      icon={DomainIcons.Applications}
      countNoun={{ singular: 'application', plural: 'applications' }}
      items={applications}
      totalCount={totalCount}
      loading={loading}
      error={error}
      info={info}
      onRetry={onRetry}
      searchValue={searchValue}
      onSearchSubmit={onSearchSubmit}
      hasActiveFilters={hasActiveFilters}
      onResetFilters={onResetFilters}
      renderFilterRail={() => renderFilterRail()}
      renderMobileFilterDrawer={() => renderFilterRail('applications-filter-mobile')}
      renderToolbar={renderToolbar}
      renderCardGrid={renderCardGrid}
      page={page}
      pageSize={pageSize}
      onPageChange={onPageChange}
      hasNextPage={hasNextPage}
    />
  );
}
