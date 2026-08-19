/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  capitalize,
  eqValues,
  getFutureDate,
  isNilOrEmpty,
  multiGroupBy,
  union,
  allEqual,
  capitalizeFirstLetter,
} from 'MainRoot/util/jsUtil';

describe('jsUtil', function () {
  describe('isNilOrEmpty', function () {
    it('returns true if the argument is null or undefined', function () {
      expect(isNilOrEmpty(null)).toBe(true);
      expect(isNilOrEmpty(undefined)).toBe(true);
    });

    it('returns true if the argument is an empty object or empty list', function () {
      expect(isNilOrEmpty({})).toBe(true);
      expect(isNilOrEmpty([])).toBe(true);
    });

    it('returns false if the argument is a non-empty object or non-empty list', function () {
      expect(isNilOrEmpty({ a: 1 })).toBe(false);
      expect(isNilOrEmpty(['foo'])).toBe(false);
    });
  });

  describe('allEqual', () => {
    it('returns true if all values in the array are equal', () => {
      expect(allEqual([1, 1, 1, 1, 1, 1])).toBe(true);
      expect(allEqual([undefined, undefined, undefined])).toBe(true);
    });

    it('returns false if at least one value in the array is different', () => {
      expect(allEqual([1, 1, 1, 4, 1, 1])).toBe(false);
      expect(allEqual([undefined, 0, undefined])).toBe(false);
    });
  });

  describe('union', function () {
    it('returns an set empty set if both inputs are empty', function () {
      expect(union(new Set(), new Set())).toEqual(new Set());
    });

    it('returns a set equal to the first one if the second one is empty', function () {
      expect(union(new Set([1, 2, 3]), new Set())).toEqual(new Set([1, 2, 3]));
    });

    it('returns a set equal to the second one if the first one is empty', function () {
      expect(union(new Set(), new Set([1, 2, 3]))).toEqual(new Set([1, 2, 3]));
    });

    it('returns the union of the two sets', function () {
      expect(union(new Set([1, 2, 3]), new Set([5, 2, 4, 'a']))).toEqual(new Set([1, 2, 3, 4, 5, 'a']));
    });

    it('does not modify either input', function () {
      const set1 = new Set([1, 2, 3]),
        set2 = new Set([3, 4, 5, 6]);

      union(set1, set2);

      expect(set1).toEqual(new Set([1, 2, 3]));
      expect(set2).toEqual(new Set([3, 4, 5, 6]));
    });

    it('always returns a new set', function () {
      const set1 = new Set([1, 2, 3]),
        set2 = new Set(),
        result = union(set1, set2);

      expect(result).toEqual(set1);
      expect(result).not.toBe(set1);
    });
  });

  describe('multiGroupBy', function () {
    it('groups items by their multiple keys', function () {
      const data = [
          {
            foo: ['bar', 'baz'],
            a: 1,
          },
          {
            foo: ['baz', 'asdf'],
            a: 2,
          },
          {
            foo: ['asdf'],
            a: 3,
          },
        ],
        keyFn = (item) => item.foo,
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

    it('drops items that do not have any values for the key', function () {
      const data = [
          {
            foo: ['bar'],
            a: 1,
          },
          {
            foo: [],
            a: 2,
          },
        ],
        keyFn = (item) => item.foo,
        results = multiGroupBy(keyFn, data);

      expect(results).toEqual({ bar: [data[0]] });
    });

    it('returns an empty object if given an empty list', function () {
      const data = [],
        keyFn = (item) => item.foo,
        results = multiGroupBy(keyFn, data);

      expect(results).toEqual({});
    });
  });

  describe('capitalize', function () {
    it('returns falsey values as-is', function () {
      expect(capitalize(null)).toBe(null);
      expect(capitalize(undefined)).toBe(undefined);
      expect(capitalize('')).toBe('');
    });

    it('uppercases the first letter of the string', function () {
      expect(capitalize('foo')).toBe('Foo');
    });

    it('leaves already-capital letters alone', function () {
      expect(capitalize('Foo')).toBe('Foo');
      expect(capitalize('FOO')).toBe('FOO');
    });
  });

  describe('getFutureDate', function () {
    const assertEndOfDayTime = (dateString) => {
      expect(dateString.indexOf('23:59:59')).not.toEqual(-1);
    };

    const assertFormat = (dateString) => {
      const dateFormatRegex = /\d{4}-[0-1]\d-[0-3]\dT[0-2][0-3]:[0-5]\d:[0-5]\d\.\d{3}(-|\+)\d{4}/;
      expect(dateFormatRegex.test(dateString)).toBe(true);
    };

    const assertToday = (dateString) => {
      const today = new Date();
      const zeroPaddedMonth = `0${today.getMonth() + 1}`.slice(-2);
      const zeroPaddedDay = `0${today.getDate()}`.slice(-2);
      const timeStamp = `${today.getFullYear()}-${zeroPaddedMonth}-${zeroPaddedDay}`;
      expect(dateString.indexOf(timeStamp)).not.toEqual(-1);
    };

    it('returns the End of Day timestamp for today if no param or 0 is provided', function () {
      assertToday(getFutureDate());
      assertToday(getFutureDate(0));
    });

    it('includes End of Day time', function () {
      assertEndOfDayTime(getFutureDate(1));
      assertEndOfDayTime(getFutureDate(0));
      assertEndOfDayTime(getFutureDate(30));
      assertEndOfDayTime(getFutureDate(120));
    });

    it('follows the expected format', function () {
      // 2020-10-23T23:59:59.999-0500' - example date
      assertFormat(getFutureDate(0));
      assertFormat(getFutureDate(1));
      assertFormat(getFutureDate(7));
      assertFormat(getFutureDate(30));
      assertFormat(getFutureDate(90));
      assertFormat(getFutureDate(120));
    });
  });

  describe('eqValues', () => {
    it('returns true for empty arrays', () => {
      expect(eqValues([], [])).toBe(true);
    });

    it('returns false for non-matching arrays', () => {
      expect(eqValues([''], ['content'])).toBe(false);
    });

    it('returns true for matching arrays', () => {
      expect(eqValues(['content'], ['content'])).toBe(true);
    });

    it('returns true for matching arrays where element order is different', () => {
      expect(eqValues([1, 2], [2, 1])).toBe(true);
    });
  });

  describe('capitalizeFirstLetter', function () {
    it('uppercases the first letter of the string', function () {
      expect(capitalizeFirstLetter('boo')).toBe('Boo');
      expect(capitalizeFirstLetter('Boo')).toBe('Boo');
      expect(capitalizeFirstLetter('BOO')).toBe('Boo');
    });
  });
});
