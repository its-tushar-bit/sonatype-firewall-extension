/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { isPurgedReportLoadError } from 'MainRoot/applicationReport/reportLoadErrors';

describe('isPurgedReportLoadError', () => {
  it('detects the retention-purge NotFoundException message', () => {
    expect(
      isPurgedReportLoadError(
        'The report for application ID a and scan ID s does not exist. Usually this means the report was ' +
          'deemed obsolete according to the data retention policies and hence purged to the trash.'
      )
    ).toBe(true);
  });

  it('returns false for transient or unrelated errors', () => {
    expect(isPurgedReportLoadError('Server Error')).toBe(false);
    expect(isPurgedReportLoadError('Unable to reach Sonatype IQ Server')).toBe(false);
    expect(isPurgedReportLoadError(null)).toBe(false);
    expect(isPurgedReportLoadError(undefined)).toBe(false);
  });
});
