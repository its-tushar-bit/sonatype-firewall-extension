/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { propEq } from 'ramda';

import CLMLocationModule from '../util/CLMLocation';

function defaultAdminPasswordChangedService($http, CLMLocations) {

  let promise = null;

  function doGet() {
    promise = $http.get(CLMLocations.getShouldDisplayDefaultPasswordWarning()).then(propEq('data', 'true'));
  }

  return {

    /**
     * @return a promise indicating whether the default account password is in need of changing according to the
     * backend. Note that this service only actually queries the backend once, and successive calls to this function
     * will return the same promise
     */
    shouldDisplayDefaultPasswordWarning() {
      if (promise === null) {
        doGet();
      }

      return promise;
    }
  };
}

defaultAdminPasswordChangedService.$inject = ['$http', 'CLMLocations'];

export default angular.module('defaultAdminPasswordChangedServiceModule', [CLMLocationModule.name])
    .service('defaultAdminPasswordChangedService', defaultAdminPasswordChangedService);
