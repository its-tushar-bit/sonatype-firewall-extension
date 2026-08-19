/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';
import { createSelector } from '@reduxjs/toolkit';

export const selectSourceControlRateLimitsSlice = prop('sourceControlRateLimits');
export const selectData = createSelector(selectSourceControlRateLimitsSlice, prop('data'));
export const selectLoading = createSelector(selectSourceControlRateLimitsSlice, prop('loading'));
export const selectLoadError = createSelector(selectSourceControlRateLimitsSlice, prop('loadError'));
export const selectSortColumn = createSelector(selectSourceControlRateLimitsSlice, prop('sortColumn'));
export const selectSortDirection = createSelector(selectSourceControlRateLimitsSlice, prop('sortDirection'));
export const selectUserRateLimitsExpanded = createSelector(
  selectSourceControlRateLimitsSlice,
  prop('userRateLimitsExpanded')
);
export const selectUserDefiningOwnersExpanded = createSelector(
  selectSourceControlRateLimitsSlice,
  prop('userDefiningOwnersExpanded')
);

export const selectUserAssociatedApplicationsExpanded = createSelector(
  selectSourceControlRateLimitsSlice,
  prop('userAssociatedApplicationsExpanded')
);
export const selectLastUpdated = createSelector(selectSourceControlRateLimitsSlice, prop('lastUpdated'));
