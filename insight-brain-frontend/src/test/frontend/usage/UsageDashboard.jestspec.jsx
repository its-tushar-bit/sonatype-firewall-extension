/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import UsageDashboard from 'MainRoot/usage/UsageDashboard';

describe('UsageDashboard', () => {
  let axiosMock;

  const summaryResponse = {
    consumed: 1478,
    limit: 50000,
    percentUsed: 3.0,
    remaining: 48522,
    resetDate: '2026-06-01',
    billingWindowStart: '2026-05-01',
    tier: 'ADVANCED',
    breakdown: {
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
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
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

  it('renders ConsumptionByStageChart in the 2-column chart row when data is present', async () => {
    axiosMock
      .onGet(/\/api\/v2\/consumption\/history\/by-source/)
      .reply(200, [{ month: '2026-05-01', consumed: 100, breakdown: { CI_CD: 100 } }]);
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText('Consumption by Stage')).toBeInTheDocument();
    });
    const sourceTitle = screen.getByText('Consumption by Source');
    const stageTitle = screen.getByText('Consumption by Stage');
    // Both tiles share the same .iq-usage-page__chart-row ancestor
    const sourceRow = sourceTitle.closest('.iq-usage-page__chart-row');
    const stageRow = stageTitle.closest('.iq-usage-page__chart-row');
    expect(sourceRow).toBeTruthy();
    expect(stageRow).toBe(sourceRow);
  });

  it('should display summary card after loading', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(screen.getByText(/1,478/)).toBeInTheDocument();
    expect(screen.getByText(/50,000/)).toBeInTheDocument();
  });

  it('should show export button', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Export')).toBeInTheDocument();
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

  it('should handle no-limit state', async () => {
    const noLimitSummary = {
      ...summaryResponse,
      limit: null,
      remaining: null,
      percentUsed: null,
    };

    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, noLimitSummary);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(screen.getByText(/1,478/)).toBeInTheDocument();
    expect(screen.queryByText('50,000')).not.toBeInTheDocument();
    expect(screen.queryByText(/remaining/i)).not.toBeInTheDocument();
  });

  it('has-limit but percentUsed null: renders em-dash in percentage label and zero-width progress fill', async () => {
    const nullPercentSummary = {
      ...summaryResponse,
      limit: 50000,
      percentUsed: null,
    };
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, nullPercentSummary);

    const { container } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    const percentage = container.querySelector('.iq-usage-card__progress-percentage');
    expect(percentage).toHaveTextContent('—');
    expect(percentage).not.toHaveTextContent('%');

    const fill = container.querySelector('.iq-usage-card__progress-fill');
    expect(fill.style.width).toBe('0%');

    expect(container.querySelector('.iq-usage-card__progress-container--over')).not.toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-container--warning')).not.toBeInTheDocument();
  });

  it('U-45: bar stays normal (indigo) below 100% regardless of warningThresholdPct', async () => {
    const customThresholdSummary = {
      ...summaryResponse,
      consumed: 30000,
      limit: 50000,
      warningThresholdPct: 60,
      percentUsed: 60.0,
      remaining: 20000,
    };
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, customThresholdSummary);

    const { container } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(container.querySelector('.iq-usage-card__progress-container--warning')).not.toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-container--normal')).toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-container--over')).not.toBeInTheDocument();
  });

  it('U-46: custom threshold 90% does not trigger warning state at 80%', async () => {
    const customThresholdSummary = {
      ...summaryResponse,
      consumed: 40000,
      limit: 50000,
      warningThresholdPct: 90,
      percentUsed: 80.0,
      remaining: 10000,
    };
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, customThresholdSummary);

    const { container } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(container.querySelector('.iq-usage-card__progress-container--warning')).not.toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-container--normal')).toBeInTheDocument();
  });

  it('null warningThresholdPct with limit: percentUsed 90 stays normal, 100 still goes to over', async () => {
    const noThresholdSummary = {
      ...summaryResponse,
      consumed: 45000,
      limit: 50000,
      warningThresholdPct: null,
      percentUsed: 90.0,
      remaining: 5000,
    };
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, noThresholdSummary);

    const { container, unmount } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(container.querySelector('.iq-usage-card__progress-container--warning')).not.toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-container--normal')).toBeInTheDocument();

    unmount();

    const overLimitNoThreshold = {
      ...summaryResponse,
      consumed: 55000,
      limit: 50000,
      warningThresholdPct: null,
      percentUsed: 110.0,
      remaining: -5000,
    };
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, overLimitNoThreshold);

    const { container: overContainer } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(overContainer.querySelector('.iq-usage-card__progress-container--over')).toBeInTheDocument();
  });

  it('over limit: renders two progress segments with proportional widths', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, {
      ...summaryResponse,
      consumed: 5000,
      limit: 1000,
      warningThresholdPct: 80,
      percentUsed: 500.0,
      remaining: -4000,
    });

    const { container } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    const within = container.querySelector('.iq-usage-card__progress-fill--within');
    const overage = container.querySelector('.iq-usage-card__progress-fill--overage');
    expect(within).toBeInTheDocument();
    expect(overage).toBeInTheDocument();
    expect(within).toHaveStyle({ width: '20%' });
    expect(overage).toHaveStyle({ width: '80%' });
  });

  it('exactly at limit (100%): renders single progress fill, no overage segment', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, {
      ...summaryResponse,
      consumed: 5000,
      limit: 5000,
      warningThresholdPct: 80,
      percentUsed: 100.0,
      remaining: 0,
    });

    const { container } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(container.querySelector('.iq-usage-card__progress-fill--within')).not.toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-fill--overage')).not.toBeInTheDocument();
    const singleFill = container.querySelector('.iq-usage-card__progress-bar > .iq-usage-card__progress-fill');
    expect(singleFill).toBeInTheDocument();
    expect(singleFill).toHaveStyle({ width: '100%' });
  });

  it('exactly at limit (100%): detail text reads "Limit reached" not "Over limit by 0"', async () => {
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, {
      ...summaryResponse,
      consumed: 5000,
      limit: 5000,
      warningThresholdPct: 80,
      percentUsed: 100.0,
      remaining: 0,
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(screen.getByText('Limit reached')).toBeInTheDocument();
    expect(screen.queryByText(/Over limit by/i)).not.toBeInTheDocument();
  });

  it('under limit: renders single progress fill only', async () => {
    const { container } = renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    expect(container.querySelector('.iq-usage-card__progress-fill--within')).not.toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-fill--overage')).not.toBeInTheDocument();
    expect(container.querySelector('.iq-usage-card__progress-fill')).toBeInTheDocument();
  });

  it('should show export error when download fails', async () => {
    const user = userEvent.setup();
    axiosMock.onGet(/\/api\/v2\/consumption\/export/).reply(500);

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('Evaluated Components')).toBeInTheDocument();
    });

    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    await waitFor(() => {
      expect(screen.getByText(/error occurred while exporting/i)).toBeInTheDocument();
    });
  });
});
