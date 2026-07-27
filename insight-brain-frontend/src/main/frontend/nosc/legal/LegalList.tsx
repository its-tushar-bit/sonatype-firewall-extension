/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { DomainIcons } from 'MainRoot/nosc/icons';
import {
  adaptLegalFacetsForRail,
  adaptLegalFindingToViolationRow,
  createDefaultLegalFilterState,
  hasActiveLegalFilters,
  LEGAL_PAGE_SIZE,
  legalFindingHrefFromViolationRow,
} from 'MainRoot/nosc/legal/legalListApi';
import { selectIsAdvancedLegalPackSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  buildLegalListRouteParams,
  parseLegalListParams,
  rawLegalListParamsSnapshot,
  violationsFiltersEqual,
} from 'MainRoot/nosc/legal/legalListQuery';
import { NEXUS_ONE_LEGAL_STATE_NAME } from 'MainRoot/nosc/legal/legalRoute';
import { useLegalList } from 'MainRoot/nosc/legal/useLegalList';
import { useNexusOneListUrlState } from 'MainRoot/nosc/list/useNexusOneListUrlState';
import ViolationsPage from 'MainRoot/nosc/violations/ViolationsPage';
import {
  ViolationFilterSetGroup,
  ViolationRow,
  ViolationsFilterState,
  ViolationsListFacets,
  ViolationThreatRange,
  ViolationWaiverType,
} from 'MainRoot/nosc/violations/violationListTypes';
import { deriveViolationFacetLabels } from 'MainRoot/nosc/violations/violationsListApi';

import '@radix-ui/themes/styles.css';

const EMPTY_ROWS: ReadonlyArray<ViolationRow> = [];

/**
 * Nexus One Legal V1 — LEGAL_VIOLATION license-risk triage (CLM-43207).
 *
 * Same index type as the Dashboard Legal Obligations card ({@code LEGAL_VIOLATION}). Rows are
 * per-(stage × LTG) for triage; the card metric collapses those dimensions. LeftNav is ungated
 * (Lifecycle); Classic ALP dashboard remains at /legal/applicationsDashboard.
 */
export default function LegalList(): JSX.Element {
  const advancedLegalPack = useSelector(selectIsAdvancedLegalPackSupported);
  const getCardHref = useCallback(
    (row: ViolationRow) => legalFindingHrefFromViolationRow(row, { advancedLegalPack }),
    [advancedLegalPack],
  );

  const {
    search,
    page,
    filters,
    fetchEnabled,
    setSearch,
    setPage,
    setFilters,
    requestUrlWrite,
  } = useNexusOneListUrlState<ViolationsFilterState>({
    stateName: NEXUS_ONE_LEGAL_STATE_NAME,
    parse: parseLegalListParams,
    build: buildLegalListRouteParams,
    rawSnapshot: rawLegalListParamsSnapshot,
    filtersEqual: violationsFiltersEqual,
  });

  const [cachedFacets, setCachedFacets] = useState<ViolationsListFacets | undefined>(undefined);
  const includeFacets = page === 1 || cachedFacets === undefined;

  const { status, data, error, retry } = useLegalList({
    page: page - 1,
    pageSize: LEGAL_PAGE_SIZE,
    search,
    includeFacets,
    filters,
    enabled: fetchEnabled,
  });

  const adaptedFacets = useMemo(() => adaptLegalFacetsForRail(data?.facets), [data?.facets]);

  useEffect(() => {
    if (adaptedFacets) {
      setCachedFacets(adaptedFacets);
    }
  }, [adaptedFacets]);
  const facets = adaptedFacets ?? cachedFacets;

  useEffect(() => {
    if (!data) return;
    const total = data.total ?? 0;
    const maxPage = total > 0 ? Math.ceil(total / LEGAL_PAGE_SIZE) : 1;
    if (page > maxPage) {
      setPage(maxPage);
      requestUrlWrite();
    }
  }, [data, page, setPage, requestUrlWrite]);

  const toggleFilter = useCallback((group: ViolationFilterSetGroup, id: string) => {
    if (group === 'states') {
      return;
    }
    setFilters((prev) => {
      const next = new Set(prev[group]);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return { ...prev, [group]: next };
    });
    setPage(1);
    requestUrlWrite();
  }, [setFilters, setPage, requestUrlWrite]);

  const changeThreatRange = useCallback((range: ViolationThreatRange) => {
    setFilters((prev) => ({ ...prev, threatRange: range }));
    setPage(1);
    requestUrlWrite();
  }, [setFilters, setPage, requestUrlWrite]);

  const changeWaiverType = useCallback((_waiverType: ViolationWaiverType) => {
    // Legal findings have no waiver status.
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(createDefaultLegalFilterState());
    setPage(1);
    requestUrlWrite();
  }, [setFilters, setPage, requestUrlWrite]);

  const submitSearch = useCallback((term: string) => {
    setSearch(term);
    setPage(1);
    requestUrlWrite();
  }, [setSearch, setPage, requestUrlWrite]);

  const goToPage = useCallback((nextPage: number) => {
    setPage(nextPage);
    requestUrlWrite();
  }, [setPage, requestUrlWrite]);

  const violations = useMemo(
    () => (data?.findings ?? []).map(adaptLegalFindingToViolationRow),
    [data?.findings],
  );
  const rows = violations.length > 0 ? violations : EMPTY_ROWS;
  const labels = useMemo(
    () =>
      deriveViolationFacetLabels(rows, {
        organizations: facets?.organizationNames,
        applications: facets?.applicationNames,
      }),
    [rows, facets?.organizationNames, facets?.applicationNames],
  );

  const loading = status === 'loading';
  const errorMessage = status === 'error' ? error?.message ?? 'Unable to load license risk findings.' : null;

  return (
    <ViolationsPage
      violations={rows}
      facets={facets}
      labels={labels}
      filters={filters}
      onFilterToggle={toggleFilter}
      onWaiverTypeChange={changeWaiverType}
      onThreatRangeChange={changeThreatRange}
      onResetFilters={resetFilters}
      loading={loading}
      error={errorMessage}
      onRetry={retry}
      totalCount={data?.total ?? 0}
      searchValue={search}
      onSearchSubmit={submitSearch}
      page={page}
      pageSize={LEGAL_PAGE_SIZE}
      onPageChange={goToPage}
      title="Legal"
      description="License risk across every application visible to your account, highest threat first — LEGAL_VIOLATION findings (same index type as the Legal Obligations card; list rows keep stage and license threat group). Obligation review remains in Classic Legal."
      hideStateFilter
      hideWaiverTypeFilter
      hideCsvExport
      threatCategorySectionTitle="License Threat Group"
      threatCategoryUseIdentityLabels
      getCardHref={getCardHref}
      hideCardStateBadges
      filtersActive={hasActiveLegalFilters(filters)}
      pageTestId="preview-legal-page"
      HeadingIcon={DomainIcons.Legal}
      errorTitle="Failed to load license risk findings"
      emptyResultNoun="license risk findings"
      emptyResultNounSingular="license risk finding"
      emptyIdleDescription="Findings from LEGAL_VIOLATION index docs will appear here once indexed reports are available."
      filterDrawerDescription="Narrow license risk by license threat group, threat, stage, organization, and application."
    />
  );
}
