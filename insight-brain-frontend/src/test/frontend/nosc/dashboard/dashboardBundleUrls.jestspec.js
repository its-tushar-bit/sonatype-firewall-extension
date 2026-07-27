/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  dashboardApiHref,
  dashboardEnterpriseReportingHref,
  dashboardLegalHref,
  dashboardOrgsAndPoliciesHref,
  dashboardSuccessMetricsHref,
  dashboardVulnerabilitiesHref,
} from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';

describe('dashboardBundleUrls deep dives (CLM-43206)', () => {
  beforeEach(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  afterEach(() => {
    setBaseUrl();
  });

  it('dashboardSuccessMetricsHref uses the clean embed path', () => {
    expect(dashboardSuccessMetricsHref()).toBe(
      'http://localhost/assets/nexus-one/index.html#/success-metrics',
    );
  });

  it('dashboardLegalHref uses the clean NOUX Legal embed path', () => {
    expect(dashboardLegalHref()).toBe('http://localhost/assets/nexus-one/index.html#/legal');
  });

  it('dashboardVulnerabilitiesHref uses the native NOUX vulnerabilities list', () => {
    expect(dashboardVulnerabilitiesHref()).toBe(
      'http://localhost/assets/nexus-one/index.html#/vulnerabilities',
    );
  });

  it('dashboardOrgsAndPoliciesHref uses the clean in-shell Orgs embed path', () => {
    expect(dashboardOrgsAndPoliciesHref()).toBe(
      'http://localhost/assets/nexus-one/index.html#/orgs-and-policies',
    );
  });

  it('dashboardEnterpriseReportingHref uses the clean in-shell Reports embed path', () => {
    expect(dashboardEnterpriseReportingHref()).toBe(
      'http://localhost/assets/nexus-one/index.html#/reports',
    );
  });

  it('dashboardApiHref uses the clean in-shell API embed path', () => {
    expect(dashboardApiHref()).toBe('http://localhost/assets/nexus-one/index.html#/api');
  });
});
