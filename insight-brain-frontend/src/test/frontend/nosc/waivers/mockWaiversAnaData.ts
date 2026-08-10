/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { WaiversIndexQueryResponse } from 'MainRoot/nosc/waivers/waiversListApi';

/**
 * Two-row fixture mirroring the wire shape of {@code POST /rest/search/index-query} for
 * {@code entityType: WAIVER}. Row 1 is a manual application-scoped waiver; row 2 is an
 * auto-waiver on an organization scope. Facets include both organizations + applications,
 * which is what the backend returns for the WAIVER entity type.
 */
export const MOCK_WAIVERS_INDEX_QUERY_RESPONSE: WaiversIndexQueryResponse = {
  entityType: 'WAIVER',
  page: 1,
  pageSize: 50,
  totalEstimate: 2,
  exactTotalEstimate: true,
  rows: [
    {
      entityType: 'WAIVER',
      source: 'local',
      id: 'waiver-1',
      title: 'Critical CVSS 9+',
      subtitle: 'Apple - Java',
      fields: {
        policyId: 'policy-crit',
        policyName: 'Critical CVSS 9+',
        threatLevel: 9,
        reason: 'Approved by security team',
        comment: 'Tracked in JIRA-123',
        createdAt: '2026-05-01T10:00:00Z',
        expiresAt: '2026-12-31T00:00:00Z',
        scopeOwnerType: 'application',
        scopeOwnerId: 'app-internal-1',
        waivedBy: 'alice',
        organizationName: 'Java Team',
        organizationId: 'org-java',
        applicationName: 'Apple - Java',
        applicationId: 'app-internal-1',
        isAuto: false,
        isRequested: false,
        status: null,
        scope: 'application',
        policyType: 'security',
      },
      href: '/preview/waivers/application/app-internal-1/waiver-1',
    },
    {
      entityType: 'WAIVER',
      source: 'local',
      id: 'waiver-auto-2',
      title: null,
      subtitle: 'Platform',
      fields: {
        policyId: null,
        policyName: null,
        threatLevel: 4,
        reason: null,
        comment: null,
        createdAt: '2026-04-15T10:00:00Z',
        expiresAt: null,
        scopeOwnerType: 'organization',
        scopeOwnerId: 'org-root',
        waivedBy: 'system',
        organizationName: 'Platform',
        organizationId: 'org-root',
        applicationName: null,
        applicationId: null,
        isAuto: true,
        isRequested: false,
        status: null,
        scope: 'organization',
        policyType: 'other',
      },
      href: '/preview/waivers/organization/org-root/waiver-auto-2',
    },
  ],
  facets: {
    status: [
      { value: 'active', count: 8 },
      { value: 'expiring', count: 2 },
      { value: 'expired', count: 1 },
      { value: 'auto-waived', count: 4 },
    ],
    auto: [
      { value: 'true', count: 4 },
      { value: 'false', count: 6 },
    ],
    threatLevel: [
      { value: '9', count: 3 },
      { value: '3', count: 2 },
    ],
    organizationName: [
      { value: 'Java Team', count: 1 },
      { value: 'Platform', count: 1 },
    ],
    applications: [{ value: 'Apple - Java', count: 1 }],
    policy: [{ value: 'Critical CVSS 9+', count: 1 }],
    scope: [
      { value: 'application', count: 1 },
      { value: 'organization', count: 1 },
    ],
    policyType: [
      { value: 'security', count: 1 },
      { value: 'other', count: 1 },
    ],
  },
  facetsOverPageOnly: false,
  nextSearchAfter: null,
  warnings: [],
};
