/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { buildApplicationsListExportPayload } from 'MainRoot/nosc/applications/applicationsListExport';

describe('applicationsListExport (CLM-42226)', () => {
  it('maps sidebar filters to the Classic export payload without search', () => {
    const payload = buildApplicationsListExportPayload({
      stageIds: new Set(['build']),
      organizationIds: new Set(['org-java']),
      applicationIds: new Set(['apple-java']),
      threatLevelIds: new Set(['Critical']),
    }, '-lastEvaluationTime');

    expect(payload).toEqual({
      orderBy: '-lastEvaluationTime',
      stageIds: ['build'],
      organizationIds: ['org-java'],
      applicationIds: ['apple-java'],
      policyThreatLevelRange: {
        minPolicyThreatLevel: 8,
        maxPolicyThreatLevel: 10,
      },
    });
  });

  it('collapses multiple threat buckets into a single Classic export envelope', () => {
    const payload = buildApplicationsListExportPayload({
      stageIds: new Set(),
      organizationIds: new Set(),
      applicationIds: new Set(),
      threatLevelIds: new Set(['Critical', 'Low']),
    }, '-lastEvaluationTime');

    expect(payload.policyThreatLevelRange).toEqual({
      minPolicyThreatLevel: 1,
      maxPolicyThreatLevel: 10,
    });
  });

  it('omits optional keys when sidebar filters are empty', () => {
    const payload = buildApplicationsListExportPayload({
      stageIds: new Set(),
      organizationIds: new Set(),
      applicationIds: new Set(),
      threatLevelIds: new Set(),
    }, '-lastEvaluationTime');

    expect(payload).toEqual({ orderBy: '-lastEvaluationTime' });
  });

  it('passes ascending lastEvaluationTime orderBy through to the Classic export payload', () => {
    const payload = buildApplicationsListExportPayload({
      stageIds: new Set(),
      organizationIds: new Set(),
      applicationIds: new Set(),
      threatLevelIds: new Set(),
    }, 'lastEvaluationTime');

    expect(payload).toEqual({ orderBy: 'lastEvaluationTime' });
  });
});
