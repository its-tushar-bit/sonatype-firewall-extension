/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildMetricsScope,
  selectDashboardMetricsScope,
} from 'MainRoot/nosc/dashboard/metrics/dashboardMetricsScope';

describe('dashboardMetricsScope (CLM-40905 AT-F16: filter scope → request)', () => {
  it('returns an empty scope when nothing is applied (default RBAC view)', () => {
    expect(buildMetricsScope(null)).toEqual({});
    expect(
      buildMetricsScope({
        organizations: new Set(),
        applications: new Set(),
        stages: new Set(),
        categories: new Set(),
      }),
    ).toEqual({});
  });

  it('maps applied org/app/stage/tag sets to the request body shape', () => {
    const scope = buildMetricsScope({
      organizations: new Set(['org-1', 'org-2']),
      applications: new Set(['app-1']),
      stages: new Set(['build']),
      categories: new Set(['tag-9']),
    });
    expect(scope).toEqual({
      organizationIds: ['org-1', 'org-2'],
      applicationIds: ['app-1'],
      stageIds: ['build'],
      tagIds: ['tag-9'],
    });
  });

  it('omits empty selections so only active facets are sent', () => {
    const scope = buildMetricsScope({
      organizations: new Set(['org-1']),
      applications: new Set(),
      stages: new Set(),
      categories: new Set(),
    });
    expect(scope).toEqual({ organizationIds: ['org-1'] });
    expect(scope).not.toHaveProperty('applicationIds');
  });

  it('selector reads dashboardFilter.appliedFilter from store state', () => {
    const state = {
      dashboardFilter: {
        appliedFilter: {
          organizations: new Set(['org-1']),
          applications: new Set(['app-2']),
          stages: new Set(),
          categories: new Set(),
        },
      },
    };
    expect(selectDashboardMetricsScope(state)).toEqual({
      organizationIds: ['org-1'],
      applicationIds: ['app-2'],
    });
  });

  it('does not forward repository filters until the metrics API supports repositoryIds', () => {
    const scope = buildMetricsScope({
      organizations: new Set(['org-1']),
      repositories: new Set(['repo-1']),
    });
    expect(scope).toEqual({ organizationIds: ['org-1'] });
    expect(scope).not.toHaveProperty('repositoryIds');
  });
});
