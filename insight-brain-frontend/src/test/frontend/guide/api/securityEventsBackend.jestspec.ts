/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  searchSecurityEvents,
  fetchSecurityEventBrowseAggregations,
  _resetBrowseAggregationsCacheForTests,
  getSecurityEventDetails,
} from 'GuideRoot/api/securityEventsBackend';
import * as apiFetchModule from 'GuideRoot/api/apiFetch';
import { ApiError } from 'GuideRoot/api/apiFetch';

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

  describe('getSecurityEventDetails', () => {
    const detail = {
      eventId: 'sonatype-2024-0001',
      title: 'Malicious package xyz',
      overview: 'A malicious package was published to npm.',
      publishedDate: '2024-01-01T00:00:00Z',
      lastUpdatedDate: '2024-01-02T00:00:00Z',
      eventSeverityCategory: 'Critical',
      eventThreatType: 'MALICIOUS_OSS',
      detail: 'Detailed analysis.',
      guidance: 'Remove it.',
      advisoryReferenceIds: [],
      cwes: [],
      malwareThreatTypes: [],
      malwareAttackVectors: [],
    };

    it('fetches the detail endpoint with the encoded event id', async () => {
      mockApiFetch.mockResolvedValue(detail);

      const result = await getSecurityEventDetails('sonatype-2024-0001');

      expect(mockApiFetch).toHaveBeenCalledWith(
        '/api/v2/guide/security-events/sonatype-2024-0001'
      );
      expect(result).toEqual(detail);
    });

    it('encodes ids containing special characters', async () => {
      mockApiFetch.mockResolvedValue(detail);

      await getSecurityEventDetails('a/b c');

      expect(mockApiFetch).toHaveBeenCalledWith('/api/v2/guide/security-events/a%2Fb%20c');
    });

    it('returns null without fetching when the id is blank', async () => {
      const result = await getSecurityEventDetails('   ');

      expect(result).toBeNull();
      expect(mockApiFetch).not.toHaveBeenCalled();
    });

    it('returns null on HTTP 404', async () => {
      mockApiFetch.mockRejectedValue(new ApiError('Not Found', 404, 'Not Found'));

      const result = await getSecurityEventDetails('missing');

      expect(result).toBeNull();
    });

    it('rethrows non-404 errors', async () => {
      mockApiFetch.mockRejectedValue(new ApiError('Boom', 500, 'Server Error'));

      await expect(getSecurityEventDetails('boom')).rejects.toThrow('Boom');
    });
  });
});
