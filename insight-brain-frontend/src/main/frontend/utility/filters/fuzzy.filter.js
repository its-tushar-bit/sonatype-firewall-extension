/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global Fuse*/
import { map, prop } from 'ramda';

function FuseFilter(input, term, searchField, resultField) {
  if (!input || !angular.isArray(input) || !term || !searchField) {
    return input;
  }
  const fuse = new Fuse(input, {
    keys: [searchField],
    threshold: 0.1,
    ignoreLocation: true,
  });
  // If provided, we need to extract `resultField` from the filtered items
  // fuse.js used to do this in older versions via its `id` config option but not anymore.
  const selectorFn = ({ item }) =>
    resultField ? prop(resultField, item) : item;
  return map(selectorFn, fuse.search(term));
}

export default function FuseFilterFactory() {
  return FuseFilter;
}
