/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { act, axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import ComponentsConsumedSidebarTile from 'MainRoot/usage/ComponentsConsumedSidebarTile';
import { getConsumptionSummaryUrl } from 'MainRoot/util/CLMLocation';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

describe('ComponentsConsumedSidebarTile', () => {
  let axiosMock;
  let stateGoSpy;
  let usageDashboardEnabledSpy;

  function makeState(overrides = {}) {
    return {
      usage: {
        summary: null,
        loadingSummary: false,
        loadErrorSummary: null,
        loadErrorSummaryStatus: null,
        ...(overrides.usage || {}),
      },
    };
  }

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo').mockImplementation(() => () => {});
    usageDashboardEnabledSpy = jest
      .spyOn(ProductFeaturesSelectors, 'selectIsUsageDashboardEnabled')
      .mockReturnValue(true);
    axiosMock.onGet(getConsumptionSummaryUrl()).reply(200, {
      consumed: 650000,
      limit: 1000000,
      percentUsed: 65,
      remaining: 350000,
    });
  });

  afterEach(() => {
    axiosMock.reset();
    stateGoSpy.mockRestore();
    usageDashboardEnabledSpy.mockRestore();
  });

  it('renders nothing when usage dashboard is disabled', () => {
    usageDashboardEnabledSpy.mockReturnValue(false);
    const { container } = render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState(),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders skeleton during initial load', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({ usage: { loadingSummary: true } }),
    });
    expect(screen.getByTestId('iq-components-consumed-tile__skeleton')).toBeInTheDocument();
  });

  it('renders an inline error placeholder when fetch fails with no cached summary (transient 5xx)', () => {
    // Regression guard: previously the tile silently disappeared on a transient
    // 5xx (returns null when error truthy AND no summary). The useEffect's
    // !error guard then prevented auto-retry, so the user had to full-page
    // reload to recover. Now we render a status placeholder so the layout slot
    // stays reserved and the failure is visible. Status 500 is intentional —
    // 401/403/404 take the silent-hide path tested below.
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({ usage: { loadErrorSummary: 'boom', loadErrorSummaryStatus: 500 } }),
    });
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/Couldn’t load data/)).toBeInTheDocument();
  });

  it.each([401, 403, 404])(
    'hides the tile silently (no placeholder) on HTTP %i — user cannot see consumption data',
    (status) => {
      // Bug guard for higher-env regression: when consumption-reporting is
      // disabled at the system-config layer (403), the user lacks
      // CONFIGURE_SYSTEM / VIEW_USAGE permission (403), the session expired
      // (401), or the endpoint is not deployed in this variant (404), the
      // BE returns an auth-class status. The sidebar tile and its inline
      // error placeholder must both be suppressed entirely — showing
      // "Components / Couldn't load data" to users who can never see
      // consumption data is misleading and was reported as a higher-env bug.
      const { container } = render(<ComponentsConsumedSidebarTile />, {
        preloadedState: makeState({
          usage: { loadErrorSummary: 'forbidden', loadErrorSummaryStatus: status },
        }),
      });
      expect(container).toBeEmptyDOMElement();
    }
  );

  it('silent-hide takes priority over cached summary data when a fresh 403 arrives', () => {
    // Order-of-guards contract: in the JSX, the `isSilentHideStatus` early-return
    // sits BEFORE the cached-data render path. So a user who was a permitted
    // viewer (consumed=5 cached in Redux), then loses access mid-session
    // (BE flips feature flag, or admin demotes role) and the next loadSummary
    // returns 403, sees the tile disappear — not stale data. This is the
    // intentional read of the "if no widget, no placeholder" rule: also
    // means no stale-but-cached widget for users who can no longer see it.
    const { container } = render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: {
          summary: { consumed: 5, limit: 100 },
          loadErrorSummary: 'forbidden',
          loadErrorSummaryStatus: 403,
        },
      }),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('renders cached summary data when a transient 5xx arrives (stale data beats broken UI)', () => {
    // Complementary contract: the `if (error && !summary)` placeholder guard
    // means a cached `summary` survives a transient 5xx. The tile keeps
    // rendering live (if stale) data instead of dropping to the placeholder.
    // A full-page reload (or successful retry) recovers fresh data; in the
    // meantime the user sees what they had a moment ago.
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: {
          summary: { consumed: 5, limit: 100 },
          loadErrorSummary: 'boom',
          loadErrorSummaryStatus: 500,
        },
      }),
    });
    // Cached values render (compact format: 5 → "5", 100 → "100")
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    // The placeholder is NOT rendered (no role="status")
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('does NOT auto-retry loadSummary after a 403 rejection (no BE-hammering loop)', async () => {
    // The useEffect's `!error` guard suppresses the retry once any error is
    // in state; this asserts the contract explicitly so a future refactor
    // that drops the guard would still keep auth-class failures from
    // re-firing on every render. act() flushes pending effects/microtasks
    // deterministically — a timer-based wait is non-deterministic per the
    // project's flakiness-class rule in CLAUDE.md §6.
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { loadErrorSummary: 'forbidden', loadErrorSummaryStatus: 403 },
      }),
    });
    await act(async () => {});
    const consumptionSummaryCalls = axiosMock.history.get.filter((req) => req.url === getConsumptionSummaryUrl());
    expect(consumptionSummaryCalls).toHaveLength(0);
  });

  it('does NOT render the error placeholder when collapsed (tile is hidden anyway)', () => {
    const { container } = render(<ComponentsConsumedSidebarTile collapsed />, {
      preloadedState: makeState({ usage: { loadErrorSummary: 'boom' } }),
    });
    // Collapsed state takes precedence over the error placeholder.
    expect(container).toBeEmptyDOMElement();
  });

  it('does NOT fire loadSummary when sidebar is collapsed (saves a wasted round-trip)', async () => {
    // Regression guard: previously the effect would still run for a collapsed
    // tile that won't render the data, wasting an HTTP request if the parent
    // kept this component mounted with collapsed={true} rather than unmounting.
    render(<ComponentsConsumedSidebarTile collapsed />, {
      preloadedState: makeState(),
    });
    // act() flushes pending effects/microtasks deterministically. A timer
    // wait would be non-deterministic flakiness per CLAUDE.md §6.
    await act(async () => {});
    const consumptionSummaryCalls = axiosMock.history.get.filter((req) => req.url === getConsumptionSummaryUrl());
    expect(consumptionSummaryCalls).toHaveLength(0);
  });

  it('renders consumed only (no progress bar) when limit is null', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 650000, limit: null } },
      }),
    });
    expect(screen.getByText(/650k/)).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('renders progress bar with compact consumed/limit text', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    // Compact format: 1.6k / 1k (rounded to nearest hundred for the k-band)
    const fill = screen.getByTestId('iq-components-consumed-tile__bar-fill');
    expect(fill).toHaveStyle({ width: '100%' });
    expect(fill.className).toMatch(/--over/);
  });

  it('compact format applies in expanded state (1.6k / 1k)', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    expect(screen.getByText(/1\.6k/)).toBeInTheDocument();
    expect(screen.getByText(/1k/)).toBeInTheDocument();
  });

  it('renders null when sidebar is collapsed', () => {
    const { container } = render(<ComponentsConsumedSidebarTile collapsed={true} />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('still renders the expanded tile when collapsed=false', () => {
    render(<ComponentsConsumedSidebarTile collapsed={false} />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1635, limit: 1000 } },
      }),
    });
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('saturates with over-limit modifier when consumed > limit', () => {
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1500000, limit: 1000000 } },
      }),
    });
    const fill = screen.getByTestId('iq-components-consumed-tile__bar-fill');
    expect(fill).toHaveStyle({ width: '100%' });
    expect(fill.className).toMatch(/iq-components-consumed-tile__bar-fill--over/);
  });

  it('navigates to usage page on click', async () => {
    const user = userEvent.setup();
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: { summary: { consumed: 1, limit: 100 } },
      }),
    });
    await user.click(screen.getByRole('button', { name: /components/i }));
    expect(stateGoSpy).toHaveBeenCalledWith('usage');
  });

  it('loadSummary request has no startDate/endDate even when a custom period is active in state (regression: sidebar shows billing-window data only)', async () => {
    // Regression guard: the sidebar tile always shows billing-window consumed/limit.
    // loadSummary must never read periodRange — otherwise opening the sidebar while
    // "Last 30 days" is active would write 30-day counts into state.summary,
    // corrupting the My Usage tile's progress bar.
    render(<ComponentsConsumedSidebarTile />, {
      preloadedState: makeState({
        usage: {
          periodPreset: 'last30Days',
          periodRange: { startDate: '2026-05-23', endDate: '2026-06-22' },
        },
      }),
    });

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThan(0);
    });

    const summaryRequests = axiosMock.history.get.filter((req) => req.url.includes('/api/v2/consumption/summary'));
    expect(summaryRequests.length).toBeGreaterThan(0);
    summaryRequests.forEach((req) => {
      expect(req.url).not.toMatch(/startDate/);
      expect(req.url).not.toMatch(/endDate/);
    });
  });
});
