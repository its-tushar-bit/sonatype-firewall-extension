/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectOtherVersionsSlice = prop('quarantinedComponentReportOtherVersions');

export const selectLoading = createSelector(selectOtherVersionsSlice, prop('loading'));

export const selectLoadError = createSelector(selectOtherVersionsSlice, prop('loadError'));

export const selectOtherVersions = createSelector(selectOtherVersionsSlice, prop('otherVersions'));

export const selectPageCount = createSelector(selectOtherVersionsSlice, prop('pageCount'));

export const selectPageSize = createSelector(selectOtherVersionsSlice, prop('pageSize'));

export const selectCurrentPage = createSelector(selectOtherVersionsSlice, prop('currentPage'));

export const selectSortAsc = createSelector(selectOtherVersionsSlice, prop('sortAsc'));

export const selectRouterSlice = prop('router');

export const selectRouterCurrentParams = createSelector(selectRouterSlice, prop('currentParams'));

export const selectToken = createSelector(selectRouterCurrentParams, prop('token'));
