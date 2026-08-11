/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  searchSecurityEvents,
  fetchSecurityEventBrowseAggregations,
  _resetBrowseAggregationsCacheForTests,
} from 'GuideRoot/api/securityEventsBackend';
import * as apiFetchModule from 'GuideRoot/api/apiFetch';

jest.mock('GuideRoot/api/apiFetch', () => ({
  ...jest.requireActual('GuideRoot/api/apiFetch'),
  apiFetch: jest.fn(),
}));

const mockApiFetch = apiFetchModule.apiFetch as jest.MockedFunction<typeof apiFetchModule.apiFetch>;

const sampleResponse = {
  hits: [],
  total: 0,
  offset: 0,
  limit: 25,
  aggregations: { bySeverityCategory: { Critical: 3 } },
};

describe('securityEventsBackend', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    _resetBrowseAggregationsCacheForTests();
  });

  describe('searchSecurityEvents', () => {
    it('forwards search params verbatim to the security-events search endpoint', async () => {
      mockApiFetch.mockResolvedValue(sampleResponse);
      const params = new URLSearchParams('severities=Critical&threatTypes=MALICIOUS_OSS&limit=25');

      await searchSecurityEvents(params);

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/v2/guide/security-events/search?severities=Critical&threatTypes=MALICIOUS_OSS&limit=25'
      );
    });
  });

  describe('fetchSecurityEventBrowseAggregations', () => {
    it('requests limit=1 and returns the aggregations', async () => {
      mockApiFetch.mockResolvedValue(sampleResponse);

      const result = await fetchSecurityEventBrowseAggregations();

      expect(mockApiFetch).toHaveBeenCalledWith('/api/v2/guide/security-events/search?limit=1');
      expect(result).toEqual({ bySeverityCategory: { Critical: 3 } });
    });

    it('memoizes within the TTL (second call does not refetch)', async () => {
      mockApiFetch.mockResolvedValue(sampleResponse);

      await fetchSecurityEventBrowseAggregations();
      await fetchSecurityEventBrowseAggregations();

      expect(mockApiFetch).toHaveBeenCalledTimes(1);
    });

    it('resolves to null and evicts on failure so the next call retries', async () => {
      mockApiFetch.mockRejectedValueOnce(new Error('unreachable'));

      const first = await fetchSecurityEventBrowseAggregations();
      expect(first).toBeNull();

      mockApiFetch.mockResolvedValueOnce(sampleResponse);
      const second = await fetchSecurityEventBrowseAggregations();
      expect(second).toEqual({ bySeverityCategory: { Critical: 3 } });
      expect(mockApiFetch).toHaveBeenCalledTimes(2);
    });
  });
});
