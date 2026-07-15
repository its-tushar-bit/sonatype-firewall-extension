/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  EMPTY_APPLICATIONS_LIST_FILTERS,
  applicationsListFiltersToRequest,
  hasActiveApplicationsListFilters,
  staticThreatLevelFacets,
  toggleApplicationsListFilterId,
} from 'MainRoot/nosc/applications/applicationsListFilters';

describe('applicationsListFilters', () => {
  it('maps empty filters to an empty request fragment', () => {
    expect(applicationsListFiltersToRequest(EMPTY_APPLICATIONS_LIST_FILTERS)).toEqual({});
    expect(hasActiveApplicationsListFilters(EMPTY_APPLICATIONS_LIST_FILTERS)).toBe(false);
  });

  it('maps stage and threat selections into API request fields', () => {
    const filters = toggleApplicationsListFilterId(
      toggleApplicationsListFilterId(EMPTY_APPLICATIONS_LIST_FILTERS, 'stageIds', 'build'),
      'threatLevelIds',
      'Critical',
    );

    expect(applicationsListFiltersToRequest(filters)).toEqual({
      stageIds: ['build'],
      policyThreatLevelRanges: [{ minPolicyThreatLevel: 8, maxPolicyThreatLevel: 10 }],
    });
    expect(hasActiveApplicationsListFilters(filters)).toBe(true);
  });

  it('omits the None threat bucket from static facet rows', () => {
    expect(staticThreatLevelFacets().map((entry) => entry.id)).not.toContain('None');
  });

  it('sends one range per selected threat bucket instead of a single envelope', () => {
    let filters = EMPTY_APPLICATIONS_LIST_FILTERS;
    filters = toggleApplicationsListFilterId(filters, 'threatLevelIds', 'Critical');
    filters = toggleApplicationsListFilterId(filters, 'threatLevelIds', 'Severe');

    expect(applicationsListFiltersToRequest(filters)).toEqual({
      policyThreatLevelRanges: [
        { minPolicyThreatLevel: 8, maxPolicyThreatLevel: 10 },
        { minPolicyThreatLevel: 4, maxPolicyThreatLevel: 7 },
      ],
    });
  });

  it('preserves disjoint bucket ranges for Critical and Low', () => {
    let filters = EMPTY_APPLICATIONS_LIST_FILTERS;
    filters = toggleApplicationsListFilterId(filters, 'threatLevelIds', 'Critical');
    filters = toggleApplicationsListFilterId(filters, 'threatLevelIds', 'Low');

    expect(applicationsListFiltersToRequest(filters)).toEqual({
      policyThreatLevelRanges: [
        { minPolicyThreatLevel: 8, maxPolicyThreatLevel: 10 },
        { minPolicyThreatLevel: 1, maxPolicyThreatLevel: 1 },
      ],
    });
  });

  it('ignores unknown threat level ids when toggling', () => {
    const filters = toggleApplicationsListFilterId(EMPTY_APPLICATIONS_LIST_FILTERS, 'threatLevelIds', 'bogus');
    expect(filters.threatLevelIds.size).toBe(0);
    expect(applicationsListFiltersToRequest(filters)).toEqual({});
  });

  it('ignores None threat level id when toggling', () => {
    const filters = toggleApplicationsListFilterId(EMPTY_APPLICATIONS_LIST_FILTERS, 'threatLevelIds', 'None');
    expect(filters.threatLevelIds.size).toBe(0);
    expect(applicationsListFiltersToRequest(filters)).toEqual({});
  });

  it('omits policyThreatLevelRanges when threat ids have no known range', () => {
    const filters = {
      ...EMPTY_APPLICATIONS_LIST_FILTERS,
      threatLevelIds: new Set(['bogus' as 'Critical']),
    };
    expect(applicationsListFiltersToRequest(filters)).toEqual({});
  });
});
