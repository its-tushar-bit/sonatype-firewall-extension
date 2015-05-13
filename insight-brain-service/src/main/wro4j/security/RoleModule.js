/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */

(function() {
  'use strict';

  var module = angular.module('RoleModule', ['ui.router', 'ui.router.state', 'BootstrapAddons', 'SecurityModule', 'CLMLocation', 'ResourceModule'], [
    '$stateProvider', function($stateProvider) {
      $stateProvider.state('roles', {
        url: '/roles',
        controller: 'RoleListController',
        templateUrl: '../security-assets/role-list.html?' + clmBuildTimestamp,
        data: {
          title: 'Roles',
          crumb: 'Roles'
        },
        resolve: {
          'isAuthorized': [
            'PermissionService', function(PermissionService) {
              return PermissionService.isAuthorized(['VIEW_ROLES'], true);
            }
          ]
        }
      }).state('roles.editor', {
        url: '/{roleId}',
        parent: 'roles',
        controller: 'RoleEditorController',
        templateUrl: '../security-assets/role-editor.html?' + clmBuildTimestamp,
        data: {
          title: 'Role Editor',
          crumb: 'Editor'
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
    '$modal', '$q', 'isAuthorized', function(RoleStore, messages, $scope, $modal, $q, isAuthorized) {
      $scope.doLoad = function() {
        if (isAuthorized) {
          $scope.error = null;

          RoleStore.refresh().then(function(results) {
            $scope.roles = results;
          }, function(error) {
            $scope.error = error;
          });
        }
      };
      $scope.isAuthorized = isAuthorized;

      $scope.doLoad();
    }
  ]);

  module.controller('RoleEditorController', [
    '$scope', '$stateParams', '$q', '$http', 'CLMLocations', 'RoleStore', 'isAuthorized',
    function($scope, $stateParams, $q, $http, CLMLocations, RoleStore, isAuthorized) {

      $scope.doLoad = function() {
        if (isAuthorized) {
          var promises = [
            RoleStore.get(),
            $http.get(CLMLocations.getRolePermissionUrl($stateParams.roleId))
          ];
          $q.all(promises).then(function(results) {
            var roles = results[0];
            var permissions = results[1].data;

            angular.forEach(roles, function(role) {
              if (role.id === $stateParams.roleId) {
                $scope.role = role;
              }
            });

            $scope.permissionCategories = permissions.permissionCategories;
          }, function (error) {
            $scope.error = error;
          });
        }
      };

      $scope.doLoad();
    }
  ]);
}());
