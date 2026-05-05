/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  readExpirationTimestamp,
  getServerClockOffset,
  createSessionExpirationTracker,
} from 'GuideRoot/auth/sessionExpiration';

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

    it('refreshFromResponse resets timers with new expiration', () => {
      const onWarning = jest.fn();
      const onExpired = jest.fn();
      // Use a timestamp with 0 milliseconds to avoid precision loss in toUTCString() round-trip
      const now = Math.floor(Date.now() / 1000) * 1000;
      jest.setSystemTime(now);

      Object.defineProperty(document, 'cookie', {
        value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${now + 3 * 60 * 1000}`,
        writable: true,
      });

      const tracker = createSessionExpirationTracker({ onWarning, onExpired });
      tracker.start();

      jest.advanceTimersByTime(60 * 1000);
      jest.setSystemTime(now + 60 * 1000);
      expect(onWarning).toHaveBeenCalledTimes(1);

      // New expiration: current time (now + 1min) + 10min = now + 11min
      Object.defineProperty(document, 'cookie', {
        value: `IQ-SESSION-EXPIRATION-TIMESTAMP=${now + 60 * 1000 + 10 * 60 * 1000}`,
        writable: true,
      });

      const mockHeaders = { get: (name: string) => name === 'Date' ? new Date(now + 60 * 1000).toUTCString() : null };
      tracker.refreshFromResponse({ headers: mockHeaders } as unknown as Response);

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
});
