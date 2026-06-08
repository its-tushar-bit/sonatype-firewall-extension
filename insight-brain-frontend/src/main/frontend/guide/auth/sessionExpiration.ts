/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getCookieValue } from './cookieUtils';

const COOKIE_NAME = 'IQ-SESSION-EXPIRATION-TIMESTAMP';
const WARNING_BEFORE_MS = 2 * 60 * 1000;

export function readExpirationTimestamp(): number | undefined {
  const raw = getCookieValue(COOKIE_NAME);
  if (raw === undefined) return undefined;
  const value = parseInt(raw, 10);
  return isNaN(value) ? undefined : value;
}

export function getServerClockOffset(dateHeader: string, clientNow: number): number {
  const serverTime = new Date(dateHeader).getTime();
  if (isNaN(serverTime)) return 0;
  return clientNow - serverTime;
}

interface TrackerOptions {
  onWarning: () => void;
  onExpired: () => void;
}

interface SessionExpirationTracker {
  start: () => void;
  stop: () => void;
  refreshFromResponse: (response: Response) => void;
}

// Module-level handle to the currently-running tracker. AuthProvider creates
// exactly one tracker for the user's session and calls start()/stop() on it,
// so any module that fields a backend response (apiFetch, loginApi, etc.) can
// notify the tracker without threading it through React context.
let activeTracker: SessionExpirationTracker | null = null;

/**
 * Notifies the active session-expiration tracker (if any) of a backend
 * response so it can re-read IQ-SESSION-EXPIRATION-TIMESTAMP and reschedule
 * its warning/expiry timers. This makes any authenticated request count as
 * activity, mirroring the legacy IQ behaviour where the warning only fires
 * after the user has actually been idle.
 *
 * Mirrors authFetch's existing rule: only successful responses (status < 400)
 * count as activity. A 401 means Shiro has already invalidated the session,
 * so extending the timers off an error response would be wrong.
 */
export function notifySessionResponse(response: Response): void {
  if (response.status >= 400) return;
  activeTracker?.refreshFromResponse(response);
}

export function createSessionExpirationTracker(
  options: TrackerOptions
): SessionExpirationTracker {
  let warningTimer: ReturnType<typeof setTimeout> | null = null;
  let expiredTimer: ReturnType<typeof setTimeout> | null = null;
  let clockOffset = 0;

  function clearTimers() {
    if (warningTimer !== null) {
      clearTimeout(warningTimer);
      warningTimer = null;
    }
    if (expiredTimer !== null) {
      clearTimeout(expiredTimer);
      expiredTimer = null;
    }
  }

  function getAdjustedExpiry(): number | undefined {
    const raw = readExpirationTimestamp();
    return raw === undefined ? undefined : raw + clockOffset;
  }

  function scheduleTimers() {
    clearTimers();
    const adjustedExpiry = getAdjustedExpiry();
    if (adjustedExpiry === undefined) return;

    const now = Date.now();
    const timeUntilExpiry = adjustedExpiry - now;
    const timeUntilWarning = timeUntilExpiry - WARNING_BEFORE_MS;

    if (timeUntilExpiry <= 0) {
      options.onExpired();
      return;
    }

    if (timeUntilWarning > 0) {
      warningTimer = setTimeout(handleWarningTick, timeUntilWarning);
    } else {
      // Already inside the warning window. Fire immediately; the expiry timer
      // below still drives the eventual logout.
      options.onWarning();
    }

    expiredTimer = setTimeout(handleExpiredTick, timeUntilExpiry);
  }

  // Re-read the cookie when each timer fires. If the cookie has been pushed
  // forward by activity since the timer was scheduled, we silently reschedule
  // instead of showing the warning / logging out — the source of truth is the
  // server-issued cookie, not the original schedule.
  function handleWarningTick() {
    const adjustedExpiry = getAdjustedExpiry();
    if (adjustedExpiry !== undefined && adjustedExpiry - Date.now() > WARNING_BEFORE_MS) {
      scheduleTimers();
      return;
    }
    options.onWarning();
  }

  function handleExpiredTick() {
    const adjustedExpiry = getAdjustedExpiry();
    if (adjustedExpiry !== undefined && adjustedExpiry > Date.now()) {
      scheduleTimers();
      return;
    }
    options.onExpired();
  }

  const tracker: SessionExpirationTracker = {
    start() {
      activeTracker = tracker;
      scheduleTimers();
    },
    stop() {
      clearTimers();
      if (activeTracker === tracker) {
        activeTracker = null;
      }
    },
    refreshFromResponse(response: Response) {
      const dateHeader = response.headers.get('Date');
      if (dateHeader) {
        clockOffset = getServerClockOffset(dateHeader, Date.now());
      }
      scheduleTimers();
    },
  };

  return tracker;
}
