/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import { NoticeStrip } from 'MainRoot/nosc/shell/NoticeStrip';
import { installRadixJsdomShims } from './radixJsdomShims';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import * as userSessionSelectors from 'MainRoot/user/userSessionSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { SystemNotice } from 'MainRoot/nosc/shell/notices/SystemNotice';
import { DefaultAdminPasswordNotice } from 'MainRoot/nosc/shell/notices/DefaultAdminPasswordNotice';
import { BaseUrlNotSetNotice } from 'MainRoot/nosc/shell/notices/BaseUrlNotSetNotice';
import { MtiqAnnouncementBanner } from 'MainRoot/nosc/shell/notices/MtiqAnnouncementBanner';
import { getSystemNoticeFetchUrl, getAnnouncementBannerFetchUrl } from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';

describe('NoticeStrip', () => {
  let axiosMock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    installRadixJsdomShims();
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  afterAll(() => {
    axiosMock.restore();
  });

  it('renders children inside a region landmark with aria-label="System notices" when showLandmark is true', () => {
    render(
      <NoticeStrip showLandmark={true}>
        <div>Test notice</div>
      </NoticeStrip>,
    );
    const region = screen.getByRole('region', { name: 'System notices' });
    expect(region).toBeInTheDocument();
    expect(screen.getByText('Test notice')).toBeInTheDocument();
  });

  it('does not render region landmark when showLandmark is false (globally suppressed)', () => {
    render(
      <NoticeStrip showLandmark={false}>
        <div>Test notice</div>
      </NoticeStrip>,
    );
    expect(screen.queryByRole('region', { name: 'System notices' })).not.toBeInTheDocument();
    // Children still render (the strip doesn't suppress them), just no ARIA landmark
    expect(screen.getByText('Test notice')).toBeInTheDocument();
  });

  it('defaults showLandmark to true (backward-compatible, renders landmark for existing callers)', () => {
    render(
      <NoticeStrip>
        <div>Test notice</div>
      </NoticeStrip>,
    );
    expect(screen.getByRole('region', { name: 'System notices' })).toBeInTheDocument();
  });

  it('calls ResizeObserver.disconnect() on unmount', () => {
    const disconnectSpy = jest.fn();
    const observeSpy = jest.fn();

    class MockResizeObserver {
      observe = observeSpy;
      unobserve = jest.fn();
      disconnect = disconnectSpy;
    }

    const originalResizeObserver = global.ResizeObserver;
    global.ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;

    const { unmount } = render(
      <NoticeStrip>
        <div>Test notice</div>
      </NoticeStrip>,
    );

    expect(observeSpy).toHaveBeenCalled();
    unmount();
    expect(disconnectSpy).toHaveBeenCalled();

    global.ResizeObserver = originalResizeObserver;
  });

  it('publishes height 0 on unmount', () => {
    const { unmount } = render(
      <NoticeStrip>
        <div>Test notice</div>
      </NoticeStrip>,
    );

    unmount();
    // After unmount, the CSS var should be set to 0
    expect(document.documentElement.style.getPropertyValue('--nosc-notice-strip-height')).toBe('0');
  });

  describe('stacked notices (real components)', () => {
    const NOW = Date.parse('2026-05-26T20:00:00Z');
    const MINUTE = 60 * 1000;
    const ACTIVE_BANNER = {
      enabled: true,
      windowId: 'w-2026-05-26-us',
      severity: 'info',
      message: 'Scheduled maintenance: May 26, 6-10 PM EDT.',
      displayFrom: new Date(NOW - 60 * MINUTE).toISOString(),
      displayUntil: new Date(NOW + 60 * MINUTE).toISOString(),
    };

    beforeEach(() => {
      jest.useFakeTimers({ doNotFake: ['performance'] }).setSystemTime(new Date(NOW));
      sessionStorage.clear();

      axiosMock.onGet(getSystemNoticeFetchUrl()).reply(200, { enabled: true, message: 'System maintenance scheduled' });
      axiosMock.onGet(getAnnouncementBannerFetchUrl()).reply(200, ACTIVE_BANNER);
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('renders all four notices in correct order with real components', async () => {
      jest.spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice').mockReturnValue(true);
      jest.spyOn(userSessionSelectors, 'selectCurrentUser').mockReturnValue({ authenticated: true });
      jest.spyOn(productFeaturesSelectors, 'selectIsBaseUrlConfigurationEnabled').mockReturnValue(true);

      const preloadedState = {
        systemNoticeConfiguration: {
          serverData: { enabled: true, message: 'System maintenance scheduled' },
        },
        userSession: {
          data: { username: 'admin' },
          shouldDisplayPasswordWarning: true,
          loading: false,
          error: null,
        },
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: { 'multi-tenant': true },
        },
        announcementBanner: {
          loading: false,
          loadError: null,
          banner: ACTIVE_BANNER,
          dismissedWindowId: null,
          suppressedByLogout: false,
        },
      };

      render(
        <NoticeStrip showLandmark={true}>
          <SystemNotice />
          <DefaultAdminPasswordNotice />
          <BaseUrlNotSetNotice />
          <MtiqAnnouncementBanner />
        </NoticeStrip>,
        { preloadedState },
      );

      // Wait for all notices to appear
      await waitFor(() => {
        expect(screen.getByTestId('nosc-system-notice')).toBeInTheDocument();
      });
      await waitFor(() => {
        expect(screen.getByTestId('nosc-default-admin-password-notice')).toBeInTheDocument();
      });
      await waitFor(() => {
        expect(screen.getByTestId('nosc-base-url-not-set-notice')).toBeInTheDocument();
      });
      await waitFor(() => {
        expect(screen.getByTestId('nosc-mtiq-announcement-banner')).toBeInTheDocument();
      });

      const region = screen.getByRole('region', { name: 'System notices' });
      expect(region).toBeInTheDocument();

      // Verify all four notice testIds are present
      const systemNotice = screen.getByTestId('nosc-system-notice');
      const passwordNotice = screen.getByTestId('nosc-default-admin-password-notice');
      const baseUrlNotice = screen.getByTestId('nosc-base-url-not-set-notice');
      const mtiqBanner = screen.getByTestId('nosc-mtiq-announcement-banner');

      // Verify DOM order
      const allStatuses = screen.getAllByRole('status');
      expect(allStatuses).toHaveLength(4);
      expect(allStatuses[0]).toBe(systemNotice);
      expect(allStatuses[1]).toBe(passwordNotice);
      expect(allStatuses[2]).toBe(baseUrlNotice);
      expect(allStatuses[3]).toBe(mtiqBanner);

      // All should be within the region
      expect(region).toContainElement(systemNotice);
      expect(region).toContainElement(mtiqBanner);

      // Verify content
      expect(systemNotice).toHaveTextContent('System maintenance scheduled');
      expect(passwordNotice).toHaveTextContent('Change Administrator Password');
      expect(baseUrlNotice).toHaveTextContent('The Base URL is not configured.');
      expect(mtiqBanner).toHaveTextContent('Scheduled maintenance');

      // Verify non-dismissible notices have no dismiss button
      expect(systemNotice.querySelector('button')).toBeNull();
      expect(passwordNotice.querySelector('button')).toBeNull();
      expect(baseUrlNotice.querySelector('button')).toBeNull();

      // Verify MTIQ banner has a dismiss button
      expect(mtiqBanner.querySelector('button')).toBeInTheDocument();
    });
  });
});
