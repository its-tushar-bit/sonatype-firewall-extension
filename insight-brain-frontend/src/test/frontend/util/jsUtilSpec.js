import {isNilOrEmpty, toURIParams, union} from '../../../main/frontend/util/jsUtil';

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

  describe('union', function() {
    it('returns an set empty set if both inputs are empty', function() {
      expect(union(new Set(), new Set())).toEqual(new Set());
    });

    it('returns a set equal to the first one if the second one is empty', function() {
      expect(union(new Set([1, 2, 3]), new Set())).toEqual(new Set([1, 2, 3]));
    });

    it('returns a set equal to the second one if the first one is empty', function() {
      expect(union(new Set(), new Set([1, 2, 3]))).toEqual(new Set([1, 2, 3]));
    });

    it('returns the union of the two sets', function() {
      expect(union(new Set([1, 2, 3]), new Set([5, 2, 4, 'a']))).toEqual(new Set([1, 2, 3, 4, 5, 'a']));
    });

    it('does not modify either input', function() {
      const set1 = new Set([1, 2, 3]),
          set2 = new Set([3, 4, 5, 6]);

      union(set1, set2);

      expect(set1).toEqual(new Set([1, 2, 3]));
      expect(set2).toEqual(new Set([3, 4, 5, 6]));
    });

    it('always returns a new set', function() {
      const set1 = new Set([1, 2, 3]),
          set2 = new Set(),
          result = union(set1, set2);

      expect(result).toEqual(set1);
      expect(result).not.toBe(set1);
    });
  });
});
