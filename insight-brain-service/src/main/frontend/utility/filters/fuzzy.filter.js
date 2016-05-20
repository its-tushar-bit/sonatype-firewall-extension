/*
 * license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global Fuse*/
(function() {
  'use strict';

  function FuseFilter(input, term, field) {
    if (!input || !angular.isArray(input) || !term || !field) {
      return input;
    }
    var fuse = new Fuse(input, {keys: [field]});

    return fuse.search(term);
  }

  function FuseFilterFactory() {
    return FuseFilter;
  }

  angular.module('utility').filter('fuzzy', FuseFilterFactory);
}());
