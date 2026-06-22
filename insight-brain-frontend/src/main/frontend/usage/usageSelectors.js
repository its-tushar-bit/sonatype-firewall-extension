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
export const selectStageBreakdown = createSelector(selectUsageSlice, prop('stageBreakdown'));
export const selectTopApps = createSelector(selectUsageSlice, prop('topApps'));
export const selectDailyHistory = createSelector(selectUsageSlice, prop('dailyHistory'));

export const selectLoadingSummary = createSelector(selectUsageSlice, prop('loadingSummary'));
export const selectLoadingHistoryBreakdown = createSelector(selectUsageSlice, prop('loadingHistoryBreakdown'));
export const selectLoadingSourceBreakdown = createSelector(selectUsageSlice, prop('loadingSourceBreakdown'));
export const selectLoadingStageBreakdown = createSelector(selectUsageSlice, prop('loadingStageBreakdown'));
export const selectLoadingTopApps = createSelector(selectUsageSlice, prop('loadingTopApps'));
export const selectLoadingDailyHistory = createSelector(selectUsageSlice, prop('loadingDailyHistory'));
export const selectLoadingAll = createSelector(selectUsageSlice, prop('loadingAll'));

export const selectLoadErrorSummary = createSelector(selectUsageSlice, prop('loadErrorSummary'));
export const selectLoadErrorHistoryBreakdown = createSelector(selectUsageSlice, prop('loadErrorHistoryBreakdown'));
export const selectLoadErrorSourceBreakdown = createSelector(selectUsageSlice, prop('loadErrorSourceBreakdown'));
export const selectLoadErrorStageBreakdown = createSelector(selectUsageSlice, prop('loadErrorStageBreakdown'));
export const selectLoadErrorTopApps = createSelector(selectUsageSlice, prop('loadErrorTopApps'));
export const selectLoadErrorDailyHistory = createSelector(selectUsageSlice, prop('loadErrorDailyHistory'));
export const selectLoadErrorAll = createSelector(selectUsageSlice, prop('loadErrorAll'));

export const selectActiveTab = createSelector(selectUsageSlice, prop('activeTab'));
export const selectCumulativeFilter = createSelector(selectUsageSlice, prop('cumulativeFilter'));
export const selectLastRefreshedAt = createSelector(selectUsageSlice, prop('lastRefreshedAt'));

// Cumulative chart reads its own field so a Trends-tab loadHistoryBreakdown
// (e.g. Daily/Weekly) can never overwrite the monthly buckets the Overview
// chart is rendering.
const selectCumulativeHistoryBreakdownSlice = createSelector(selectUsageSlice, prop('cumulativeHistoryBreakdown'));

export const selectCumulativeChartSeries = createSelector(
  selectDailyHistory,
  selectCumulativeHistoryBreakdownSlice,
  selectCumulativeFilter,
  (dailyHistory, cumulativeHistoryBreakdown, filter) => {
    if (filter === 'thisMonth') {
      return dailyHistory?.dailyHistory ?? [];
    }
    if (!Array.isArray(cumulativeHistoryBreakdown)) return [];
    const N = filter === 'last6Months' ? 6 : 3;
    const sorted = [...cumulativeHistoryBreakdown].sort((a, b) => (a.month || '').localeCompare(b.month || ''));
    const tail = sorted.slice(-N);
    let cumulative = 0;
    return tail.map((entry) => {
      cumulative += entry.consumed || 0;
      return { date: entry.month, components: entry.consumed || 0, componentsCumulative: cumulative };
    });
  }
);
