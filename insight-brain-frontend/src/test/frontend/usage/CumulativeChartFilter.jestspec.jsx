/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import CumulativeChartFilter from 'MainRoot/usage/CumulativeChartFilter';

const preloadedState = (filter) => ({
  usage: {
    activeTab: 'overview',
    cumulativeFilter: filter,
    summary: null,
    historyBreakdown: [],
    chartAggregation: 'daily',
    cumulativeHistoryBreakdown: [],
    cumulativeChartAggregation: 'daily',
    sourceBreakdown: [],
    stageBreakdown: [],
    topApps: null,
    dailyHistory: null,
    lastRefreshedAt: null,
    loadingSummary: false,
    loadingHistoryBreakdown: false,
    loadingSourceBreakdown: false,
    loadingStageBreakdown: false,
    loadingTopApps: false,
    loadingDailyHistory: false,
    loadingAll: false,
    loadErrorSummary: null,
    loadErrorHistoryBreakdown: null,
    loadErrorSourceBreakdown: null,
    loadErrorStageBreakdown: null,
    loadErrorTopApps: null,
    loadErrorDailyHistory: null,
    loadErrorAll: null,
  },
});

it('renders three options (This month, Last 3 months, Last 6 months)', () => {
  render(<CumulativeChartFilter />, { preloadedState: preloadedState('thisMonth') });
  expect(screen.getByRole('button', { name: /This month/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Last 3 months/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Last 6 months/i })).toBeInTheDocument();
});

it('marks the active option with aria-pressed=true', () => {
  render(<CumulativeChartFilter />, { preloadedState: preloadedState('last3Months') });
  expect(screen.getByRole('button', { name: /Last 3 months/i })).toHaveAttribute('aria-pressed', 'true');
});

it('dispatches setCumulativeFilter on click', async () => {
  const user = userEvent.setup();
  const { store } = render(<CumulativeChartFilter />, { preloadedState: preloadedState('thisMonth') });
  await user.click(screen.getByRole('button', { name: /Last 6 months/i }));
  expect(store.getState().usage.cumulativeFilter).toBe('last6Months');
});
