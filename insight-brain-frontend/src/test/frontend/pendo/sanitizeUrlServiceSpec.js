/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import pendoModule from '../../../main/frontend/pendo/module';
import ownerManagerModule from '../../../main/frontend/owner.manager/owner.manager.module';
import dashboardModule from '../../../main/frontend/dashboard/dashboard.module';
import labsModule from '../../../main/frontend/labs/module';

describe('sanitizeUrlService', function() {
  var sanitizeUrlService,
      baseUrl,
      consoleWarnSpy;

  beforeEach(angular.mock.module(pendoModule.name, ownerManagerModule.name, dashboardModule.name, labsModule.name,
      function($provide, $stateProvider) {
        $provide.service('BaseUrl', () => ({ get: () => baseUrl }));

        // create a route that includes both a query parameter from a parent route and a path parameter in the child
        // route.  This is a combination that doesn't currently exist in the app, which is why we need to mock it, but
        // which could come about in the future and which, if not handled carefully, could cause information leakage
        $stateProvider.state('sanitizeUrlServiceSpecMockRoute', {
          url: '?queryParam',
          abstract: true
        }).state('sanitizeUrlServiceSpecMockRoute.child', {
          url: '/sanitizeUrlServiceSpecMockRoute/{foo}'
        }).state('sanitizeUrlServiceSpecMockQueryParamsRoute', {
          url: '/sanitizeUrlServiceSpecMockQueryParamsRoute/{foo}?type&sidebarReference&sidebarId&bar'
        });
      }
  ));

  beforeEach(inject(function(_sanitizeUrlService_) {
    sanitizeUrlService = _sanitizeUrlService_;
    baseUrl = 'http://localhost:8070';
    consoleWarnSpy = spyOn(console, 'warn');
  }));

  it('removes the baseUrl', function() {
    expect(sanitizeUrlService.sanitize('http://localhost:8070/assets/index.html')).toBe('/assets/index.html');

    baseUrl = 'https://foobar.com/iq';

    expect(sanitizeUrlService.sanitize('https://foobar.com/iq/assets/index.html')).toBe('/assets/index.html');
  });

  it('replaces hash-route parameter values with their SHA-256 hashed values', function() {
    const url = 'http://localhost:8070/assets/index.html#/management/edit/organization/foo/licenseThreatGroup/bar',
        expectedOrgHash = '2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae',
        expectedLtgHash = 'fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9',
        expectedUrl =
            `/assets/index.html#/management/edit/organization/${expectedOrgHash}/licenseThreatGroup/${expectedLtgHash}`;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
  });

  it('replaces hash-route parameter values with their SHA-256 hashed values for routes using colon-syntax', function() {
    // success metrics URL parameters happen to be declared using an alternate syntax
    const url = 'http://localhost:8070/assets/index.html#/labs/successMetrics/foo',
        expectedReportHash = '2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae',
        expectedUrl =
            `/assets/index.html#/labs/successMetrics/${expectedReportHash}`;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
  });

  it('doesn\'t obfuscate ROOT_ORGANIZATION_ID', function() {
    const url = 'http://localhost:8070/assets/index.html#/management/edit/organization/ROOT_ORGANIZATION_ID/' +
            'licenseThreatGroup/bar',
        expectedLtgHash = 'fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9',
        expectedUrl = '/assets/index.html#/management/edit/organization/ROOT_ORGANIZATION_ID/licenseThreatGroup/' +
            expectedLtgHash;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
  });

  it('doesn\'t crash on an unexpected URL', function() {
    const url = 'http://localhost:8070/assets/index.html#/asdf';

    expect(sanitizeUrlService.sanitize(url)).toBe('/assets/index.html#/asdf');
  });

  it('includes valueless hash query parameters', function() {
    const url = 'http://localhost:8070/assets/index.html#/sanitizeUrlServiceSpecMockRoute/asdf?queryParam',
        expectedHash = 'f0e4c2f76c58916ec258f246851bea091d14d4247a2fc3e18694461b1816e13b',
        expectedUrl = `/assets/index.html#/sanitizeUrlServiceSpecMockRoute/${expectedHash}?queryParam`;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
    expect(consoleWarnSpy).not.toHaveBeenCalled();
  });

  it('obfuscates query parameters that need to be obfuscated', function() {
    const url =
            'http://localhost:8070/assets/index.html#/sanitizeUrlServiceSpecMockQueryParamsRoute/' +
            'asdf?sidebarId=thisisagreatsidebar',
        routeHash = 'f0e4c2f76c58916ec258f246851bea091d14d4247a2fc3e18694461b1816e13b',
        sidebarIdHash = '99c28420b8db9206bf8dcb10ff14dcbde8cc2b2160a9758b8eb9b695d05c1f50',
        expectedUrl =
            `/assets/index.html#/sanitizeUrlServiceSpecMockQueryParamsRoute/${routeHash}?sidebarId=${sidebarIdHash}`;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
    expect(consoleWarnSpy).not.toHaveBeenCalled();
  });

  it('does not obfuscate query parameters that don\'t need to be obfuscated', function() {
    const url =
            'http://localhost:8070/assets/index.html#/sanitizeUrlServiceSpecMockQueryParamsRoute/asdf?type=violation',
        expectedHash = 'f0e4c2f76c58916ec258f246851bea091d14d4247a2fc3e18694461b1816e13b',
        expectedUrl = `/assets/index.html#/sanitizeUrlServiceSpecMockQueryParamsRoute/${expectedHash}?type=violation`;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
    expect(consoleWarnSpy).not.toHaveBeenCalled();
  });

  it('does not obfuscate query parameters that are unknown and warns in the console', function() {
    const url = 'http://localhost:8070/assets/index.html#/sanitizeUrlServiceSpecMockQueryParamsRoute/asdf?bar=baz',
        expectedHash = 'f0e4c2f76c58916ec258f246851bea091d14d4247a2fc3e18694461b1816e13b',
        expectedUrl = `/assets/index.html#/sanitizeUrlServiceSpecMockQueryParamsRoute/${expectedHash}?bar=baz`;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
    expect(consoleWarnSpy)
        .toHaveBeenCalledWith('Possible unobfuscated query param bar=baz detected in sanitizeUrlService');
  });

  it('passes through external URLs unchanged', function() {
    const url = 'http://links.sonatype.com/asdf';

    expect(sanitizeUrlService.sanitize(url)).toBe(url);
  });

  it('replaces hash-route parameter values in fragment-id-only URLs', function() {
    const url = '#/management/edit/organization/foo/licenseThreatGroup/bar',
        expectedOrgHash = '2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae',
        expectedLtgHash = 'fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9',
        expectedUrl = `#/management/edit/organization/${expectedOrgHash}/licenseThreatGroup/${expectedLtgHash}`;

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
  });
});
