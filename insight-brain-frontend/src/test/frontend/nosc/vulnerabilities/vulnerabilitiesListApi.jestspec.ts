/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildVulnerabilitiesListRequest,
  createDefaultVulnerabilitiesFilterState,
  mapVulnerabilitiesListResponse,
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

  it('maps list response with safe defaults', () => {
    expect(mapVulnerabilitiesListResponse(undefined)).toEqual({
      vulnerabilities: [],
      facets: null,
      total: 0,
    });
  });
});
