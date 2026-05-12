/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/dom';
import { axiosMockAdapter, render, userEvent } from 'TestRoot/SpecUtil';

import AnnouncementBanner from 'MainRoot/announcementBanner/AnnouncementBanner';
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
    productFeatures: {
      loading: false,
      loadError: null,
      productFeatures: { 'multi-tenant': true },
    },
    announcementBanner: {
      loading: false,
      loadError: null,
      banner,
      dismissedWindowId,
      suppressedByLogout,
    },
  };
}

function onPremState({ banner = null } = {}) {
  return {
    productFeatures: {
      loading: false,
      loadError: null,
      productFeatures: { 'single-tenant': true },
    },
    announcementBanner: {
      loading: false,
      loadError: null,
      banner,
      dismissedWindowId: null,
      suppressedByLogout: false,
    },
  };
}

describe('AnnouncementBanner', () => {
  let axiosMock;

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
    render(<AnnouncementBanner />, { preloadedState: multiTenantState({ banner: ACTIVE_BANNER }) });

    expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument();
  });

  it('renders nothing on on-prem even when a banner payload is present', () => {
    const { container } = render(<AnnouncementBanner />, { preloadedState: onPremState({ banner: ACTIVE_BANNER }) });

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when banner is disabled', () => {
    const { container } = render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, enabled: false } }),
    });

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when now is before displayFrom', () => {
    const { container } = render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({
        banner: {
          ...ACTIVE_BANNER,
          displayFrom: new Date(NOW + 10 * MINUTE).toISOString(),
        },
      }),
    });

    expect(container).toBeEmptyDOMElement();
  });

  it('auto-hides past displayUntil without any user action', () => {
    const { container } = render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({
        banner: {
          ...ACTIVE_BANNER,
          displayUntil: new Date(NOW - 1).toISOString(),
        },
      }),
    });

    expect(container).toBeEmptyDOMElement();
  });

  it('renders nothing when logout is in progress (suppressedByLogout)', () => {
    // Prevents the banner from flashing between the moment the user clicks logout and the navigation away.
    const { container } = render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: ACTIVE_BANNER, suppressedByLogout: true }),
    });

    expect(container).toBeEmptyDOMElement();
  });

  it('reappears in the DOM when logout fails (suppressedByLogout cleared via logout/rejected)', async () => {
    // Full cycle at component level: banner visible -> logout/pending hides it -> logout/rejected restores it.
    const { store } = render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: ACTIVE_BANNER }),
    });
    expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument();

    store.dispatch({ type: 'userSession/logout/pending' });
    await waitFor(() => expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument());

    store.dispatch({ type: 'userSession/logout/rejected' });
    await waitFor(() => expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument());
  });

  it('hides after the user clicks dismiss and writes the windowId to sessionStorage', async () => {
    const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });
    render(<AnnouncementBanner />, { preloadedState: multiTenantState({ banner: ACTIVE_BANNER }) });

    expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument();

    const closeButton = screen.getByRole('button', { name: /dismiss announcement banner/i });
    await user.click(closeButton);

    await waitFor(() => {
      expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument();
    });
    expect(sessionStorage.getItem(DISMISS_STORAGE_KEY)).toBe(ACTIVE_BANNER.windowId);
  });

  it('stays hidden when pre-existing sessionStorage dismissal matches current windowId', () => {
    sessionStorage.setItem(DISMISS_STORAGE_KEY, ACTIVE_BANNER.windowId);
    const { container } = render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: ACTIVE_BANNER, dismissedWindowId: ACTIVE_BANNER.windowId }),
    });

    expect(container).toBeEmptyDOMElement();
  });

  it('dispatches hydrateDismissed on mount so a prior-session dismissal in sessionStorage takes effect', async () => {
    // preloadedState carries NO dismissal; only the hydrateDismissed dispatch can move sessionStorage
    // into the store and hide the banner. If the dispatch is ever removed from the mount effect, the
    // banner would render before this assertion fires.
    sessionStorage.setItem(DISMISS_STORAGE_KEY, ACTIVE_BANNER.windowId);
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: ACTIVE_BANNER, dismissedWindowId: null }),
    });

    await waitFor(() => {
      expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument();
    });
  });

  it('re-appears when a new windowId is published after a prior dismissal', () => {
    sessionStorage.setItem(DISMISS_STORAGE_KEY, 'old-window');
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({
        banner: { ...ACTIVE_BANNER, windowId: 'new-window' },
        dismissedWindowId: 'old-window',
      }),
    });

    expect(screen.getByText(/Scheduled maintenance/)).toBeInTheDocument();
  });

  it('loads the banner on mount and re-polls at the refresh interval', async () => {
    const getSpy = jest.fn(() => [200, ACTIVE_BANNER]);
    axiosMock.reset();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(getSpy);

    render(<AnnouncementBanner />, { preloadedState: multiTenantState() });

    await waitFor(() => {
      expect(getSpy).toHaveBeenCalledTimes(1);
    });

    jest.advanceTimersByTime(REFRESH_INTERVAL_MS + 100);

    await waitFor(() => {
      expect(getSpy).toHaveBeenCalledTimes(2);
    });
  });

  it('dispatches a reload exactly at displayUntil so the selector re-evaluates when the window ends', async () => {
    const shortBanner = {
      ...ACTIVE_BANNER,
      displayFrom: new Date(NOW - 10 * 1000).toISOString(),
      displayUntil: new Date(NOW + 30 * SECOND).toISOString(),
    };
    const getSpy = jest.fn(() => [200, shortBanner]);
    axiosMock.reset();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(getSpy);

    render(<AnnouncementBanner />, { preloadedState: multiTenantState({ banner: shortBanner }) });
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(1));

    jest.advanceTimersByTime(29 * SECOND);
    expect(getSpy).toHaveBeenCalledTimes(1);

    jest.advanceTimersByTime(2 * SECOND);
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(2));
  });

  it('dispatches a reload exactly at displayFrom so the banner appears when the window opens', async () => {
    const futureBanner = {
      ...ACTIVE_BANNER,
      displayFrom: new Date(NOW + 30 * SECOND).toISOString(),
      displayUntil: new Date(NOW + 60 * SECOND).toISOString(),
    };
    const getSpy = jest.fn(() => [200, futureBanner]);
    axiosMock.reset();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(getSpy);

    render(<AnnouncementBanner />, { preloadedState: multiTenantState({ banner: futureBanner }) });
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(1));

    // Before displayFrom - still hidden
    expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument();

    jest.advanceTimersByTime(29 * SECOND);
    expect(getSpy).toHaveBeenCalledTimes(1);
    expect(screen.queryByText(/Scheduled maintenance/)).not.toBeInTheDocument();

    // At displayFrom - reload fires and banner appears
    jest.advanceTimersByTime(2 * SECOND);
    await waitFor(() => expect(getSpy).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByText(/Scheduled maintenance/)).toBeInTheDocument());
  });

  it('renders an info-severity banner with the iq-announcement-banner--info modifier by default', () => {
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: ACTIVE_BANNER }),
    });

    const banner = document.getElementById('iq-announcement-banner');
    expect(banner).not.toBeNull();
    expect(banner.className).toContain('iq-announcement-banner--info');
  });

  it('renders a warning-severity banner with the iq-announcement-banner--warning modifier', () => {
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'warning' } }),
    });

    const banner = document.getElementById('iq-announcement-banner');
    expect(banner).not.toBeNull();
    expect(banner.className).toContain('iq-announcement-banner--warning');
  });

  it('renders a critical-severity banner with the iq-announcement-banner--critical modifier', () => {
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'critical' } }),
    });

    const banner = document.getElementById('iq-announcement-banner');
    expect(banner).not.toBeNull();
    expect(banner.className).toContain('iq-announcement-banner--critical');
  });

  it('uses role="status" (implicit aria-live="polite") for info severity', () => {
    render(<AnnouncementBanner />, { preloadedState: multiTenantState({ banner: ACTIVE_BANNER }) });
    expect(document.getElementById('iq-announcement-banner').getAttribute('role')).toBe('status');
  });

  it('uses role="status" (implicit aria-live="polite") for warning severity', () => {
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'warning' } }),
    });
    expect(document.getElementById('iq-announcement-banner').getAttribute('role')).toBe('status');
  });

  it('uses role="alert" (implicit aria-live="assertive") for critical severity so screen readers interrupt', () => {
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'critical' } }),
    });
    expect(document.getElementById('iq-announcement-banner').getAttribute('role')).toBe('alert');
  });

  it('does not fetch the banner on on-prem (neither on mount nor on the polling interval)', async () => {
    const getSpy = jest.fn(() => [200, ACTIVE_BANNER]);
    axiosMock.reset();
    axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(getSpy);

    render(<AnnouncementBanner />, { preloadedState: onPremState() });
    jest.advanceTimersByTime(10 * MINUTE + 100);

    expect(getSpy).not.toHaveBeenCalled();
  });

  it('falls back to info styling for an unknown severity', () => {
    render(<AnnouncementBanner />, {
      preloadedState: multiTenantState({ banner: { ...ACTIVE_BANNER, severity: 'unknown' } }),
    });

    const banner = document.getElementById('iq-announcement-banner');
    expect(banner).not.toBeNull();
    expect(banner.className).toContain('iq-announcement-banner--info');
  });
});
