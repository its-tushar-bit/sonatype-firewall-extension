import {isNilOrEmpty, multiGroupBy, union} from '../../../main/frontend/util/jsUtil';

describe('jsUtil', function() {

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

  describe('multiGroupBy', function() {
    it('groups items by their multiple keys', function() {
      const data = [{
            foo: ['bar', 'baz'],
            a: 1
          }, {
            foo: ['baz', 'asdf'],
            a: 2
          }, {
            foo: ['asdf'],
            a: 3
          }],
          keyFn = item => item.foo,
          results = multiGroupBy(keyFn, data);

      expect(results.bar.length).toBe(1);
      expect(results.bar).toContain(data[0]);

      expect(results.baz.length).toBe(2);
      expect(results.baz).toContain(data[0]);
      expect(results.baz).toContain(data[1]);

      expect(results.asdf.length).toBe(2);
      expect(results.asdf).toContain(data[1]);
      expect(results.asdf).toContain(data[2]);
    });

    it('drops items that do not have any values for the key', function() {
      const data = [{
            foo: ['bar'],
            a: 1
          }, {
            foo: [],
            a: 2
          }],
          keyFn = item => item.foo,
          results = multiGroupBy(keyFn, data);

      expect(results).toEqual({ bar: [data[0]] });
    });

    it('returns an empty object if given an empty list', function() {
      const data = [],
          keyFn = item => item.foo,
          results = multiGroupBy(keyFn, data);

      expect(results).toEqual({});
    });
  });
});
