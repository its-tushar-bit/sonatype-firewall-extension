/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/dom';
import { axiosMockAdapter, render, userEvent } from 'TestRoot/SpecUtil';
import { MtiqAnnouncementBanner } from 'MainRoot/nosc/shell/notices/MtiqAnnouncementBanner';
// @ts-expect-error — legacy slice file is intentionally .js
import { DISMISS_STORAGE_KEY } from 'MainRoot/announcementBanner/announcementBannerSlice';
import { getAnnouncementBannerFetchUrl } from 'MainRoot/util/CLMLocation';

const SECOND = 1000;
const MINUTE = 60 * SECOND;
const REFRESH_INTERVAL_MS = 5 * MINUTE;

const NOW = Date.parse('2026-05-26T20:00:00Z');
const FROM = new Date(NOW - 60 * MINUTE).toISOString();
const UNTIL = new Date(NOW + 60 * MINUTE).toISOString();

const ACTIVE_BANNER = {
  enabled: true,
  windowId: 'w-2026-05-26-us',
  severity: 'info',
  message: 'Scheduled maintenance: May 26, 6-10 PM EDT.',
  displayFrom: FROM,
  displayUntil: UNTIL,
};

function multiTenantState({ banner = null, dismissedWindowId = null, suppressedByLogout = false } = {}) {
  return {
    productFeatures: { loading: false, loadError: null, productFeatures: { 'multi-tenant': true } },
    announcementBanner: { loading: false, loadError: null, banner, dismissedWindowId, suppressedByLogout },
  };
}

function onPremState({ banner = null } = {}) {
  return {
    productFeatures: { loading: false, loadError: null, productFeatures: { 'single-tenant': true } },
    announcementBanner: { loading: false, loadError: null, banner, dismissedWindowId: null, suppressedByLogout: false },
  };
}

describe('MtiqAnnouncementBanner', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.useFakeTimers({ doNotFake: ['performance'] }).setSystemTime(new Date(NOW));
    sessionStorage.clear();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(200, ACTIVE_BANNER);
  });

  afterEach(() => {
    jest.useRealTimers();
    axiosMock.reset();
  });

  it('renders the active banner text in MTIQ', () => {
    render(<MtiqAnnouncementBanner />, { preloadedState: multiTenantState({ banner: ACTIVE_BANNER }) });
    expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument();
  });

  it('renders nothing on on-prem and never polls the endpoint', () => {
    const getSpy = jest.fn(() => [200, ACTIVE_BANNER]);
    axiosMock.reset();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(getSpy);

    const { container } = render(<MtiqAnnouncementBanner />, { preloadedState: onPremState({ banner: ACTIVE_BANNER }) });
    jest.advanceTimersByTime(10 * MINUTE + 100);

    expect(container).toBeEmptyDOMElement();
    expect(getSpy).not.toHaveBeenCalled();
  });

  it('renders nothing before displayFrom and auto-appears at the boundary without reload', async () => {
    const futureBanner = {
      ...ACTIVE_BANNER,
      displayFrom: new Date(NOW + 30 * SECOND).toISOString(),
      displayUntil: new Date(NOW + 60 * SECOND).toISOString(),
    };
    const getSpy = jest.fn(() => [200, futureBanner]);
    axiosMock.reset();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(getSpy);

    render(<MtiqAnnouncementBanner />, { preloadedState: multiTenantState({ banner: futureBanner }) });
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(1));
    expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument();

    jest.advanceTimersByTime(31 * SECOND);
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument());
  });

  it('auto-hides past displayUntil without any user action', () => {
    const { container } = render(<MtiqAnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, displayUntil: new Date(NOW - 1).toISOString() } }),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('reappears when logout fails (suppressedByLogout cleared via logout/rejected)', async () => {
    // Full cycle at component level: banner visible -> logout/pending hides it -> logout/rejected restores it.
    const { store } = render(<MtiqAnnouncementBanner />, { preloadedState: multiTenantState({ banner: ACTIVE_BANNER }) });
    expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument();

    store.dispatch({ type: 'userSession/logout/pending' });
    await waitFor(() => expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument());

    store.dispatch({ type: 'userSession/logout/rejected' });
    await waitFor(() => expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument());
  });

  it('stays hidden when pre-existing sessionStorage dismissal matches current windowId', () => {
    sessionStorage.setItem(DISMISS_STORAGE_KEY, ACTIVE_BANNER.windowId);
    const { container } = render(<MtiqAnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: ACTIVE_BANNER, dismissedWindowId: ACTIVE_BANNER.windowId }),
    });
    expect(container).toBeEmptyDOMElement();
  });

  it('hides after dismiss and persists windowId to sessionStorage', async () => {
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    render(<MtiqAnnouncementBanner />, { preloadedState: multiTenantState({ banner: ACTIVE_BANNER }) });

    await user.click(screen.getByRole('button', { name: /dismiss announcement banner/i }));

    await waitFor(() => expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument());
    expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBe(ACTIVE_BANNER.windowId);
  });

  it('re-appears when a new windowId supersedes a prior dismissal', () => {
    sessionStorage.setItem(DISMISS_STORAGE_KEY, 'old-window');
    render(<MtiqAnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, windowId: 'new-window' }, dismissedWindowId: 'old-window' }),
    });
    expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument();
  });

  it('dispatches hydrateDismissed on mount so a cross-bundle (Classic) dismissal takes effect here', async () => {
    sessionStorage.setItem(DISMISS_STORAGE_KEY, ACTIVE_BANNER.windowId);
    render(<MtiqAnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: ACTIVE_BANNER, dismissedWindowId: null }),
    });
    await waitFor(() => expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument());
  });

  it('re-polls at the refresh interval', async () => {
    const getSpy = jest.fn(() => [200, ACTIVE_BANNER]);
    axiosMock.reset();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(getSpy);

    render(<MtiqAnnouncementBanner />, { preloadedState: multiTenantState() });
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(1));

    jest.advanceTimersByTime(REFRESH_INTERVAL_MS + 100);
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(2));
  });

  it('uses role="alert" for critical severity so screen readers interrupt', () => {
    render(<MtiqAnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'critical' } }),
    });
    expect(screen.getByTestId('nosc-mtiq-announcement-banner')).toHaveAttribute('role', 'alert');
  });

  it('uses role="status" for warning severity', () => {
    render(<MtiqAnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'warning' } }),
    });
    expect(screen.getByTestId('nosc-mtiq-announcement-banner')).toHaveAttribute('role', 'status');
  });

  it('falls back to info severity for an unknown severity value', () => {
    render(<MtiqAnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'unknown' } }),
    });
    expect(screen.getByTestId('nosc-mtiq-announcement-banner')).toHaveAttribute('role', 'status');
  });
});
