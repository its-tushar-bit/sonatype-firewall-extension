/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildVulnerabilitiesListRequest,
  createDefaultVulnerabilitiesFilterState,
  hasActiveVulnerabilityFilters,
  mapVulnerabilitiesListResponse,
  scopeLabel,
  VULNERABILITIES_DEFAULT_ORDER_BY,
  VULNERABILITIES_PAGE_SIZE,
} from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';

describe('vulnerabilitiesListApi', () => {
  it('builds a My Scan Data request with defaults', () => {
    expect(buildVulnerabilitiesListRequest({ page: 0 })).toEqual({
      tab: 'myScanData',
      page: 0,
      pageSize: VULNERABILITIES_PAGE_SIZE,
      includeFacets: true,
      orderBy: VULNERABILITIES_DEFAULT_ORDER_BY,
    });
  });

  it('omits blank search and keeps catalog tab', () => {
    expect(
      buildVulnerabilitiesListRequest({
        tab: 'catalog',
        page: 2,
        search: '  ',
      }),
    ).toEqual({
      tab: 'catalog',
      page: 2,
      pageSize: VULNERABILITIES_PAGE_SIZE,
      includeFacets: true,
      orderBy: VULNERABILITIES_DEFAULT_ORDER_BY,
    });
  });

  it('serializes active severity, CVSS, and ecosystem filters', () => {
    expect(
      buildVulnerabilitiesListRequest({
        page: 0,
        filters: {
          ...createDefaultVulnerabilitiesFilterState(),
          severities: new Set(['high', 'critical']),
          ecosystems: new Set(['npm']),
          cvssRange: [7, 10],
        },
      }),
    ).toEqual({
      tab: 'myScanData',
      page: 0,
      pageSize: VULNERABILITIES_PAGE_SIZE,
      includeFacets: true,
      orderBy: VULNERABILITIES_DEFAULT_ORDER_BY,
      severities: ['critical', 'high'],
      minCvssScore: 7,
      maxCvssScore: 10,
      ecosystems: ['npm'],
    });
  });

  it('serializes organization, application, and stage scope filters (CLM-43211)', () => {
    expect(
      buildVulnerabilitiesListRequest({
        page: 0,
        filters: {
          ...createDefaultVulnerabilitiesFilterState(),
          organizations: new Set(['org-b', 'org-a']),
          applications: new Set(['app-1']),
          stages: new Set(['release', 'build']),
        },
      }),
    ).toEqual({
      tab: 'myScanData',
      page: 0,
      pageSize: VULNERABILITIES_PAGE_SIZE,
      includeFacets: true,
      orderBy: VULNERABILITIES_DEFAULT_ORDER_BY,
      organizationIds: ['org-a', 'org-b'],
      applicationIds: ['app-1'],
      stageIds: ['build', 'release'],
    });
  });

  it('omits estate scope filters on Catalog (My Scan Data only)', () => {
    expect(
      buildVulnerabilitiesListRequest({
        tab: 'catalog',
        page: 0,
        filters: {
          ...createDefaultVulnerabilitiesFilterState(),
          organizations: new Set(['org-a']),
          applications: new Set(['app-1']),
          stages: new Set(['build']),
          severities: new Set(['critical']),
        },
      }),
    ).toEqual({
      tab: 'catalog',
      page: 0,
      pageSize: VULNERABILITIES_PAGE_SIZE,
      includeFacets: true,
      orderBy: VULNERABILITIES_DEFAULT_ORDER_BY,
      severities: ['critical'],
    });
  });

  it('treats scope selections as active filters', () => {
    expect(
      hasActiveVulnerabilityFilters({
        ...createDefaultVulnerabilitiesFilterState(),
        stages: new Set(['build']),
      }),
    ).toBe(true);
    expect(hasActiveVulnerabilityFilters(createDefaultVulnerabilitiesFilterState())).toBe(false);
  });

  it('labels scope ids by name and falls back to the id when unnamed', () => {
    expect(scopeLabel({ 'org-1': 'Platform' }, 'org-1')).toBe('Platform');
    expect(scopeLabel({ 'org-1': '  ' }, 'org-1')).toBe('org-1');
    expect(scopeLabel(undefined, 'org-2')).toBe('org-2');
  });

  it('maps list response with safe defaults', () => {
    expect(mapVulnerabilitiesListResponse(undefined)).toEqual({
      vulnerabilities: [],
      facets: null,
      total: 0,
    });
  });
});
