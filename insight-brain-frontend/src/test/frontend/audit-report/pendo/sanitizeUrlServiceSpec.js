/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import auditReportPendoModule from '../../../../main/frontend/audit-report/pendo/module';

describe('audit-report sanitizeUrlService', function () {
  var sanitizeUrlService, baseUrl;

  beforeEach(
    angular.mock.module(auditReportPendoModule.name, function ($provide) {
      $provide.service('BaseUrl', () => ({ get: () => baseUrl }));
    })
  );

  beforeEach(inject(function (_sanitizeUrlService_) {
    sanitizeUrlService = _sanitizeUrlService_;

    baseUrl = 'http://localhost:8070';
  }));

  it('removes the baseUrl', function () {
    expect(sanitizeUrlService.sanitize('http://localhost:8070/assets/audit-report/index.html')).toBe(
      '/assets/audit-report/index.html'
    );

    baseUrl = 'https://foobar.com/iq';

    expect(sanitizeUrlService.sanitize('https://foobar.com/iq/assets/audit-report/index.html')).toBe(
      '/assets/audit-report/index.html'
    );
  });

  it('removes the query parameter', function () {
    const url = 'http://localhost:8070/assets/audit-report/index.html?repositoryId=12345';

    expect(sanitizeUrlService.sanitize(url)).toBe('/assets/audit-report/index.html');
  });

  it("doesn't crash on an unexpected URL", function () {
    const url = 'http://localhost:8070/assets/audit-report/foo';

    expect(sanitizeUrlService.sanitize(url)).toBe('/assets/audit-report/foo');
  });

  it('passes through external URLs unchanged', function () {
    const url = 'http://links.sonatype.com/asdf';

    expect(sanitizeUrlService.sanitize(url)).toBe(url);
  });
});
