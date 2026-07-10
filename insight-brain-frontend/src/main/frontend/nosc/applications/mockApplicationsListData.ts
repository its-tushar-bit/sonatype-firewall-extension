/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { THREAT_GROUPS } from 'MainRoot/nosc/applications/applicationDetailUtils';
import {
  ApplicationRiskScore,
  ApplicationsFilterFacetCounts,
} from 'MainRoot/nosc/applications/applicationListTypes';

/**
 * Development-only mock card rows shaped like POST /rest/dashboard/applications/list
 * (CLM-42228). Remove once the list API lands on main.
 */
export const MOCK_APPLICATION_RISK_SCORES: ReadonlyArray<ApplicationRiskScore> = [
  {
    organizationName: 'Java-team',
    organizationId: 'org-java',
    applicationName: 'Apple - Java',
    applicationId: 'apple-java',
    totalApplicationRisk: {
      totalRisk: 47,
      criticalRisk: 3,
      severeRisk: 8,
      moderateRisk: 21,
      lowRisk: 15,
    },
    stageRisks: [
      {
        stageTypeId: 'develop',
        stageTypeName: 'Develop',
        scanId: 'scan-apple-develop',
        evaluationDate: '2026-07-08T14:22:00Z',
        risk: { totalRisk: 12, criticalRisk: 1, severeRisk: 2, moderateRisk: 5, lowRisk: 4 },
      },
      {
        stageTypeId: 'source',
        stageTypeName: 'Source',
        scanId: 'scan-apple-source',
        evaluationDate: '2026-07-08T15:10:00Z',
        risk: { totalRisk: 8, criticalRisk: 0, severeRisk: 1, moderateRisk: 3, lowRisk: 4 },
      },
      {
        stageTypeId: 'build',
        stageTypeName: 'Build',
        scanId: 'scan-apple-build',
        evaluationDate: '2026-07-09T09:05:00Z',
        risk: { totalRisk: 47, criticalRisk: 3, severeRisk: 8, moderateRisk: 21, lowRisk: 15 },
      },
      {
        stageTypeId: 'stage-release',
        stageTypeName: 'Stage Release',
        scanId: 'scan-apple-stage-release',
        evaluationDate: '2026-07-07T18:40:00Z',
        risk: { totalRisk: 22, criticalRisk: 1, severeRisk: 3, moderateRisk: 10, lowRisk: 8 },
      },
    ],
  },
  {
    organizationName: 'Java-team',
    organizationId: 'org-java',
    applicationName: 'Banana - Java',
    applicationId: 'banana-java',
    totalApplicationRisk: {
      totalRisk: 12,
      criticalRisk: 0,
      severeRisk: 2,
      moderateRisk: 4,
      lowRisk: 6,
    },
    stageRisks: [
      {
        stageTypeId: 'build',
        stageTypeName: 'Build',
        scanId: 'scan-banana-build',
        evaluationDate: '2026-07-06T11:30:00Z',
        risk: { totalRisk: 12, criticalRisk: 0, severeRisk: 2, moderateRisk: 4, lowRisk: 6 },
      },
    ],
  },
  {
    organizationName: 'Platform',
    organizationId: 'org-platform',
    applicationName: 'Cherry - Platform',
    applicationId: 'cherry-platform',
    totalApplicationRisk: {
      totalRisk: 0,
      criticalRisk: 0,
      severeRisk: 0,
      moderateRisk: 0,
      lowRisk: 0,
    },
    stageRisks: [
      {
        stageTypeId: 'develop',
        stageTypeName: 'Develop',
        scanId: 'scan-cherry-develop',
        evaluationDate: '2026-07-05T08:00:00Z',
        risk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
      },
    ],
  },
];

/** Stub facet counts derived from mock rows until CLM-42228 facets merge. */
export function deriveMockApplicationsFilterFacets(
  applications: ReadonlyArray<ApplicationRiskScore>,
): ApplicationsFilterFacetCounts {
  const stageCounts = new Map<string, { id: string; label: string; count: number }>();
  const orgCounts = new Map<string, { id: string; label: string; count: number }>();
  const appCounts = new Map<string, { id: string; label: string; count: number }>();

  applications.forEach((app) => {
    orgCounts.set(app.organizationId, {
      id: app.organizationId,
      label: app.organizationName,
      count: (orgCounts.get(app.organizationId)?.count ?? 0) + 1,
    });
    appCounts.set(app.applicationId, {
      id: app.applicationId,
      label: app.applicationName,
      count: 1,
    });
    app.stageRisks.forEach((stage) => {
      stageCounts.set(stage.stageTypeId, {
        id: stage.stageTypeId,
        label: stage.stageTypeName,
        count: (stageCounts.get(stage.stageTypeId)?.count ?? 0) + 1,
      });
    });
  });

  return {
    totalApplications: applications.length,
    threatLevels: THREAT_GROUPS.map(({ group, range }) => ({
      id: group,
      label: `${range} ${group}`,
      count: group === 'None' ? 1 : Math.max(0, applications.length - 1),
    })),
    stages: Array.from(stageCounts.values()).sort((a, b) => a.label.localeCompare(b.label)),
    organizations: Array.from(orgCounts.values()).sort((a, b) => a.label.localeCompare(b.label)),
    applications: Array.from(appCounts.values()).sort((a, b) => a.label.localeCompare(b.label)),
  };
}

export const MOCK_APPLICATIONS_FILTER_FACETS = deriveMockApplicationsFilterFacets(MOCK_APPLICATION_RISK_SCORES);
