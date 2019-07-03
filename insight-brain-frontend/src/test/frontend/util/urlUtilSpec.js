import {getBaseUrl, toURIParams} from '../../../main/frontend/util/urlUtil';

describe('urlUtil', function() {
  describe('toURIParams', function() {
    it('encodes only defined parameters', function() {
      const params = {
        foo: null,
        'f o o': '?x=шеллы',
        baz: undefined,
        bar: '?x=test'
      };
      expect(toURIParams(params)).toEqual('f%20o%20o=%3Fx%3D%D1%88%D0%B5%D0%BB%D0%BB%D1%8B&bar=%3Fx%3Dtest');
    });
    it('handles empty object', function() {
      expect(toURIParams({})).toEqual('');
    });
  });

  describe('getBaseUrl', function() {
    it('returns the portion of the url before the \'/assets/\' token', function() {
      expect(getBaseUrl('foo/bar/assets/')).toBe('foo/bar');
    });

    it('returns the portion of the url before the \'/rest/report/\' token iff \'/assets/\' token is not present',
        function() {
          expect(getBaseUrl('foo/bar/rest/report/')).toBe('foo/bar');
          expect(getBaseUrl('foo/bar/rest/report/assets/')).toBe('foo/bar/rest/report');
          expect(getBaseUrl('foo/bar/assets/rest/report')).toBe('foo/bar');
        }
    );

    it('returns empty string if no tokens found', function() {
      expect(getBaseUrl('foo/bar/assetss/')).toBe('');
    });
  });
});
