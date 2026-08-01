/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  nouxApplicationPoliciesHref,
  nouxApplicationReportHref,
  nouxApplicationSourceControlHref,
  nouxApplicationWaiversHref,
  nouxComingSoonHref,
  nouxHashHref,
  nouxReportsHref,
  nouxWaiversListHref,
} from 'MainRoot/nosc/routing/nouxNavigation';

describe('nouxNavigation', () => {
  it('prefixes in-app paths as nexus-one hash hrefs', () => {
    expect(nouxHashHref('/waivers')).toBe('#/waivers');
    expect(nouxHashHref('waivers')).toBe('#/waivers');
  });

  it('builds in-shell application report hrefs', () => {
    expect(
      nouxApplicationReportHref({ publicId: 'banana-java2', scanId: 'scan-1' }),
    ).toBe('#/applications/banana-java2/report/scan-1');
    expect(
      nouxApplicationReportHref({
        publicId: 'app/with/slashes',
        scanId: 'scan-1',
        componentHash: 'abc',
        tabId: 'overview',
      }),
    ).toBe(
      '#/applications/app%2Fwith%2Fslashes/report/scan-1?componentHash=abc&tabId=overview',
    );
  });

  it('builds Coming Soon and waivers list hrefs', () => {
    expect(nouxComingSoonHref('source-control')).toBe('#/coming-soon/source-control');
    expect(nouxComingSoonHref('reports')).toBe('#/coming-soon/reports');
    expect(nouxWaiversListHref()).toBe('#/waivers');
  });

  it('builds App Detail Quick Action embed hrefs', () => {
    expect(nouxApplicationPoliciesHref('banana-java2')).toBe(
      '#/management/view/application/banana-java2',
    );
    expect(nouxApplicationWaiversHref('banana-java2')).toBe('#/applications/banana-java2/waivers');
    expect(nouxApplicationSourceControlHref('banana-java2')).toBe(
      '#/management/edit/application/banana-java2/source-control',
    );
    expect(nouxReportsHref()).toBe('#/reports');
  });
});
