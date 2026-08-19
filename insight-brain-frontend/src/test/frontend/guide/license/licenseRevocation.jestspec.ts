/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  GUIDE_LICENSE_HEADER,
  GUIDE_LICENSE_UNAVAILABLE,
  isGuideLicenseUnavailable,
  setLicenseRevocationHandler,
  notifyLicenseRevoked,
} from 'GuideRoot/license/licenseRevocation';

function responseWithHeaders(headers?: Record<string, string>): Response {
  if (headers === undefined) {
    // Mirrors the body-less mock shape used by backend tests (e.g. searchBackend.jestspec.ts),
    // where the fake Response has no `headers` field at all.
    return {} as Response;
  }
  return { headers: new Headers(headers) } as Response;
}

describe('licenseRevocation', () => {
  afterEach(() => {
    setLicenseRevocationHandler(null);
  });

  describe('isGuideLicenseUnavailable', () => {
    it('returns true when the marker header equals the unavailable value', () => {
      const response = responseWithHeaders({ [GUIDE_LICENSE_HEADER]: GUIDE_LICENSE_UNAVAILABLE });

      expect(isGuideLicenseUnavailable(response)).toBe(true);
    });

    it('matches the marker header case-insensitively', () => {
      const response = responseWithHeaders({ 'x-sonatype-guide-license': GUIDE_LICENSE_UNAVAILABLE });

      expect(isGuideLicenseUnavailable(response)).toBe(true);
    });

    it('returns false when the marker header carries a different value', () => {
      const response = responseWithHeaders({ [GUIDE_LICENSE_HEADER]: 'something-else' });

      expect(isGuideLicenseUnavailable(response)).toBe(false);
    });

    it('returns false when the marker header is absent', () => {
      const response = responseWithHeaders({ 'Content-Type': 'application/json' });

      expect(isGuideLicenseUnavailable(response)).toBe(false);
    });

    it('returns false (does not throw) when the response has no headers object', () => {
      const response = responseWithHeaders(undefined);

      expect(isGuideLicenseUnavailable(response)).toBe(false);
    });
  });

  describe('handler notification', () => {
    it('invokes the registered handler on notifyLicenseRevoked', () => {
      const handler = jest.fn();
      setLicenseRevocationHandler(handler);

      notifyLicenseRevoked();

      expect(handler).toHaveBeenCalledTimes(1);
    });

    it('is a no-op when no handler is registered', () => {
      expect(() => notifyLicenseRevoked()).not.toThrow();
    });

    it('stops invoking the handler once it is cleared with null', () => {
      const handler = jest.fn();
      setLicenseRevocationHandler(handler);
      setLicenseRevocationHandler(null);

      notifyLicenseRevoked();

      expect(handler).not.toHaveBeenCalled();
    });
  });
});
