/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { act, axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import UsageDashboard from 'MainRoot/usage/UsageDashboard';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

describe('UsageDashboard', () => {
  let axiosMock, stateGoSpy;

  const summaryResponse = {
    consumed: 1478,
    limit: 50000,
    percentUsed: 3.0,
    remaining: 48522,
    resetDate: '2026-06-01',
    billingWindowStart: '2026-05-01',
    tier: 'ADVANCED',
    // Field name mirrors the backend `ConsumptionSummaryDTO` JSON shape, which
    // serialises this as `activityBreakdown` (not `breakdown`).
    activityBreakdown: {
      'App Scan + Re-evaluate': 746,
      'Continuous Monitoring': 434,
      'Component Details': 23,
      'Version Recommendations': 32,
      'Reachability Analysis': 87,
      APIs: 156,
    },
  };

  const defaultPreloadedState = {
    usage: {
      summary: null,
      loadingSummary: false,
      loadingHistoryBreakdown: false,
      loadingSourceBreakdown: false,
      loadingTopApps: false,
      loadingDailyHistory: false,
      loadingAll: false,
      loadErrorSummary: null,
      loadErrorHistoryBreakdown: null,
      loadErrorSourceBreakdown: null,
      loadErrorTopApps: null,
      loadErrorDailyHistory: null,
      loadErrorAll: null,
      activeTab: 'overview',
      cumulativeFilter: 'thisMonth',
      lastRefreshedAt: null,
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    axiosMock.reset();
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, summaryResponse);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/breakdown/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/history\/by-source/).reply(200, []);
    axiosMock
      .onGet(/\/api\/v2\/consumption\/history\/by-stage/)
      .reply(200, [{ month: '2026-05-01', consumed: 250, breakdown: { build: 250 } }]);
    axiosMock.onGet(/\/api\/v2\/consumption\/top-apps/).reply(200, []);
    axiosMock.onGet(/\/api\/v2\/consumption\/daily-history/).reply(200, null);
  });

  const renderComponent = (props = {}, preloadedState) => {
    return render(<UsageDashboard isAuthorized={true} {...props} />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('should render page heading', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Usage' })).toBeInTheDocument();
    });
  });

  it('renders Usage Categories tile with the activityBreakdown counts after load', async () => {
    // Integration guard: confirms summary.activityBreakdown reaches UsageCategoriesTile.
    // Previously the fixture used `breakdown` (wrong field) so the categories tile
    // silently rendered nothing — every category-related test was a no-op.
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Usage Categories')).toBeInTheDocument();
    });
    expect(screen.getByText('APIs')).toBeInTheDocument();
    expect(screen.getByText('App Scan + Re-evaluate')).toBeInTheDocument();
    expect(screen.getByText('746')).toBeInTheDocument();
  });

  it('should fetch all consumption data on mount', async () => {
    renderComponent();

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(6);
    });

    const urls = axiosMock.history.get.map((req) => req.url);
    expect(urls.some((url) => url.includes('/api/v2/consumption/summary'))).toBe(true);
    expect(urls.some((url) => url.includes('/api/v2/consumption/history/breakdown'))).toBe(true);
    expect(urls.some((url) => url.includes('/api/v2/consumption/history/by-source'))).toBe(true);
    expect(urls.some((url) => url.includes('/api/v2/consumption/history/by-stage'))).toBe(true);
    expect(urls.some((url) => url.includes('/api/v2/consumption/top-apps'))).toBe(true);
    expect(urls.some((url) => url.includes('/api/v2/consumption/daily-history'))).toBe(true);
  });

  it('should show export button', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Export Report')).toBeInTheDocument();
    });
  });

  it('should disable export button when not authorized', async () => {
    renderComponent({ isAuthorized: false });

    await waitFor(() => {
      const exportButton = screen.getByRole('button', { name: /export/i });
      expect(exportButton).toBeDisabled();
    });
  });

  it('should show error message when not authorized', async () => {
    renderComponent({ isAuthorized: false });

    await waitFor(() => {
      expect(screen.getByText(/do not have permission/i)).toBeInTheDocument();
    });
  });

  it('should show retry button when API fails', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(500, { message: 'Server Error' });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    });
  });

  // Tab navigation — Overview tab is default

  it('default tab is Overview: shows My Usage tile content', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('My usage')).toBeInTheDocument();
    });
  });

  it('default tab is Overview: does not show Consumption by Source donut content', async () => {
    renderComponent();

    // Wait for load to complete, then assert Trends content is absent
    await waitFor(() => {
      expect(screen.getByText('My usage')).toBeInTheDocument();
    });

    expect(screen.queryByText('Consumption by Source')).not.toBeInTheDocument();
  });

  it('clicking Trends tab shows Consumption by Source content', async () => {
    axiosMock
      .onGet(/\/api\/v2\/consumption\/history\/by-source/)
      .reply(200, [{ month: '2026-05-01', consumed: 100, breakdown: { CI_CD: 100 } }]);

    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('My usage')).toBeInTheDocument();
    });

    const trendsTab = screen.getByRole('tab', { name: /trends/i });
    await user.click(trendsTab);

    await waitFor(() => {
      expect(screen.getByText('Consumption by Source')).toBeInTheDocument();
    });
  });

  it('renders ConsumptionByStageChart in the 2-column chart row after switching to Trends', async () => {
    axiosMock
      .onGet(/\/api\/v2\/consumption\/history\/by-source/)
      .reply(200, [{ month: '2026-05-01', consumed: 100, breakdown: { CI_CD: 100 } }]);

    const user = userEvent.setup();
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('My usage')).toBeInTheDocument();
    });

    const trendsTab = screen.getByRole('tab', { name: /trends/i });
    await user.click(trendsTab);

    await waitFor(() => {
      expect(screen.getByText('Consumption by Stage')).toBeInTheDocument();
    });

    const sourceTitle = screen.getByText('Consumption by Source');
    const stageTitle = screen.getByText('Consumption by Stage');
    const sourceRow = sourceTitle.closest('.iq-usage-page__chart-row');
    const stageRow = stageTitle.closest('.iq-usage-page__chart-row');
    expect(sourceRow).toBeTruthy();
    expect(stageRow).toBe(sourceRow);
  });

  it('?tab=trends deep-link: shows Trends content on initial render via preloadedState', async () => {
    axiosMock
      .onGet(/\/api\/v2\/consumption\/history\/by-source/)
      .reply(200, [{ month: '2026-05-01', consumed: 100, breakdown: { CI_CD: 100 } }]);

    renderComponent(
      {},
      {
        ...defaultPreloadedState,
        usage: { ...defaultPreloadedState.usage, activeTab: 'trends' },
        router: { currentParams: { tab: 'trends' }, currentState: { name: 'usage' } },
      }
    );

    // Wait for initial load to complete (loading spinner clears), then assert Trends tab is active.
    // Use ARIA `aria-selected` rather than a CSS class — the class name varies across RSC versions.
    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /trends/i })).toHaveAttribute('aria-selected', 'true');
    });

    expect(screen.getByText('Consumption by Source')).toBeInTheDocument();
  });

  it('refresh button click dispatches refresh and eventually sets lastRefreshedAt', async () => {
    const user = userEvent.setup();
    const { store } = renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /refresh usage data/i })).toBeInTheDocument();
    });

    const refreshBtn = screen.getByRole('button', { name: /refresh usage data/i });
    await user.click(refreshBtn);

    await waitFor(() => {
      expect(store.getState().usage.lastRefreshedAt).not.toBeNull();
    });
  });

  it('60s ticker advances the "Last refreshed" relative time and cleans up on unmount', () => {
    // Preload lastRefreshedAt = now so the ticker effect fires and the subtitle
    // reads "a few seconds ago". After 65s of fake-timer advance, the subtitle
    // should update to "a minute ago".
    const now = Date.now();
    jest.useFakeTimers({ now });
    const stateWithTimestamp = {
      ...defaultPreloadedState,
      usage: { ...defaultPreloadedState.usage, lastRefreshedAt: now },
    };
    const { unmount } = renderComponent({}, stateWithTimestamp);

    expect(screen.getByText(/last refreshed/i).textContent).toMatch(/a few seconds ago/);

    // Jest's fake-timer advance also bumps Date.now(), so a single 65s advance
    // both fires the 60s setInterval callback (causing setTick → re-render)
    // AND moves the wall clock past moment.js's "a few seconds ago" cutoff
    // (~44s) into "a minute ago" (~89s). act() flushes the queued setState.
    act(() => {
      jest.advanceTimersByTime(65000);
    });

    expect(screen.getByText(/last refreshed/i).textContent).toMatch(/a minute ago/);

    unmount();
    // Advancing further after unmount must not throw — the interval should be
    // cleared by the effect cleanup.
    expect(() => jest.advanceTimersByTime(120000)).not.toThrow();

    jest.useRealTimers();
  });

  it('should show export error when download fails', async () => {
    const user = userEvent.setup();
    axiosMock.onGet(/\/api\/v2\/consumption\/export/).reply(500);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /export/i })).toBeInTheDocument();
    });

    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    await waitFor(() => {
      expect(screen.getByText(/error occurred while exporting/i)).toBeInTheDocument();
    });
  });

  // Tab ↔ URL sync regression tests (items 1, 2, 3).
  // These tests verify that clicking a tab updates the URL (slice→URL write-back),
  // that reloading on Trends restores the correct tab (URL→slice), and that
  // clicking Overview from a ?tab=trends URL updates the URL to ?tab=overview.
  // All three FAILED on 06307bc48a before the isUrlToSlicePending guard was removed.

  it('clicking Trends tab triggers stateGo with tab=trends (URL write-back)', async () => {
    const user = userEvent.setup();
    const { store } = renderComponent(
      {},
      {
        ...defaultPreloadedState,
        router: { currentParams: { tab: 'overview' }, currentState: { name: 'usage' } },
      }
    );

    await waitFor(() => {
      expect(screen.getByText('My usage')).toBeInTheDocument();
    });

    stateGoSpy.mockClear();

    const trendsTab = screen.getByRole('tab', { name: /trends/i });
    await user.click(trendsTab);

    await waitFor(() => {
      expect(store.getState().usage.activeTab).toBe('trends');
    });

    expect(stateGoSpy).toHaveBeenCalledWith('usage', { tab: 'trends' }, { location: 'replace' });
  });

  it('deep-link ?tab=trends → slice activeTab becomes trends (URL→slice)', () => {
    const { store } = renderComponent(
      {},
      {
        ...defaultPreloadedState,
        usage: { ...defaultPreloadedState.usage, activeTab: 'overview' },
        router: { currentParams: { tab: 'trends' }, currentState: { name: 'usage' } },
      }
    );

    // The URL→slice effect fires synchronously on mount when the URL tab
    // differs from the slice default. By the time render returns, the
    // dispatch has been enqueued — we need to wait for the state update.
    return waitFor(() => {
      expect(store.getState().usage.activeTab).toBe('trends');
    });
  });

  it('deep-link ?tab=trends survives URL→slice→URL cycle: Trends stays active, stateGo never called with tab=overview', async () => {
    // Regression guard for the mount-time race: on reload the slice starts with
    // activeTab='overview' (initial state). Without the urlSyncInProgressRef guard,
    // the slice→URL effect fires on that stale value before the URL→slice dispatch
    // reduces, calling stateGo('usage', { tab:'overview' }) and overwriting the URL.
    const { store } = renderComponent(
      {},
      {
        ...defaultPreloadedState,
        usage: { ...defaultPreloadedState.usage, activeTab: 'overview' }, // initial state before URL→slice
        router: { currentParams: { tab: 'trends' }, currentState: { name: 'usage' } },
      }
    );

    // Wait for URL→slice to reduce
    await waitFor(() => {
      expect(store.getState().usage.activeTab).toBe('trends');
    });

    // The slice→URL effect must NOT have fired with 'overview' — that would have
    // stomped the deep-link URL back to overview.
    const overwriteCalls = stateGoSpy.mock.calls.filter(([, params]) => params && params.tab === 'overview');
    expect(overwriteCalls).toHaveLength(0);
  });

  it('clicking Overview from ?tab=trends triggers stateGo with tab=overview', async () => {
    const user = userEvent.setup();
    const { store } = renderComponent(
      {},
      {
        ...defaultPreloadedState,
        usage: { ...defaultPreloadedState.usage, activeTab: 'trends' },
        router: { currentParams: { tab: 'trends' }, currentState: { name: 'usage' } },
      }
    );

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /overview/i })).toBeInTheDocument();
    });

    stateGoSpy.mockClear();

    const overviewTab = screen.getByRole('tab', { name: /overview/i });
    await user.click(overviewTab);

    await waitFor(() => {
      expect(store.getState().usage.activeTab).toBe('overview');
    });

    expect(stateGoSpy).toHaveBeenCalledWith('usage', { tab: 'overview' }, { location: 'replace' });
  });
});
