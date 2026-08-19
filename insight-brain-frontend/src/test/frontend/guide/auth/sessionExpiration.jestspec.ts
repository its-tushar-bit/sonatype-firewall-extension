/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  readExpirationTimestamp,
  getServerClockOffset,
  createSessionExpirationTracker,
  notifySessionResponse,
} from 'GuideRoot/auth/sessionExpiration';

function setExpirationCookie(timestamp: number | null): void {
  const value = timestamp === null
    ? 'OTHER=val'
    : `IQ-SESSION-EXPIRATION-TIMESTAMP=${timestamp}`;
  Object.defineProperty(document, 'cookie', { value, writable: true });
}

function makeResponse(status: number, dateMs?: number): Response {
  const headers = {
    get: (name: string) =>
      name === 'Date' && dateMs !== undefined ? new Date(dateMs).toUTCString() : null,
  };
  return { status, headers } as unknown as Response;
}

describe('sessionExpiration', () => {
  afterEach(() => {
    Object.defineProperty(document, 'cookie', { value: '', writable: true });
  });

  describe('readExpirationTimestamp', () => {
    it('reads the IQ-SESSION-EXPIRATION-TIMESTAMP cookie value', () => {
      Object.defineProperty(document, 'cookie', {
        value: 'IQ-SESSION-EXPIRATION-TIMESTAMP=1714838400000',
        writable: true,
      });

      expect(readExpirationTimestamp()).toBe(1714838400000);
    });

    it('returns undefined when cookie is absent', () => {
      Object.defineProperty(document, 'cookie', {
        value: 'OTHER=val',
        writable: true,
      });

      expect(readExpirationTimestamp()).toBeUndefined();
    });

    it('returns undefined when cookie value is not a number', () => {
      Object.defineProperty(document, 'cookie', {
        value: 'IQ-SESSION-EXPIRATION-TIMESTAMP=not-a-number',
        writable: true,
      });

      expect(readExpirationTimestamp()).toBeUndefined();
    });
  });

  describe('getServerClockOffset', () => {
    it('computes positive offset when client clock is ahead', () => {
      const serverDate = new Date('2026-05-04T12:00:00Z');
      const clientNow = serverDate.getTime() + 5000;
      const offset = getServerClockOffset(serverDate.toUTCString(), clientNow);

      expect(offset).toBe(5000);
    });

    it('computes negative offset when client clock is behind', () => {
      const serverDate = new Date('2026-05-04T12:00:00Z');
      const clientNow = serverDate.getTime() - 3000;
      const offset = getServerClockOffset(serverDate.toUTCString(), clientNow);

      expect(offset).toBe(-3000);
    });

    it('returns 0 for invalid date header', () => {
      expect(getServerClockOffset('not-a-date', Date.now())).toBe(0);
    });
  });

  describe('createSessionExpirationTracker', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('fires onWarning 2 minutes before expiration', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      const now = Date.now();
      const expiresIn = 5 * 60 * 1000;

      Object.defineProperty(document, 'cookie', {
        value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${now + expiresIn}`,
        writable: true,
      });

      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      jest.advanceTimersByTime(3 * 60 * 1000);
      expect(onWarning).toHaveBeenCalledTimes(1);
      expect(onExpired).not.toHaveBeenCalled();

      tracker.stop();
    });

    it('fires onExpired at expiration time', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      const now = Date.now();
      const expiresIn = 5 * 60 * 1000;

      Object.defineProperty(document, 'cookie', {
        value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${now + expiresIn}`,
        writable: true,
      });

      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      jest.advanceTimersByTime(5 * 60 * 1000);
      expect(onExpired).toHaveBeenCalledTimes(1);

      tracker.stop();
    });

    it('fires onExpired immediately when session is already past expiry', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();

      Object.defineProperty(document, 'cookie', {
        value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${Date.now() - 1000}`,
        writable: true,
      });

      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      jest.advanceTimersByTime(0);
      expect(onExpired).toHaveBeenCalledTimes(1);

      tracker.stop();
    });

    it('does not fire callbacks when cookie is absent', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      Object.defineProperty(document, 'cookie', { value: '', writable: true });

      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      jest.advanceTimersByTime(10 * 60 * 1000);
      expect(onWarning).not.toHaveBeenCalled();
      expect(onExpired).not.toHaveBeenCalled();

      tracker.stop();
    });

    it('stop() clears timers', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      const now = Date.now();

      Object.defineProperty(document, 'cookie', {
        value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${now + 5 * 60 * 1000}`,
        writable: true,
      });

      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();
      tracker.stop();

      jest.advanceTimersByTime(10 * 60 * 1000);
      expect(onWarning).not.toHaveBeenCalled();
      expect(onExpired).not.toHaveBeenCalled();
    });

    it('does not fire onWarning if the cookie has been extended past the warning window before the timer fires', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      const now = Date.now();
      jest.setSystemTime(now);

      // Initial cookie: expires in 5 min. Warning timer scheduled for now + 3 min.
      setExpirationCookie(now + 5 * 60 * 1000);
      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      // Background activity (e.g. an apiFetch response) updates the cookie before the
      // warning timer fires — we simulate this by writing the cookie directly without
      // calling refreshFromResponse, since the bug class includes any code path that
      // refreshes the cookie without notifying the tracker.
      jest.advanceTimersByTime(2 * 60 * 1000);
      jest.setSystemTime(now + 2 * 60 * 1000);
      setExpirationCookie(now + 2 * 60 * 1000 + 30 * 60 * 1000);

      // Original warning timer fires at now + 3 min. It must re-read the cookie and,
      // seeing the new expiry is well outside the warning window, reschedule silently.
      jest.advanceTimersByTime(60 * 1000);
      jest.setSystemTime(now + 3 * 60 * 1000);
      expect(onWarning).not.toHaveBeenCalled();
      expect(onExpired).not.toHaveBeenCalled();

      // New expiry is at now + 32 min (2 min + 30 min from when the cookie was bumped).
      // Warning fires 2 min before that, i.e. at now + 30 min.
      jest.advanceTimersByTime(27 * 60 * 1000);
      jest.setSystemTime(now + 30 * 60 * 1000);
      expect(onWarning).toHaveBeenCalledTimes(1);

      tracker.stop();
    });

    it('does not fire onExpired if the cookie has been extended before the expiry timer fires', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      const now = Date.now();
      jest.setSystemTime(now);

      setExpirationCookie(now + 5 * 60 * 1000);
      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      // Let the warning fire on schedule, then extend the cookie before expiry.
      jest.advanceTimersByTime(3 * 60 * 1000);
      jest.setSystemTime(now + 3 * 60 * 1000);
      onWarning.mockClear();

      setExpirationCookie(now + 3 * 60 * 1000 + 20 * 60 * 1000);

      jest.advanceTimersByTime(2 * 60 * 1000);
      jest.setSystemTime(now + 5 * 60 * 1000);
      expect(onExpired).not.toHaveBeenCalled();

      tracker.stop();
    });

    it('refreshFromResponse resets timers with new expiration', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      // Use a timestamp with 0 milliseconds to avoid precision loss in toUTCString() round-trip
      const now = Math.floor(Date.now() / 1000) * 1000;
      jest.setSystemTime(now);

      setExpirationCookie(now + 3 * 60 * 1000);

      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      jest.advanceTimersByTime(60 * 1000);
      jest.setSystemTime(now + 60 * 1000);
      expect(onWarning).toHaveBeenCalledTimes(1);

      // New expiration: current time (now + 1min) + 10min = now + 11min
      setExpirationCookie(now + 60 * 1000 + 10 * 60 * 1000);

      tracker.refreshFromResponse(makeResponse(200, now + 60 * 1000));

      onWarning.mockClear();
      onExpired.mockClear();

      // Advance 8 total minutes from refresh -> warning should fire
      // New expiry is now + 11min, warning at now + 9min
      // Current time after jest.setSystemTime above is now + 1min
      // Need 8 more minutes to reach now + 9min
      jest.advanceTimersByTime(8 * 60 * 1000);
      jest.setSystemTime(now + 60 * 1000 + 8 * 60 * 1000);
      expect(onWarning).toHaveBeenCalledTimes(1);

      tracker.stop();
    });
  });

  describe('notifySessionResponse', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('forwards a successful response to the active tracker', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      const now = Math.floor(Date.now() / 1000) * 1000;
      jest.setSystemTime(now);

      // Warning would fire at now + 3 min based on the original cookie.
      setExpirationCookie(now + 5 * 60 * 1000);
      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      // Simulated apiFetch response after 1 minute extends the session by 30 min.
      jest.advanceTimersByTime(60 * 1000);
      jest.setSystemTime(now + 60 * 1000);
      setExpirationCookie(now + 60 * 1000 + 30 * 60 * 1000);
      notifySessionResponse(makeResponse(200, now + 60 * 1000));

      // The originally-scheduled warning would have fired at now + 3 min — it must not.
      jest.advanceTimersByTime(2 * 60 * 1000);
      jest.setSystemTime(now + 3 * 60 * 1000);
      expect(onWarning).not.toHaveBeenCalled();

      // The new warning fires 2 minutes before the new expiry (now + 31 min → warning at now + 29 min).
      jest.advanceTimersByTime(26 * 60 * 1000);
      jest.setSystemTime(now + 29 * 60 * 1000);
      expect(onWarning).toHaveBeenCalledTimes(1);

      tracker.stop();
    });

    it('ignores responses with status >= 400 (does not extend on errors)', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      const now = Math.floor(Date.now() / 1000) * 1000;
      jest.setSystemTime(now);

      setExpirationCookie(now + 5 * 60 * 1000);
      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      // The cookie hasn't moved on the server (e.g. 401 cleared the session). An
      // error response must not reset the timer; the original warning still fires.
      jest.advanceTimersByTime(60 * 1000);
      jest.setSystemTime(now + 60 * 1000);
      notifySessionResponse(makeResponse(401, now + 60 * 1000));

      jest.advanceTimersByTime(2 * 60 * 1000);
      jest.setSystemTime(now + 3 * 60 * 1000);
      expect(onWarning).toHaveBeenCalledTimes(1);

      tracker.stop();
    });

    it('is a no-op when no tracker is active', () => {
      // Stopped trackers unregister themselves; calling notifySessionResponse afterwards must not throw.
      const tracker = createSessionExpirationTracker({ onWarning: jest.fn(), onExpired: jest.fn() });
      tracker.start();
      tracker.stop();

      expect(() => notifySessionResponse(makeResponse(200, Date.now()))).not.toThrow();
    });
  });
});
