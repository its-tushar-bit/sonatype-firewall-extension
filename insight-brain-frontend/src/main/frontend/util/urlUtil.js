/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  append,
  compose,
  filter,
  flatten,
  isEmpty,
  isNil,
  not,
  join,
  map,
  nth,
  reduceWhile,
  replace,
  toPairs,
  zip,
} from 'ramda';

const toNonNullPairs = compose(filter(compose(not, isNil, nth(1))), toPairs);
const pairToURIParam = compose(join('='), map(encodeURIComponent));
/**
 * {k: String} -> String
 * Converts object to URI params string omitting empty values
 */
export const toURIParams = compose(
  join('&'),
  map(pairToURIParam),
  map(([k, v]) => [k, join(',', [v])]),
  toNonNullPairs
);

export function getBaseUrl(url) {
  const segments = ['/assets/', '/rest/report/'];

  function reducer(acc, segment) {
    const idx = url.indexOf(segment);
    return idx === -1 ? acc : url.substring(0, idx);
  }

  return reduceWhile(isEmpty, reducer, '', segments);
}

// for testing only
export function _setBaseUrlForTesting(url) {
  BASE_URL = url;
}

// exported for testing only
export function setBaseUrl() {
  BASE_URL = getBaseUrl(window.location.href);
}

export let BASE_URL;
setBaseUrl();

const BUNDLE_ASSETS_PATH = {
  classic: '/assets/index.html',
  'nexus-one': '/assets/nexus-one/index.html',
};

function normalizeHashPath(path) {
  const trimmed = (path ?? '').trim();
  if (!trimmed || trimmed === '#') {
    return '/';
  }
  const withoutHash = trimmed.startsWith('#') ? trimmed.slice(1) : trimmed;
  return withoutHash.startsWith('/') ? withoutHash : `/${withoutHash}`;
}

/**
 * Absolute URL for the classic or Nexus One HTML bundle, with an optional in-hash route.
 * Uses {@link BASE_URL} / {@link getBaseUrl} so paths stay correct behind IQ context roots and proxies.
 *
 * @param {'classic' | 'nexus-one'} bundle
 * @param {string} [hashPath]
 * @returns {string}
 */
export function bundleIndexUrl(bundle, hashPath) {
  const root = BASE_URL || getBaseUrl(window.location?.href ?? '');
  const url = new URL(`${root}${BUNDLE_ASSETS_PATH[bundle]}`);
  if (hashPath != null && hashPath !== '') {
    url.hash = normalizeHashPath(hashPath);
  }
  return url.toString();
}

/**
 * This function is meant to be used in a tagged template literal, e.g. like this:
 * uriTemplate`/api/v2/vulnerabilities/${refId}`
 *
 * constructing such a tagged template will result in a string holding an absolute URL where the params
 * are properly escaped. That is, the resulting string will be the template, prepended with the value of BASE_URL,
 * and with all placeholder expression values escaped using the built-in `encodeURIComponent` function.
 *
 * For ease of use with long URIs, the template passed to this function can include whitespace, including newlines,
 * which will all be stripped out of the return value
 */
export function uriTemplate(strings, ...params) {
  const escapedParams = map(encodeURIComponent, params),
    whitespaceStrippedStrings = map(replace(/\s+/g, ''), strings),
    // a template like `${foo}/bar/${baz}` will result in strings being ['', '/bar/', ''] and
    // params being a 2-value array containing the values of the foo and baz variables. Thus
    // the strings array will always contain the first "part" as well as the last "part" - and accordingly
    // it will always be one entry longer than the parts array. The last part will thus be
    // droppedby `zip` so we have to add it back with `append`
    finalPart = whitespaceStrippedStrings[whitespaceStrippedStrings.length - 1],
    parts = append(finalPart, flatten(zip(whitespaceStrippedStrings, escapedParams)));

  return `${BASE_URL || ''}${join('', parts)}`;
}

// Defined as a function to easily mock in jest/jasmine unit tests and prevent redirections during those tests
export function logoutRedirection(toLocation) {
  if (toLocation != null) {
    window.location.href = toLocation;
  } else {
    window.location.assign('../');
  }
}
