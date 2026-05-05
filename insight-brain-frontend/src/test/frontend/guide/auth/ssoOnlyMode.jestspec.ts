/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { fetchIsSsoOnlyEnabled } from 'GuideRoot/auth/ssoOnlyMode';

describe('fetchIsSsoOnlyEnabled', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('returns true when response contains "enable-sso-only"', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(['enable-sso-only']),
    });

    expect(await fetchIsSsoOnlyEnabled()).toBe(true);
    expect(global.fetch).toHaveBeenCalledWith('/rest/product/features/enableSsoOnly', { credentials: 'same-origin' });
  });

  it('returns false when response is an empty array', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([]),
    });

    expect(await fetchIsSsoOnlyEnabled()).toBe(false);
  });

  it('returns false when response contains other features but not sso-only', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(['some-other-feature']),
    });

    expect(await fetchIsSsoOnlyEnabled()).toBe(false);
  });

  it('returns false on network error', async () => {
    global.fetch = jest.fn().mockRejectedValue(new TypeError('Network error'));

    expect(await fetchIsSsoOnlyEnabled()).toBe(false);
  });

  it('returns false on non-OK response', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
    });

    expect(await fetchIsSsoOnlyEnabled()).toBe(false);
  });
});
