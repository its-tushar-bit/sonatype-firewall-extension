/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

export const selectUsageSlice = prop('usage');

export const selectSummary = createSelector(selectUsageSlice, prop('summary'));
export const selectHistoryBreakdown = createSelector(selectUsageSlice, prop('historyBreakdown'));
export const selectChartAggregation = createSelector(selectUsageSlice, prop('chartAggregation'));
export const selectSourceBreakdown = createSelector(selectUsageSlice, prop('sourceBreakdown'));
export const selectTopApps = createSelector(selectUsageSlice, prop('topApps'));
export const selectDailyHistory = createSelector(selectUsageSlice, prop('dailyHistory'));

export const selectLoadingSummary = createSelector(selectUsageSlice, prop('loadingSummary'));
export const selectLoadingHistoryBreakdown = createSelector(selectUsageSlice, prop('loadingHistoryBreakdown'));
export const selectLoadingSourceBreakdown = createSelector(selectUsageSlice, prop('loadingSourceBreakdown'));
export const selectLoadingTopApps = createSelector(selectUsageSlice, prop('loadingTopApps'));
export const selectLoadingDailyHistory = createSelector(selectUsageSlice, prop('loadingDailyHistory'));
export const selectLoadingAll = createSelector(selectUsageSlice, prop('loadingAll'));

export const selectLoadErrorSummary = createSelector(selectUsageSlice, prop('loadErrorSummary'));
export const selectLoadErrorHistoryBreakdown = createSelector(selectUsageSlice, prop('loadErrorHistoryBreakdown'));
export const selectLoadErrorSourceBreakdown = createSelector(selectUsageSlice, prop('loadErrorSourceBreakdown'));
export const selectLoadErrorTopApps = createSelector(selectUsageSlice, prop('loadErrorTopApps'));
export const selectLoadErrorDailyHistory = createSelector(selectUsageSlice, prop('loadErrorDailyHistory'));
export const selectLoadErrorAll = createSelector(selectUsageSlice, prop('loadErrorAll'));
