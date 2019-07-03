/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {compose, filter, isEmpty, isNil, join, map, not, nth, reduceWhile, toPairs} from 'ramda';

const toNonNullPairs = compose(filter(compose(not, isNil, nth(1))), toPairs);
const pairToURIParam = compose(join('='), map(encodeURIComponent));
/**
 * {k: String} -> String
 * Converts object to URI params string omitting empty values
 */
export const toURIParams = compose(join('&'), map(pairToURIParam), toNonNullPairs);

export function getBaseUrl(url) {
  const segments = ['/assets/', '/rest/report/'];

  function reducer(acc, segment) {
    const idx = url.indexOf(segment);
    return idx === -1 ? acc : url.substring(0, idx);
  }

  return reduceWhile(isEmpty, reducer, '', segments);
}
