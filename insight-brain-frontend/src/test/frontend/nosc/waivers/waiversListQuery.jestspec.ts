/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildWaiversListRouteParams,
  DEFAULT_WAIVERS_LIST_ORDER_BY,
  parseWaiversListParams,
  sortSlugToOrderBy,
  waiversListOrderByLabel,
} from 'MainRoot/nosc/waivers/waiversListQuery';

describe('waiversListQuery', () => {
  it('parses a fully populated URL param set', () => {
    const state = parseWaiversListParams({
      q: 'guava',
      sort: 'severity',
      page: '2',
      threat: 'Critical,Severe',
      lifecycle: 'active,expiring',
      expiry: 'Active',
      auto: 'Auto,Manual',
      state: 'existing,requested',
      scope: 'application',
      policyType: 'security,license',
      org: 'Java Team',
      app: 'Apple - Java',
      policy: 'Critical CVSS 9+',
    });
    expect(state.search).toBe('guava');
    expect(state.orderBy).toBe('-policyWaiverThreatLevel');
    expect(state.page).toBe(2);
    expect([...state.filters.threatLevelIds].sort()).toEqual(['Critical', 'Severe']);
    expect([...state.filters.lifecycleStatusIds].sort()).toEqual(['active', 'expiring']);
    expect([...state.filters.expiryStatusIds]).toEqual(['Active']);
    expect([...state.filters.autoStatusIds].sort()).toEqual(['Auto', 'Manual']);
    expect([...state.filters.waiverStateIds].sort()).toEqual(['existing', 'requested']);
    expect([...state.filters.scopeIds]).toEqual(['application']);
    expect([...state.filters.policyTypeIds].sort()).toEqual(['license', 'security']);
    expect([...state.filters.organizationIds]).toEqual(['Java Team']);
    expect([...state.filters.applicationIds]).toEqual(['Apple - Java']);
    expect([...state.filters.policyIds]).toEqual(['Critical CVSS 9+']);
  });

  it('defaults to newest first sort and page 1 when params are missing', () => {
    const state = parseWaiversListParams({});
    expect(state.orderBy).toBe(DEFAULT_WAIVERS_LIST_ORDER_BY);
    expect(state.page).toBe(1);
    expect(state.search).toBe('');
  });

  it('parses numeric page params (UI-Router may coerce query values)', () => {
    expect(parseWaiversListParams({ page: 2 }).page).toBe(2);
    expect(parseWaiversListParams({ page: 3.9 }).page).toBe(3);
    expect(parseWaiversListParams({ page: 0 }).page).toBe(1);
  });

  it('drops invalid threat / expiry / auto / state tokens without falling over', () => {
    const state = parseWaiversListParams({
      threat: 'None,Bogus,Critical',
      lifecycle: 'active,who-knows,auto-waived',
      expiry: 'Never,WhoKnows',
      auto: 'Robot,Auto',
      state: 'excluded,existing,bogus',
      scope: 'repository,application',
      policyType: 'SECURITY,security',
    });
    expect([...state.filters.threatLevelIds]).toEqual(['Critical']);
    expect([...state.filters.lifecycleStatusIds]).toEqual(['active', 'auto-waived']);
    expect([...state.filters.expiryStatusIds]).toEqual(['Never']);
    expect([...state.filters.autoStatusIds]).toEqual(['Auto']);
    expect([...state.filters.waiverStateIds]).toEqual(['existing']);
    expect([...state.filters.scopeIds]).toEqual(['application']);
    expect([...state.filters.policyTypeIds]).toEqual(['security']);
  });

  it('builds hash params with defaults omitted for round-trip cleanliness', () => {
    const params = buildWaiversListRouteParams({
      search: '',
      orderBy: DEFAULT_WAIVERS_LIST_ORDER_BY,
      page: 1,
      filters: {
        threatLevelIds: new Set(),
        lifecycleStatusIds: new Set(),
        expiryStatusIds: new Set(),
        autoStatusIds: new Set(),
        waiverStateIds: new Set(),
        scopeIds: new Set(),
        policyTypeIds: new Set(),
        organizationIds: new Set(),
        applicationIds: new Set(),
        policyIds: new Set(),
      },
    });
    expect(params).toEqual({
      q: undefined,
      sort: undefined,
      page: undefined,
      threat: undefined,
      lifecycle: undefined,
      expiry: undefined,
      auto: undefined,
      state: undefined,
      scope: undefined,
      policyType: undefined,
      org: undefined,
      app: undefined,
      policy: undefined,
    });
  });

  it('round-trips a non-default state back to the parsed shape', () => {
    const initial = {
      search: 'guava',
      orderBy: 'expiration' as const,
      page: 3,
      filters: {
        threatLevelIds: new Set(['Critical', 'Low'] as const),
        lifecycleStatusIds: new Set(['expired', 'auto-waived'] as const),
        expiryStatusIds: new Set(['Active'] as const),
        autoStatusIds: new Set(['Manual'] as const),
        waiverStateIds: new Set(['requested'] as const),
        scopeIds: new Set(['organization'] as const),
        policyTypeIds: new Set(['license'] as const),
        organizationIds: new Set(['Java Team']),
        applicationIds: new Set(['Apple - Java']),
        policyIds: new Set(['Critical CVSS 9+']),
      },
    };
    const params = buildWaiversListRouteParams(initial);
    const reparsed = parseWaiversListParams(params as Record<string, unknown>);
    expect(reparsed.search).toBe(initial.search);
    expect(reparsed.orderBy).toBe(initial.orderBy);
    expect(reparsed.page).toBe(initial.page);
    expect([...reparsed.filters.threatLevelIds].sort()).toEqual(['Critical', 'Low']);
    expect([...reparsed.filters.lifecycleStatusIds].sort()).toEqual(['auto-waived', 'expired']);
    expect([...reparsed.filters.expiryStatusIds]).toEqual(['Active']);
    expect([...reparsed.filters.autoStatusIds]).toEqual(['Manual']);
    expect([...reparsed.filters.waiverStateIds]).toEqual(['requested']);
    expect([...reparsed.filters.scopeIds]).toEqual(['organization']);
    expect([...reparsed.filters.policyTypeIds]).toEqual(['license']);
    expect([...reparsed.filters.organizationIds]).toEqual(['Java Team']);
    expect([...reparsed.filters.applicationIds]).toEqual(['Apple - Java']);
    expect([...reparsed.filters.policyIds]).toEqual(['Critical CVSS 9+']);
  });

  it('round-trips the expires soon deep-link lifecycle filter', () => {
    const state = parseWaiversListParams({ lifecycle: 'expiring' });
    expect([...state.filters.lifecycleStatusIds]).toEqual(['expiring']);
    expect(buildWaiversListRouteParams(state).lifecycle).toBe('expiring');
  });

  it('sort slug conversion is total (unknown → default)', () => {
    expect(sortSlugToOrderBy('nonsense')).toBe(DEFAULT_WAIVERS_LIST_ORDER_BY);
    expect(sortSlugToOrderBy(null)).toBe(DEFAULT_WAIVERS_LIST_ORDER_BY);
    expect(sortSlugToOrderBy(undefined)).toBe(DEFAULT_WAIVERS_LIST_ORDER_BY);
  });

  it('provides friendly sort labels for the toolbar select', () => {
    expect(waiversListOrderByLabel('-policyWaiverCreatedAt')).toBe('Newest first');
    expect(waiversListOrderByLabel('policyWaiverCreatedAt')).toBe('Oldest first');
    expect(waiversListOrderByLabel('-policyWaiverThreatLevel')).toBe('Highest threat first');
    expect(waiversListOrderByLabel('policyWaiverThreatLevel')).toBe('Lowest threat first');
    expect(waiversListOrderByLabel('expiration')).toBe('Expiring soonest');
  });
});
