/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';
import { FILTER_STATES } from './enterpriseReportingFilterSlice';

export const selectEnterpriseReportingFilter = prop('enterpriseReportingFilter');

export const selectIsFilterDirty = createSelector(
  selectEnterpriseReportingFilter,
  ({ filterState }) => filterState === FILTER_STATES.CHANGED
);

export const selectInErrorState = createSelector(
  selectEnterpriseReportingFilter,
  ({ loadDefaultFilterError, loadSavedFiltersError }) => !!loadDefaultFilterError || !!loadSavedFiltersError
);

export const selectIsDefaultAlertRendered = createSelector(
  selectEnterpriseReportingFilter,
  ({ showDefaultFilterSuccessAlert, saveDefaultFilterError }) =>
    !!showDefaultFilterSuccessAlert || !!saveDefaultFilterError
);

export const selectIsSavedFilterApplied = createSelector(
  selectEnterpriseReportingFilter,
  ({ previewFilterName, appliedFilterName }) => previewFilterName === appliedFilterName
);

export const selectShowRevertButton = createSelector(
  selectIsFilterDirty,
  selectIsSavedFilterApplied,
  (isDirty, isApplied) => isDirty && isApplied
);

export const selectSaveButtonDisabled = createSelector(
  selectEnterpriseReportingFilter,
  selectIsFilterDirty,
  selectInErrorState,
  ({ loadingIframe }, isDirty, inErrorState) => {
    return !isDirty || loadingIframe || inErrorState;
  }
);

export const selectMakeDefaultBaseDisabled = createSelector(
  selectIsFilterDirty,
  selectIsSavedFilterApplied,
  selectEnterpriseReportingFilter,
  selectInErrorState,
  (filtersAreDirty, savedFilterApplied, { loadingIframe }, inErrorState) => {
    return filtersAreDirty || !savedFilterApplied || loadingIframe || inErrorState;
  }
);

export const selectCombinedLoading = createSelector(
  selectEnterpriseReportingFilter,
  ({ loadingSavedFilters, loadingDefaultFilter, loadingIframe }) =>
    loadingSavedFilters || loadingDefaultFilter || loadingIframe
);

export const selectCombinedErrors = createSelector(
  selectEnterpriseReportingFilter,
  ({ loadSavedFiltersError, loadDefaultFilterError }) => loadSavedFiltersError || loadDefaultFilterError
);

export const selectFiltersToDisplay = createSelector(
  selectEnterpriseReportingFilter,
  ({ appliedFilter, previewFilter, appliedFilterName, previewFilterName }) => {
    const selectedFilter = appliedFilterName === previewFilterName ? appliedFilter : previewFilter;
    // Safeguard: ensure we always return a valid object, defaulting to empty object
    return selectedFilter && typeof selectedFilter === 'object' ? selectedFilter : {};
  }
);
