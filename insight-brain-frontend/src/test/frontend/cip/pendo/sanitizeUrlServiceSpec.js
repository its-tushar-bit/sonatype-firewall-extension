/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import cipLoaderPendoModule from '../../../../main/frontend/cip/pendo/module';

describe('cip-loader sanitizeUrlService', function() {
  var sanitizeUrlService,
      baseUrl;

  beforeEach(angular.mock.module(cipLoaderPendoModule.name, function($provide) {
    $provide.service('BaseUrl', () => ({ get: () => baseUrl }));
  }));

  beforeEach(inject(function(_sanitizeUrlService_) {
    sanitizeUrlService = _sanitizeUrlService_;

    baseUrl = 'http://localhost:8070';
  }));

  it('removes the baseUrl', function() {
    expect(sanitizeUrlService.sanitize('http://localhost:8070/assets/foo'))
        .toBe('/assets/foo');

    baseUrl = 'https://foobar.com/iq';

    expect(sanitizeUrlService.sanitize('https://foobar.com/iq/assets/foo'))
        .toBe('/assets/foo');
  });

  it('passes through external URLs unchanged', function() {
    const url = 'http://links.sonatype.com/asdf';

    expect(sanitizeUrlService.sanitize(url)).toBe(url);
  });

  it('hashes the application id and report id', function() {
    const url = 'http://localhost:8070/rest/report/app1/1a2b3c4d5e6fedcba/index.html',
        expectedUrl = '/rest/report/172dd4a0366000604e2c4de41457aa1eb3093bb59ead22e0f1d472a2aaade094/' +
            'cd36bee75bcbfe9c171192c81aac9607619451cd21143ef7bb70a01e624e81ce/index.html';

    expect(sanitizeUrlService.sanitize(url)).toBe(expectedUrl);
  });
});
