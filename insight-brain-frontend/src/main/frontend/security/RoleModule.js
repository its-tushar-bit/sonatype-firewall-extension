/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */

import resourceModule from '../Resource';
import CLMLocationModule from '../util/CLMLocation';
import BootstrapAddonsModule from '../util/BootstrapAddonsModule';
import {SecurityModule} from './UserModule';

const module = angular.module('RoleModule', [
  'ui.router', 'ui.router.state', BootstrapAddonsModule.name, SecurityModule.name, CLMLocationModule.name,
  resourceModule.name
], [
  '$stateProvider', function($stateProvider) {
    $stateProvider.state('roles', {
      url: '/roles',
      controller: 'RoleListController',
      templateUrl: 'security/role-list.html?' + clmBuildTimestamp,
      data: {
        title: 'Roles',
        crumb: 'Roles'
      },
      resolve: {
        'rolePermissions': [
          'PermissionService', function(PermissionService) {
            return PermissionService.getValidPermissions(['VIEW_ROLES', 'EDIT_ROLES'], true)
                .then(function(validPermissions) {
                  return {
                    viewRoles: validPermissions.indexOf('VIEW_ROLES') >= 0,
                    editRoles: validPermissions.indexOf('EDIT_ROLES') >= 0
                  };
                });
          }
        ]
      }
    }).state('roles.editor', {
      url: '/{roleId}',
      controller: 'RoleEditorController',
      templateUrl: 'security/role-editor.html?' + clmBuildTimestamp,
      data: {
        title: 'Role Editor',
        crumb: 'Editor'
      }
    });
  }
]);

export default module;

module.service('RoleStore', [
  'CLMLocations', 'StoreFactory', function(clmLocations, StoreFactory) {
    return StoreFactory.getStore({
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
  '$q', '$state', 'rolePermissions', function(RoleStore, messages, $scope, $q, $state, rolePermissions) {
    $scope.doLoad = function() {
      if (rolePermissions.viewRoles) {
        $scope.error = null;

        RoleStore.refresh().then(function(results) {
          $scope.roles = results;
        }, function(error) {
          $scope.error = error;
        });
      }
    };
    $scope.newRole = function () {
      $state.go('roles.editor', { roleId: '_new_'});
    };
    $scope.readOnly = !rolePermissions.editRoles;
    $scope.isAuthorized = rolePermissions.viewRoles;

    $scope.doLoad();
  }
]);

module.controller('RoleEditorController', [
  '$scope', '$stateParams', '$q', '$http', '$state', 'CLMLocations', 'RoleStore', 'Messages', 'rolePermissions',
  function($scope, $stateParams, $q, $http, $state, CLMLocations, RoleStore, Messages, rolePermissions) {
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
      if (rolePermissions.viewRoles) {
        var request;

        if ($stateParams.roleId === '_new_') {
          request = $http.get(CLMLocations.getRoleForNewUrl());
        }
        else {
          request = $http.get(CLMLocations.getRoleByIdUrl($stateParams.roleId));
        }

        request.then(function (response) {
          var role = response.data;
          $scope.readOnly = !rolePermissions.editRoles || role.builtIn;
          $scope.role = role;
          $scope.dirtyRole = angular.copy(role);
        }, function (error) {
          $scope.error = error;
        });
      }
      else {
        $scope.error = 'You do not have permission to view this';
      }
    };

    $scope.save = function () {
      $scope.submitActive = true;

      $http[$scope.role.id ? 'put' : 'post'](CLMLocations.getRoleListUrl(), $scope.dirtyRole).then(function () {
        RoleStore.refresh();
        delete $scope.dirtyRole;
        $state.go('roles');
      }, function(error) {
        $scope.errorFn(error);
      });
    };

    $scope.cancel = function () {
      $state.go('roles');
    };

    $scope.isDirty = function() {
      return $scope.dirtyRole ? !angular.equals($scope.dirtyRole, $scope.role) : false;
    };

    $scope.$on('pageChangeStarted', function(e) {
      if ($scope.isDirty()) {
        e.preventDefault();
      }
    });

    $scope.doLoad();
  }
]);

module.controller('DeleteRoleController', ['$scope', '$state', '$stateParams', 'RoleStore', 'Dialog', 'Messages',
  function ($scope, $state, $stateParams, RoleStore, Dialog, Messages) {
    function error() {
      Dialog.open({
        title: 'Failed to Delete',
        body: Messages.getHttpErrorMessage(arguments),
        buttons: [{
          name: 'Close',
          dismiss: true
        }]
      });
    }

    $scope.deleteRole = function () {
      Dialog.open({
        title: 'Delete Role',
        body: 'Are you sure you want to delete the Role <strong>' + $('<div/>').text($scope.role.name).html() +
               '</strong>?',
        buttons: [{
          name: 'Delete',
          type: 'primary'
        }, {
          name: 'Cancel',
          type: 'cancel',
          dismiss: true
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
