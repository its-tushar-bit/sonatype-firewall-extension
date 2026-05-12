/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectAnnouncementBanner,
  selectAnnouncementBannerDismissedWindowId,
  selectAnnouncementBannerVisible,
} from 'MainRoot/announcementBanner/announcementBannerSelectors';

const NOW = Date.parse('2026-05-26T20:00:00Z');
const FROM = new Date(NOW - 60 * 60 * 1000).toISOString();
const UNTIL = new Date(NOW + 60 * 60 * 1000).toISOString();

function makeState({
  tenantMode = 'multi-tenant',
  banner = null,
  dismissedWindowId = null,
  suppressedByLogout = false,
} = {}) {
  return {
    productFeatures: {
      productFeatures: {
        [tenantMode === 'multi-tenant' ? 'multi-tenant' : 'single-tenant']: true,
      },
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

function activeBanner(overrides = {}) {
  return {
    enabled: true,
    windowId: 'w1',
    severity: 'info',
    message: 'Scheduled maintenance',
    displayFrom: FROM,
    displayUntil: UNTIL,
    ...overrides,
  };
}

describe('announcementBannerSelectors', () => {
  beforeEach(() => {
    jest.useFakeTimers().setSystemTime(new Date(NOW));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('passthrough selectors', () => {
    it('selectAnnouncementBanner returns the banner value', () => {
      const banner = activeBanner();
      expect(selectAnnouncementBanner(makeState({ banner }))).toEqual(banner);
    });

    it('selectAnnouncementBannerDismissedWindowId returns the stored dismissal id', () => {
      expect(
        selectAnnouncementBannerDismissedWindowId(makeState({ dismissedWindowId: 'w1' }))
      ).toBe('w1');
    });
  });

  describe('selectAnnouncementBannerVisible', () => {
    it('is false on on-prem (single-tenant)', () => {
      expect(
        selectAnnouncementBannerVisible(makeState({ tenantMode: 'single-tenant', banner: activeBanner() }))
      ).toBe(false);
    });

    it('is false when tenant mode has not resolved yet (product features still loading)', () => {
      // Neither multi-tenant nor single-tenant flag present — fail-closed default.
      const state = makeState({ banner: activeBanner() });
      state.productFeatures.productFeatures = {};
      expect(selectAnnouncementBannerVisible(state)).toBe(false);
    });

    it('is false when banner is disabled', () => {
      expect(
        selectAnnouncementBannerVisible(makeState({ banner: activeBanner({ enabled: false }) }))
      ).toBe(false);
    });

    it('is false when banner is null', () => {
      expect(selectAnnouncementBannerVisible(makeState({ banner: null }))).toBe(false);
    });

    it('is true when banner is enabled and now is within the window', () => {
      expect(selectAnnouncementBannerVisible(makeState({ banner: activeBanner() }))).toBe(true);
    });

    it('is false when now is before displayFrom', () => {
      const banner = activeBanner({
        displayFrom: new Date(NOW + 10 * 60 * 1000).toISOString(),
        displayUntil: new Date(NOW + 20 * 60 * 1000).toISOString(),
      });
      expect(selectAnnouncementBannerVisible(makeState({ banner }))).toBe(false);
    });

    it('is false when now is at or after displayUntil (auto-hide)', () => {
      const banner = activeBanner({
        displayFrom: new Date(NOW - 20 * 60 * 1000).toISOString(),
        displayUntil: new Date(NOW - 10 * 60 * 1000).toISOString(),
      });
      expect(selectAnnouncementBannerVisible(makeState({ banner }))).toBe(false);
    });

    it('is false when dismissedWindowId matches current banner.windowId', () => {
      expect(
        selectAnnouncementBannerVisible(makeState({ banner: activeBanner(), dismissedWindowId: 'w1' }))
      ).toBe(false);
    });

    it('is false when suppressedByLogout is set — even with an otherwise-visible banner', () => {
      expect(
        selectAnnouncementBannerVisible(makeState({ banner: activeBanner(), suppressedByLogout: true }))
      ).toBe(false);
    });

    it('is true when dismissedWindowId is stale (different from banner.windowId)', () => {
      expect(
        selectAnnouncementBannerVisible(
          makeState({ banner: activeBanner({ windowId: 'w2' }), dismissedWindowId: 'w1' })
        )
      ).toBe(true);
    });

    it('is false when timestamps are malformed', () => {
      const banner = activeBanner({ displayFrom: 'not-a-date', displayUntil: 'also-bad' });
      expect(selectAnnouncementBannerVisible(makeState({ banner }))).toBe(false);
    });

    it('accepts numeric epoch-second timestamps (Jackson OffsetDateTime default)', () => {
      const banner = activeBanner({
        displayFrom: (NOW - 60 * 60 * 1000) / 1000,
        displayUntil: (NOW + 60 * 60 * 1000) / 1000,
      });
      expect(selectAnnouncementBannerVisible(makeState({ banner }))).toBe(true);
    });

    it('honors displayUntil with numeric epoch-second timestamp (auto-hide)', () => {
      const banner = activeBanner({
        displayFrom: (NOW - 60 * 60 * 1000) / 1000,
        displayUntil: (NOW - 1) / 1000,
      });
      expect(selectAnnouncementBannerVisible(makeState({ banner }))).toBe(false);
    });

    it('defaults to always-on when displayFrom and displayUntil are missing and banner is enabled', () => {
      const banner = activeBanner({ displayFrom: null, displayUntil: null });
      expect(selectAnnouncementBannerVisible(makeState({ banner }))).toBe(true);
    });

    it('honors displayFrom alone (open-ended future: show from start time forever)', () => {
      const past = new Date(NOW - 60 * 60 * 1000).toISOString();
      const future = new Date(NOW + 60 * 60 * 1000).toISOString();
      // displayFrom in the past, no displayUntil — banner should be visible.
      expect(
        selectAnnouncementBannerVisible(makeState({ banner: activeBanner({ displayFrom: past, displayUntil: null }) }))
      ).toBe(true);
      // displayFrom in the future, no displayUntil — banner stays hidden until the start time.
      expect(
        selectAnnouncementBannerVisible(makeState({ banner: activeBanner({ displayFrom: future, displayUntil: null }) }))
      ).toBe(false);
    });

    it('honors displayUntil alone (show immediately until the deadline)', () => {
      const past = new Date(NOW - 60 * 60 * 1000).toISOString();
      const future = new Date(NOW + 60 * 60 * 1000).toISOString();
      // No displayFrom, displayUntil in the future — banner is visible.
      expect(
        selectAnnouncementBannerVisible(makeState({ banner: activeBanner({ displayFrom: null, displayUntil: future }) }))
      ).toBe(true);
      // No displayFrom, displayUntil in the past — banner is already expired.
      expect(
        selectAnnouncementBannerVisible(makeState({ banner: activeBanner({ displayFrom: null, displayUntil: past }) }))
      ).toBe(false);
    });
  });
});
