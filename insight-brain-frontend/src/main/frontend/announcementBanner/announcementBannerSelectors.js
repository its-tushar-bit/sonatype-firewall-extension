/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop } from 'ramda';

import { selectTenantMode } from 'MainRoot/productFeatures/productFeaturesSelectors';

export const selectAnnouncementBannerState = prop('announcementBanner');
export const selectAnnouncementBanner = createSelector(selectAnnouncementBannerState, (s) => s.banner);
export const selectAnnouncementBannerDismissedWindowId = createSelector(
  selectAnnouncementBannerState,
  (s) => s.dismissedWindowId
);

/**
 * Parse the backend's OffsetDateTime (Jackson emits a fractional epoch in seconds by default; ISO-8601
 * strings are also accepted). Returns ms or null.
 */
function parseInstantMillis(value) {
  if (value == null) return null;
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value * 1000 : null;
  }
  if (typeof value === 'string') {
    const parsed = Date.parse(value);
    return Number.isNaN(parsed) ? null : parsed;
  }
  return null;
}

/**
 * True when the banner should render now. Not memoised via createSelector because the result depends on
 * Date.now() in addition to state, so memoising on state reference alone would stick at a stale value the
 * moment the wall clock crosses a display boundary.
 */
export const selectAnnouncementBannerVisible = (state) => {
  if (selectTenantMode(state) !== 'multi-tenant') {
    return false;
  }
  const bannerState = selectAnnouncementBannerState(state);
  if (bannerState.suppressedByLogout) {
    return false;
  }
  const { banner, dismissedWindowId } = bannerState;
  if (!banner || !banner.enabled) {
    return false;
  }
  // Dismissal is keyed on windowId so a new window of the same banner re-appears even for users who
  // dismissed the previous one. A banner without a windowId is therefore effectively undismissable by
  // design — the operator is expected to always supply one (and the admin-endpoint validation enforces
  // this when enabled=true).
  if (dismissedWindowId && banner.windowId && dismissedWindowId === banner.windowId) {
    return false;
  }
  return isWithinDisplayWindow(banner, Date.now());
};

/**
 * True when `now` falls inside the banner's [displayFrom, displayUntil) window. Malformed timestamps fail
 * safe (return false) so a bad payload cannot inadvertently reveal the banner.
 */
function isWithinDisplayWindow(banner, now) {
  const bounds = [
    { value: banner.displayFrom, compare: (ms) => now >= ms },
    { value: banner.displayUntil, compare: (ms) => now < ms },
  ];
  for (const { value, compare } of bounds) {
    if (value == null) continue;
    const ms = parseInstantMillis(value);
    if (ms === null || !compare(ms)) return false;
  }
  return true;
}
