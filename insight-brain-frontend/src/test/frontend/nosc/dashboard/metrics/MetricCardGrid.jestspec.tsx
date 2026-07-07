/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { axiosMockAdapter, render, screen, waitFor, within } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import { MetricCardGrid } from 'MainRoot/nosc/dashboard/metrics/MetricCardGrid';
import * as metricsHook from 'MainRoot/nosc/dashboard/metrics/useDashboardMetrics';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';

const LAST_UPDATED = 1_700_000_000_000;

const FULL_BODY = {
  applications: { total: 42, breakdown: null, source: 'index' },
  violations: {
    total: 9,
    breakdown: { critical: 2, severe: 3, moderate: 3, low: 1 },
    source: 'index',
  },
  waivers: { total: 5, breakdown: { existing: 4, requested: 1 }, source: 'sql' },
  lastUpdatedAt: LAST_UPDATED,
};

function preloadedStateWithFilter(applied?: Record<string, Set<string>>) {
  return {
    dashboardFilter: {
      appliedFilter: applied ?? {
        organizations: new Set<string>(),
        applications: new Set<string>(),
        stages: new Set<string>(),
        categories: new Set<string>(),
      },
    },
  } as any;
}

function renderGrid(preloadedState = preloadedStateWithFilter()) {
  return render(
    <Theme>
      <MetricCardGrid />
    </Theme>,
    { preloadedState },
  );
}

describe('MetricCardGrid (CLM-40905 AT-F16: landing grid)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupNexusOneBundleLocation();
  });

  afterEach(() => {
    axiosMock.reset();
    jest.restoreAllMocks();
  });

  it('renders the grid chrome with per-card skeletons before the response resolves (AT-F16: loading)', () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(() => new Promise(() => {}));
    renderGrid();

    expect(screen.getByTestId('preview-dashboard-metrics-grid')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-applications-skeleton')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-violations-skeleton')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-waivers-skeleton')).toBeInTheDocument();
  });

  it('issues exactly one POST on mount carrying the active filter ids (AT-F16: filter scope → request)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid(
      preloadedStateWithFilter({
        organizations: new Set(['org-1']),
        applications: new Set(['app-2', 'app-3']),
        stages: new Set(['build']),
        categories: new Set(['tag-1']),
      }),
    );

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());

    expect(axiosMock.history.post).toHaveLength(1);
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
      organizationIds: ['org-1'],
      applicationIds: ['app-2', 'app-3'],
      stageIds: ['build'],
      tagIds: ['tag-1'],
    });
  });

  it('sends an empty body for the default (unfiltered) scope', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({});
  });

  it('renders the Applications, Violations (+breakdown) and Waivers cards from the response', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42'));

    // Violations: total + severity breakdown.
    expect(screen.getByTestId('metric-card-violations-value')).toHaveTextContent('9');
    const vBreakdown = screen.getByTestId('metric-card-violations-breakdown');
    expect(within(vBreakdown).getByText('Critical')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-violations-sub-critical-value')).toHaveTextContent('2');
    expect(screen.getByTestId('metric-card-violations-sub-low-value')).toHaveTextContent('1');

    // Waivers: total + existing/requested.
    expect(screen.getByTestId('metric-card-waivers-value')).toHaveTextContent('5');
    expect(screen.getByTestId('metric-card-waivers-sub-existing-value')).toHaveTextContent('4');
    expect(screen.getByTestId('metric-card-waivers-sub-requested-value')).toHaveTextContent('1');
  });

  it('renders a zero total honestly (AT-F16: empty/zero state)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, {
      applications: { total: 0, breakdown: null, source: 'index' },
      violations: { total: 0, breakdown: { critical: 0, severe: 0, moderate: 0, low: 0 }, source: 'index' },
      waivers: { total: 0, breakdown: { existing: 0, requested: 0 }, source: 'sql' },
      lastUpdatedAt: null,
    });
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('0'));
    expect(screen.getByTestId('metric-card-violations-value')).toHaveTextContent('0');
  });

  it('omits the Components card when the metric is absent, and renders it when present (additive/graceful)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    const { unmount } = renderGrid();
    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());
    expect(screen.queryByTestId('metric-card-components')).not.toBeInTheDocument();
    unmount();

    axiosMock.reset();
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, {
      ...FULL_BODY,
      components: { total: 137, breakdown: null, source: 'index' },
    });
    renderGrid();
    await waitFor(() => expect(screen.getByTestId('metric-card-components-value')).toHaveTextContent('137'));
  });

  it('shows a "metrics are building" panel with retry on 409, not a raw error (AT-F16: index not ready)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(409, { message: 'building' });
    renderGrid();

    const panel = await screen.findByTestId('dashboard-metrics-not-ready');
    expect(panel).toHaveTextContent(/building/i);
    expect(within(panel).getByRole('button', { name: /retry/i })).toBeInTheDocument();
    expect(screen.queryByTestId('dashboard-metrics-error')).not.toBeInTheDocument();
  });

  it('shows an error panel with a working retry that recovers, without blanking the grid (AT-F16: error+retry)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).replyOnce(500, {});
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    const user = userEvent.setup();
    renderGrid();

    const errorPanel = await screen.findByTestId('dashboard-metrics-error');
    await user.click(within(errorPanel).getByRole('button', { name: /retry/i }));

    await waitFor(() => expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42'));
  });

  it('keeps last-known-good cards visible when a refresh fails after a successful load', async () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue({
      status: 'error',
      data: FULL_BODY,
      error: new Error('refresh failed'),
      retry: jest.fn(),
    });
    renderGrid();

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('dashboard-metrics-error')).toHaveTextContent(/last successful values/i);
  });

  it('shows a refreshing notice while stale cards are held during a scope refetch', async () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue({
      status: 'loading',
      data: FULL_BODY,
      error: null,
      retry: jest.fn(),
    });
    renderGrid();

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('dashboard-metrics-refreshing')).toHaveTextContent(/refreshing metrics/i);
  });

  it('shows a building banner while stale cards are held on a 409 refresh', async () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue({
      status: 'not-ready',
      data: FULL_BODY,
      error: null,
      retry: jest.fn(),
    });
    renderGrid();

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('dashboard-metrics-not-ready')).toHaveTextContent(/last successful values/i);
  });

  it('does not POST metrics while the dashboard filter rail is still loading', () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    render(
      <Theme>
        <MetricCardGrid />
      </Theme>,
      {
        preloadedState: {
          dashboardFilter: {
            loading: true,
            appliedFilter: {
              organizations: new Set(['org-1']),
              applications: new Set<string>(),
              stages: new Set<string>(),
              categories: new Set<string>(),
            },
          },
        } as any,
      },
    );

    expect(axiosMock.history.post).toHaveLength(0);
    expect(screen.getByTestId('metric-card-applications-skeleton')).toBeInTheDocument();
  });

  it('derives a freshness label from lastUpdatedAt using a controlled clock (AT-F16: Updated Xs ago)', async () => {
    jest.spyOn(Date, 'now').mockReturnValue(LAST_UPDATED + 15_000);
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() =>
      expect(screen.getByTestId('dashboard-metrics-freshness')).toHaveTextContent('Updated 15s ago'),
    );
  });

  it('degrades gracefully when lastUpdatedAt is null (no freshness line, no crash)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, { ...FULL_BODY, lastUpdatedAt: null });
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());
    expect(screen.queryByTestId('dashboard-metrics-freshness')).not.toBeInTheDocument();
  });

  it('exposes accessible card headings and named click-through links (AT-F16: a11y)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    // Wait for the ready state (loading skeletons also render headings, so gate on a value).
    await waitFor(() => expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42'));
    expect(screen.getByRole('heading', { level: 2, name: 'Applications' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Policy Violations' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Waivers' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Applications, 42 total, open list' })).toBeInTheDocument();
  });
});
