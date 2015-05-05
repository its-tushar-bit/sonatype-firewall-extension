/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */

(function() {
  'use strict';

  var module = angular.module('RoleModule', ['ui.router', 'SecurityModule', 'CLMLocation', 'ResourceModule'], [
    '$stateProvider', function($stateProvider) {
      $stateProvider.state('roles', {
        url: '/roles',
        controller: 'RoleListController',
        templateUrl: '../security-assets/role-list.html?' + clmBuildTimestamp,
        data: {
          title: 'Roles'
        },
        resolve: {
          'hasAdminPermission': [
            'PermissionService', function(PermissionService) {
              return PermissionService.isAuthorized(['ADMIN'], true);
            }
          ]
        }
      });
    }
  ]);

  module.service('RoleStore', [
    'CLMLocations', 'CLMResource', function(clmLocations, clmResource) {
      return clmResource.getStore({
        id: 'id',
        template: {
          id: null,
          name: '',
          description: '',
          permissions: []
        },
        url: clmLocations.getRoleListUrl()
      });
    }
  ]);

  module.controller('RoleListController', [
    'RoleStore', 'Messages', '$scope',
    '$modal', '$q', 'hasAdminPermission', function(RoleStore, messages, $scope, $modal, $q, hasAdminPermission) {
      $scope.doLoad = function() {
        if (hasAdminPermission) {
          $scope.error = null;

          RoleStore.refresh().then(function(results) {
            $scope.roles = results;
          }, function(error) {
            $scope.error = error;
          });
        }
      };
      $scope.roleClick = function() {
        //TODO: part of seperate task
      };
      $scope.isAuthorized = hasAdminPermission;

      $scope.doLoad();
    }
  ]);
}());
