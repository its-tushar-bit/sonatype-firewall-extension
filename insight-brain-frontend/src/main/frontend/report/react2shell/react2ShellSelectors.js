/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectReact2ShellSlice = prop('react2Shell');

export const selectLoading = createSelector(selectReact2ShellSlice, prop('loading'));

export const selectSorting = createSelector(selectReact2ShellSlice, prop('sorting'));

export const selectError = createSelector(selectReact2ShellSlice, prop('error'));

export const selectSummaryMetrics = createSelector(selectReact2ShellSlice, prop('aggregates'));

export const selectImpactData = createSelector(selectReact2ShellSlice, (state) => {
  return state.impactData || [];
});

export const selectPagination = createSelector(selectReact2ShellSlice, prop('pagination'));

export const selectCurrentPage = createSelector(selectReact2ShellSlice, prop('currentPage'));

export const selectSortBy = createSelector(selectReact2ShellSlice, prop('sortBy'));

export const selectSortOrder = createSelector(selectReact2ShellSlice, prop('sortOrder'));
