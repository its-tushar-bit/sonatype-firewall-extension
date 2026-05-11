/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  any,
  chain,
  curry,
  either,
  flip,
  isEmpty,
  isNil,
  lensPath,
  lensProp,
  map,
  prop,
  set,
  transduce,
  over,
  not,
  addIndex,
  compose,
  symmetricDifference,
} from 'ramda';
import moment from 'moment';

/**
 * Convert Set to Array (in IE9 compatible way)
 * @param set Set
 * @returns {Array}. If provided set is null or undefined, returns null or undefined respectively.
 */
export function setToArray<T>(inputSet: Set<T> | null | undefined): T[] | null | undefined {
  if (inputSet == null || typeof (inputSet as Iterable<T>)[Symbol.iterator] !== 'function') {
    return inputSet as null | undefined;
  }
  const array: T[] = [];
  inputSet.forEach((v) => array.push(v));
  return array;
}

/**
 * String -> a -> b -> b
 * set the specified property
 */
export const propSet = curry(
  <T extends Record<string, unknown>>(propName: string, value: unknown, target: T): T =>
    set(lensProp<T>(propName), value as T[string], target)
);

/**
 * [String] -> a -> b -> b
 * Set nested property using path
 */
export const pathSet = curry(
  <T>(path: (string | number)[], value: unknown, target: T): T => set(lensPath(path), value, target)
);

/**
 * [String] -> a -> b -> b
 * Toggle nested property using path
 */
export const togglePath = curry(
  <T>(path: (string | number)[], target: T): T => over(lensPath(path), not, target)
);

/**
 * {k: v} -> k -> v | Undefined
 *
 * Calling this function with an object creates lookup function to get value by key:
 * const findPerson = lookup(personsMap);
 * const person = findPerson(key);
 */
export const lookup = flip(prop);

export const getDaysFromNow = (timestamp: number): number =>
  Math.floor((timestamp - Date.now()) / (1000 * 60 * 60 * 24));

export const getExpiryDate = (timestamp: number): string => moment(timestamp).format('MMMM DD, yyyy');

export const isNilOrEmpty = either(isNil, isEmpty);

export const union = <T>(set1: Set<T>, set2: Set<T>): Set<T> =>
  new Set((setToArray(set1) ?? []).concat(setToArray(set2) ?? []));

/**
 * Like groupBy, but where the key function returns a list of strings instead of a single string, and items
 * are grouped according to each string in their list.
 */
export function multiGroupBy<T>(keyFn: (item: T) => string[], items: T[]): Record<string, T[]> {
  // For a given item, returns a series of 2-val tuples holding each distinct key value for the item and the item
  // itself
  const pairsForItem = (item: T): [string, T][] => map((k: string) => [k, item] as [string, T], keyFn(item)),
    pairIterator = function (acc: Record<string, T[]>, [key, item]: [string, T]) {
      // for efficiency, mutably build up the accumulator.  This is alright since the construction of the
      // accumulator is internal to multiGroupBy
      const currentValAtKey = acc[key];
      if (currentValAtKey) {
        currentValAtKey.push(item);
      } else {
        acc[key] = [item];
      }

      return acc;
    };

  return transduce(chain(pairsForItem), pairIterator, {} as Record<string, T[]>, items);
}

// Return a string equivalent to the input but with the first letter uppercase
export function capitalize(str: string | null | undefined): string | null | undefined {
  if (!str) {
    return str;
  } else {
    return str.charAt(0).toUpperCase() + str.substring(1);
  }
}

/**
 * Returns an ISO date (with offset) created from the moment this function is called
 * and adding the number of daysToAdd.
 * Note that time portion of the String will always be end of day.
 */
export function getFutureDate(daysToAdd = 0): string {
  return moment().add(daysToAdd, 'days').endOf('day').format('YYYY-MM-DDTHH:mm:ss.SSSZZ');
}

/**
 * Returns an ISO date (with offset) created from a date with format YYYY-MM-DD
 */
export function getISODateFromDateInput(date: string): string {
  return moment(date).format('YYYY-MM-DDTHH:mm:ss.SSSZZ');
}

/**
 * Returns a mapping function that includes the index similar to Array.prototype.map
 */
export const mapIndexed = addIndex(map);

export const getKey = prop('key');

/***
 * [*] → [*] → Boolean
 *
 * returns true if two lists are equal by value.
 *
 * Note.
 * The Comparison does not account for the order of elements within each list.
 */
export const eqValues = compose(isEmpty, symmetricDifference);

/**
 * [a] → Boolean
 *
 * Iteration function with index similar to Array.prototype.some
 *
 * Returns true if at least one of the elements of the list match the predicate, false otherwise.
 */
export const anyIndexed = addIndex<unknown>(any as never);

// Return a string only with first letter uppercase
export const capitalizeFirstLetter = (string: string | null | undefined): string | null | undefined => {
  if (!string) {
    return string;
  } else {
    string = string.toLowerCase();
    return string.toLowerCase().charAt(0).toUpperCase() + string.slice(1);
  }
};

/**
 * Checks whether all elements in the array are equal
 */
export const allEqual = <T>(arr: T[]): boolean => arr.every((val) => val === arr[0]);

export function copyToClipboard(textToCopy: string): void {
  const selection = window.document.getSelection();
  const node = window.document.createElement('textarea');
  node.style.position = 'absolute';
  node.style.left = '-10000px';
  node.textContent = textToCopy;
  window.document.body.appendChild(node);
  selection?.removeAllRanges();
  node.select();
  window.document.execCommand('copy');
  selection?.removeAllRanges();
  window.document.body.removeChild(node);
}

interface SortConfiguration {
  columnName: string;
  dir: 'asc' | 'desc';
}

/**
 * Changes the multi-column criteria of a collection of objects
 * based on its index/position, taking the first element as the
 * highest priority and the last one as the least priority.
 *
 * If the column name passed as argument is not the highest priority, then
 * the refered column object in the collection will be shifted from the current
 * position to the top of the list, similarly in the way a priority queue works,
 * in the way the previous highest priority column will be shifted to the second,
 * the second to third and so on.
 *
 * If the refered column is already at the highest priority, then sort direction
 * will be changed from asc to desc or vice versa.
 *
 * If columnName is refering to an unexisting column in sortConfigurationCollection,
 * then a new object setting sort direction as `asc` by default.
 */
export const changeMultiColumnSortCriteria = (
  columnName: string,
  [...sortConfigurationCollection]: SortConfiguration[]
): SortConfiguration[] => {
  if (typeof columnName !== 'string') throw Error('columnName should be a string');
  if (!Array.isArray(sortConfigurationCollection))
    throw Error(
      'sortConfigurationCollection should be an array of objects like this: {columnName: string, dir: ("asc"|"desc")}'
    );
  const columnObjIndex = sortConfigurationCollection.findIndex(
    (currentColObj) => currentColObj.columnName === columnName
  );
  if (columnObjIndex === -1) {
    // the column object does not exist in sortConfigurationCollection, so its added into it
    sortConfigurationCollection = [{ columnName, dir: 'asc' }, ...sortConfigurationCollection];
  } else if (columnObjIndex === 0) {
    // the column is in the highest priority, so sort direction must change
    // falsy value  -> asc
    // asc          -> desc
    // desc         -> asc
    sortConfigurationCollection[columnObjIndex].dir =
      sortConfigurationCollection[columnObjIndex]?.dir === 'asc' ? 'desc' : 'asc';
  } else {
    // the column does exist in sortConfigurationCollection, so the priority must change
    sortConfigurationCollection = [
      sortConfigurationCollection.splice(columnObjIndex, 1)[0],
      ...sortConfigurationCollection,
    ];
  }
  return sortConfigurationCollection;
};

export const round = (num: number, decimalPlaces = 0): number => {
  const p = Math.pow(10, decimalPlaces);
  const n = num * p * (1 + Number.EPSILON);
  return Math.round(n) / p;
};
