/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { ViolationsListResponse } from 'MainRoot/nosc/violations/violationListTypes';

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * Test/preview fixture shaped like POST /rest/dashboard/violations/list. Used by jest specs and local
 * previews; not imported by production code paths. {@code firstOccurredTime} mirrors the Wave B page
 * SQL enrich ({@code PolicyViolation.openTime}).
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
      componentVersion: '2.14.0',
      componentIdentifier: { format: 'maven' },
      stage: 'Build',
      state: 'OPEN',
      waivedWithAutoWaiver: false,
      firstOccurredTime: Date.now() - 2 * DAY_MS,
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
      componentVersion: '4.17.15',
      componentIdentifier: { format: 'npm' },
      stage: 'Release',
      state: 'OPEN',
      waivedWithAutoWaiver: false,
      firstOccurredTime: Date.now() - 40 * DAY_MS,
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
      componentVersion: '1.33',
      componentIdentifier: { format: 'maven' },
      stage: 'Build',
      state: 'WAIVED',
      waivedWithAutoWaiver: true,
      firstOccurredTime: Date.now() - 400 * DAY_MS,
    },
  ],
  facets: {
    totalViolations: 3,
    states: { OPEN: 2, WAIVED: 1 },
    // The single waived row (busybox) is auto-waived, so only the AUTO bucket has a count.
    waiverTypes: { AUTO: 1 },
    threatCategories: { security: 1, license: 1, quality: 1 },
    stages: { build: 2, release: 1 },
    organizations: { 'org-java': 2, 'org-platform': 1 },
    applications: { 'app-apple': 1, 'app-banana': 1, 'app-cherry': 1 },
    // Optional wire fields (CLM-42757); exercises off-page-friendly label merge in FE tests.
    organizationNames: { 'org-java': 'Java-team', 'org-platform': 'Platform' },
    applicationNames: {
      'app-apple': 'Apple - Java',
      'app-banana': 'Banana - Java',
      'app-cherry': 'Cherry - Platform',
    },
  },
  total: 3,
  page: 0,
  pageSize: 25,
  hasNextPage: false,
  source: 'index',
};
