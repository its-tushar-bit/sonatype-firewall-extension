/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildWaiversIndexQueryRequest,
  mapApiWaiverRow,
  mapWaiversIndexQueryResponse,
} from 'MainRoot/nosc/waivers/waiversListApi';
import { EMPTY_WAIVERS_LIST_FILTERS } from 'MainRoot/nosc/waivers/waiversListFilters';
import { MOCK_WAIVERS_INDEX_QUERY_RESPONSE } from 'TestRoot/nosc/waivers/mockWaiversAnaData';

describe('waiversListApi (index-query)', () => {
  it('builds a page-1 request with sort and no cursor', () => {
    expect(
      buildWaiversIndexQueryRequest({
        page: 1,
        sort: '-policyWaiverCreatedAt',
      }),
    ).toEqual({
      entityType: 'WAIVER',
      page: 1,
      pageSize: 50,
      includeFacets: true,
      sort: '-policyWaiverCreatedAt',
    });
  });

  it('sends structured filters + free-text query', () => {
    const request = buildWaiversIndexQueryRequest({
      page: 1,
      search: '  guava  ',
      sort: '-policyWaiverCreatedAt',
      filters: {
        ...EMPTY_WAIVERS_LIST_FILTERS,
        organizationIds: new Set(['Java Team']),
        applicationIds: new Set(['Apple - Java']),
        policyIds: new Set(['policy-crit']),
        threatLevelIds: new Set(['Critical']),
        expiryStatusIds: new Set(['Active']),
        autoStatusIds: new Set(['Manual']),
      },
    });
    expect(request.entityType).toBe('WAIVER');
    expect(request.filters).toEqual({
      query: 'guava',
      organizations: ['Java Team'],
      applications: ['Apple - Java'],
      policies: ['policy-crit'],
      policyThreatLevel: [8, 10],
      expiryStatus: ['Active'],
      includeAutoWaivers: false,
    });
  });

  it('omits includeAutoWaivers when both Auto and Manual are selected', () => {
    const request = buildWaiversIndexQueryRequest({
      page: 1,
      filters: {
        ...EMPTY_WAIVERS_LIST_FILTERS,
        autoStatusIds: new Set(['Auto', 'Manual']),
      },
    });
    expect(request.filters?.includeAutoWaivers).toBeUndefined();
  });

  it('sends isAuto=["true"] for Auto-only selection', () => {
    const request = buildWaiversIndexQueryRequest({
      page: 1,
      filters: {
        ...EMPTY_WAIVERS_LIST_FILTERS,
        autoStatusIds: new Set(['Auto']),
      },
    });
    expect(request.filters?.isAuto).toEqual(['true']);
    expect(request.filters?.includeAutoWaivers).toBeUndefined();
  });

  it('collapses multiple threat buckets into a single min/max envelope', () => {
    const request = buildWaiversIndexQueryRequest({
      page: 1,
      filters: {
        ...EMPTY_WAIVERS_LIST_FILTERS,
        threatLevelIds: new Set(['Low', 'Critical']),
      },
    });
    expect(request.filters?.policyThreatLevel).toEqual([1, 10]);
  });

  it('sends searchAfter cursor only on page > 1 when supplied', () => {
    const p2 = buildWaiversIndexQueryRequest({
      page: 2,
      searchAfter: 'cursor-abc',
    });
    expect(p2.searchAfter).toBe('cursor-abc');
    expect(p2.page).toBe(2);

    const p1 = buildWaiversIndexQueryRequest({
      page: 1,
      searchAfter: 'cursor-xyz',
    });
    expect(p1.searchAfter).toBeUndefined();
  });

  it('maps a two-row index-query response with facets and totals', () => {
    const mapped = mapWaiversIndexQueryResponse(MOCK_WAIVERS_INDEX_QUERY_RESPONSE);
    expect(mapped.waivers).toHaveLength(2);
    expect(mapped.waivers[0]).toMatchObject({
      id: 'waiver-1',
      policyId: 'policy-crit',
      policyName: 'Critical CVSS 9+',
      threatLevel: 9,
      scopeOwnerType: 'application',
      scopeOwnerId: 'app-internal-1',
      isAuto: false,
    });
    expect(mapped.waivers[1].isAuto).toBe(true);
    expect(mapped.total).toBe(2);
    expect(mapped.exactTotalEstimate).toBe(true);
    expect(mapped.hasNextPage).toBe(false);
    expect(mapped.facets.organizations).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ id: 'Java Team', count: 1 }),
        expect.objectContaining({ id: 'Platform', count: 1 }),
      ]),
    );
    expect(mapped.facets.applications).toEqual([
      expect.objectContaining({ id: 'Apple - Java', count: 1 }),
    ]);
    // Threat + expiry + auto sections are always populated (static rows) so the rail renders.
    expect(mapped.facets.threatLevels.length).toBeGreaterThan(0);
    expect(mapped.facets.expiryStatuses).toEqual([
      expect.objectContaining({ id: 'Active' }),
      expect.objectContaining({ id: 'Expired' }),
      expect.objectContaining({ id: 'Never' }),
    ]);
    expect(mapped.facets.autoStatuses).toEqual([
      expect.objectContaining({ id: 'Auto' }),
      expect.objectContaining({ id: 'Manual' }),
    ]);
    // Policies are derived from the current page's rows (waiver-1 has a policyId).
    expect(mapped.facets.policies).toEqual([
      { id: 'policy-crit', label: 'Critical CVSS 9+', count: 1 },
    ]);
  });

  it('flags hasNextPage when nextSearchAfter is returned', () => {
    const mapped = mapWaiversIndexQueryResponse({
      ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE,
      nextSearchAfter: 'cursor-2',
    });
    expect(mapped.hasNextPage).toBe(true);
    expect(mapped.nextSearchAfter).toBe('cursor-2');
  });

  it('preserves exactTotalEstimate=false so the toolbar can render N+', () => {
    const mapped = mapWaiversIndexQueryResponse({
      ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE,
      totalEstimate: 10000,
      exactTotalEstimate: false,
    });
    expect(mapped.exactTotalEstimate).toBe(false);
    expect(mapped.total).toBe(10000);
  });

  it('drops rows missing the identifying id', () => {
    expect(mapApiWaiverRow({ title: 'orphan' })).toBeNull();
    const mapped = mapWaiversIndexQueryResponse({
      ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE,
      rows: [
        ...MOCK_WAIVERS_INDEX_QUERY_RESPONSE.rows!,
        { entityType: 'WAIVER', source: 'local', title: 'orphan' },
      ],
    });
    expect(mapped.waivers).toHaveLength(2);
  });
});
