import {isNilOrEmpty, toURIParams} from '../../../main/frontend/util/jsUtil';

describe('jsUtil', function() {
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

  describe('isNilOrEmpty', function() {
    it('returns true if the argument is null or undefined', function() {
      expect(isNilOrEmpty(null)).toBe(true);
      expect(isNilOrEmpty(undefined)).toBe(true);
    });

    it('returns true if the argument is an empty object or empty list', function() {
      expect(isNilOrEmpty({})).toBe(true);
      expect(isNilOrEmpty([])).toBe(true);
    });

    it('returns false if the argument is a non-empty object or non-empty list', function() {
      expect(isNilOrEmpty({ a: 1 })).toBe(false);
      expect(isNilOrEmpty(['foo'])).toBe(false);
    });
  });
});
