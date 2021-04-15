/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import angularCookiesModuleName from 'angular-cookies';
import CLMLocationModule from '../util/CLMLocation';

function telemetryService($http, $cookies, CLMLocations) {
  function submitData(purpose, attributes, sync) {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', CLMLocations.getTelemetryUrl(), sync !== true);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.setRequestHeader(
      $http.defaults.xsrfHeaderName,
      $cookies.get($http.defaults.xsrfCookieName)
    );
    xhr.send(
      JSON.stringify({
        purpose,
        attributes,
        timestamp: new Date().getTime(),
      })
    );
  }

  return {
    submitData,
  };
}

telemetryService.$inject = ['$http', '$cookies', 'CLMLocations'];

export default angular
  .module('telemetryServiceModule', [
    CLMLocationModule.name,
    angularCookiesModuleName,
  ])
  .service('telemetryService', telemetryService);
