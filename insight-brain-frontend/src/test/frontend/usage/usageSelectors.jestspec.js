/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectActiveTab,
  selectCumulativeFilter,
  selectLastRefreshedAt,
  selectCumulativeChartSeries,
} from 'MainRoot/usage/usageSelectors';

const baseUsage = {
  activeTab: 'overview',
  cumulativeFilter: 'thisMonth',
  lastRefreshedAt: null,
  dailyHistory: {
    dailyHistory: [
      { date: '2026-06-01', components: 1, componentsCumulative: 1 },
      { date: '2026-06-02', components: 2, componentsCumulative: 3 },
    ],
  },
  // selectCumulativeChartSeries reads cumulativeHistoryBreakdown (not historyBreakdown)
  // since PR A's regression fix that splits Trends-owned vs Overview-owned data.
  cumulativeHistoryBreakdown: [
    { month: '2026-04-01', consumed: 100, breakdown: { 'App Scan + Re-evaluate': 100 } },
    { month: '2026-05-01', consumed: 200, breakdown: { 'App Scan + Re-evaluate': 200 } },
    { month: '2026-06-01', consumed: 300, breakdown: { 'App Scan + Re-evaluate': 300 } },
  ],
};

it('selectActiveTab returns activeTab', () => {
  expect(selectActiveTab({ usage: baseUsage })).toBe('overview');
});

it('selectCumulativeFilter returns the filter', () => {
  expect(selectCumulativeFilter({ usage: { ...baseUsage, cumulativeFilter: 'last3Months' } })).toBe('last3Months');
});

it('selectLastRefreshedAt returns lastRefreshedAt', () => {
  expect(selectLastRefreshedAt({ usage: baseUsage })).toBeNull();
  expect(selectLastRefreshedAt({ usage: { ...baseUsage, lastRefreshedAt: 1234567890 } })).toBe(1234567890);
});

it('selectCumulativeChartSeries returns dailyHistory entries when filter=thisMonth', () => {
  expect(selectCumulativeChartSeries({ usage: baseUsage })).toEqual(baseUsage.dailyHistory.dailyHistory);
});

it('selectCumulativeChartSeries returns last 3 monthly buckets when filter=last3Months', () => {
  const series = selectCumulativeChartSeries({ usage: { ...baseUsage, cumulativeFilter: 'last3Months' } });
  expect(series).toHaveLength(3);
  expect(series[0].date).toBe('2026-04-01');
  expect(series[2].date).toBe('2026-06-01');
});

it('selectCumulativeChartSeries returns last 6 monthly buckets when filter=last6Months', () => {
  const sixMonthBreakdown = [
    { month: '2026-01-01', consumed: 30, breakdown: {} },
    { month: '2026-02-01', consumed: 50, breakdown: {} },
    { month: '2026-03-01', consumed: 75, breakdown: {} },
    { month: '2026-04-01', consumed: 100, breakdown: {} },
    { month: '2026-05-01', consumed: 200, breakdown: {} },
    { month: '2026-06-01', consumed: 300, breakdown: {} },
  ];
  const series = selectCumulativeChartSeries({
    usage: { ...baseUsage, cumulativeFilter: 'last6Months', cumulativeHistoryBreakdown: sixMonthBreakdown },
  });
  expect(series).toHaveLength(6);
  expect(series[0].date).toBe('2026-01-01');
  expect(series[5].date).toBe('2026-06-01');
});

it('selectCumulativeChartSeries returns dailyHistory entries when filter=thisMonth — uses dailyHistory not cumulativeHistoryBreakdown', () => {
  expect(selectCumulativeChartSeries({ usage: { ...baseUsage, dailyHistory: null } })).toEqual([]);
});

it('selectCumulativeChartSeries returns empty array when cumulativeHistoryBreakdown is null and filter=last3Months', () => {
  expect(
    selectCumulativeChartSeries({
      usage: { ...baseUsage, cumulativeFilter: 'last3Months', cumulativeHistoryBreakdown: null },
    })
  ).toEqual([]);
});

it('selectCumulativeChartSeries maps components and cumulative fields correctly for last3Months', () => {
  const series = selectCumulativeChartSeries({ usage: { ...baseUsage, cumulativeFilter: 'last3Months' } });
  expect(series[0]).toEqual({ date: '2026-04-01', components: 100, componentsCumulative: 100 });
  expect(series[1]).toEqual({ date: '2026-05-01', components: 200, componentsCumulative: 300 });
  expect(series[2]).toEqual({ date: '2026-06-01', components: 300, componentsCumulative: 600 });
});

it('selectCumulativeChartSeries returns all available rows when historyBreakdown has fewer than N months', () => {
  // Edge case: a fresh tenant has only 2 months of data. last6Months selector
  // should return the 2 rows it has, not crash and not pad with empty rows.
  const twoMonthBreakdown = [
    { month: '2026-05-01', consumed: 50, breakdown: {} },
    { month: '2026-06-01', consumed: 80, breakdown: {} },
  ];
  const series = selectCumulativeChartSeries({
    usage: { ...baseUsage, cumulativeFilter: 'last6Months', cumulativeHistoryBreakdown: twoMonthBreakdown },
  });
  expect(series).toHaveLength(2);
  expect(series[0]).toEqual({ date: '2026-05-01', components: 50, componentsCumulative: 50 });
  expect(series[1]).toEqual({ date: '2026-06-01', components: 80, componentsCumulative: 130 });
});
