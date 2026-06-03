/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classnames from 'classnames';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faCircleExclamation, faExclamationTriangle, faInfoCircle, faTimes } from '@fortawesome/free-solid-svg-icons';

import {
  parseInstantMillis,
  selectAnnouncementBanner,
  selectAnnouncementBannerVisible,
} from 'MainRoot/announcementBanner/announcementBannerSelectors';
import { actions } from 'MainRoot/announcementBanner/announcementBannerSlice';
import { selectTenantMode } from 'MainRoot/productFeatures/productFeaturesSelectors';

const REFRESH_INTERVAL_MS = 5 * 60 * 1000;

// Cap on boundary-timer delays. Beyond a day the 5-minute poll will pick up the state change anyway.
const MAX_BOUNDARY_TIMER_MS = 24 * 60 * 60 * 1000;

const SEVERITY_ICON = {
  info: faInfoCircle,
  warning: faExclamationTriangle,
  critical: faCircleExclamation,
};

/**
 * Full-width announcement banner for IQ Cloud users. Renders nothing on on-prem, when disabled, outside the
 * configured time window, or when the user has dismissed the current windowId this session.
 */
export default function AnnouncementBanner() {
  const dispatch = useDispatch();
  const visible = useSelector(selectAnnouncementBannerVisible);
  const banner = useSelector(selectAnnouncementBanner);
  // On-prem never renders the banner (selectAnnouncementBannerVisible gates on multi-tenant), so skip the
  // initial load and the 5-minute poll there too — no reason to hammer the unused endpoint.
  const isMultiTenant = useSelector(selectTenantMode) === 'multi-tenant';

  useEffect(() => {
    if (!isMultiTenant) return undefined;
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
    const timers = [];
    const scheduleAt = (whenMs) => {
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

  const handleDismiss = () => {
    dispatch(actions.dismiss(banner.windowId));
  };

  const severity = SEVERITY_ICON[banner.severity] ? banner.severity : 'info';
  // Critical banners use role="alert" (implicit aria-live="assertive") so screen readers interrupt the
  // current announcement (imminent outage, service down, etc.); info/warning use role="status" (implicit
  // aria-live="polite") to avoid interrupting screen-reader flow.
  const alertRole = severity === 'critical' ? 'alert' : 'status';

  return (
    <div
      id="iq-announcement-banner"
      role={alertRole}
      className={classnames('nx-system-notice', 'iq-announcement-banner', `iq-announcement-banner--${severity}`)}
    >
      <NxFontAwesomeIcon className="iq-announcement-banner__icon" icon={SEVERITY_ICON[severity]} />
      <span className="iq-announcement-banner__message">{banner.message}</span>
      <button
        type="button"
        className="iq-announcement-banner__close"
        aria-label="Dismiss announcement banner"
        onClick={handleDismiss}
      >
        <NxFontAwesomeIcon icon={faTimes} />
      </button>
    </div>
  );
}
