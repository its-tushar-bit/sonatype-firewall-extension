/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import CLMContextLocationModule, { getPermissionContextTestUrl } from './CLMContextLocation';
import { getProductFeaturesUrl } from '../util/CLMLocation';

var module = angular.module('PermissionServiceModule', [CLMContextLocationModule.name]);
export default module;

module.service('PermissionService', [
  '$http',
  'CLMContextLocations',
  '$q',
  function ($http, CLMContextLocations, $q) {
    return {
      isContextAuthorized: function (permissions, ownerType, ownerId) {
        const deferred = $q.defer();

        $http.put(getPermissionContextTestUrl(ownerType, ownerId), permissions).then(
          function (data) {
            deferred.resolve(permissions.length === data.data.length);
          },
          function () {
            deferred.reject(arguments);
          }
        );

        return deferred.promise;
      },

      isAuthorized: function (permissions, globalContext) {
        var deferred = $q.defer();

        $http.put(CLMContextLocations.getPermissionTestUrl(globalContext), permissions).then(
          function (data) {
            deferred.resolve(permissions.length === data.data.length);
          },
          function () {
            deferred.reject(arguments);
          }
        );

        return deferred.promise;
      },
      getValidPermissions: function (permissions, globalContext) {
        var deferred = $q.defer();

        $http.put(CLMContextLocations.getPermissionTestUrl(globalContext), permissions).then(
          function (data) {
            deferred.resolve(data.data);
          },
          function () {
            deferred.reject(arguments);
          }
        );

        return deferred.promise;
      },
      isAutomationFeatureEnabled: isAutomationFeatureEnabled,
    };
  },
]);

function isAutomationFeatureEnabled() {
  var promise = axios
    .get(getProductFeaturesUrl())
    .then((response) => response.data.includes('automation'))
    .catch(() => false);

  return promise;
}

module.directive('authorizationWrapper', function () {
  return {
    transclude: true,
    replace: true,
    template:
      '<div>' +
      '<div ng-if="authed" ng-transclude></div>' +
      '<div ng-if="!authed">' +
      '<div class="iq-alert iq-alert--error">' +
      '<strong>Error</strong> It appears you do not have permission to access this page.  If you ' +
      'believe this to be incorrect please contact your administrator.' +
      '</div>' +
      '</div>' +
      '</div>',
    scope: {
      authed: '=authorizationWrapper',
    },
  };
});
