/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  var appSecurityModule = angular.module('ApplicationSecurityModule', ['CommonServices']);

  appSecurityModule.service('RoleMappingStore', ['CLMAppLocations', 'CLMResource', function(clmAppLocations, clmResource) {
    var config = {
      id: 'id',
      template: {
        roleId: null,
        roleName: '',
        roleDescription: '',
        membersByOwner: []
      },
      url: clmAppLocations.getRoleMappingUrl(),
      params: {
        timestamp: new Date().getTime()
      }
    }, store = clmResource.getStore(config);    
    
    return store;
  }]);

  appSecurityModule.controller('AppSecurityController', ['$scope', 'RoleMappingStore', 'CLMAppLocations', function($scope, RoleMappingStore, clmAppLocations) {
    $scope.context = {
      roleEditMap: {},
      roles: []
    };
    $scope.doLoad = function() {
      $scope.error = null;
      
      for ( var i = 0 ; i < 10 ; i++ ) {
        var role = {
          roleId: '' + (i + 1),
          roleName: 'Role ' + (i + 1),
          roleDescription: 'This is a role, really it is',
          membersByOwner: [{
            ownerId: clmAppLocations.getEntityId(),
            ownerName: 'app or org name',
            ownerType: clmAppLocations.isApplication() ? 'application' : 'organization',
            members: []
          },{
            ownerId: 'id',
            ownerName: 'app or org name',
            ownerType: 'organization',
            members: []
          }]
        };
        
        for ( var j = 0 ; j < 100 ; j++ ) {
          var member = {
            type: '',
            internalName: 'Member ' + (j + 1) + '_' + (i + 1),
            displayName: 'Member ' + (j + 1) + '_' + (i + 1)
          };
          role.membersByOwner[j%2].members.push(member);
        }
        
        $scope.context.roles.push(role);
      }
      /*      
      RoleMappingStore.refresh().then(function(data) {
        $scope.context.roles = data.membersByRole;
      }, function(error) {
        $scope.error = error;
      });
      */
    };

    $scope.editClick = function(role) {
      $scope.context.roleEditMap[role.roleId] = role;
      $scope.$broadcast('roleEditClick', {
        roleId: role.id
      });
    };
    
    $scope.getUserNames = function(role) {
      var value = null;
      angular.forEach(role.membersByOwner, function(owner){
        if (owner.ownerId === clmAppLocations.getEntityId() ) {
          angular.forEach(owner.members, function(member){
            if (!value) {
              value = member.displayName;
            } else {
              value += ', ' + member.displayName;
            }
          });
        }
      });
      return value;
    };
    
    $scope.getInheritedUserNames = function(role) {
      var value = null;
      angular.forEach(role.membersByOwner, function(owner){
        if (owner.ownerId !== clmAppLocations.getEntityId() ) {
          angular.forEach(owner.members, function(member){
            if (!value) {
              value = member.displayName;
            } else {
              value += ', ' + member.displayName;
            }
          });
        }
      });
      return value;
    };

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

    $scope.buttons = [{
      name : 'Cancel',
      isDisabled : function () {
        return false;
      },
      click : function () {
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
      }
    }, {
      name : 'Save',
      type : 'primary',
      isDisabled : function () {
        return false;
      },
      click : function () {
        if ($scope.isDirty()) {
          return $http.put(clmAppLocations.getRoleMappingUrl($scope.roleId), $scope.mappings[0].members).success(function () {
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
      }
    }];

    $scope.addUser = function (user) {
      $scope.mappings[0].members.push({
        type : "USER",
        displayName : $scope.getRealname(user),
        internalName : user.username
      });
    };

    $scope.removeUser = function ($index) {
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
  }]);

  /**
   * Filter which removes users present in a second array.
   */
  appSecurityModule.filter('userNotIn', function () {
    return function (input, mappings) {
      var result = null,
          modified = false;

      if (angular.isArray(input) && angular.isArray(mappings)) {
        result = angular.copy(input);

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

  appSecurityModule.directive('clmButtons', [function () {
    return {
      scope : {
        buttons : '=clmButtons'
      },
      template : '<span ng-repeat="button in buttons"> <button type="button" class="btn" ng-disabled="isDisabled($index)"' +
          ' ng-class="{\'btn-danger\' : button.type == \'danger\',\'btn-primary\' : button.type == \'primary\'}"' +
          ' ng-click="buttonClick($index)">{{button.name}}</button></span>',
      link: function(scope, element, attrs) {
        var disabled = false;

        scope.buttonClick = function ($index) {
          function fn() {
            disabled = false;
          }

          var promise = scope.buttons[$index].click.apply(scope, arguments);
          if (promise) {
            disabled = true;
            promise.then(fn, fn);
          }
        };
        scope.isDisabled = function ($index) {
          return disabled || scope.buttons[$index].isDisabled();
        };
      }
    };
  }]);

  //TODO: need to rework this editor for latest server side changes, and list controller changes
  appSecurityModule.directive('appSecurityEditor', [function () {
    return {
      scope : {
        appSecurityEditor : '=appSecurityEditor',
        context : '=',
        currentRole : '=role',
        hide : '&'
      },
      controller : 'AppSecurityEditorController',
      templateUrl : 'appSecurityEditor',
      link : function (scope) {
        function filterMembers(filterFn) {
          var members = [];
          angular.forEach(scope.context.roles, function(role){
            if (filterFn(role)) {
              angular.forEach(role.membersByOwner, function(owner){
                angular.forEach(owner.members, function(member){
                  if (members.indexOf(member) < 0) {
                    members.push(member);
                  }
                });
              });
            }
          });
          return members;
        }
        scope.getMembers = function () {
          var members = filterMembers(function(role){
            if (scope.currentRole.roleId === role.roleId) {
              return true;
            }            
            return false;
          });
          
          return members;
        };
        
        scope.getInheritedMembers = function() {
          var members = filterMembers(function(role){
            if (scope.currentRole.roleId != role.roleId) {
              return true;
            }            
            return false;
          });
    
          return members;
        }
        
        scope.isDirty = function () {
          return false;//!angular.equals(scope.mappings, scope.appSecurityEditor);
        };

        scope.$watch('appSecurityEditor', function (newVal) {
          if (newVal) {
            // TODO uncomment
//          scope.users = angular.copy(newVal);
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