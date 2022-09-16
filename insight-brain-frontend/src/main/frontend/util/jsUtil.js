/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
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
export function setToArray(set) {
  if (set == null) {
    return set;
  }
  let array = [];
  set.forEach((v) => array.push(v));
  return array;
}

/**
 * String -> a -> b -> b
 * set the specified property
 */
export const propSet = curry((propName, value, target) => set(lensProp(propName), value, target));

/**
 * [String] -> a -> b -> b
 * Set nested property using path
 */
export const pathSet = curry((path, value, target) => set(lensPath(path), value, target));

/**
 * [String] -> a -> b -> b
 * Toggle nested property using path
 */
export const togglePath = curry((path, target) => over(lensPath(path), not, target));

/**
 * {k: v} -> k -> v | Undefined
 *
 * Calling this function with an object creates lookup function to get value by key:
 * const findPerson = lookup(personsMap);
 * const person = findPerson(key);
 */
export const lookup = flip(prop);

export const getDaysFromNow = (timestamp) => Math.floor((timestamp - Date.now()) / (1000 * 60 * 60 * 24));

export const getExpiryDate = (timestamp) => moment(timestamp).format('MMMM DD, yyyy');

export const isNilOrEmpty = either(isNil, isEmpty);

export const union = (set1, set2) => new Set(setToArray(set1).concat(setToArray(set2)));

/**
 * Like groupBy, but where the key function returns a list of strings instead of a single string, and items
 * are grouped according to each string in their list.
 */
export function multiGroupBy(keyFn, items) {
  // For a given item, returns a series of 2-val tuples holding each distinct key value for the item and the item
  // itself
  const pairsForItem = (item) => map((k) => [k, item], keyFn(item)),
    pairIterator = function (acc, [key, item]) {
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

  return transduce(chain(pairsForItem), pairIterator, {}, items);
}

// Return a string equivalent to the input but with the first letter uppercase
export function capitalize(str) {
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
 * @param {String} daysToAdd number of days to add to current date
 */
export function getFutureDate(daysToAdd = 0) {
  return moment().add(daysToAdd, 'days').endOf('day').format('YYYY-MM-DDTHH:mm:ss.SSSZZ');
}

/**
 * Returns an ISO date (with offset) created from a date with format YYYY-MM-DD
 * @param {String} date date created with the NxDateInput component (YYYY-MM-DD)
 */
export function getISODateFromDateInput(date) {
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

// Return a string only with first letter uppercase
export const capitalizeFirstLetter = (string) => {
  if (!string) {
    return string;
  } else {
    string = string.toLowerCase();
    return string.toLowerCase().charAt(0).toUpperCase() + string.slice(1);
  }
};

/**
 * Checks whether all elements in the array are equal
 * @param {*} arr - array with primitive values
 * @returns Boolean
 */
export const allEqual = (arr) => arr.every((val) => val === arr[0]);
