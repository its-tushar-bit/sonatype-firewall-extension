/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { VulnerabilitiesListResponse } from 'MainRoot/nosc/vulnerabilities/vulnerabilityListTypes';
import { VULNERABILITIES_PAGE_SIZE } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';

/** Test-only list API fixture (kept out of the production bundle). */
export const MOCK_VULNERABILITIES_LIST_RESPONSE: VulnerabilitiesListResponse = {
  source: 'index',
  page: 0,
  pageSize: VULNERABILITIES_PAGE_SIZE,
  total: 3,
  hasNextPage: false,
  vulnerabilities: [
    {
      vulnerabilityId: 'CVE-2024-0001',
      title: 'Critical remote code execution in example-lib',
      cvssScore: 9.8,
      severity: 'critical',
      ecosystem: 'maven',
    },
    {
      vulnerabilityId: 'CVE-2024-0002',
      title: 'High severity denial of service',
      cvssScore: 7.5,
      severity: 'high',
      ecosystem: 'npm',
    },
    {
      vulnerabilityId: 'CVE-2024-0003',
      title: 'Medium information disclosure',
      cvssScore: 5.3,
      severity: 'medium',
      ecosystem: 'pypi',
    },
  ],
  facets: {
    totalVulnerabilities: 3,
    severities: {
      critical: 1,
      high: 1,
      medium: 1,
      low: 0,
      none: 0,
    },
    ecosystems: {
      maven: 1,
      npm: 1,
      pypi: 1,
    },
  },
};
