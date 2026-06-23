/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import UsagePeriodFilter from 'MainRoot/usage/UsagePeriodFilter';

// NxStatefulDropdown renders children only when open. RSC does NOT set role="menuitem" on
// nx-dropdown-button children — they remain role="button". The plan assumed "menuitem" but
// that is not how RSC works. Tests use role="button" after opening the dropdown.

const baseUsageState = {
  summary: null,
  summaryForPeriod: null,
  historyBreakdown: [],
  cumulativeHistoryBreakdown: [],
  chartAggregation: 'daily',
  cumulativeChartAggregation: 'daily',
  sourceBreakdown: [],
  stageBreakdown: [],
  topApps: null,
  dailyHistory: null,
  activeTab: 'overview',
  cumulativeFilter: 'thisMonth',
  lastRefreshedAt: null,
  loadingSummary: false,
  loadingSummaryForPeriod: false,
  loadingHistoryBreakdown: false,
  loadingSourceBreakdown: false,
  loadingStageBreakdown: false,
  loadingTopApps: false,
  loadingDailyHistory: false,
  loadingAll: false,
  loadErrorSummary: null,
  loadErrorSummaryForPeriod: null,
  loadErrorHistoryBreakdown: null,
  loadErrorSourceBreakdown: null,
  loadErrorStageBreakdown: null,
  loadErrorTopApps: null,
  loadErrorDailyHistory: null,
  loadErrorAll: null,
};

const preloadedState = (overrides = {}) => ({
  usage: {
    ...baseUsageState,
    periodPreset: 'currentBillingPeriod',
    periodRange: { startDate: null, endDate: null },
    ...overrides,
  },
});

// Helper: open the dropdown by clicking the toggle button
async function openDropdown(user) {
  const toggle = document.querySelector('.nx-dropdown__toggle');
  await user.click(toggle);
}

describe('UsagePeriodFilter', () => {
  it('renders a "Period:" label and a trigger button showing the active range by default', () => {
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    // Static label is outside the button
    expect(screen.getByText('Period:')).toBeInTheDocument();
    // Dropdown toggle shows only the range label, no "Period:" prefix
    expect(document.querySelector('.nx-dropdown__toggle')).toHaveTextContent('Current billing period');
    expect(document.querySelector('.nx-dropdown__toggle')).not.toHaveTextContent('Period:');
  });

  it('trigger label reflects the active range when preset is custom', () => {
    render(<UsagePeriodFilter />, {
      preloadedState: preloadedState({
        periodPreset: 'custom',
        periodRange: { startDate: '2026-06-01', endDate: '2026-06-30' },
      }),
    });
    expect(document.querySelector('.nx-dropdown__toggle')).toHaveTextContent('Jun 1 - Jun 30, 2026');
    expect(document.querySelector('.nx-dropdown__toggle')).not.toHaveTextContent('Period:');
  });

  it('opens a menu listing the 5 presets', async () => {
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);

    // "Current billing period" now appears in both the trigger button and the menu
    // item, so use getAllByText for it; the other labels are unique.
    expect(screen.getAllByText('Current billing period').length).toBeGreaterThanOrEqual(1);
    ['Last calendar month', 'Last 30 days', 'Last 90 days', 'Custom range…'].forEach((label) => {
      expect(screen.getByText(label)).toBeInTheDocument();
    });
  });

  it('selecting a non-custom preset dispatches setPeriodPreset and closes the menu', async () => {
    const user = userEvent.setup();
    const { store } = render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Last 30 days'));
    expect(store.getState().usage.periodPreset).toBe('last30Days');
    expect(store.getState().usage.periodRange.startDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(store.getState().usage.periodRange.endDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('selecting "Custom range…" swaps to date inputs + Apply (does NOT dispatch setPeriodRange yet)', async () => {
    const user = userEvent.setup();
    const { store } = render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    expect(screen.getByLabelText('Start date')).toBeInTheDocument();
    expect(screen.getByLabelText('End date')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Apply' })).toBeInTheDocument();
    // Not dispatched until Apply
    expect(store.getState().usage.periodPreset).toBe('currentBillingPeriod');
  });

  it('Apply is disabled until both dates are entered', async () => {
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    expect(screen.getByRole('button', { name: 'Apply' })).toBeDisabled();
    await user.type(screen.getByLabelText('Start date'), '2026-06-01');
    expect(screen.getByRole('button', { name: 'Apply' })).toBeDisabled();
    await user.type(screen.getByLabelText('End date'), '2026-06-30');
    expect(screen.getByRole('button', { name: 'Apply' })).toBeEnabled();
  });

  it('Apply on Custom range dispatches setPeriodRange with chosen dates and sets preset=custom', async () => {
    const user = userEvent.setup();
    const { store } = render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), '2026-06-01');
    await user.type(screen.getByLabelText('End date'), '2026-06-30');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(store.getState().usage.periodRange).toEqual({ startDate: '2026-06-01', endDate: '2026-06-30' });
    expect(store.getState().usage.periodPreset).toBe('custom');
  });

  it('Apply with start > end shows inline error and does NOT dispatch', async () => {
    // Regression guard: without client-side validation, the backend rejects
    // start > end with HTTP 400 — but the dropdown would still close and the
    // 400 would surface as a page-level error banner, disconnected from the
    // inputs that caused it.
    const user = userEvent.setup();
    const { store } = render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), '2026-06-30');
    await user.type(screen.getByLabelText('End date'), '2026-06-01');
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    expect(screen.getByRole('alert')).toHaveTextContent(/start date must be on or before end date/i);
    // No dispatch: slice state still on defaults.
    expect(store.getState().usage.periodPreset).toBe('currentBillingPeriod');
    expect(store.getState().usage.periodRange).toEqual({ startDate: null, endDate: null });
    // Dropdown stays open in custom mode so the user can fix the input.
    expect(screen.getByLabelText('Start date')).toBeInTheDocument();
  });

  it('inline error clears once the user fixes the date order', async () => {
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), '2026-06-30');
    await user.type(screen.getByLabelText('End date'), '2026-06-01');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByRole('alert')).toBeInTheDocument();

    // Edit end so end >= start → error should clear.
    await user.clear(screen.getByLabelText('End date'));
    await user.type(screen.getByLabelText('End date'), '2026-07-15');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('inline error reappears if the user edits into a still-invalid date order', async () => {
    // Regression guard: updateDraft used to clear the error unconditionally on
    // any keystroke. A user who corrected end='2026-06-01' to end='2026-06-15'
    // while start was still '2026-06-30' would see the error vanish even
    // though the input was still invalid — and Apply would silently 400.
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), '2026-06-30');
    await user.type(screen.getByLabelText('End date'), '2026-06-01');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByRole('alert')).toBeInTheDocument();

    // Edit end but still leave end < start. Error must persist.
    await user.clear(screen.getByLabelText('End date'));
    await user.type(screen.getByLabelText('End date'), '2026-06-15');
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
});

describe('UsagePeriodFilter 366-day cap', () => {
  // 2026-01-01 + 366 inclusive days = 2027-01-01 (exactly 366 days).
  // 2026-01-01 + 367 inclusive days = 2027-01-02.
  const START = '2026-01-01';
  const END_366 = '2027-01-01'; // exactly 366 days inclusive
  const END_365 = '2026-12-31'; // exactly 365 days inclusive
  const END_367 = '2027-01-02'; // 367 days inclusive — should be rejected

  it('typing a 367-day range surfaces an inline error', async () => {
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), START);
    await user.type(screen.getByLabelText('End date'), END_367);

    expect(screen.getByRole('alert')).toHaveTextContent(/date range cannot exceed 366 days/i);
  });

  it('Apply is a no-op (does not dispatch) when range exceeds 366 days', async () => {
    const user = userEvent.setup();
    const { store } = render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), START);
    await user.type(screen.getByLabelText('End date'), END_367);
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    expect(screen.getByRole('alert')).toHaveTextContent(/date range cannot exceed 366 days/i);
    expect(store.getState().usage.periodPreset).toBe('currentBillingPeriod');
    expect(store.getState().usage.periodRange).toEqual({ startDate: null, endDate: null });
    // Dropdown stays open so the user can fix the range.
    expect(screen.getByLabelText('Start date')).toBeInTheDocument();
  });

  it('366-day range applies successfully without error', async () => {
    const user = userEvent.setup();
    const { store } = render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), START);
    await user.type(screen.getByLabelText('End date'), END_366);
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(store.getState().usage.periodRange).toEqual({ startDate: START, endDate: END_366 });
  });

  it('365-day range applies successfully without error', async () => {
    const user = userEvent.setup();
    const { store } = render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), START);
    await user.type(screen.getByLabelText('End date'), END_365);
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(store.getState().usage.periodRange).toEqual({ startDate: START, endDate: END_365 });
  });

  it('error clears when user shortens range back to 366 days', async () => {
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), START);
    await user.type(screen.getByLabelText('End date'), END_367);
    expect(screen.getByRole('alert')).toBeInTheDocument();

    await user.clear(screen.getByLabelText('End date'));
    await user.type(screen.getByLabelText('End date'), END_366);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('UsagePeriodFilter network scope (re-scope guard)', () => {
  // These tests verify that the period filter dispatches loadSummaryForPeriod
  // (one /summary call) and NOT loadAllUsageData (six calls).
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.onGet(/\/api\/v2\/consumption\/summary/).reply(200, { consumed: 50, limit: 1000 });
  });

  it('selecting a preset fires exactly one /summary request', async () => {
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Last 30 days'));

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
    });
    const url = axiosMock.history.get[0].url;
    expect(url).toMatch(/\/api\/v2\/consumption\/summary/);
    expect(url).not.toMatch(/breakdown/);
    expect(url).not.toMatch(/by-source/);
    expect(url).not.toMatch(/by-stage/);
    expect(url).not.toMatch(/top-apps/);
    expect(url).not.toMatch(/daily-history/);
  });

  it('clicking Apply on custom range fires exactly one /summary request', async () => {
    const user = userEvent.setup();
    render(<UsagePeriodFilter />, { preloadedState: preloadedState() });
    await openDropdown(user);
    await user.click(screen.getByText('Custom range…'));
    await user.type(screen.getByLabelText('Start date'), '2026-06-01');
    await user.type(screen.getByLabelText('End date'), '2026-06-30');
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBe(1);
    });
    expect(axiosMock.history.get[0].url).toMatch(/\/api\/v2\/consumption\/summary/);
  });
});
