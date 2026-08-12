/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  DEFAULT_APPLICATIONS_THREAT_RANGE,
  EMPTY_APPLICATIONS_LIST_FILTERS,
  applicationsListFiltersToRequest,
  hasActiveApplicationsListFilters,
  normalizeApplicationsThreatRange,
  toggleApplicationsListFilterId,
} from 'MainRoot/nosc/applications/applicationsListFilters';

describe('applicationsListFilters', () => {
  it('maps empty filters to an empty request fragment', () => {
    expect(applicationsListFiltersToRequest(EMPTY_APPLICATIONS_LIST_FILTERS)).toEqual({});
    expect(hasActiveApplicationsListFilters(EMPTY_APPLICATIONS_LIST_FILTERS)).toBe(false);
  });

  it('maps stage and threat range into API request fields', () => {
    const filters = {
      ...toggleApplicationsListFilterId(EMPTY_APPLICATIONS_LIST_FILTERS, 'stageIds', 'build'),
      threatRange: [8, 10] as const,
      ageInDays: 30,
    };

    expect(applicationsListFiltersToRequest(filters)).toEqual({
      stageIds: ['build'],
      policyThreatLevelRanges: [{ minPolicyThreatLevel: 8, maxPolicyThreatLevel: 10 }],
      ageInDays: 30,
    });
    expect(hasActiveApplicationsListFilters(filters)).toBe(true);
  });

  it('omits policyThreatLevelRanges for the default full-domain slider', () => {
    expect(
      applicationsListFiltersToRequest({
        ...EMPTY_APPLICATIONS_LIST_FILTERS,
        threatRange: DEFAULT_APPLICATIONS_THREAT_RANGE,
      }),
    ).toEqual({});
  });

  it('normalizes inverted and out-of-bounds threat ranges', () => {
    expect(normalizeApplicationsThreatRange([12, -3])).toEqual([0, 10]);
    expect(normalizeApplicationsThreatRange([7, 2])).toEqual([2, 7]);
  });

  it('treats non-finite threat values as the domain minimum', () => {
    expect(normalizeApplicationsThreatRange([Number.NaN, 8])).toEqual([0, 8]);
    expect(normalizeApplicationsThreatRange([3, Number.POSITIVE_INFINITY])).toEqual([0, 3]);
  });

  it('keeps level-zero-only ranges active (full domain is the only default)', () => {
    expect(
      applicationsListFiltersToRequest({
        ...EMPTY_APPLICATIONS_LIST_FILTERS,
        threatRange: [0, 0],
      }),
    ).toEqual({
      policyThreatLevelRanges: [{ minPolicyThreatLevel: 0, maxPolicyThreatLevel: 0 }],
    });
  });

  it('serializes policy types as a comma-delimited string and states as an array (CLM-43211)', () => {
    const filters = {
      ...EMPTY_APPLICATIONS_LIST_FILTERS,
      policyTypes: new Set(['security', 'license']),
      violationStates: new Set(['OPEN']),
    };

    expect(applicationsListFiltersToRequest(filters)).toEqual({
      policyThreatCategories: 'license,security',
      policyViolationStates: ['OPEN'],
    });
    expect(hasActiveApplicationsListFilters(filters)).toBe(true);
  });

  it('toggles set-valued filter ids', () => {
    const filters = toggleApplicationsListFilterId(
      EMPTY_APPLICATIONS_LIST_FILTERS,
      'organizationIds',
      'org-a',
    );
    expect(filters.organizationIds.has('org-a')).toBe(true);
    expect(
      toggleApplicationsListFilterId(filters, 'organizationIds', 'org-a').organizationIds.size,
    ).toBe(0);
  });
});
