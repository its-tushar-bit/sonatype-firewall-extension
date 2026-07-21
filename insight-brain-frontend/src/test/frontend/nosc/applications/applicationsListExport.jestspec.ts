/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { EMPTY_APPLICATIONS_LIST_FILTERS } from 'MainRoot/nosc/applications/applicationsListFilters';
import { buildApplicationsListExportPayload } from 'MainRoot/nosc/applications/applicationsListExport';

describe('applicationsListExport (CLM-42226)', () => {
  it('maps sidebar filters to the Classic export payload without search', () => {
    const payload = buildApplicationsListExportPayload({
      stageIds: new Set(['build']),
      organizationIds: new Set(['org-java']),
      applicationIds: new Set(['apple-java']),
      threatRange: [8, 10],
    }, '-lastEvaluationTime');

    expect(payload).toEqual({
      orderBy: '-TOTAL_RISK',
      stageIds: ['build'],
      organizationIds: ['org-java'],
      applicationIds: ['apple-java'],
      policyThreatLevelRange: {
        minPolicyThreatLevel: 8,
        maxPolicyThreatLevel: 10,
      },
    });
  });

  it('omits optional keys when sidebar filters are empty', () => {
    const payload = buildApplicationsListExportPayload(
      EMPTY_APPLICATIONS_LIST_FILTERS,
      '-lastEvaluationTime',
    );

    expect(payload).toEqual({ orderBy: '-TOTAL_RISK' });
  });

  it('maps Martha lastEvaluationTime sort to Classic TOTAL_RISK for PostgreSQL export', () => {
    const payload = buildApplicationsListExportPayload(
      EMPTY_APPLICATIONS_LIST_FILTERS,
      'lastEvaluationTime',
    );

    expect(payload).toEqual({ orderBy: 'TOTAL_RISK' });
  });
});
