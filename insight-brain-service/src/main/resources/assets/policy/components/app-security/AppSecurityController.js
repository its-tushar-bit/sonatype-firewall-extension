/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  var appSecurityModule = angular.module('ApplicationSecurityModule', function() {
  });

  appSecurityModule.service('RoleStore', ['CLMLocations', 'CLMResource', function(clmLocations, clmResource) {
    var config = {
      id: 'id',
      template: {
        id: null,
        name: ''
      },
      url: clmLocations.getRoleListUrl(),
      params: {
        timestamp: new Date().getTime()
      }
    }, store = clmResource.getStore(config);

    return store;
  }]);

  appSecurityModule.controller('AppSecurityController', ['$scope', 'RoleStore', function($scope, RoleStore) {
    $scope.context = {
      roleEditMap: {},
      roles: []
    };
    $scope.doLoad = function() {
      $scope.error = null;
      
      var testItem = RoleStore.create();
      testItem.id = '1';
      testItem.name = 'Role 1';
      
      $scope.context.roles.push(testItem);
      
      testItem = RoleStore.create();
      testItem.id = '2';
      testItem.name = 'Role 2';
      
      $scope.context.roles.push(testItem);

      //TODO: uncomment following code when server ready
      /*RoleStore.refresh().then(function(data) {
        $scope.context.roles = data;
      }, function(error) {
        $scope.error = error;
      });*/
    };
    
    $scope.editClick = function(role) {
      $scope.context.roleEditMap[role.id] = role;
      $scope.$broadcast('roleEditClick', {
        roleId: role.id
      });
    };

    $scope.doLoad();
  }]);

  appSecurityModule.controller('AppSecurityEditorController', [function() {
  }]);
  
  appSecurityModule.directive('roleItem', function() {
    return {
      restrict: 'A',
      templateUrl: 'role-item',
      scope: {
        role: '=',
        context: '='
      },
      controller: 'AppSecurityEditorController',
      link: function(scope, element, attrs) {
        // so data changes dont affect orig
        if (scope.role) {
          scope.role = scope.role.$clone();  
        }
      }
    };
  });

  appSecurityModule.directive('expandRoleOnEvent', function() {
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$on(attrs.expandRoleOnEvent, function(event, data) {
          $('#collapse' + data.roleId).collapse('show');
        });
      }
    };
  });
}());