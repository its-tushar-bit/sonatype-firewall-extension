/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Copied from our AngularCommon library
export default function agoLastDayFilter() {
  return function (agoString) {
    if (
      agoString.indexOf('seconds ago') > -1 ||
      agoString.indexOf('minute') > -1 ||
      agoString.indexOf('hour') > -1
    ) {
      return 'Less than a day ago';
    }
    return agoString;
  };
}
