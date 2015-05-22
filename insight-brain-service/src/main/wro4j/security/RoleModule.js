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
    '$scope', '$stateParams', '$q', '$http', '$state', 'CLMLocations', 'RoleStore', 'Messages', 'isAuthorized',
    function($scope, $stateParams, $q, $http, $state, CLMLocations, RoleStore, Messages, isAuthorized) {
      $scope.errorFn = function(error) {
        $scope.submitActive = false;
        $scope.editorAlerts = [{
          type: 'error',
          msg: 'An error occurred while saving the Role. (' +
              Messages.getHttpErrorMessage(error) + ')'
        }];
      };

      $scope.editorAlerts = [];

      $scope.doLoad = function() {
        if (isAuthorized) {
          var request;

          if ($stateParams.roleId === '_new_') {
            request = $http.get(CLMLocations.getRoleForNewUrl());
          }
          else {
            request = $http.get(CLMLocations.getRoleByIdUrl($stateParams.roleId));
          }

          request.success(function (role) {
            $scope.role = role;
            $scope.dirtyRole = angular.copy(role);
          }).error(function () {
            $scope.error = arguments;
          });
        }
      };

      $scope.save = function () {
        $scope.submitActive = true;

        $http[$scope.role.id ? 'put' : 'post'](CLMLocations.getRoleListUrl(), $scope.dirtyRole).success(function () {
          RoleStore.refresh();
          $state.go('roles');
        }).error($scope.errorFn);
      };

      $scope.cancel = function () {
        $state.go('roles');
      };

      $scope.doLoad();
    }
  ]);

  module.controller('DeleteRoleController', ['$scope', '$state', '$stateParams', 'RoleStore', 'Dialog', 'Messages', function ($scope, $state, $stateParams, RoleStore, Dialog, Messages) {
    function error() {
      Dialog.open({
        title : 'Failed to Delete',
        body : Messages.getHttpErrorMessage(arguments),
        buttons :  [{
          name: 'Close',
          dismiss: true
        }]
      });
    }

    $scope.deleteRole = function () {
      Dialog.open({
        title : 'Delete Role',
        body : 'Are you sure you want to delete the Role <strong>' + $('<div/>').text($scope.role.name).html() +
               '</strong>?',
        buttons :  [{
          name: 'Cancel',
          type: 'cancel',
          dismiss: true
        }, {
          name: 'Delete',
          type: 'danger'
        }]
      }).result.then(function () {
        RoleStore.get().then(function (roles) {
          angular.forEach(roles, function (role) {
            if (role.id === $stateParams.roleId) {
              role.$delete().then(function () {
                $state.go('roles');
              }, function (error) {
                error(error);
              });
            }
          });
        }, error);
      });
    };
  }]);
}());
