/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ViolationsListResponse } from 'MainRoot/nosc/violations/violationListTypes';

/**
 * Test/preview fixture shaped like POST /rest/dashboard/violations/list (CLM-42254). Used by jest
 * specs and local previews; not imported by production code paths.
 */
export const MOCK_VIOLATIONS_LIST_RESPONSE: ViolationsListResponse = {
  violations: [
    {
      policyViolationId: 'pv-log4j-critical',
      threatLevel: 10,
      severity: 'critical',
      threatCategory: 'security',
      policyId: 'policy-security-critical',
      policyName: 'Security - Critical',
      organizationId: 'org-java',
      organizationName: 'Java-team',
      applicationId: 'app-apple',
      applicationPublicId: 'apple-java',
      applicationName: 'Apple - Java',
      componentName: 'log4j-core',
      componentIdentifier: { format: 'maven', coordinates: { version: '2.14.0' } },
      stage: 'Build',
      state: 'OPEN',
      waivedWithAutoWaiver: false,
    },
    {
      policyViolationId: 'pv-lodash-license',
      threatLevel: 8,
      severity: 'severe',
      threatCategory: 'license',
      policyId: 'policy-legal-copyleft',
      policyName: 'Legal - Copyleft',
      organizationId: 'org-java',
      organizationName: 'Java-team',
      applicationId: 'app-banana',
      applicationPublicId: 'banana-java',
      applicationName: 'Banana - Java',
      componentName: 'lodash',
      componentIdentifier: { format: 'npm', coordinates: { version: '4.17.15' } },
      stage: 'Release',
      state: 'OPEN',
      waivedWithAutoWaiver: false,
    },
    {
      policyViolationId: 'pv-busybox-quality',
      threatLevel: 3,
      severity: 'moderate',
      threatCategory: 'quality',
      policyId: 'policy-quality-standards',
      policyName: 'Quality - Standards',
      organizationId: 'org-platform',
      organizationName: 'Platform',
      applicationId: 'app-cherry',
      applicationPublicId: 'cherry-platform',
      applicationName: 'Cherry - Platform',
      componentName: 'busybox',
      componentIdentifier: { format: 'maven', coordinates: { version: '1.33' } },
      stage: 'Build',
      state: 'WAIVED',
      waivedWithAutoWaiver: true,
    },
  ],
  facets: {
    totalViolations: 3,
    states: { OPEN: 2, WAIVED: 1 },
    threatCategories: { security: 1, license: 1, quality: 1 },
    stages: { build: 2, release: 1 },
    organizations: { 'org-java': 2, 'org-platform': 1 },
    applications: { 'app-apple': 1, 'app-banana': 1, 'app-cherry': 1 },
  },
  total: 3,
  page: 0,
  pageSize: 25,
  hasNextPage: false,
  source: 'index',
};
