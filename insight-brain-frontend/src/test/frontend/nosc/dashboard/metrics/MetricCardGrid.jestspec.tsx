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
import * as activeFilterHook from 'MainRoot/nosc/dashboard/metrics/useDashboardActiveFilter';
import * as metricsHook from 'MainRoot/nosc/dashboard/metrics/useDashboardMetrics';
import { getDashboardMetricsUrl } from 'MainRoot/util/CLMLocation';
import { setupNexusOneBundleLocation } from 'TestRoot/nosc/dashboard/dashboardTestHrefs';
import type { DashboardMetricsResponse } from 'MainRoot/nosc/dashboard/metrics/dashboardMetricsTypes';
import type { UseDashboardMetricsResult } from 'MainRoot/nosc/dashboard/metrics/useDashboardMetrics';

const LAST_UPDATED = 1_700_000_000_000;

const FULL_BODY = {
  applications: { total: 42, breakdown: { stages: 5 }, source: 'index' },
  violations: {
    total: 9,
    breakdown: { critical: 2, severe: 3, moderate: 3, low: 1 },
    source: 'index',
  },
  waivers: { total: 5, breakdown: { existing: 4, requested: 1 }, source: 'sql' },
  lastUpdatedAt: LAST_UPDATED,
};

// FULL_BODY plus the CLM-40927 cheap-tier metrics (index-native; all optional in the contract).
const CHEAP_TIER_BODY = {
  ...FULL_BODY,
  components: { total: 137, breakdown: null, source: 'index' },
  vulnerabilities: {
    total: 66,
    breakdown: { critical: 5, high: 12, medium: 18, low: 7 },
    source: 'index',
  },
  legal: { total: 32, breakdown: { applications: 5, components: 12 }, source: 'index' },
  organizations: { total: 7, breakdown: null, source: 'index' },
  policies: { total: 24, breakdown: null, source: 'index' },
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
    { preloadedState }
  );
}

function metricsResult(overrides: Partial<UseDashboardMetricsResult> = {}): UseDashboardMetricsResult {
  return {
    status: 'ready',
    data: FULL_BODY,
    error: null,
    heavyLoading: false,
    heavyError: null,
    retry: jest.fn(),
    retryHeavy: jest.fn(),
    ...overrides,
  };
}

describe('MetricCardGrid (CLM-40905 AT-F16: landing grid)', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    setupNexusOneBundleLocation();
    jest.spyOn(activeFilterHook, 'useDashboardActiveFilter').mockReturnValue({
      loading: false,
      scope: {},
      needsAcknowledgement: false,
      error: null,
      retry: jest.fn(),
    });
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
    expect(screen.queryByTestId('metric-card-violations-skeleton')).not.toBeInTheDocument();
    expect(screen.getByTestId('metric-card-waivers-skeleton')).toBeInTheDocument();
  });

  it('issues two-phase metric POSTs carrying the active filter ids (AT-F16: filter scope → request)', async () => {
    const scope = {
      organizationIds: ['org-1'],
      applicationIds: ['app-2', 'app-3'],
      stageIds: ['build'],
      tagIds: ['tag-1'],
    };
    jest.spyOn(activeFilterHook, 'useDashboardActiveFilter').mockReturnValue({
      loading: false,
      scope,
      needsAcknowledgement: false,
      error: null,
      retry: jest.fn(),
    });
    const useDashboardMetrics = jest.spyOn(metricsHook, 'useDashboardMetrics');
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());

    expect(useDashboardMetrics).toHaveBeenCalledWith(scope, true);
    await waitFor(() => expect(axiosMock.history.post.length).toBeGreaterThanOrEqual(2));
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
      organizationIds: ['org-1'],
      applicationIds: ['app-2', 'app-3'],
      stageIds: ['build'],
      tagIds: ['tag-1'],
      includeHeavyMetrics: false,
    });
  });

  it('sends an empty body for the default (unfiltered) scope', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({ includeHeavyMetrics: false });
  });

  it('renders the Applications, Violations (+breakdown) and Waivers cards from the response', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42'));

    expect(screen.getByTestId('metric-card-applications-secondary-value')).toHaveTextContent('5');

    // Violations: total + severity breakdown.
    expect(screen.getByTestId('metric-card-violations-value')).toHaveTextContent('9');
    const vBreakdown = screen.getByTestId('metric-card-violations-breakdown');
    expect(within(vBreakdown).getByText('Critical')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-violations-sub-critical-value')).toHaveTextContent('2');
    expect(screen.getByTestId('metric-card-violations-sub-low-value')).toHaveTextContent('1');

    // Waivers: total + existing/requested (stat rows, no severity dots).
    expect(screen.getByTestId('metric-card-waivers-value')).toHaveTextContent('5');
    expect(screen.getByTestId('metric-card-waivers-sub-existing-waivers-value')).toHaveTextContent('4');
    expect(screen.getByTestId('metric-card-waivers-sub-requested-waivers-value')).toHaveTextContent('1');
  });

  it('keeps mixed successful and unavailable cards visible without displaying a broader-scope number', () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue(
      metricsResult({
        data: {
          ...FULL_BODY,
          applications: {
            total: null,
            breakdown: null,
            source: null,
            errorCode: 'UNSUPPORTED_FILTER_COMBINATION',
            unsupportedDimensions: ['stageIds', 'tagIds'],
          },
        } as DashboardMetricsResponse,
      })
    );

    renderGrid();

    const applicationsCard = screen.getByTestId('metric-card-applications');
    expect(within(applicationsCard).getByRole('status')).toHaveTextContent('Unavailable for Stage and Tag filters');
    expect(within(applicationsCard).queryByTestId('metric-card-applications-value')).not.toBeInTheDocument();
    expect(screen.getByTestId('metric-card-violations-value')).toHaveTextContent('9');
  });

  it('renders METRIC_UNAVAILABLE SQL tiles as temporarily unavailable instead of 0', () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue(
      metricsResult({
        data: {
          ...FULL_BODY,
          applications: {
            total: null,
            breakdown: null,
            source: 'sql',
            errorCode: 'METRIC_UNAVAILABLE',
          },
          organizations: {
            total: null,
            breakdown: null,
            source: 'sql',
            errorCode: 'METRIC_UNAVAILABLE',
          },
          policies: {
            total: null,
            breakdown: null,
            source: 'sql',
            errorCode: 'METRIC_UNAVAILABLE',
          },
        } as DashboardMetricsResponse,
      })
    );

    renderGrid();

    const applicationsCard = screen.getByTestId('metric-card-applications');
    expect(within(applicationsCard).getByRole('status')).toHaveTextContent('Temporarily unavailable');
    expect(within(applicationsCard).queryByTestId('metric-card-applications-value')).not.toBeInTheDocument();
    expect(within(screen.getByTestId('metric-card-orgs-and-policies')).getByRole('status')).toHaveTextContent(
      'Temporarily unavailable'
    );
    expect(screen.getByTestId('metric-card-violations-value')).toHaveTextContent('9');
  });

  it('renders a zero total honestly (AT-F16: empty/zero state)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, {
      applications: { total: 0, breakdown: { stages: 5 }, source: 'index' },
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
    // Heavy-tier skeleton may flash while the second POST is in flight; wait until it settles.
    await waitFor(() => expect(screen.queryByTestId('metric-card-components')).not.toBeInTheDocument());
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

  // #16359 regression guards: the filter gate and the stale-data refresh banners
  // must survive the CLM-40927 card additions.
  it('does not POST metrics while the dashboard filter rail is still loading', () => {
    jest.spyOn(activeFilterHook, 'useDashboardActiveFilter').mockReturnValue({
      loading: true,
      scope: { organizationIds: ['org-1'] },
      needsAcknowledgement: false,
      error: null,
      retry: jest.fn(),
    });
    const useDashboardMetrics = jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue(metricsResult());
    renderGrid();

    expect(useDashboardMetrics).toHaveBeenCalledWith({ organizationIds: ['org-1'] }, false);
    expect(screen.getByTestId('metric-card-applications-skeleton')).toBeInTheDocument();
  });

  it('disables metrics while the active filter needs acknowledgement', () => {
    jest.spyOn(activeFilterHook, 'useDashboardActiveFilter').mockReturnValue({
      loading: false,
      scope: { organizationIds: ['org-1'] },
      needsAcknowledgement: true,
      error: null,
      retry: jest.fn(),
    });
    const useDashboardMetrics = jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue(metricsResult());

    renderGrid();

    expect(useDashboardMetrics).toHaveBeenCalledWith({ organizationIds: ['org-1'] }, false);
    expect(screen.getByTestId('metric-card-applications-skeleton')).toBeInTheDocument();
  });

  it('shows a retryable active-filter error before metric states', async () => {
    const retry = jest.fn();
    jest.spyOn(activeFilterHook, 'useDashboardActiveFilter').mockReturnValue({
      loading: false,
      scope: {},
      needsAcknowledgement: false,
      error: new Error('filter failed'),
      retry,
    });
    const useDashboardMetrics = jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue(metricsResult());
    const user = userEvent.setup();

    renderGrid();

    expect(useDashboardMetrics).toHaveBeenCalledWith({}, false);
    const error = screen.getByTestId('dashboard-active-filter-error');
    await user.click(within(error).getByRole('button', { name: /retry/i }));
    expect(retry).toHaveBeenCalledTimes(1);
  });

  it('keeps summary cards visible and retries only delayed detailed metrics', async () => {
    const retryHeavy = jest.fn();
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue(
      metricsResult({
        heavyError: new Error('heavy failed'),
        retryHeavy,
      })
    );
    const user = userEvent.setup();

    renderGrid();

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('dashboard-heavy-metrics-delayed')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /retry detailed metrics/i }));
    expect(retryHeavy).toHaveBeenCalledTimes(1);
  });

  it('keeps last-known-good cards visible when a refresh fails after a successful load', () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue({
      status: 'error',
      data: FULL_BODY,
      error: new Error('refresh failed'),
      heavyLoading: false,
      heavyError: null,
      retry: jest.fn(),
      retryHeavy: jest.fn(),
    });
    renderGrid();

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('dashboard-metrics-error')).toHaveTextContent(/last successful values/i);
  });

  it('shows a refreshing notice while stale cards are held during a scope refetch', () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue({
      status: 'loading',
      data: FULL_BODY,
      error: null,
      heavyLoading: false,
      heavyError: null,
      retry: jest.fn(),
      retryHeavy: jest.fn(),
    });
    renderGrid();

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('dashboard-metrics-refreshing')).toHaveTextContent(/refreshing metrics/i);
  });

  it('shows a building banner while stale cards are held on a 409 refresh', () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue({
      status: 'not-ready',
      data: FULL_BODY,
      error: null,
      heavyLoading: false,
      heavyError: null,
      retry: jest.fn(),
      retryHeavy: jest.fn(),
    });
    renderGrid();

    expect(screen.getByTestId('metric-card-applications-value')).toHaveTextContent('42');
    expect(screen.getByTestId('dashboard-metrics-not-ready')).toHaveTextContent(/last successful values/i);
  });

  it('derives a freshness label from lastUpdatedAt using a controlled clock (AT-F16: Updated Xs ago)', async () => {
    jest.spyOn(Date, 'now').mockReturnValue(LAST_UPDATED + 15_000);
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-freshness')).toHaveTextContent('Updated 15s ago'));
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
    expect(screen.getByRole('heading', { level: 2, name: 'Violations' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Waivers' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Applications/ })).toBeInTheDocument();
  });

  it('renders the cheap-tier cards (vulnerabilities, legal, orgs+policies, scanned components) from the response (CLM-40927)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, CHEAP_TIER_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('metric-card-vulnerabilities-value')).toHaveTextContent('66'));

    // Vulnerabilities: total + Critical/High/Medium/Low severity breakdown.
    const vulnBreakdown = screen.getByTestId('metric-card-vulnerabilities-breakdown');
    expect(within(vulnBreakdown).getByText('Critical')).toBeInTheDocument();
    expect(within(vulnBreakdown).getByText('High')).toBeInTheDocument();
    expect(within(vulnBreakdown).getByText('Medium')).toBeInTheDocument();
    expect(within(vulnBreakdown).getByText('Low')).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-vulnerabilities-sub-critical-value')).toHaveTextContent('5');
    expect(screen.getByTestId('metric-card-vulnerabilities-sub-high-value')).toHaveTextContent('12');
    expect(screen.getByTestId('metric-card-vulnerabilities-sub-medium-value')).toHaveTextContent('18');
    expect(screen.getByTestId('metric-card-vulnerabilities-sub-low-value')).toHaveTextContent('7');

    // Legal Obligations: dual-hero Applications / Components (no single hero total).
    expect(screen.queryByTestId('metric-card-legal-value')).not.toBeInTheDocument();
    expect(screen.getByTestId('metric-card-legal-dual-applications-value')).toHaveTextContent('5');
    expect(screen.getByTestId('metric-card-legal-dual-components-value')).toHaveTextContent('12');

    // Orgs and Policies: dual-hero Organizations / Policies.
    expect(screen.queryByTestId('metric-card-orgs-and-policies-value')).not.toBeInTheDocument();
    expect(screen.getByTestId('metric-card-orgs-and-policies-dual-organizations-value')).toHaveTextContent('7');
    expect(screen.getByTestId('metric-card-orgs-and-policies-dual-policies-value')).toHaveTextContent('24');

    // Scanned Components: hero total + Total Policy Violations secondary stat.
    expect(screen.getByRole('heading', { name: 'Scanned Components' })).toBeInTheDocument();
    expect(screen.getByTestId('metric-card-components-value')).toHaveTextContent('137');
    expect(screen.getByTestId('metric-card-components-secondary-value')).toHaveTextContent('9');
  });

  it('omits the cheap-tier cards when their metrics are absent (additive/graceful)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());
    // Summary-tier absence is immediate; heavy-tier cards omit after the heavy POST settles.
    expect(screen.queryByTestId('metric-card-orgs-and-policies')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.queryByTestId('metric-card-vulnerabilities')).not.toBeInTheDocument());
    expect(screen.queryByTestId('metric-card-legal')).not.toBeInTheDocument();
  });

  it('always renders the bottom quick-link row (CLM-40927 / CLM-43206)', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, FULL_BODY);
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-quicklinks')).toBeInTheDocument());
    expect(screen.getByRole('heading', { name: 'Success Metrics' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Enterprise Reporting' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'API' })).toBeInTheDocument();
    // In-shell Classic embeds stay on the Nexus One bundle (CLM-43206), matching LeftNav.
    expect(screen.getByTestId('dashboard-quicklink-success-metrics').getAttribute('href')).toMatch(
      /#\/success-metrics$/,
    );
    expect(screen.getByTestId('dashboard-quicklink-enterprise-reporting').getAttribute('href')).toMatch(
      /#\/reports$/,
    );
    expect(screen.getByTestId('dashboard-quicklink-api').getAttribute('href')).toMatch(/#\/api$/);
  });

  it('still renders quick links when the initial metrics request fails (static navigation)', () => {
    jest.spyOn(metricsHook, 'useDashboardMetrics').mockReturnValue({
      status: 'error',
      data: null,
      error: new Error('network'),
      heavyLoading: false,
      heavyError: null,
      retry: jest.fn(),
      retryHeavy: jest.fn(),
    });
    renderGrid();

    expect(screen.getByTestId('dashboard-metrics-error')).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-quicklinks')).toBeInTheDocument();
    expect(screen.getByTestId('dashboard-quicklink-success-metrics')).toBeInTheDocument();
  });

  it('omits the components secondary stat when violations are absent', async () => {
    const { violations: _omit, ...withoutViolations } = FULL_BODY;
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, {
      ...withoutViolations,
      components: { total: 137, breakdown: null, source: 'index' },
    });
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('metric-card-components-value')).toHaveTextContent('137'));
    expect(screen.queryByTestId('metric-card-components-secondary-value')).not.toBeInTheDocument();
  });

  it('renders legal with headline total when breakdown is null', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, {
      ...FULL_BODY,
      legal: { total: 10, breakdown: null, source: 'index' },
    });
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('metric-card-legal-value')).toHaveTextContent('10'));
    expect(screen.queryByTestId('metric-card-legal-dual-applications-value')).not.toBeInTheDocument();
  });

  it('omits orgs-and-policies when only one side of the pair is present', async () => {
    axiosMock.onPost(getDashboardMetricsUrl()).reply(200, {
      ...FULL_BODY,
      policies: { total: 24, breakdown: null, source: 'index' },
    });
    renderGrid();

    await waitFor(() => expect(screen.getByTestId('dashboard-metrics-ready')).toBeInTheDocument());
    expect(screen.queryByTestId('metric-card-orgs-and-policies')).not.toBeInTheDocument();
  });
});
