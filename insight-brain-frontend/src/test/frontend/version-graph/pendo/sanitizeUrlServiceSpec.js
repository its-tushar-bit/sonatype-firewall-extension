/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as urlUtil from 'MainRoot/util/urlUtil';

import SanitizeUrlService from 'MainRoot/version-graph/pendo/SanitizeUrlService';

describe('version-graph sanitizeUrlService', function () {
  let sanitizeUrlService;

  beforeEach(function () {
    urlUtil._setBaseUrlForTesting('http://localhost:8070');
  });

  beforeEach(function () {
    sanitizeUrlService = new SanitizeUrlService();
  });

  afterEach(function () {
    urlUtil.setBaseUrl();
  });

  it('removes the baseUrl', function () {
    expect(sanitizeUrlService.sanitize('http://localhost:8070/assets/version-graph/index.html')).toBe(
      '/assets/version-graph/index.html'
    );

    urlUtil._setBaseUrlForTesting('https://foobar.com/iq');

    expect(sanitizeUrlService.sanitize('https://foobar.com/iq/assets/version-graph/index.html')).toBe(
      '/assets/version-graph/index.html'
    );
  });

  it("doesn't crash on an unexpected URL", function () {
    const url = 'http://localhost:8070/assets/version-graph/foo';

    expect(sanitizeUrlService.sanitize(url)).toBe('/assets/version-graph/foo');
  });

  it('passes through external URLs unchanged', function () {
    const url = 'http://links.sonatype.com/asdf';

    expect(sanitizeUrlService.sanitize(url)).toBe(url);
  });
});
