/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';
import {
  classicViolationHref,
  classicVulnerabilityHref,
  componentDisplayNameLabel,
  getMostRecentScanId,
  getSecurityVulnerabilityRefId,
  isSecurityPolicyCategory,
  tabFromViolationDetailStateName,
  violationDetailStateNameForTab,
  VIOLATION_DETAIL_TAB_IDS,
} from 'MainRoot/nosc/violations/detail/violationDetailUtils';

describe('violationDetailUtils', () => {
  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  it('exposes the supported tab ids in display order', () => {
    expect(VIOLATION_DETAIL_TAB_IDS).toEqual(['overview', 'vulnerability', 'waivers']);
  });

  it('detects security policy categories case-insensitively', () => {
    expect(isSecurityPolicyCategory('security')).toBe(true);
    expect(isSecurityPolicyCategory('Security')).toBe(true);
    expect(isSecurityPolicyCategory('SECURITY')).toBe(true);
    expect(isSecurityPolicyCategory('license')).toBe(false);
    expect(isSecurityPolicyCategory(undefined)).toBe(false);
  });

  it('maps child state names to tab ids with overview as the fallback', () => {
    expect(tabFromViolationDetailStateName('nexusOneViolationDetail.overview')).toBe('overview');
    expect(tabFromViolationDetailStateName('nexusOneViolationDetail.vulnerability')).toBe('vulnerability');
    expect(tabFromViolationDetailStateName('nexusOneViolationDetail.waivers')).toBe('waivers');
    expect(tabFromViolationDetailStateName('nexusOneViolationDetail.unknown')).toBe('overview');
    expect(tabFromViolationDetailStateName('otherState.waivers')).toBe('overview');
    expect(tabFromViolationDetailStateName(undefined)).toBe('overview');
  });

  it('maps tab ids to child state names under the violation detail parent', () => {
    expect(violationDetailStateNameForTab('overview')).toBe('nexusOneViolationDetail.overview');
    expect(violationDetailStateNameForTab('vulnerability')).toBe('nexusOneViolationDetail.vulnerability');
    expect(violationDetailStateNameForTab('waivers')).toBe('nexusOneViolationDetail.waivers');
  });

  it('builds Classic-bundle escape URLs for violation and vulnerability', () => {
    expect(classicViolationHref('violation-123')).toContain('/violation/violation-123');
    expect(classicViolationHref('violation-123')).toMatch(/\/assets\/index\.html#/);
    expect(classicVulnerabilityHref('CVE-2026-0001')).toContain('/vulnerabilities/CVE-2026-0001');
    expect(classicVulnerabilityHref('CVE-2026-0001')).toMatch(/\/assets\/index\.html#/);
  });

  it('concatenates display name parts (backend includes format-specific separators)', () => {
    // Maven format: backend interleaves " : " separator parts between field parts.
    // Concatenating preserves the backend's format-specific separators.
    expect(
      componentDisplayNameLabel({
        parts: [
          { field: 'Group', value: 'axis' },
          { value: ' : ' },
          { field: 'Artifact', value: 'axis' },
          { value: ' : ' },
          { field: 'Version', value: '1.2' },
        ],
      }),
    ).toBe('axis : axis : 1.2');
  });

  it('respects format-specific separators for NuGet (space separator)', () => {
    // NuGet format: backend uses " " as separator, not " : ".
    // Concatenating preserves the space separator correctly.
    expect(
      componentDisplayNameLabel({
        parts: [
          { field: 'packageId', value: 'Newtonsoft.Json' },
          { value: ' ' },
          { field: 'version', value: '12.0.3' },
        ],
      }),
    ).toBe('Newtonsoft.Json 12.0.3');
  });

  it('respects format-specific separators for Conda (slash separator)', () => {
    // Conda format: backend uses "/" and "." as separators.
    expect(
      componentDisplayNameLabel({
        parts: [
          { field: 'channel', value: 'conda-forge' },
          { value: '/' },
          { field: 'subdir', value: 'linux-64' },
          { value: '/' },
          { field: 'name', value: 'numpy' },
          { value: '-' },
          { field: 'version', value: '1.21.0' },
          { value: '.' },
          { field: 'build', value: 'py310h20f0308_0' },
          { value: '.tar.bz2' },
        ],
      }),
    ).toBe('conda-forge/linux-64/numpy-1.21.0.py310h20f0308_0.tar.bz2');
  });

  it('respects format-specific separators for RPM (dash and dot separators)', () => {
    // RPM format: backend uses "-" and "." as separators.
    expect(
      componentDisplayNameLabel({
        parts: [
          { field: 'name', value: 'bash' },
          { value: '-' },
          { field: 'version', value: '4.2.46' },
          { value: '.' },
          { field: 'release', value: '34.el7' },
          { value: '.' },
          { field: 'arch', value: 'x86_64' },
        ],
      }),
    ).toBe('bash-4.2.46.34.el7.x86_64');
  });

  it('handles display name parts without interleaved separators', () => {
    // Some test fixtures may use simplified parts without interleaved separators.
    // Concatenating still works but the output depends on what parts are present.
    expect(
      componentDisplayNameLabel({
        parts: [
          { field: 'group', value: 'com.example' },
          { field: 'name', value: 'demo-lib' },
          { field: 'version', value: '1.0.0' },
        ],
      }),
    ).toBe('com.exampledemo-lib1.0.0');
  });

  it('filters out empty parts before concatenating', () => {
    // Empty string parts (value: '') are filtered out via filter(Boolean).
    // This prevents edge cases where empty parts would produce extra separators.
    expect(
      componentDisplayNameLabel({
        parts: [
          { field: 'namespace', value: 'axis' },
          { field: 'name', value: '' },
          { field: 'version', value: '1.2' },
        ],
      }),
    ).toBe('axis1.2');
  });

  it('picks the most recent scan id from stage data', () => {
    expect(
      getMostRecentScanId({
        build: {
          mostRecentEvaluationTime: '2026-07-18T10:00:00Z',
          mostRecentScanId: 'scan-old',
        },
        stage: {
          mostRecentEvaluationTime: '2026-07-19T10:00:00Z',
          mostRecentScanId: 'scan-new',
        },
      }),
    ).toBe('scan-new');
  });

  it('compares mixed ISO-8601 formats by instant, not string order', () => {
    expect(
      getMostRecentScanId({
        build: {
          mostRecentEvaluationTime: '2026-07-19T10:00:00.000Z',
          mostRecentScanId: 'scan-ms',
        },
        stage: {
          mostRecentEvaluationTime: '2026-07-19T10:00:01Z',
          mostRecentScanId: 'scan-later',
        },
      }),
    ).toBe('scan-later');
  });

  it('reads the first SECURITY_VULNERABILITY_REFID from constraint reasons only', () => {
    expect(
      getSecurityVulnerabilityRefId({
        constraintViolations: [
          {
            constraintName: 'License',
            reasons: [{ reason: 'Banned license' }],
          },
          {
            constraintName: 'Security',
            reasons: [
              {
                reason: 'Known CVE',
                reference: { type: 'SECURITY_VULNERABILITY_REFID', value: 'CVE-2026-0001' },
              },
            ],
          },
        ],
      }),
    ).toBe('CVE-2026-0001');
    expect(
      getSecurityVulnerabilityRefId({
        constraintViolations: [
          {
            constraintName: 'License',
            reasons: [
              {
                reason: 'Other ref',
                reference: { type: 'OTHER', value: 'not-a-cve' },
              },
            ],
          },
        ],
      }),
    ).toBeNull();
    expect(getSecurityVulnerabilityRefId(undefined)).toBeNull();
  });
});
