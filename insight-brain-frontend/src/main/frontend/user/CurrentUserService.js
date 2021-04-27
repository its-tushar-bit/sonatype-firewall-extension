/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function CurrentUserService($http, $q, clmLocations) {
  let deferred = $q.defer();

  return {
    fetch(waitForLogin = true) {
      // NOTE: When waiting for login, the http promise might remain unresolved forever if login is cancelled.  A
      // successive attempt to login again should result in a new call to `fetch` to get a fresh promise.

      // waitForLogin is passed in as a request configuration here so that HttpInterceptors can look for it
      // when deciding whether to show the login modal
      $http.get(clmLocations.getSessionUrl(), { waitForLogin }).then(
        function ({ data }) {
          deferred.resolve(data);
        },
        function (response) {
          // 401 means the user is not logged in (and waitForLogin was false), in which case do nothing.
          // Only report other errors
          if (response.status !== 401) {
            deferred.reject(response);
          }
        }
      );
    },

    waitForLogin() {
      return deferred.promise;
    },
  };
}

CurrentUserService.$inject = ['$http', '$q', 'CLMLocations'];
