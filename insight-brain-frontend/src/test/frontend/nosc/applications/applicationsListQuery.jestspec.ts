/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildApplicationsListRouteParams,
  parseApplicationsListParams,
  sortSlugToOrderBy,
} from 'MainRoot/nosc/applications/applicationsListQuery';

describe('applicationsListQuery (CLM-42226)', () => {
  it('parseApplicationsListParams reads search, sort, page, and filter csv params', () => {
    const parsed = parseApplicationsListParams({
      q: 'apple pie',
      sort: 'oldest',
      page: '3',
      stage: 'build,develop',
      org: 'org-java',
      app: 'apple-java',
      threat: 'Critical,Severe',
    });

    expect(parsed.search).toBe('apple pie');
    expect(parsed.orderBy).toBe('lastEvaluationTime');
    expect(parsed.page).toBe(2);
    expect(Array.from(parsed.filters.stageIds)).toEqual(['build', 'develop']);
    expect(Array.from(parsed.filters.organizationIds)).toEqual(['org-java']);
    expect(Array.from(parsed.filters.applicationIds)).toEqual(['apple-java']);
    expect(Array.from(parsed.filters.threatLevelIds)).toEqual(['Critical', 'Severe']);
  });

  it('ignores None in deep-linked threat params because it is not selectable in the rail', () => {
    const parsed = parseApplicationsListParams({ threat: 'Critical,None' });
    expect(Array.from(parsed.filters.threatLevelIds)).toEqual(['Critical']);
  });

  it('buildApplicationsListRouteParams drops invalid threat tokens from deep links', () => {
    const parsed = parseApplicationsListParams({ threat: 'Bogus,Critical' });
    expect(Array.from(parsed.filters.threatLevelIds)).toEqual(['Critical']);
    expect(buildApplicationsListRouteParams(parsed)).toEqual({ threat: 'Critical' });
  });

  it('buildApplicationsListRouteParams omits default sort and page 1', () => {
    expect(
      buildApplicationsListRouteParams({
        search: '',
        orderBy: sortSlugToOrderBy('latest'),
        page: 0,
        filters: {
          stageIds: new Set(),
          organizationIds: new Set(),
          applicationIds: new Set(),
          threatLevelIds: new Set(),
        },
      }),
    ).toEqual({});
  });

  it('trims whitespace-only search from URL params', () => {
    expect(parseApplicationsListParams({ q: '  ' }).search).toBe('');
  });

  it('round-trips non-default toolbar and filter state', () => {
    const filters = {
      stageIds: new Set(['build']),
      organizationIds: new Set(['org-a']),
      applicationIds: new Set(['app-a']),
      threatLevelIds: new Set(['Moderate' as const]),
    };
    const params = buildApplicationsListRouteParams({
      search: 'banana',
      orderBy: 'lastEvaluationTime',
      page: 1,
      filters,
    });
    const parsed = parseApplicationsListParams(params);
    expect(parsed.search).toBe('banana');
    expect(parsed.orderBy).toBe('lastEvaluationTime');
    expect(parsed.page).toBe(1);
    expect(Array.from(parsed.filters.stageIds)).toEqual(['build']);
    expect(Array.from(parsed.filters.organizationIds)).toEqual(['org-a']);
    expect(Array.from(parsed.filters.applicationIds)).toEqual(['app-a']);
    expect(Array.from(parsed.filters.threatLevelIds)).toEqual(['Moderate']);
  });
});
