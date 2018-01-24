/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {curry, set, lensProp, lensPath, prop, flip} from 'ramda';

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
  set.forEach(v => array.push(v));
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
 * {k: v} -> k -> v | Undefined
 *
 * Calling this function with an object creates lookup function to get value by key:
 * const findPerson = lookup(personsMap);
 * const person = findPerson(key);
 */
export const lookup = flip(prop);
