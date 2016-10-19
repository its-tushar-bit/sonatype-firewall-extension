/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular */
(function() {
  'use strict';

  var groupings = [{
    type: 'GROUP',
    header: 'GROUPS',
    icon: 'group'
  },{
    type: 'USER',
    header: 'USERS',
    icon: 'user'
  }];
  var showGroupings = function(grouping, mappings) {
    if (!mappings) {
      return false;
    }
    for (var i = 0; i < mappings.length; i++) {
      var mapping = mappings[i];
      // Mapping can either have an array of members in a members property or be an array of members itself
      if (mapping.members) {
        for (var j = 0; j < mapping.members.length; j++) {
          var member = mapping.members[j];
          if (member.type === grouping.type) {
            return true;
          }
        }
      } else {
        if (mapping.type === grouping.type) {
          return true;
        }
      }
    }
    return false;
  };

  var appSecurityModule = angular.module('ApplicationSecurityModule', ['CommonServices', 'ui.utils', 'ngSanitize']);

  appSecurityModule.controller('AppSecurityController', ['$scope', '$http', 'CLMAppLocations', 'isAuthorized', function($scope, $http, clmAppLocations, isAuthorized) {
    $scope.groupings = groupings;
    $scope.showGrouping = showGroupings;
    $scope.isAuthorized = isAuthorized;

    $scope.doLoad = function() {
      if (isAuthorized) {
        $scope.error = null;

        $http.get(clmAppLocations.getRoleMappingUrl()).then(function(response) {
          var data = response.data;
          $scope.context = {
            roles: data.membersByRole,
            ldapRealm: data.ldapRealm,
            groupSearchEnabled: data.groupSearchEnabled
          };
        }, function(error) {
          $scope.error = error;
        });
      }
    };

    $scope.$on('roleSaveComplete',function(event, roleId, newMappings){
      for (var i = 0 ; i < $scope.context.roles.length ; i++) {
        if ($scope.context.roles[i].roleId === roleId) {
          $scope.context.roles[i].membersByOwner[0].members = newMappings.members.slice();
          break;
        }
      }
      event.preventDefault();
    });

    $scope.doLoad();
  }]);

  appSecurityModule.controller('AppSecurityEditorController', ['$scope', '$http', '$timeout', 'Dialog', 'CLMAppLocations', 'Messages', function ($scope, $http, $timeout, Dialog, clmAppLocations, Messages) {
    $scope.alerts = [];

    $scope.groupings = groupings;
    $scope.showGroupingHeader = showGroupings;

    $scope.cancel = function () {
      if ($scope.isDirty()) {
        Dialog.open({
          title: 'Unsaved Changes',
          body: 'This role may contain unsaved changes, continuing will discard them.',
          buttons: [{
            name: 'Continue',
            type: 'primary',
            click: function() {
              $scope.hide();
            }
          }, {
            name: 'Cancel',
            type: 'cancel'
          }]
        });
      } else {
        $scope.hide();
      }
    };

    $scope.save = function () {
      if ($scope.isDirty()) {
        return $http.put(clmAppLocations.getRoleMappingUrl($scope.roleId), $scope.mappings[0].members).then(function () {
          $scope.$emit('roleSaveComplete', $scope.roleId, $scope.mappings[0]);
          $scope.hide();
        }, function (error) {
          $scope.alerts.push({
            type: 'error',
            msg: Messages.getHttpErrorMessage(error)
          });
        });
      } else {
        $scope.hide();
      }
    };

    $scope.addMember = function (member) {
      $scope.mappings[0].members.push({
        type : member.type,
        displayName : member.displayName,
        internalName : member.internalName,
        email : member.email,
        realm : member.realm
      });
    };

    $scope.isDuplicate = function (internalName, realm, type) {
      if (internalName) {
        var nameregex = new RegExp('^' + internalName + '$', 'i');
        for (var i=0; i<$scope.mappings[0].members.length; i++) {
          if (nameregex.test($scope.mappings[0].members[i].internalName) &&
                  $scope.mappings[0].members[i].realm === realm &&
                  $scope.mappings[0].members[i].type === type) {
            return true;
          }
        }
      }
      return false;
    };

    $scope.addGroup = function () {
      if (!$scope.isDuplicate($scope.queryString, $scope.ldapRealm, 'GROUP')) {
        $scope.mappings[0].members.push({
          type : 'GROUP',
          displayName : $scope.queryString,
          internalName : $scope.queryString,
          email : null,
          realm : $scope.ldapRealm
        });
        $scope.queryString = '';
      }
    };

    $scope.removeMember = function ($parentIndex, member) {
      if ($parentIndex === 0) {
        for (var i=0; i<$scope.mappings[0].members.length; i++) {
          if (member === $scope.mappings[0].members[i]) {
            $scope.mappings[0].members.splice(i, 1);
            break;
          }
        }
      }
    };

    $scope.setResults = function (members, error) {
      $scope.queryResults = members;

      if (error) {
        $scope.alerts.push({
          type: 'error',
          msg: error
        });
      }
    };

    $scope.$watch('queryString', function () {
      // clear the alerts
      $scope.alerts.length = 0;
    });
  }]);

  /**
   * Filter which removes users present in a second array.
   */
  appSecurityModule.filter('memberNotIn', function () {
    return function (input, mappings) {
      var result = null,
          modified = false;

      if (angular.isArray(input) && angular.isArray(mappings)) {
        result = input.slice();

        for (var i=0; i<result.length; i++) {
          for (var x=0; x<mappings[0].members.length; x++) {
            if (result[i].internalName === mappings[0].members[x].internalName &&
                result[i].type === mappings[0].members[x].type) {
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

  appSecurityModule.directive('appSecurityEditor', [function () {
    return {
      scope : {
        appSecurityEditor : '=appSecurityEditor',
        roleId : '=',
        hide : '&',
        groupSearchEnabled : '=',
        ldapRealm : '='
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
  
  appSecurityModule.directive('userListItem', function () {
    return {
      restrict : 'A',
      scope : {
        iconClass : '@',
        user : '=userListItem',
        queryString : '@',
        selected : '&'
      },
      template :
        '<div class="large-select-list-item" ng-class="{ active : selected() }">' +
          '<div class="large-select-list-item-icon">' +
            '<i class="{{iconClass}}"></i>' +
          '</div>' +
          '<div class="large-select-list-item-content">' +
            '<div class="flexbox-container">\n' +
              '<span ng-bind-html="user.displayName"></span>\n' +
              '<span class="large-select-list-item-detail">{{user.internalName}}</span>\n' +
            '</div>\n' +
            '<div class="flexbox-container">\n' +
              '<span class="large-select-list-item-detail">{{user.email}}</span>\n' +
              '<span class="large-select-list-item-detail">{{user.realm}}</span>\n' +
            '</div>' +
          '</div>' +
        '</div>'
    };
  });
  
  appSecurityModule.directive('appUserSearch', ['$timeout', '$http', 'CLMAppLocations', 'Messages', function ($timeout, $http, clmAppLocations, Messages) {
    return {
      restrict : 'A',
      scope : {
        setResults : '&',
        queryString : '=',
        groups : '@',
        requestActive : '=',
        ownerType : '@',
        ownerId : '@'
      },
      template : '<form ng-submit="userSearch()" name="userSearch" style="margin:0; padding-top: 10px;">' +
          '<div class="input-prepend input-append">' +
            '<span class="add-on">' +
              '<i class="icon-search" ng-show="!requestActive"> </i>' +
              '<img src="img/loading.gif" ng-show="requestActive">' +
            '</span><input id="user-search-filter" placeholder="Isaac Asim*" type="text" name="filter" ng-model="queryString" focus-input="true">' +
            '<div class="btn-group">' +
              '<button id="user-search-button" type="submit" class="btn" ng-disabled="!queryString">Search</button>' +
              '<div id="user-search-help" style="display: inline-block; padding-left: 8px;">' +
                '<i class="glyphicons-sonatype help"></i>' +
              '</div>' +
            '</div>' +
          '</div>' +
        '</form>',
      priority : 99,
      link : function ($scope, element) {
        
        // Configure a help popover that explains how searches are conducted.
        var modal = element.parents('.modal');
        var options = {
          trigger: 'hover',
          placement: 'right',
          content: 'User searches with prefixed and suffixed wildcards (e.g. *Isaac*) may take a long time to complete or timeout. For faster results limit searches to suffixed wildcards.',
          container: 'body'
        };
        var helpDiv = element.find('#user-search-help');
        helpDiv.popover(options);
        if(modal.length > 0) {
          helpDiv.data('popover').tip().css('z-index', parseInt(modal.css('z-index')) + 1);
        }
        
        var filterTimeout = null;

        $scope.requestActive = 0;
        $scope.queryString = $scope.queryString || '';

        $scope.userSearch = function () {
          var newVal = $scope.queryString;

          if (!newVal) {
            $scope.setResults({
              $members : null, // Empty query, empty results
              $error : null
            });
            return;
          }

          if (filterTimeout) {
            $timeout.cancel(filterTimeout);
          }

          filterTimeout = $timeout(function () {
            $scope.requestActive++;

            $http.get(clmAppLocations.getFindUsersUrl($scope.ownerType, $scope.ownerId), {
              params : {
                q : newVal,
                groups : $scope.groups
              }
            }).then(function (response) {
              $scope.requestActive--;
              if ($scope.queryString === newVal) {
                $scope.setResults({
                  $members : response.data.members,
                  $error : response.data.error
                });
              }
            }, function (error) {
              $scope.requestActive--;

              $scope.setResults({
                $members : null,
                $error : Messages.getHttpErrorMessage(error)
              });
            });
          }, 500);
        };
      }
    };
  }]);

  /**
   * Custom filter to allow for '*' wildcards during filtering
   */
  appSecurityModule.filter('displayNameContains', function() {
    /**
     * Filter the list of items by comparing queryString to displayName property for items with the appropriate type.
     * '*' wildcards are removed from the queryString and any remaining RegEx characters are escaped before performing the test.
     */
    return function(members, queryString, type) {
      var filtered = [];
      // escape any regex characters as a precaution
      var safeQueryString = queryString.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, '\\$1').replace(/\\\*/g, '.*');
      var match = new RegExp(safeQueryString, 'i');
      for (var i = 0; i < members.length; i++) {
        var member = members[i];
        if (match.test(member.displayName) && member.type === type) {
          filtered.push(member);
        }
      }
      return filtered;
    };
  });

}());
