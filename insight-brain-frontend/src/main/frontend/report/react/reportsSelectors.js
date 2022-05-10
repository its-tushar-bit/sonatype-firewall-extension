/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectReportsSlice = prop('reports');

export const selectReportsLoading = createSelector(selectReportsSlice, prop('loading'));

export const selectReportsLoadError = createSelector(selectReportsSlice, prop('loadError'));

export const selectHasMoreReports = createSelector(selectReportsSlice, prop('hasMoreResults'));

export const selectReportsStages = createSelector(selectReportsSlice, prop('stages'));

export const selectAppliedSortReports = createSelector(selectReportsSlice, prop('appliedSort'));

export const selectApplicationsInformationList = createSelector(
  selectReportsSlice,
  prop('applicationsInformationList')
);

export const selectReportsPages = createSelector(selectReportsSlice, prop('pages'));

export const selectReportsFilter = createSelector(selectReportsSlice, prop('appFilter'));
export const selectLoadingPublicIds = createSelector(selectReportsSlice, prop('loadingPublicIds'));
