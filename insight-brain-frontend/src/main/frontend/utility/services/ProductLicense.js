/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';

export default function ProductLicense($http, CLMLocations) {
  let promise = null;

  return {
    load() {
      if (!promise) {
        promise = $http.get(CLMLocations.getValidateLicenseUrl()).then(prop('data'));
      }

      return promise;
    },
  };
}

ProductLicense.$inject = ['$http', 'CLMLocations'];
