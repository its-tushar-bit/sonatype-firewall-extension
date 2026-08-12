/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  estateComponentDetailHref,
  estateComponentPathFromScan,
} from 'MainRoot/nosc/components/detail/estateComponentDetailHref';

describe('estateComponentDetailHref', () => {
  it('builds overview URL for a hash', () => {
    expect(estateComponentDetailHref('abc123')).toBe('#/components/abc123');
  });

  it('encodes hash and appends tab segment', () => {
    expect(estateComponentDetailHref('a/b', 'violations')).toBe('#/components/a%2Fb/violations');
  });

  it('omits overview segment when tab is overview', () => {
    expect(estateComponentDetailHref('abc123', 'overview')).toBe('#/components/abc123');
  });

  it('builds each non-overview tab segment', () => {
    expect(estateComponentDetailHref('h1', 'vulnerabilities')).toBe('#/components/h1/vulnerabilities');
    expect(estateComponentDetailHref('h1', 'violations')).toBe('#/components/h1/violations');
    expect(estateComponentDetailHref('h1', 'applications')).toBe('#/components/h1/applications');
  });

  it('appends optional path context query params', () => {
    expect(
      estateComponentDetailHref('abc123', 'applications', {
        organizationId: 'org-1',
        applicationId: 'app-1',
        reportId: 'report-1',
      })
    ).toBe(
      '#/components/abc123/applications?organizationId=org-1&applicationId=app-1&reportId=report-1'
    );
  });
});

describe('estateComponentPathFromScan', () => {
  it('returns undefined for scan-only context (Path pin would not stick)', () => {
    expect(estateComponentPathFromScan('scan-1')).toBeUndefined();
    expect(estateComponentPathFromScan(null)).toBeUndefined();
    expect(estateComponentPathFromScan('   ')).toBeUndefined();
  });

  it('returns undefined when org or app is missing', () => {
    expect(
      estateComponentPathFromScan('scan-1', {
        organizationId: 'org-1',
      })
    ).toBeUndefined();
    expect(
      estateComponentPathFromScan('scan-1', {
        applicationId: 'app-1',
      })
    ).toBeUndefined();
  });

  it('pins reportId only with both org and app internal ids', () => {
    expect(
      estateComponentPathFromScan('scan-1', {
        organizationId: 'org-1',
        applicationId: 'app-1',
      })
    ).toEqual({
      organizationId: 'org-1',
      applicationId: 'app-1',
      reportId: 'scan-1',
    });
  });

  it('trims whitespace-only extras via the pin gate', () => {
    expect(
      estateComponentPathFromScan('scan-1', {
        organizationId: '  ',
        applicationId: 'app-1',
      })
    ).toBeUndefined();
  });

  it('allows org+app pin without a scan id', () => {
    expect(
      estateComponentPathFromScan(undefined, {
        organizationId: 'org-1',
        applicationId: 'app-1',
      })
    ).toEqual({
      organizationId: 'org-1',
      applicationId: 'app-1',
      reportId: undefined,
    });
  });
});
