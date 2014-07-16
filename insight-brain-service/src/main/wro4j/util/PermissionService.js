/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  angular.module('PermissionServiceModule', ['CLMAppLocation']).service('PermissionService', [
    '$http', 'CLMAppLocations', '$rootScope', '$q', function($http, CLMAppLocations, $rootScope, $q) {
      function isAuthorized(permissions, required, condition) {
        var deferred = $q.defer();
        if (!condition) {
          deferred.resolve();
          return deferred.promise;
        }
        else {
          $http.put(CLMAppLocations.getPermissionTestUrl(), permissions).then(function(data) {
            if (angular.equals(permissions, data.data)) {
              deferred.resolve();
            }
            else {
              if (required) {
                $rootScope.error = 'Insufficient Permissions';
              }
              deferred.reject();
            }
          }, function() {
            if (required) {
              $rootScope.error = 'Permission check failed';
            }
            deferred.reject(arguments);
          });

          return deferred.promise;
        }
      }

      return {
        isAuthorized: isAuthorized
      };
    }
  ]);
}());