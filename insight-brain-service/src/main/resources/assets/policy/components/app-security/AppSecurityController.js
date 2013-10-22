/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  var appSecurityModule = angular.module('ApplicationSecurityModule', ['CommonServices', 'ui.utils']);
  
  function getMembersForOwner(role, ownerId, exclude) {
    var arrToSearch = role.membersByOwner;
    if (arrToSearch == undefined) {
      arrToSearch = role;
    }
    var members = [];
    
    angular.forEach(arrToSearch, function(owner){
      if (!exclude && owner.ownerId === ownerId) {
        members = owner.members;
      } else if (exclude && owner.ownerId !== ownerId) {
        members = members.concat(owner.members);
      }
    });
    
    return members;
  }

  appSecurityModule.controller('AppSecurityController', ['$scope', '$http', 'CLMAppLocations', '$rootScope', function($scope, $http, clmAppLocations, $rootScope) {
    $scope.context = {
      roleEditMap: {},
      roles: []
    };
    
    $scope.doLoad = function() {
      $scope.error = null;
      
      $http.get(clmAppLocations.getRoleMappingUrl()).success(function (data) {
        $scope.context.roles = data.membersByRole;
      }).error(function (error) {
        $scope.error = arguments;
      });
    };

    $scope.editClick = function(role) {
      $scope.context.roleEditMap[role.roleId] = role;
    };
    
    $scope.getUserNames = function(role) {
      var valueList = [];
      angular.forEach(getMembersForOwner(role, clmAppLocations.getEntityId()), function(member){
        valueList.push(member.displayName);
      });
      AngularUtils.alphaSort(valueList);
      return valueList.join(', ');
    };
    
    $scope.getInheritedUserNames = function(role) {
      var valueList = [];
      angular.forEach(getMembersForOwner(role, clmAppLocations.getEntityId(), true), function(member){
        valueList.push(member.displayName);
      });
      AngularUtils.alphaSort(valueList);
      return valueList.join(', ');
    };
    
    $rootScope.$on('roleSaveComplete',function(event, roleId, newMappings){
      for (var i = 0 ; i < $scope.context.roles.length ; i++) {
        if ($scope.context.roles[i].roleId === roleId) {
          $scope.context.roles[i].membersByOwner[0].members = newMappings.members.slice();
          break;
        }
      }
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

  appSecurityModule.controller('AppSecurityEditorController', ['$scope', '$http', '$timeout', '$modal', 'CLMAppLocations', 'Messages', '$rootScope', function ($scope, $http, $timeout, $modal, clmAppLocations, Messages, $rootScope) {
    var filterTimeout = null;

    $scope.alerts = [];
    $scope.requestActive = 0;

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
          $rootScope.$broadcast('roleSaveComplete', $scope.roleId, $scope.mappings[0]);
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

    $scope.removeUser = function ($parentIndex, $index) {
      if ($parentIndex === 0)
        $scope.mappings[0].members.splice($index, 1);
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

    $scope.$watch('queryString', function (newVal) {
      if (!newVal) {
        $scope.queryResults = []; // Empty query, empty results
        return;
      }

      if (filterTimeout) {
        $timeout.cancel(filterTimeout);
      }

      $scope.lastQuery = newVal;
      filterTimeout = $timeout(function () {
        $scope.requestActive++;

        $http.get('../rest/user/query', {
          params : {
            q : newVal
          }
        }).success(function (data) {
          $scope.requestActive--;
          if ($scope.queryString === newVal || $scope.queryString.indexOf(newVal) === 0 && $scope.requestActive > 0) {
            $scope.queryResults = data;
          }
        }).error(function () {
          $scope.requestActive--;
          if ($scope.requestActive === 0) {
            $scope.queryResults = [];
          }
          $scope.filterError = Messages.getHttpErrorMessage(arguments);
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
          for (var m=0; m<mappings.length; m++) {
            for (var x=0; x<mappings[m].members.length; x++) {
              if (result[i].username === mappings[m].members[x].internalName) {
                result.splice(i, 1);
                i--;
                m = mappings.length;
                modified = true;
                break;
              }
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