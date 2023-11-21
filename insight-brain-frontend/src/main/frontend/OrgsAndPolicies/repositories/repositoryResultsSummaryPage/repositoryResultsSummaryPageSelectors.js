/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
export const selectRepositoryResultsSummaryPageSlice = prop('repositoryResultsSummaryPage');
export const selectRepositoryInformation = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('repositoryInfo')
);
export const selectViolationStateFilters = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('selectedViolationStateFilters')
);
export const selectThreatLevelFilters = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('selectedThreatLevelFilters')
);
export const selectMatchStateFilters = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('selectedMatchStateFilters')
);
export const selectShowFilterPopover = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('showFilterPopover')
);
export const selectRepositoryComponents = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('repositoryComponents')
);
export const selectLoadingRepositoryComponents = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('loadingRepositoryComponents')
);
export const selectErrorComponentsTable = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('errorComponentsTable')
);
export const selectComponentsRequestBody = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('componentsRequestBody')
);
export const selectSearchFiltersValues = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('searchFiltersValues')
);
export const selectHasMoreResults = createSelector(selectRepositoryResultsSummaryPageSlice, prop('hasMoreResults'));
export const selectCurrentPage = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  (state) => state.componentsRequestBody.page
);
export const selectSortConfiguration = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  (state) => state.componentsRequestBody.sortFields[0]
);
export const selectReEvaluateMaskSuccess = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('reEvaluateMaskSuccess')
);
export const selectShowMaskSuccessDialog = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  prop('showMaskSuccessDialog')
);
export const selectAggregate = createSelector(
  selectRepositoryResultsSummaryPageSlice,
  (state) => state.componentsRequestBody.aggregate
);
