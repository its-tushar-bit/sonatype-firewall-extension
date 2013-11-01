/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  var appSecurityModule = angular.module('ApplicationSecurityModule', ['CommonServices', 'ui.utils']);

  appSecurityModule.controller('AppSecurityController', ['$scope', '$http', 'CLMAppLocations', function($scope, $http, clmAppLocations) {
    function getUserLists(role) {
      var lists = {
         applied : [],
         inherited : []
      };
      angular.forEach(role, function (mappings, index) {
        angular.forEach(mappings.members, function (user) {
          if (index === 0) {
            lists.applied.push(user.displayName);
          } else {
            lists.inherited.push(user.displayName);
          }
        });
      });
      AngularUtils.alphaSort(lists.applied);
      AngularUtils.alphaSort(lists.inherited);
      return lists;
    }
    function createUserLists() {
      $scope.context.roleUsers = {};
      angular.forEach($scope.context.roles, function (role) {
        $scope.context.roleUsers[role.roleId] = getUserLists(role.membersByOwner);
      });
    }

    $scope.doLoad = function() {
      $scope.error = null;

      $http.get(clmAppLocations.getRoleMappingUrl(), {
        params: {
          timestamp: new Date().getTime()
      }}).success(function (data) {
        $scope.context = {
          roleEditMap: {},
          roles:  data.membersByRole
        };

        createUserLists();
      }).error(function (error) {
        $scope.error = arguments;
      });
    };

    $scope.editClick = function(role) {
      $scope.context.roleEditMap[role.roleId] = role;
    };

    $scope.$on('roleSaveComplete',function(event, roleId, newMappings){
      for (var i = 0 ; i < $scope.context.roles.length ; i++) {
        if ($scope.context.roles[i].roleId === roleId) {
          $scope.context.roles[i].membersByOwner[0].members = newMappings.members.slice();
          createUserLists();
          break;
        }
      }
      event.preventDefault();
    });

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

  appSecurityModule.controller('AppSecurityEditorController', ['$scope', '$http', '$timeout', '$modal', 'CLMAppLocations', 'Messages', function ($scope, $http, $timeout, $modal, clmAppLocations, Messages) {
    var filterTimeout = null;

    $scope.alerts = [];
    $scope.requestActive = 0;
    $scope.queryString = '';

    $scope.cancel = function () {
      if ($scope.isDirty()) {
        $modal.open({
          backdrop : 'static',
          template :  '<div class="modal-header"><h3>Unsaved Changes</h3></div>' +
            '<div class="modal-body">The page may contain unsaved changes, continuing will discard them.</div>' +
            '<div class="modal-footer">' +
            '<button type="button" class="btn" ng-click="$dismiss(false)">Cancel</button>' +
            '<button type="button" class="btn btn-danger" ng-click="$close(true)">Continue</button>' +
            '</div>'
        }).result.then(function () {
          $scope.hide();
        });
      } else {
        $scope.hide();
      }
    };
    
    $scope.save = function () {
      if ($scope.isDirty()) {
        return $http.put(clmAppLocations.getRoleMappingUrl($scope.roleId), $scope.mappings[0].members).success(function () {
          $scope.$emit('roleSaveComplete', $scope.roleId, $scope.mappings[0]);
          $scope.hide();
        }).error(function () {
          $scope.alerts.push({
            type: 'error',
            msg: Messages.getHttpErrorMessage(arguments)
          });
        });
      } else {
        $scope.hide();
      }
    };

    $scope.addUser = function (user) {
      $scope.mappings[0].members.push({
        type : "USER",
        displayName : $scope.getRealname(user),
        internalName : user.username
      });
      AngularUtils.alphaSort($scope.mappings[0].members, false, 'displayName');
    };

    $scope.removeUser = function ($parentIndex, member) {
      if ($parentIndex === 0)
        for (var i=0; i<$scope.mappings[0].members.length; i++) {
          if (member === $scope.mappings[0].members[i]) {
            $scope.mappings[0].members.splice(i, 1);
            break;
          }
        }
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
      if (tip && user.realm) {
        tip += ' ';
      }
      if (user.realm) {
        tip += '(' + user.realm + ')';
      }
      return tip;
    };

    $scope.$watch('queryString', function (newVal) {
      if (!newVal) {
        $scope.queryResults = null; // Empty query, empty results
        return;
      }

      if (filterTimeout) {
        $timeout.cancel(filterTimeout);
      }

      $scope.lastQuery = newVal;
      filterTimeout = $timeout(function () {
        $scope.requestActive++;
        //clear the alerts
        $scope.alerts.length = 0;

        $http.get(clmAppLocations.getFindUsersUrl(), {
          params : {
            q : newVal
          }
        }).success(function (data) {
          $scope.requestActive--;
          if ($scope.queryString === newVal || $scope.queryString.indexOf(newVal) === 0) {
            $scope.queryResults = data;
          }
        }).error(function () {
          $scope.requestActive--;
          if ($scope.requestActive === 0) {
            $scope.queryResults = null;
          }
          $scope.alerts.push({
            type: 'error',
            msg: Messages.getHttpErrorMessage(arguments)
          });
        });
      }, 500);
    });
  }]);

  /**
   * Filter which removes users present in a second array.
   */
  appSecurityModule.filter('userNotIn', function () {
    return function (input, mappings) {
      var result = null,
          modified = false;

      if (angular.isArray(input) && angular.isArray(mappings)) {
        result = input.slice();

        for (var i=0; i<result.length; i++) {
          for (var x=0; x<mappings[0].members.length; x++) {
            if (result[i].username === mappings[0].members[x].internalName) {
              result.splice(i, 1);
              i--;
              modified = true;
              break;
            }
          }
        }
      }
      return modified ? result : input;
    };
  });

  appSecurityModule.directive('appSecurityEditor', ['CLMAppLocations', function (clmAppLocations) {
    return {
      scope : {
        appSecurityEditor : '=appSecurityEditor',
        roleId : '=',
        hide : '&'
      },
      controller : 'AppSecurityEditorController',
      templateUrl : 'appSecurityEditor',
      link : function (scope) {
        scope.isDirty = function () {
          return !angular.equals(scope.mappings, scope.appSecurityEditor);
        };

        scope.$watch('appSecurityEditor', function (newVal) {
          if (newVal) {
            scope.mappings = angular.copy(newVal);
          }
        });

        scope.$on('pageChangeStarted', function (e) {
          if (scope.isDirty()) {
            e.preventDefault();
          }
        });
      }
    };
  }]);
}());