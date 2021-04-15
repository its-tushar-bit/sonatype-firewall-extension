/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
export default function namePartFilter() {
  return function (input) {
    if (angular.isArray(input)) {
      var result = [];
      angular.forEach(input, function (part) {
        if (part.field) {
          result.push(part);
        }
      });
      return result;
    }
    return input;
  };
}
