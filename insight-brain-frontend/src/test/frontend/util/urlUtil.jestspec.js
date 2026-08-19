/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  bundleIndexUrl,
  getBaseUrl,
  logoutRedirection,
  toURIParams,
  uriTemplate,
  setBaseUrl,
  _setBaseUrlForTesting,
} from '../../../main/frontend/util/urlUtil';

describe('urlUtil', function () {
  describe('toURIParams', function () {
    it('encodes only defined parameters', function () {
      const params = {
        foo: null,
        'f o o': '?x=шеллы',
        baz: undefined,
        bar: '?x=test',
      };
      expect(toURIParams(params)).toEqual('f%20o%20o=%3Fx%3D%D1%88%D0%B5%D0%BB%D0%BB%D1%8B&bar=%3Fx%3Dtest');
    });
    it('handles empty object', function () {
      expect(toURIParams({})).toEqual('');
    });
  });

  describe('getBaseUrl', function () {
    it("returns the portion of the url before the '/assets/' token", function () {
      expect(getBaseUrl('foo/bar/assets/')).toBe('foo/bar');
    });

    it("returns the portion of the url before the '/rest/report/' token iff '/assets/' token is not present", function () {
      expect(getBaseUrl('foo/bar/rest/report/')).toBe('foo/bar');
      expect(getBaseUrl('foo/bar/rest/report/assets/')).toBe('foo/bar/rest/report');
      expect(getBaseUrl('foo/bar/assets/rest/report')).toBe('foo/bar');
    });

    it('returns empty string if no tokens found', function () {
      expect(getBaseUrl('foo/bar/assetss/')).toBe('');
    });
  });

  describe('uriTemplate', function () {
    beforeEach(function () {
      _setBaseUrlForTesting('http://test-host:8888');
    });

    afterEach(function () {
      setBaseUrl();
    });

    it('prepends the value with the base URI when there are no interpolated params', function () {
      expect(uriTemplate`/api/myApi/`).toBe('http://test-host:8888/api/myApi/');
    });

    it('constructs a URI prepended with the base URI and with the interpolated parameters escaped', function () {
      const foo = 'bar/?=',
        baz = 'qwerty&%ＡＳＤＦ';

      expect(uriTemplate`/api/myApi/${foo}?stuff=${baz}`).toBe(
        'http://test-host:8888/api/myApi/bar%2F%3F%3D?stuff=qwerty%26%25%EF%BC%A1%EF%BC%B3%EF%BC%A4%EF%BC%A6'
      );

      expect(uriTemplate`/api/myApi/${foo}?stuff=${baz}extraendstuff`).toBe(
        'http://test-host:8888/api/myApi/bar%2F%3F%3D?stuff=qwerty' +
          '%26%25%EF%BC%A1%EF%BC%B3%EF%BC%A4%EF%BC%A6extraendstuff'
      );
    });

    it('strips whitespace out of the template', function () {
      const foo = 'FOOPARAM',
        baz = 'BAZPARAM',
        result = uriTemplate` /api/myApi/
            ${foo}/
            asdfa  asdfdfhefgd
            /

            ?baz=	${baz} 
          `;

      expect(result).toBe('http://test-host:8888/api/myApi/FOOPARAM/asdfaasdfdfhefgd/?baz=BAZPARAM');
    });
  });

  describe('bundleIndexUrl', function () {
    beforeEach(function () {
      _setBaseUrlForTesting('https://iq.example/iq');
    });

    afterEach(function () {
      setBaseUrl();
    });

    it('builds classic and nexus-one bundle URLs with optional hash routes', function () {
      expect(bundleIndexUrl('classic')).toBe('https://iq.example/iq/assets/index.html');
      expect(bundleIndexUrl('nexus-one', '/hello1')).toBe(
        'https://iq.example/iq/assets/nexus-one/index.html#/hello1'
      );
      expect(bundleIndexUrl('classic', '/dashboard')).toBe(
        'https://iq.example/iq/assets/index.html#/dashboard'
      );
    });

    it('normalizes hash fragments', function () {
      expect(bundleIndexUrl('nexus-one', '#/reports')).toBe(
        'https://iq.example/iq/assets/nexus-one/index.html#/reports'
      );
    });
  });

  describe('logoutRedirection', function () {
    let assignSpy;
    let originalLocation;

    beforeEach(function () {
      _setBaseUrlForTesting('http://localhost:8070');
      originalLocation = window.location;
      assignSpy = jest.fn();
      Object.defineProperty(window, 'location', {
        value: { ...originalLocation, assign: assignSpy, href: '' },
        writable: true,
        configurable: true,
      });
    });

    afterEach(function () {
      setBaseUrl();
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
        configurable: true,
      });
    });

    it('navigates to the IdP Location when present', function () {
      logoutRedirection('https://idp.example/v2/logout');
      expect(window.location.href).toBe('https://idp.example/v2/logout');
      expect(assignSpy).not.toHaveBeenCalled();
    });

    it('falls back to the IQ root, not a bundle-relative path, so Nexus One logout reaches sign-in', function () {
      logoutRedirection(undefined);
      expect(assignSpy).toHaveBeenCalledWith('http://localhost:8070/');
      expect(assignSpy).not.toHaveBeenCalledWith('../');
    });

    it('preserves a context-root IQ install', function () {
      _setBaseUrlForTesting('https://iq.example/iq');
      logoutRedirection(null);
      expect(assignSpy).toHaveBeenCalledWith('https://iq.example/iq/');
    });
  });
});
