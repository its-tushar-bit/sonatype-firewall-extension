/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { fetchLicenseSummary } from 'GuideRoot/license/licenseApi';

describe('fetchLicenseSummary', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('returns products and productEdition from /rest/product/license/validate', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        productEdition: 'Lifecycle',
        products: ['Sonatype Lifecycle', 'Sonatype Guide'],
      }),
    });

    const result = await fetchLicenseSummary();

    expect(global.fetch).toHaveBeenCalledWith('/rest/product/license/validate', {
      credentials: 'same-origin',
    });
    expect(result).toEqual({
      productEdition: 'Lifecycle',
      products: ['Sonatype Lifecycle', 'Sonatype Guide'],
    });
  });

  it('returns empty products when response has no products field', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ productEdition: null }),
    });

    const result = await fetchLicenseSummary();

    expect(result.products).toEqual([]);
    expect(result.productEdition).toBeNull();
  });

  it('throws on non-ok response', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 402,
      statusText: 'Payment Required',
    });

    await expect(fetchLicenseSummary()).rejects.toThrow('402 Payment Required');
  });
});
