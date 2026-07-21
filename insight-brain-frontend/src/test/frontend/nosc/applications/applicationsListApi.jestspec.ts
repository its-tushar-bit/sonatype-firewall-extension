/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  evaluationTimeToIso,
  mapApiApplicationRiskScore,
  mapApplicationsListResponse,
} from 'MainRoot/nosc/applications/applicationsListApi';

describe('applicationsListApi', () => {
  it('maps evaluationTime to ISO evaluationDate on stage rows', () => {
    const mapped = mapApiApplicationRiskScore({
      applicationId: 'apple-java',
      applicationName: 'Apple - Java',
      organizationId: 'org-java',
      organizationName: 'Java-team',
      totalApplicationRisk: { totalRisk: 1, criticalRisk: 0, severeRisk: 0, moderateRisk: 1, lowRisk: 0 },
      stageRisks: [
        {
          stageTypeId: 'build',
          stageTypeName: 'Build',
          scanId: 'scan-apple-build',
          evaluationTime: Date.parse('2026-07-09T09:05:00Z'),
          risk: { totalRisk: 1, criticalRisk: 0, severeRisk: 0, moderateRisk: 1, lowRisk: 0 },
        },
      ],
    });

    expect(mapped?.stageRisks[0].evaluationDate).toBe('2026-07-09T09:05:00.000Z');
  });

  it('maps lastEvaluationTime to lastEvaluationDate on application rows', () => {
    const mapped = mapApiApplicationRiskScore({
      applicationId: 'apple-java',
      applicationName: 'Apple - Java',
      lastEvaluationTime: Date.parse('2026-07-09T09:05:00Z'),
      totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
      stageRisks: [],
    });

    expect(mapped?.lastEvaluationDate).toBe('2026-07-09T09:05:00.000Z');
  });

  it('treats zero epoch millis as absent evaluation timestamps', () => {
    expect(evaluationTimeToIso(0)).toBeUndefined();
    const mapped = mapApiApplicationRiskScore({
      applicationId: 'apple-java',
      applicationName: 'Apple - Java',
      lastEvaluationTime: 0,
      totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
      stageRisks: [
        {
          stageTypeId: 'build',
          stageTypeName: 'Build',
          scanId: 'scan-apple-build',
          evaluationTime: 0,
          risk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
        },
      ],
    });

    expect(mapped?.lastEvaluationDate).toBeUndefined();
    expect(mapped?.stageRisks[0].evaluationDate).toBeUndefined();
  });

  it('uses API total for toolbar/filter counts even when the page is shorter', () => {
    const mapped = mapApplicationsListResponse({
      applications: [
        {
          applicationId: 'apple-java',
          applicationName: 'Apple - Java',
          organizationId: 'org-java',
          organizationName: 'Java-team',
          totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
          stageRisks: [],
        },
      ],
      facets: { totalApplications: 42 },
      total: 42,
      page: 0,
      pageSize: 50,
      hasNextPage: false,
    });

    expect(mapped.total).toBe(42);
    expect(mapped.facets.totalApplications).toBe(42);
    expect(mapped.applications).toHaveLength(1);
    expect(mapped.facets).not.toHaveProperty('threatLevels');
  });

  it('falls back to raw API application count when total is omitted', () => {
    const mapped = mapApplicationsListResponse({
      applications: [
        {
          applicationId: 'apple-java',
          applicationName: 'Apple - Java',
          totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
          stageRisks: [],
        },
        {
          applicationId: 'banana-java',
          applicationName: 'Banana - Java',
          totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
          stageRisks: [],
        },
      ],
    });

    expect(mapped.total).toBe(2);
  });

  it('skips blank facet ids from API facet maps', () => {
    const mapped = mapApplicationsListResponse({
      applications: [
        {
          applicationId: 'apple-java',
          applicationName: 'Apple - Java',
          organizationId: 'org-java',
          organizationName: 'Java-team',
          totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
          stageRisks: [],
        },
      ],
      facets: {
        totalApplications: 1,
        organizations: { '': 1, 'org-java': 1 },
      },
      total: 1,
    });

    expect(mapped.facets.organizations.map((entry) => entry.id)).toEqual(['org-java']);
  });

  it('uses server facet display names for org/app ids not on the current page', () => {
    const mapped = mapApplicationsListResponse({
      applications: [
        {
          applicationId: 'apple-java',
          applicationName: 'Apple - Java',
          organizationId: 'org-java',
          organizationName: 'Java-team',
          totalApplicationRisk: { totalRisk: 0, criticalRisk: 0, severeRisk: 0, moderateRisk: 0, lowRisk: 0 },
          stageRisks: [],
        },
      ],
      facets: {
        totalApplications: 2,
        organizations: {
          'org-java': 1,
          '04f82a7a0df844dca7038341b8321df2': 800,
        },
        organizationNames: {
          'org-java': 'Java-team',
          '04f82a7a0df844dca7038341b8321df2': 'AI Operations',
        },
        applications: {
          d1ec2c0e6c6848f98df0cdb889eeadae: 1,
        },
        applicationNames: {
          d1ec2c0e6c6848f98df0cdb889eeadae: 'Banana - Java',
        }
      },
      total: 2,
    });

    expect(mapped.facets.organizations).toEqual(
      expect.arrayContaining([
        { id: '04f82a7a0df844dca7038341b8321df2', label: 'AI Operations', count: 800 },
        { id: 'org-java', label: 'Java-team', count: 1 },
      ]),
    );
    expect(mapped.facets.applications).toEqual([
      { id: 'd1ec2c0e6c6848f98df0cdb889eeadae', label: 'Banana - Java', count: 1 },
    ]);
  });
});
