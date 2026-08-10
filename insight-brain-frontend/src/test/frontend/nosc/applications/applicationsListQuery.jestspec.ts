/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  DEFAULT_APPLICATIONS_THREAT_RANGE,
  EMPTY_APPLICATIONS_LIST_FILTERS,
} from 'MainRoot/nosc/applications/applicationsListFilters';
import {
  buildApplicationsListRouteParams,
  parseApplicationsListParams,
  parseApplicationsThreatRange,
  sortSlugToOrderBy,
} from 'MainRoot/nosc/applications/applicationsListQuery';

describe('applicationsListQuery (CLM-42226)', () => {
  it('parseApplicationsListParams reads search, sort, page, and filter params', () => {
    const parsed = parseApplicationsListParams({
      q: 'apple pie',
      sort: 'oldest',
      page: '3',
      stage: 'build,develop',
      org: 'org-java',
      app: 'apple-java',
      threat: '2-10',
    });

    expect(parsed.search).toBe('apple pie');
    expect(parsed.orderBy).toBe('lastEvaluationTime');
    expect(parsed.page).toBe(2);
    expect(Array.from(parsed.filters.stageIds)).toEqual(['build', 'develop']);
    expect(Array.from(parsed.filters.organizationIds)).toEqual(['org-java']);
    expect(Array.from(parsed.filters.applicationIds)).toEqual(['apple-java']);
    expect(parsed.filters.threatRange).toEqual([2, 10]);
  });

  it('falls back to the default threat range for legacy bucket tokens', () => {
    expect(parseApplicationsThreatRange('Critical,Severe')).toEqual(DEFAULT_APPLICATIONS_THREAT_RANGE);
    expect(parseApplicationsListParams({ threat: 'Critical' }).filters.threatRange).toEqual(
      DEFAULT_APPLICATIONS_THREAT_RANGE,
    );
  });

  it('buildApplicationsListRouteParams omits default sort, page 1, and full threat range', () => {
    expect(
      buildApplicationsListRouteParams({
        search: '',
        orderBy: sortSlugToOrderBy('highest-threat'),
        page: 0,
        filters: EMPTY_APPLICATIONS_LIST_FILTERS,
      }),
    ).toEqual({});
  });

  it('trims whitespace-only search from URL params', () => {
    expect(parseApplicationsListParams({ q: '  ' }).search).toBe('');
  });

  it('round-trips non-default toolbar and filter state', () => {
    const filters = {
      ...EMPTY_APPLICATIONS_LIST_FILTERS,
      stageIds: new Set(['build']),
      organizationIds: new Set(['org-a']),
      applicationIds: new Set(['app-a']),
      policyTypes: new Set(['security']),
      violationStates: new Set(['OPEN']),
      threatRange: [4, 7] as const,
    };
    const params = buildApplicationsListRouteParams({
      search: 'banana',
      orderBy: '-lastEvaluationTime',
      page: 1,
      filters,
    });
    const parsed = parseApplicationsListParams(params);
    expect(parsed.search).toBe('banana');
    expect(parsed.orderBy).toBe('-lastEvaluationTime');
    expect(parsed.page).toBe(1);
    expect(Array.from(parsed.filters.stageIds)).toEqual(['build']);
    expect(Array.from(parsed.filters.organizationIds)).toEqual(['org-a']);
    expect(Array.from(parsed.filters.applicationIds)).toEqual(['app-a']);
    expect(Array.from(parsed.filters.policyTypes)).toEqual(['security']);
    expect(Array.from(parsed.filters.violationStates)).toEqual(['OPEN']);
    expect(parsed.filters.threatRange).toEqual([4, 7]);
    expect(params.threat).toBe('4-7');
    expect(params.policyType).toBe('security');
    expect(params.violationState).toBe('OPEN');
    expect(params.sort).toBe('latest');
  });

  it('round-trips lowest threat sort explicitly', () => {
    const params = buildApplicationsListRouteParams({
      search: '',
      orderBy: 'maxPolicyThreatLevel',
      page: 0,
      filters: EMPTY_APPLICATIONS_LIST_FILTERS,
    });

    expect(params.sort).toBe('lowest-threat');
    expect(parseApplicationsListParams(params).orderBy).toBe('maxPolicyThreatLevel');
  });

  it('defaults unknown sort slugs to highest threat', () => {
    expect(sortSlugToOrderBy('highest-risk')).toBe('-maxPolicyThreatLevel');
    expect(sortSlugToOrderBy('not-a-sort')).toBe('-maxPolicyThreatLevel');
  });
});
