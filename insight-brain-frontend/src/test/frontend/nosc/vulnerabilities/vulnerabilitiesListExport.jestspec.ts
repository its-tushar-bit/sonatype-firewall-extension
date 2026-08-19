/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { buildVulnerabilitiesExportPayload } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListExport';
import { createDefaultVulnerabilitiesFilterState } from 'MainRoot/nosc/vulnerabilities/vulnerabilitiesListApi';

describe('vulnerabilitiesListExport', () => {
  it('builds My Scan Data export payload without pagination', () => {
    expect(
      buildVulnerabilitiesExportPayload({
        search: 'log4j',
        orderBy: '-cvssScore',
        filters: {
          ...createDefaultVulnerabilitiesFilterState(),
          severities: new Set(['critical']),
          ecosystems: new Set(['maven']),
          cvssRange: [7, 10],
        },
      }),
    ).toEqual({
      tab: 'myScanData',
      orderBy: '-cvssScore',
      search: 'log4j',
      severities: ['critical'],
      ecosystems: ['maven'],
      minCvssScore: 7,
      maxCvssScore: 10,
    });
  });
});
