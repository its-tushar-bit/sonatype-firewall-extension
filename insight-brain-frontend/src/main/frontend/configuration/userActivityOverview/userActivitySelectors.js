/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop, propOr } from 'ramda';

/**
 * Selectors for user activity state
 */

// Base selector for the user activity slice
export const selectUserActivitySlice = (state) => state.userActivity || {};

// Data selectors
export const selectUserActivityData = createSelector(selectUserActivitySlice, propOr([], 'users'));

export const selectTotalUsers = createSelector(selectUserActivitySlice, propOr(0, 'totalUsers'));

export const selectDateRange = createSelector(
  selectUserActivitySlice,
  propOr({ startDate: null, endDate: null }, 'dateRange')
);

export const selectPagination = createSelector(
  selectUserActivitySlice,
  propOr({ limit: 100, offset: 0, hasMore: false }, 'pagination')
);

// Search selectors
export const selectSearchFilter = createSelector(selectUserActivitySlice, propOr('', 'searchFilter'));

// Loading and error selectors
export const selectUserActivityLoading = createSelector(selectUserActivitySlice, propOr(false, 'loading'));

export const selectUserActivityError = createSelector(selectUserActivitySlice, prop('loadError'));

export const selectUserActivityExporting = createSelector(selectUserActivitySlice, propOr(false, 'exporting'));

export const selectUserActivityExportError = createSelector(selectUserActivitySlice, prop('exportError'));

// Details export selectors
export const selectUserActivityDetailsExporting = createSelector(
  selectUserActivitySlice,
  propOr(false, 'detailsExporting')
);

export const selectUserActivityDetailsExportError = createSelector(selectUserActivitySlice, prop('detailsExportError'));

// Filter drawer selectors
export const selectFilterDrawerOpen = createSelector(selectUserActivitySlice, propOr(false, 'filterDrawerOpen'));

// Two-state filter selectors (like Dashboard)
export const selectSelectedFilters = createSelector(
  selectUserActivitySlice,
  propOr({ selectedAge: 30 }, 'selectedFilters')
);

export const selectAppliedFilters = createSelector(
  selectUserActivitySlice,
  propOr({ selectedAge: 30 }, 'appliedFilters')
);

export const selectFiltersAreDirty = createSelector(selectUserActivitySlice, propOr(false, 'filtersAreDirty'));

// Convenience selectors for specific filter values
export const selectSelectedAge = createSelector(selectSelectedFilters, propOr(30, 'selectedAge'));

export const selectAppliedAge = createSelector(selectAppliedFilters, propOr(30, 'selectedAge'));

// Combined selectors for convenience
export const selectUserActivityState = createSelector(
  selectUserActivityData,
  selectTotalUsers,
  selectUserActivityLoading,
  selectUserActivityError,
  selectUserActivityExporting,
  selectUserActivityExportError,
  selectDateRange,
  selectPagination,
  (users, totalUsers, loading, loadError, exporting, exportError, dateRange, pagination) => ({
    users,
    totalUsers,
    loading,
    loadError,
    exporting,
    exportError,
    dateRange,
    pagination,
  })
);

// Feature flag selector for user activity tracking
export const selectIsUserActivityTrackingEnabled = createSelector(
  (state) => state.productFeatures?.productFeatures || {},
  (productFeatures) => propOr(false, 'user-activity-tracking', productFeatures)
);
