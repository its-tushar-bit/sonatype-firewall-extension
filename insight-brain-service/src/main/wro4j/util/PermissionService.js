/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var module = angular.module('PermissionServiceModule', ['CLMAppLocation']);

  module.service('PermissionService', [
    '$http', 'CLMAppLocations', '$rootScope', '$q', function($http, CLMAppLocations, $rootScope, $q) {

      return {
        isAuthorized : function (permissions, globalContext) {
          var deferred = $q.defer();

          $http.put(CLMAppLocations.getPermissionTestUrl(globalContext), permissions).then(function(data) {
            deferred.resolve(angular.equals(permissions, data.data));
          }, function() {
            deferred.reject(arguments);
          });

          return deferred.promise;
        }
      };
    }
  ]);

  module.directive('authorizationWrapper', function () {
    return {
      transclude : true,
      replace : true,
      template : '<div>' +
                   '<div ng-if="authed" ng-transclude></div>' +
                   '<div ng-if="!authed" class="container">' +
                     '<div class="alert alert-error clm-error">' +
                       '<p><strong>Error</strong></p>' +
                       '<p>It appears you do not have permission to access this page.  If you believe this to be incorrect please contact your CLM administrator.</p>' +
                     '</div>' +
                   '</div>' +
                 '</div>',
      scope : {
        authed : '=authorizationWrapper'
      }
    };
  });
}());