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

  function scheduleTimers() {
    clearTimers();
    const serverExpiry = readExpirationTimestamp();
    if (serverExpiry === undefined) return;

    const adjustedExpiry = serverExpiry + clockOffset;
    const now = Date.now();
    const timeUntilExpiry = adjustedExpiry - now;
    const timeUntilWarning = timeUntilExpiry - WARNING_BEFORE_MS;

    if (timeUntilExpiry <= 0) {
      options.onExpired();
      return;
    }

    if (timeUntilWarning > 0) {
      warningTimer = setTimeout(() => {
        options.onWarning();
      }, timeUntilWarning);
    } else {
      options.onWarning();
    }

    expiredTimer = setTimeout(() => {
      options.onExpired();
    }, timeUntilExpiry);
  }

  return {
    start: scheduleTimers,
    stop: clearTimers,
    refreshFromResponse(response: Response) {
      const dateHeader = response.headers.get('Date');
      if (dateHeader) {
        clockOffset = getServerClockOffset(dateHeader, Date.now());
      }
      scheduleTimers();
    },
  };
}
