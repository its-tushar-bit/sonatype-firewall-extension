/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Filter an array to ensure that null entries are always at the end.
 */
export default function emptyToEndFilter(extractColumn) {
  return function (array, key) {
    if (!angular.isArray(array)) {
      return;
    }
    // in the event of a compound sort, use the first field
    var sortField = angular.isArray(key) ? key[0] : key;
    var sortColumn = extractColumn(sortField);
    return array
      .filter(function (item) {
        return item[sortColumn];
      })
      .concat(
        array.filter(function (item) {
          return !item[sortColumn];
        })
      );
  };
}

emptyToEndFilter.$inject = ['extractColumn'];
