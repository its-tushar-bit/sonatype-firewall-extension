/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var module = angular.module('PermissionServiceModule', ['CLMAppLocation']);

  module.service('PermissionService', [
    '$http', 'CLMAppLocations', '$q', function($http, CLMAppLocations, $q) {
      return {
        isContextAuthorized: function (permissions, ownerType, ownerId) {
          var deferred = $q.defer();

          $http.put(CLMAppLocations.getPermissionContextTestUrl(ownerType, ownerId), permissions).then(function(data) {
            deferred.resolve(permissions.length === data.data.length);
          }, function() {
            deferred.reject(arguments);
          });

          return deferred.promise;
        },
        isAuthorized: function(permissions, globalContext) {
          var deferred = $q.defer();

          $http.put(CLMAppLocations.getPermissionTestUrl(globalContext), permissions).then(function(data) {
            deferred.resolve(permissions.length === data.data.length);
          }, function() {
            deferred.reject(arguments);
          });

          return deferred.promise;
        },
        getValidPermissions: function(permissions, globalContext) {
          var deferred = $q.defer();

          $http.put(CLMAppLocations.getPermissionTestUrl(globalContext), permissions).then(function(data) {
            deferred.resolve(data.data);
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
                     '<div class="clm-alert alert alert-error">' +
                       '<p><strong>Error</strong></p>' +
                       '<p>It appears you do not have permission to access this page.  If you believe this to be incorrect please contact your administrator.</p>' +
                     '</div>' +
                   '</div>' +
                 '</div>',
      scope : {
        authed : '=authorizationWrapper'
      }
    };
  });
}());
