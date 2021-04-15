/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Remove stages which are not part of the filter
 */
export default function stageFilter() {
  return function (input, filter) {
    if (angular.isArray(input) && filter && filter.stageTypeFilters.length > 0) {
      for (var i = 0; i < input.length; i++) {
        if ($.inArray(input[i].id || input[i].stageTypeId, filter.stageTypeFilters) === -1) {
          input.splice(i, 1);
          --i;
        }
      }
    }
    return input;
  };
}
