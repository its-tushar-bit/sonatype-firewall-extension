/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function CurrentUserService($http, $q, clmLocations) {
  var deferred = $q.defer();
  $http.get(clmLocations.getSessionUrl()).then(function (response) {
    deferred.resolve(response.data);
  }, function (errorResponse) {
    deferred.reject(errorResponse.data);
  });
  return deferred.promise;
}

CurrentUserService.$inject = ['$http', '$q', 'CLMLocations'];
