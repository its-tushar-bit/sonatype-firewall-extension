/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import EvaluatedComponentsTile from 'MainRoot/usage/EvaluatedComponentsTile';

/**
 * Builds a preloadedState for the usage slice. Merges supplied overrides on top
 * of the minimal valid shape so individual tests only need to specify the parts
 * they care about.
 */
function makeState(overrides = {}) {
  return {
    usage: {
      activeTab: 'overview',
      cumulativeFilter: 'thisMonth',
      summary: null,
      historyBreakdown: [],
      cumulativeHistoryBreakdown: [],
      chartAggregation: 'daily',
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
      ...overrides,
    },
  };
}

describe('EvaluatedComponentsTile', () => {
  it('renders nothing when dailyHistory is null and entries are empty', () => {
    const { container } = render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({ dailyHistory: null, historyBreakdown: [] }),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when entries, dailyAverage, and peakDay are all empty', () => {
    const { container } = render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [],
          dailyAverage: 0,
          peakDay: { count: 0, date: null },
        },
      }),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders when only dailyAverage is present (regression guard for empty-state logic)', () => {
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [],
          dailyAverage: 123,
          peakDay: null,
        },
      }),
    });
    expect(screen.getByText('Cumulative Components Evaluated')).toBeInTheDocument();
    expect(screen.getByText('123')).toBeInTheDocument();
  });

  it('rounds fractional dailyAverage to integer (no decimal in widget value)', () => {
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [{ date: '2026-04-15', components: 1, componentsCumulative: 1 }],
          dailyAverage: 117.526,
          peakDay: null,
        },
      }),
    });
    expect(screen.getByText('118')).toBeInTheDocument();
    expect(screen.queryByText(/117\.526|117\.5/)).not.toBeInTheDocument();
  });

  it('renders formatted dailyAverage and peakDay count with thousands separators', () => {
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [{ date: '2026-04-15', components: 1500, componentsCumulative: 1500 }],
          dailyAverage: 1234,
          peakDay: { count: 5678, date: '2026-04-15' },
        },
      }),
    });
    expect(screen.getByText('1,234')).toBeInTheDocument();
    expect(screen.getByText('5,678')).toBeInTheDocument();
  });

  it('Peak Day widget shows the count only — never the date (per Figma)', () => {
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [{ date: '2026-04-15', components: 1, componentsCumulative: 1 }],
          dailyAverage: 100,
          peakDay: { count: 5000, date: '2026-04-15' },
        },
      }),
    });
    // Peak count is rendered, but the 'Apr 15' subline is intentionally absent.
    expect(screen.getByText('5,000')).toBeInTheDocument();
    expect(screen.queryByText('Apr 15')).not.toBeInTheDocument();
  });

  it('renders the chart container when entries are present', () => {
    const { container } = render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [{ date: '2026-04-15', components: 1, componentsCumulative: 1 }],
          dailyAverage: 100,
          peakDay: { count: 500, date: '2026-04-15' },
        },
      }),
    });
    // After the Figma-fidelity rework the inner "Usage Trend" subheading was removed; the
    // chart sits directly under the insights grid. Assert the container renders.
    expect(container.querySelector('.iq-usage-trend-chart')).toBeInTheDocument();
  });

  it('chart container exposes role="img" with a descriptive aria-label', () => {
    // Regression guard for WCAG 1.1.1: the recharts SVG by itself has no
    // accessible name, so the wrapper provides one.
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [{ date: '2026-04-15', components: 1, componentsCumulative: 1 }],
          dailyAverage: 100,
          peakDay: { count: 500, date: '2026-04-15' },
        },
      }),
    });
    expect(screen.getByRole('img', { name: /cumulative components evaluated/i })).toBeInTheDocument();
  });

  it('does not render chart container when entries is empty', () => {
    const { container } = render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [],
          dailyAverage: 100,
          peakDay: { count: 500, date: '2026-04-15' },
        },
      }),
    });
    expect(container.querySelector('.iq-usage-trend-chart')).toBeNull();
  });

  it('renders CumulativeChartFilter in the tile header', () => {
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        dailyHistory: {
          dailyHistory: [{ date: '2026-04-15', components: 1, componentsCumulative: 1 }],
          dailyAverage: 50,
          peakDay: null,
        },
      }),
    });
    // CumulativeChartFilter renders three filter buttons
    expect(screen.getByRole('button', { name: /This month/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Last 3 months/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Last 6 months/i })).toBeInTheDocument();
  });

  it('renders 3-month series when filter=last3Months', () => {
    const preloadedState = makeState({
      cumulativeFilter: 'last3Months',
      // selectCumulativeChartSeries reads cumulativeHistoryBreakdown (not the
      // shared historyBreakdown field) since the Trends/Overview data split.
      cumulativeHistoryBreakdown: [
        { month: '2026-04-01', consumed: 100, breakdown: {} },
        { month: '2026-05-01', consumed: 200, breakdown: {} },
        { month: '2026-06-01', consumed: 300, breakdown: {} },
      ],
      dailyHistory: { dailyHistory: [], dailyAverage: 0, peakDay: null },
    });
    const { container } = render(<EvaluatedComponentsTile />, { preloadedState });
    // selectCumulativeChartSeries produces 3 month-bucketed entries, so the chart container renders.
    expect(container.querySelector('.iq-usage-trend-chart')).toBeInTheDocument();
    expect(screen.getByText('Cumulative Components Evaluated')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Last 3 months/i })).toHaveAttribute('aria-pressed', 'true');
  });

  it('chart aria-label switches between cumulative (daily) and monthly variants', () => {
    // thisMonth: stacked cumulative + daily series.
    const dailyState = makeState({
      cumulativeFilter: 'thisMonth',
      dailyHistory: {
        dailyHistory: [{ date: '2026-06-01', components: 50, componentsCumulative: 50 }],
        dailyAverage: 50,
        peakDay: { count: 50, date: '2026-06-01' },
      },
    });
    const { unmount } = render(<EvaluatedComponentsTile />, { preloadedState: dailyState });
    expect(screen.getByRole('img', { name: /cumulative components evaluated/i })).toBeInTheDocument();
    unmount();

    // last3Months: single monthly Usage series — aria-label drops the cumulative claim.
    const monthlyState = makeState({
      cumulativeFilter: 'last3Months',
      cumulativeChartAggregation: 'monthly',
      cumulativeHistoryBreakdown: [
        { month: '2026-04-01', consumed: 100, breakdown: {} },
        { month: '2026-05-01', consumed: 200, breakdown: {} },
        { month: '2026-06-01', consumed: 300, breakdown: {} },
      ],
      dailyHistory: { dailyHistory: [], dailyAverage: 0, peakDay: null },
    });
    render(<EvaluatedComponentsTile />, { preloadedState: monthlyState });
    expect(screen.getByRole('img', { name: /monthly components evaluated/i })).toBeInTheDocument();
    // Honest legend: the "Cumulative Usage" claim is gone for windowed-only data.
    expect(screen.queryByRole('img', { name: /cumulative usage series/i })).not.toBeInTheDocument();
  });

  it('renders tile shell with inline error when /daily-history failed on thisMonth filter', () => {
    // Regression guard for T9 (manual verification): when a page-level period
    // range > 92 days makes /daily-history 400 but the other 5 endpoints
    // succeed, the tile used to return null because all data fell to defaults.
    // The tile should still render the title and surface the loadErrorDailyHistory
    // so the user knows why the chart is empty.
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        summary: { consumed: 1234, limit: 10000 },
        dailyHistory: null,
        cumulativeFilter: 'thisMonth',
        loadErrorDailyHistory: 'Date range exceeds maximum of 92 days',
      }),
    });
    expect(screen.getByText('Cumulative Components Evaluated')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/Date range exceeds maximum of 92 days/);
    // Total Components Evaluated widget still renders from summary.consumed
    expect(screen.getByText('1,234')).toBeInTheDocument();
  });

  it('renders tile shell with inline error when /history/breakdown failed on last3Months filter', () => {
    // The error source switches based on the active filter: last3/6Months
    // reads cumulativeHistoryBreakdown (from /history/breakdown), so the
    // chart-empty state should surface loadErrorHistoryBreakdown.
    render(<EvaluatedComponentsTile />, {
      preloadedState: makeState({
        summary: { consumed: 500, limit: 10000 },
        cumulativeFilter: 'last3Months',
        cumulativeChartAggregation: 'monthly',
        cumulativeHistoryBreakdown: [],
        loadErrorHistoryBreakdown: 'Server error',
      }),
    });
    expect(screen.getByText('Cumulative Components Evaluated')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/Server error/);
  });
});
