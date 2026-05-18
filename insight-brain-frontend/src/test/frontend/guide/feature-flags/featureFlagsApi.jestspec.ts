/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { fetchFeatureFlags } from 'GuideRoot/feature-flags/featureFlagsApi';

describe('fetchFeatureFlags', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  it('returns the array of feature ids on a 200 response', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      json: () => Promise.resolve(['guide-ui', 'sbom-manager']),
    }) as unknown as typeof fetch;

    const result = await fetchFeatureFlags();

    expect(result).toEqual(['guide-ui', 'sbom-manager']);
    expect(global.fetch).toHaveBeenCalledWith(
      '/rest/product/features',
      expect.objectContaining({ credentials: 'same-origin' })
    );
  });

  it('throws on a non-ok response so callers can handle it', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
      json: () => Promise.resolve({}),
    }) as unknown as typeof fetch;

    await expect(fetchFeatureFlags()).rejects.toThrow('500 Internal Server Error');
  });

  it('returns empty array when the body is not an array', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      json: () => Promise.resolve({ unexpected: 'shape' }),
    }) as unknown as typeof fetch;

    const result = await fetchFeatureFlags();

    expect(result).toEqual([]);
  });
});
