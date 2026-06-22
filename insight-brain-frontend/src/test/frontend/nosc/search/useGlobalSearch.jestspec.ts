/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import { renderHook, waitFor } from '@testing-library/react';
import MockAdapter from 'axios-mock-adapter';
import axios from 'axios';
import {
  useGlobalSearch,
  buildBucketQuery,
  ENTITY_BUCKETS,
} from 'MainRoot/nosc/search/useGlobalSearch';

/**
 * CLM-39549: behavioral tests for the debounced multi-entity search hook.
 *
 * Covers the contract Anastasia / Bugbot flagged as untested:
 *   - Lucene escaping (including && and || operators)
 *   - pageSize actually caps the merged result list
 *   - totalHits is a cross-bucket sum, reported as non-exact
 *   - partial bucket failure still surfaces the buckets that worked
 *   - total failure surfaces a loadError
 */

const GROUP = (items: unknown[]) => [{ groupBy: 'ITEM_TYPE', searchResultItemDTOS: items }];

const ITEMS = [
  { itemType: 'APPLICATION', resultIndex: 0, applicationPublicId: 'app-1', applicationName: 'Webgoat' },
  { itemType: 'SECURITY_VULNERABILITY', resultIndex: 1, vulnerabilityId: 'CVE-2021-44228' },
  {
    itemType: 'NON_VULNERABLE_COMPONENT',
    resultIndex: 2,
    componentHash: 'h1',
    componentName: 'log4j-core',
    componentIdentifier: { format: 'maven' },
  },
];

function okResponse(totalNumberOfHits = ITEMS.length) {
  return {
    searchQuery: 'q',
    page: 0,
    pageSize: 12,
    totalNumberOfHits,
    isExactTotalNumberOfHits: true,
    groupingByDTOS: GROUP(ITEMS),
  };
}

describe('useGlobalSearch', () => {
  let mock: MockAdapter;

  beforeEach(() => {
    mock = new MockAdapter(axios);
  });

  afterEach(() => {
    mock.restore();
  });

  it('returns empty state and makes no request for a query shorter than 2 chars', () => {
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(200, okResponse());
    const { result } = renderHook(() => useGlobalSearch('a'));
    // A sub-MIN_QUERY_LENGTH query takes the effect's early-return path and never
    // schedules the debounced fetch, so we can assert synchronously — no real-time
    // sleep (a flakiness source per CLAUDE.md) is needed.
    expect(result.current.results).toHaveLength(0);
    expect(result.current.loading).toBe(false);
    expect(mock.history.get).toHaveLength(0);
  });

  it('escapes Lucene special characters including && and || in the outgoing query', async () => {
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(200, okResponse());
    renderHook(() => useGlobalSearch('a&&b||c'));
    await waitFor(() => expect(mock.history.get.length).toBeGreaterThan(0), { timeout: 2000 });
    const decoded = decodeURIComponent(mock.history.get[0].url ?? '');
    expect(decoded).toContain('a\\&\\&b\\|\\|c');
  });

  it('caps the merged result list to the requested pageSize', async () => {
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(200, okResponse());
    const { result } = renderHook(() => useGlobalSearch('log4j', { mode: 'full', pageSize: 2 }));
    await waitFor(() => expect(result.current.results.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(result.current.results.length).toBeLessThanOrEqual(2);
  });

  it('reports totalHits as the cross-bucket sum and marks it non-exact', async () => {
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(200, okResponse(5));
    const { result } = renderHook(() => useGlobalSearch('log4j', { pageSize: 50 }));
    await waitFor(() => expect(result.current.totalHits).toBeGreaterThan(0), { timeout: 2000 });
    // Each successful bucket contributes its totalNumberOfHits (5); summed across
    // every request the hook fanned out.
    const successfulBuckets = mock.history.get.length;
    expect(result.current.totalHits).toBe(successfulBuckets * 5);
    expect(result.current.isExactTotal).toBe(false);
  });

  it('still surfaces working buckets when some buckets fail (partial failure)', async () => {
    let callCount = 0;
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(() => {
      callCount += 1;
      // Fail every other bucket.
      return callCount % 2 === 0 ? [500, 'boom'] : [200, okResponse()];
    });
    const { result } = renderHook(() => useGlobalSearch('log4j', { pageSize: 50 }));
    await waitFor(() => expect(result.current.results.length).toBeGreaterThan(0), { timeout: 2000 });
    expect(result.current.loadError).toBeNull();
  });

  it('surfaces a loadError when every bucket fails', async () => {
    mock.onGet(/\/api\/v2\/search\/advanced/).reply(500, 'boom');
    const { result } = renderHook(() => useGlobalSearch('log4j'));
    await waitFor(() => expect(result.current.loadError).not.toBeNull(), { timeout: 2000 });
    expect(result.current.results).toHaveLength(0);
  });
});

describe('buildBucketQuery', () => {
  const applicationBucket = ENTITY_BUCKETS.find((b) => b.bucketKey === 'APPLICATION')!;
  const violationBucket = ENTITY_BUCKETS.find((b) => b.bucketKey === 'POLICY_VIOLATION')!;
  const waiverBucket = ENTITY_BUCKETS.find((b) => b.bucketKey === 'WAIVER')!;

  it('escapes && and || so they are not parsed as Lucene operators', () => {
    expect(buildBucketQuery(applicationBucket, 'a&&b')).toContain('a\\&\\&b');
    expect(buildBucketQuery(applicationBucket, 'x||y')).toContain('x\\|\\|y');
  });

  it('does NOT escape "/" so GAV / path-style component queries keep matching', () => {
    const componentBucket = ENTITY_BUCKETS.find((b) => b.bucketKey === 'NON_VULNERABLE_COMPONENT')!;
    const query = buildBucketQuery(componentBucket, 'org/apache/log4j');
    // A lone "/" is a literal in Lucene; escaping it (org\/apache\/log4j) would
    // stop the wildcard from matching the indexed coordinate value.
    expect(query).toContain('componentName:*org/apache/log4j*');
    expect(query).not.toContain('org\\/apache\\/log4j');
  });

  it('returns an empty string for blank input', () => {
    expect(buildBucketQuery(applicationBucket, '   ')).toBe('');
  });

  it('pins active violations to policyViolationWaiverStatus:Active', () => {
    const query = buildBucketQuery(violationBucket, 'log4j');
    expect(query).toContain('itemType:POLICY_VIOLATION');
    expect(query).toContain('policyViolationWaiverStatus:Active');
    expect(query).toContain('policyViolationPolicyName:*log4j*');
  });

  it('pins waived rows to Waived or AutoWaived status', () => {
    const query = buildBucketQuery(waiverBucket, 'log4j');
    expect(query).toContain('itemType:POLICY_VIOLATION');
    expect(query).toContain('policyViolationWaiverStatus:Waived');
    expect(query).toContain('policyViolationWaiverStatus:AutoWaived');
    expect(query).toContain('policyViolationPolicyName:*log4j*');
  });
});
