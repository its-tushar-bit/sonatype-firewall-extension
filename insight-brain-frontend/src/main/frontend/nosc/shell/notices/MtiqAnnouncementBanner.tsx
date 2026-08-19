/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
// @ts-expect-error — legacy selectors file is intentionally .js
import {
  parseInstantMillis,
  selectAnnouncementBanner,
  selectAnnouncementBannerVisible,
} from 'MainRoot/announcementBanner/announcementBannerSelectors';
// @ts-expect-error — legacy slice file is intentionally .js
import { actions } from 'MainRoot/announcementBanner/announcementBannerSlice';
import { selectTenantMode } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { NoticeBanner } from './NoticeBanner';

const REFRESH_INTERVAL_MS = 5 * 60 * 1000;

// Cap on boundary-timer delays. Beyond a day the 5-minute poll will pick up the state change anyway.
const MAX_BOUNDARY_TIMER_MS = 24 * 60 * 60 * 1000;

type AnnouncementSeverity = 'info' | 'warning' | 'critical';
const KNOWN_SEVERITIES: readonly AnnouncementSeverity[] = ['info', 'warning', 'critical'];

function isAnnouncementSeverity(value: string): value is AnnouncementSeverity {
  return (KNOWN_SEVERITIES as readonly string[]).includes(value);
}

/**
 * Nexus One port of Classic's `AnnouncementBanner`. MTIQ-only — renders
 * nothing on-prem and does not poll the endpoint there. Every gate below is
 * ported unchanged from Classic; see `announcementBannerSlice.js` /
 * `announcementBannerSelectors.js` (untouched by this port) for the source
 * of truth on the multi-tenant gate, the half-open display window, dismissal,
 * and logout suppression.
 */
export function MtiqAnnouncementBanner(): JSX.Element | null {
  const dispatch = useDispatch();
  const visible = useSelector(selectAnnouncementBannerVisible);
  const banner = useSelector(selectAnnouncementBanner);
  // On-prem never renders the banner (selectAnnouncementBannerVisible gates on multi-tenant), so skip the
  // initial load and the 5-minute poll there too — no reason to hammer the unused endpoint.
  const isMultiTenant = useSelector(selectTenantMode) === 'multi-tenant';

  useEffect(() => {
    if (!isMultiTenant) return undefined;
    // Preview's Redux store is separate from Classic's even though sessionStorage is shared — without this,
    // a banner dismissed in Classic would reappear here.
    dispatch(actions.hydrateDismissed());
    dispatch(actions.loadAnnouncementBanner());

    const intervalId = setInterval(() => {
      dispatch(actions.loadAnnouncementBanner());
    }, REFRESH_INTERVAL_MS);

    return () => clearInterval(intervalId);
  }, [dispatch, isMultiTenant]);

  // Dispatch a reload at each time-window boundary so the visibility selector re-evaluates exactly when the
  // banner should appear or disappear, not whenever another state change happens to trigger a render.
  const displayFrom = banner ? banner.displayFrom : null;
  const displayUntil = banner ? banner.displayUntil : null;
  useEffect(() => {
    if (!isMultiTenant) return undefined;
    const timers: ReturnType<typeof setTimeout>[] = [];
    const scheduleAt = (whenMs: number | null) => {
      if (whenMs == null) return;
      const delay = whenMs - Date.now();
      if (delay > 0 && delay <= MAX_BOUNDARY_TIMER_MS) {
        timers.push(setTimeout(() => dispatch(actions.loadAnnouncementBanner()), delay));
      }
    };
    scheduleAt(parseInstantMillis(displayFrom));
    scheduleAt(parseInstantMillis(displayUntil));
    return () => timers.forEach(clearTimeout);
  }, [displayFrom, displayUntil, dispatch, isMultiTenant]);

  if (!visible || !banner) {
    return null;
  }

  const handleDismiss = (): void => {
    dispatch(actions.dismiss(banner.windowId));
  };

  const severity: AnnouncementSeverity = isAnnouncementSeverity(banner.severity) ? banner.severity : 'info';
  // Design uses one muted-orange color for every notice — severity no longer drives color. It still drives
  // the icon glyph and, for critical, the interrupting role="alert" so screen readers announce
  // imminent-outage-class messages without waiting for the next polite update.
  const icon = severity === 'info' ? 'info' : 'alert-triangle';

  return (
    <NoticeBanner
      icon={icon}
      assertive={severity === 'critical'}
      onDismiss={handleDismiss}
      dismissLabel="Dismiss announcement banner"
      testId="nosc-mtiq-announcement-banner"
    >
      {banner.message ?? ''}
    </NoticeBanner>
  );
}
