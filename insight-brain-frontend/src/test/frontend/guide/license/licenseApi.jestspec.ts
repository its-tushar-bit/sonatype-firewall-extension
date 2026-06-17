/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { fetchLicensedSolutions } from 'GuideRoot/license/licenseApi';

describe('fetchLicensedSolutions', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('returns the licensed solutions list from /api/v2/solutions/licensed', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        { id: 'lifecycle', url: '/lifecycle' },
        { id: 'guide', url: '/guide' },
      ]),
    });

    const result = await fetchLicensedSolutions();

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v2/solutions/licensed?allowRelativeUrls=true',
      { credentials: 'same-origin' }
    );
    expect(result).toEqual([
      { id: 'lifecycle', url: '/lifecycle' },
      { id: 'guide', url: '/guide' },
    ]);
  });

  it('returns an empty array when the response body is not an array', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(null),
    });

    const result = await fetchLicensedSolutions();

    expect(result).toEqual([]);
  });

  it('throws on non-ok response', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 402,
      statusText: 'Payment Required',
    });

    await expect(fetchLicensedSolutions()).rejects.toThrow('402 Payment Required');
  });
});
