/*
 * license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global Fuse*/
function FuseFilter(input, term, field) {
  if (!input || !angular.isArray(input) || !term || !field) {
    return input;
  }
  var fuse = new Fuse(input, {keys: [field], threshold: 0.1});

  return fuse.search(term);
}

export default function FuseFilterFactory() {
  return FuseFilter;
}
