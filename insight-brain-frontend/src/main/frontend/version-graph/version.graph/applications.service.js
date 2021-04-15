/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global Brain*/

export default function ApplicationsService($http, $q) {
  var deferred = null;
  return {
    get: function () {
      if (deferred === null) {
        deferred = $q.defer();
        $http.get(Brain.getIntegratorApplicationListUrl()).then(
          function (response) {
            deferred.resolve(response.data.applicationSummaries);
          },
          function (error) {
            deferred.reject(error);
            deferred = null; // all future requests to retrigger
          }
        );
      }
      return deferred.promise;
    },
  };
}

ApplicationsService.$inject = ['$http', '$q'];
