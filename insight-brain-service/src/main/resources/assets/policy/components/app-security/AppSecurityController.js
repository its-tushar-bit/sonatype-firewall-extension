/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  var appSecurityModule = angular.module('ApplicationSecurityModule', ['CommonServices']);

  appSecurityModule.service('RoleStore', ['CLMLocations', 'CLMResource', function(clmLocations, clmResource) {
    var config = {
      id: 'id',
      template: {
        id: null,
        name: '',
        users: []
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

      for ( var i = 0; i < 10; i++) {
        var testItem = RoleStore.create();
        testItem.id = '' + (i + 1);
        testItem.name = 'Role ' + testItem.id;

        for ( var j = 0; j < 10; j++) {
          testItem.users.push({
            name: 'user' + (j + 1),
            id: '' + (j + 1)
          });
        }
        $scope.context.roles.push(testItem);
      }

      // TODO: uncomment following code when server ready
      /*
       * RoleStore.refresh().then(function(data) { $scope.context.roles = data; }, function(error) { $scope.error =
       * error; });
       */
    };

    $scope.editClick = function(role) {
      $scope.context.roleEditMap[role.id] = role;
      $scope.$broadcast('roleEditClick', {
        roleId: role.id
      });
    };
    
    $scope.getUserNames = function(role) {
      var value = null;
      angular.forEach(role.users, function(user){
        if (!value) {
          value = user.name;
        } else {
          value += ', ' + user.name;
        }
      });
      return value;
    }

    $scope.doLoad();
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

  appSecurityModule.controller('AppSecurityEditorController', ['$scope', '$http', '$timeout', 'Messages', function ($scope, $http, $timeout, Messages) {
    var filterTimeout = null;

    $scope.$watch('queryString', function (newVal) {
      if (!newVal) {
        $scope.queryResults = []; // Empty query, empty results
        return;
      }

      if (filterTimeout) {
        $timeout.cancel(filterTimeout);
      }

      filterTimeout = $timeout(function () {
        $scope.requestActive = true;
        $scope.lastQuery = newVal;

        $http.get('../rest/user/query', {
          params : {
            q : newVal
          }
        }).success(function (data) {
          $scope.requestActive = false;
          $scope.queryResults = data;
        }).error(function () {
          $scope.requestActive = false;
          $scope.queryResults = [];
          $scope.filterError = Messages.getHttpErrorMessage(arguments);
        });
      }, 500);
    });

    $scope.addUser = function (user) {
      $scope.users.applied.push(user);
    };
    $scope.removeUser = function ($index) {
      $scope.users.applied.splice($index, 1);
    };

    $scope.getRealname = function (user) {
      if (!user) {
        return;
      }
      var name = '';
      if (user.firstName) {
        name += user.firstName + ' ';
      }
      if (user.lastName) {
        name += user.lastName;
      }
      return name.trim();
    };
    $scope.getTooltip = function (user) {
      var tip = $scope.getRealname(user);
      if (tip && user.email) {
        tip += ' ';
      }
      if (user.email) {
        tip += '<' + user.email + '>';
      }
      return tip;
    };
  }]);

  /**
   * Filter which removes users present in a second array.
   */
  appSecurityModule.filter('userNotIn', function () {
    return function (input, arg) {
      if (angular.isArray(input) && angular.isArray(arg)) {
        input = angular.copy(input);
        for (var i=0; i<input.length; i++) {
          for (var a=0; a<arg.length; a++) {
            if (angular.equals(input[i], arg[a])) {
              input.splice(i, 1);
              i--;
              break;
            }
          }
        }
      }
      return input;
    };
  });

  appSecurityModule.directive('appSecurityEditor', [function () {
    return {
      scope : {
        appSecurityEditor : '=appSecurityEditor',
        hide : '&'
      },
      controller : 'AppSecurityEditorController',
      templateUrl : 'appSecurityEditor',
      link : function (scope) {
        scope.$watch('appSecurityEditor', function (newVal) {
          // TODO uncomment
//          scope.users = angular.copy(newVal);
        });

        // TODO Remove mock data
        scope.users = {
          applied : [],
          inherited : [{
            firstName : 'Old',
            lastName : 'Man'
          }]
        };
      }
    };
  }]);
}());