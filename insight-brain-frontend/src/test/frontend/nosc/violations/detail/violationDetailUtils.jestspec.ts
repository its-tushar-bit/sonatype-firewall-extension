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
    expect(classicViolationHref('violation-123')).toContain(
      '/sidebarView/violation/violation-123',
    );
    expect(classicViolationHref('violation-123')).toMatch(/\/assets\/index\.html#/);
    expect(classicVulnerabilityHref('CVE-2026-0001')).toContain('/vulnerabilities/CVE-2026-0001');
    expect(classicVulnerabilityHref('CVE-2026-0001')).toMatch(/\/assets\/index\.html#/);
  });

  it('joins multi-part display names with colons', () => {
    expect(
      componentDisplayNameLabel({
        parts: [
          { field: 'group', value: 'com.example' },
          { field: 'name', value: 'demo-lib' },
          { field: 'version', value: '1.0.0' },
        ],
      }),
    ).toBe('com.example:demo-lib:1.0.0');
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
});
