/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  EMPTY_WAIVERS_LIST_FILTERS,
  filtersEqual,
  hasActiveWaiversListFilters,
  toggleWaiversListFilterId,
  waiversListFiltersToRequest,
} from 'MainRoot/nosc/waivers/waiversListFilters';

describe('waiversListFilters', () => {
  it('starts empty and has no active filters', () => {
    expect(hasActiveWaiversListFilters(EMPTY_WAIVERS_LIST_FILTERS)).toBe(false);
    expect(waiversListFiltersToRequest(EMPTY_WAIVERS_LIST_FILTERS)).toEqual({});
  });

  it('toggles a threat-level id on and back off', () => {
    const on = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'threatLevelIds', 'Critical');
    expect(on.threatLevelIds.has('Critical')).toBe(true);
    expect(hasActiveWaiversListFilters(on)).toBe(true);
    const off = toggleWaiversListFilterId(on, 'threatLevelIds', 'Critical');
    expect(off.threatLevelIds.has('Critical')).toBe(false);
  });

  it('ignores non-selectable threat-level ids like None or bogus values', () => {
    expect(toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'threatLevelIds', 'None'))
      .toBe(EMPTY_WAIVERS_LIST_FILTERS);
    expect(toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'threatLevelIds', 'Bogus'))
      .toBe(EMPTY_WAIVERS_LIST_FILTERS);
  });

  it('ignores non-canonical expiry / auto ids', () => {
    expect(
      toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'expiryStatusIds', 'WhoKnows'),
    ).toBe(EMPTY_WAIVERS_LIST_FILTERS);
    expect(
      toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'lifecycleStatusIds', 'WhoKnows'),
    ).toBe(EMPTY_WAIVERS_LIST_FILTERS);
    expect(
      toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'autoStatusIds', 'Robot'),
    ).toBe(EMPTY_WAIVERS_LIST_FILTERS);
  });

  it('maps lifecycle status selections to the lifecycleStatus request filter', () => {
    let filters = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'lifecycleStatusIds', 'expiring');
    filters = toggleWaiversListFilterId(filters, 'lifecycleStatusIds', 'auto-waived');

    expect(waiversListFiltersToRequest(filters).lifecycleStatus).toEqual(['expiring', 'auto-waived']);
  });

  it('maps a Manual-only selection to includeAutoWaivers=false', () => {
    const filters = toggleWaiversListFilterId(
      EMPTY_WAIVERS_LIST_FILTERS,
      'autoStatusIds',
      'Manual',
    );
    expect(waiversListFiltersToRequest(filters).includeAutoWaivers).toBe(false);
  });

  it('omits includeAutoWaivers when both Auto and Manual are selected', () => {
    const step1 = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'autoStatusIds', 'Auto');
    const step2 = toggleWaiversListFilterId(step1, 'autoStatusIds', 'Manual');
    expect(waiversListFiltersToRequest(step2).includeAutoWaivers).toBeUndefined();
  });

  it('maps an Auto-only selection to isAuto=["true"]', () => {
    const filters = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'autoStatusIds', 'Auto');
    const req = waiversListFiltersToRequest(filters);
    expect(req.isAuto).toEqual(['true']);
    expect(req.includeAutoWaivers).toBeUndefined();
  });

  it('collapses multiple threat buckets into a min/max envelope', () => {
    const step1 = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'threatLevelIds', 'Low');
    const step2 = toggleWaiversListFilterId(step1, 'threatLevelIds', 'Critical');
    expect(waiversListFiltersToRequest(step2).policyThreatLevel).toEqual([1, 10]);
  });

  it('serializes free-form set filters (orgs, apps, policies) as arrays', () => {
    let filters = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'organizationIds', 'Java Team');
    filters = toggleWaiversListFilterId(filters, 'applicationIds', 'Apple - Java');
    filters = toggleWaiversListFilterId(filters, 'policyIds', 'Critical CVSS 9+');
    const req = waiversListFiltersToRequest(filters);
    expect(req.organizations).toEqual(['Java Team']);
    expect(req.applications).toEqual(['Apple - Java']);
    expect(req.policy).toEqual(['Critical CVSS 9+']);
  });

  it('maps waiverStates / scope / policyTypes and never emits excluded', () => {
    let filters = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'waiverStateIds', 'existing');
    filters = toggleWaiversListFilterId(filters, 'waiverStateIds', 'requested');
    filters = toggleWaiversListFilterId(filters, 'scopeIds', 'application');
    filters = toggleWaiversListFilterId(filters, 'policyTypeIds', 'security');
    const req = waiversListFiltersToRequest(filters);
    expect(req.waiverStates).toEqual(expect.arrayContaining(['existing', 'requested']));
    expect(req.waiverStates).not.toContain('excluded');
    expect(req.scope).toEqual(['application']);
    expect(req.policyTypes).toEqual(['security']);
    expect(
      toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'waiverStateIds', 'excluded'),
    ).toBe(EMPTY_WAIVERS_LIST_FILTERS);
  });

  it('treats set-order differences as equal', () => {
    let a = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'organizationIds', 'A');
    a = toggleWaiversListFilterId(a, 'organizationIds', 'B');
    let b = toggleWaiversListFilterId(EMPTY_WAIVERS_LIST_FILTERS, 'organizationIds', 'B');
    b = toggleWaiversListFilterId(b, 'organizationIds', 'A');
    expect(filtersEqual(a, b)).toBe(true);
  });
});
