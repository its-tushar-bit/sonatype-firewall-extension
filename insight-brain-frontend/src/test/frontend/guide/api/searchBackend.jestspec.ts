/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { searchAll } from 'GuideRoot/api/searchBackend';
import { ApiError } from 'GuideRoot/api/apiFetch';

const realFetch = global.fetch;
const mockFetch = jest.fn();

beforeAll(() => {
  global.fetch = mockFetch as unknown as typeof global.fetch;
});

afterAll(() => {
  global.fetch = realFetch;
});

beforeEach(() => {
  mockFetch.mockReset();
});

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    json: async () => body,
  } as unknown as Response;
}

describe('searchBackend', () => {
  describe('searchAll', () => {
    it('calls the global search endpoint with searchParams.toString() appended and returns the parsed JSON body', async () => {
      const fakeResponse = {
        hits: [{ format: 'npm', originId: 'lodash', name: 'lodash', version: '4.17.21' }],
        total: 1,
        offset: 0,
        limit: 10,
        aggregations: { byType: { component: 1, vulnerability: 0 } },
      };
      mockFetch.mockResolvedValue(jsonResponse(fakeResponse));

      const params = new URLSearchParams('query=lodash&limit=10&formats=npm&publishedWindow=30d');
      const result = await searchAll(params);

      expect(mockFetch).toHaveBeenCalledTimes(1);
      const [url] = mockFetch.mock.calls[0];
      expect(url).toBe('/api/v2/guide/global/search?query=lodash&limit=10&formats=npm&publishedWindow=30d');
      expect(result).toEqual(fakeResponse);
    });

    it('throws ApiError with the response status on a non-2xx response', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 503,
        statusText: 'Service Unavailable',
        json: async () => { throw new Error('not json'); },
      } as unknown as Response);

      await expect(searchAll(new URLSearchParams('query=lodash'))).rejects.toMatchObject({
        name: 'ApiError',
        status: 503,
      });
      await expect(searchAll(new URLSearchParams('query=lodash'))).rejects.toBeInstanceOf(ApiError);
    });

    it('forwards repeated formats entries verbatim', async () => {
      mockFetch.mockResolvedValue(jsonResponse({ hits: [], total: 0, offset: 0, limit: 25, aggregations: {} }));

      const params = new URLSearchParams([['formats', 'maven'], ['formats', 'npm']]);
      await searchAll(params);

      const [url] = mockFetch.mock.calls[0];
      expect(url).toBe('/api/v2/guide/global/search?formats=maven&formats=npm');
    });
  });
});
