/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, AngularUtils, ZeroClipboard, clmBuildTimestamp, $ */
(function() {
  'use strict';
  angular.module('SecurityModule', ['ui.router', 'ManagementModule', 'AngularCommon', 'ApplicationSecurityModule', 'PermissionServiceModule'], ['$stateProvider',
      function($stateProvider) {
        $stateProvider.state('administrators', {
          url: '/administrators',
          template : '<div authorization-wrapper="isAuthorized">' +
                       '<div class="mid-content"><h1 class="page-title"><div class="container globalrole">Administrators</div></h1></div>' +
                       '<div class="container globalrole" ng-include="\'../policy-assets/components/app-security/app-security.html?' + clmBuildTimestamp + '\'"></div>' +
                     '</div>',
          data : {
            title : 'Administrators'
          },
          controller : 'AppSecurityController',
          resolve : {
            'hasPermission' : ['PermissionService', function (PermissionService) {
              return PermissionService.isAuthorized(['ADMIN'], true);
            }]
          }
        });
      }]);
}());

(function() {
  'use strict';

  var module = angular.module('UserModule', ['ui.router', 'SecurityModule', 'CLMLocation', 'ResourceModule'],
          ['$stateProvider', function($stateProvider) {
            $stateProvider.state('users', {
              url: '/users',
              controller: 'UserListController',
              templateUrl: '../security-assets/user-list.html?' + clmBuildTimestamp,
              data : {
                title : 'Users'
              },
              resolve : {
                'hasAdminPermission' : ['PermissionService', function (PermissionService) {
                  return PermissionService.isAuthorized(['ADMIN'], true);
                }]
              }
            });
          }]);

  module.service('UserStore', ['CLMLocations', 'CLMResource', function(clmLocations, clmResource) {
    var config = {
      id: 'id',
      template: {
        id: null,
        username: '',
        password: '',
        firstName: '',
        lastName: '',
        email: ''
      },
      url: clmLocations.getUserUrl()
    }, store = clmResource.getStore(config);

    return store;
  }]);

  module.controller('UserListController', ['$http', 'CLMLocations', 'UserStore', 'Messages', 'CurrentUser', '$scope',
      '$modal', '$q', 'hasAdminPermission', function($http, clmLocations, UserStore, messages, CurrentUser, $scope, $modal, $q, hasAdminPermission) {
        var username = null;

        $scope.context = {
          userEditMap: {},
          users: []
        };
        $scope.isAuthorized = hasAdminPermission;
        $scope.doLoad = function() {
          if (hasAdminPermission) {
            $scope.error = null;

            $q.all([UserStore.refresh(), CurrentUser]).then(function(results) {
              $scope.context.users = results[0];
              username = results[1].username;
            }, function(error) {
              $scope.error = error;
            });
          }
        };
        $scope.editClick = function(user) {
          $scope.context.userEditMap[user.id] = user;
          $scope.$broadcast('userEditClick', {
            userId: user.id
          });
        };
        $scope.isCurrentUser = function(user) {
          return username === user.username;
        };
        $scope.resetPasswordClick = function(user) {
          $modal.open({
            templateUrl: 'reset-password-modal',
            scope: $scope,
            backdrop: 'static',
            keyboard : false,
            controller: ['$scope', function(scope) {
              scope.state = 'ready';
              scope.user = user;
              scope.cancelClick = function(){
                scope.$close();
              };
              scope.resetClick = function(){
                scope.state = 'pending';
                $http.put(clmLocations.getUserUrl() + '/' + user.id + '/reset').success(function(data) {
                  scope.newPassword = data.newPassword;
                  scope.state = 'complete';
                }).error(function(error){
                  scope.state = 'failed';
                  scope.error = error;
                });
              };
              scope.okClick = function(){
                scope.$close();
              };
              scope.flashInstalled = function() {
                return AngularUtils.hasFlash();
              };
            }]
          });
        };
        $scope.removeClick = function(user) {
          $modal.open({
            backdrop: 'static',
            templateUrl: 'delete-user-modal',
            controller: ['$scope', '$modalInstance', function($localScope, $modalInstance) {
              $localScope.username = user.username;
              $localScope.discard = function() {
                user.$delete().then(function() {
                  $modalInstance.close();
                }, function(error) {
                  $localScope.alerts = [AngularUtils.toAlert(error.data)];
                });
              };
              $localScope.cancel = function() {
                $modalInstance.close();
              };
            }]
          });
        };
        $scope.doLoad();
      }]);

  module.controller('UserController', ['$scope', 'UserStore', function($scope, UserStore) {
    function isDirty() {
      return $scope.user && $scope.user.isDirty();
    }
    $scope.saveClick = function(user) {
      if (!$scope.saving) {
        $scope.alerts = null;
        $scope.saving = true;
        $scope.user.$save().then(function() {
          if ($scope.context.userEditMap[user.id]) {
            $scope.context.userEditMap[user.id] = null;
          } else {
            $scope.user = null;
          }
          $scope.context.users.sort(function(a, b) {
            if (a.usernameLowercase < b.usernameLowercase) {
              return -1;
            } else if (a.usernameLowercase > b.usernameLowercase) {
              return 1;
            } else {
              return 0;
            }
          });
          $scope.saving = false;
        }, function(error) {
          $scope.alerts = [AngularUtils.toAlert(error.data)];
          $scope.saving = false;
        });
      }
    };
    $scope.cancelClick = function(user) {
      if ($scope.context.userEditMap[user.id]) {
        $scope.context.userEditMap[user.id] = null;
      } else {
        $scope.user = null;
      }
    };
    $scope.newUserClick = function() {
      $scope.user = UserStore.create();
    };
    // make sure user is aware they are about to lose changes
    $scope.$on('pageChangeStarted', function(event) {
      if (isDirty()) {
        event.preventDefault();
      }
    });
  }]);

  module.directive('userItem', function() {
    return {
      restrict: 'A',
      templateUrl: 'user-item',
      scope: {
        user: '=',
        context: '='
      },
      controller: 'UserController',
      link: function(scope) {
        // so data changes dont affect orig
        if (scope.user && scope.user.id) {
          scope.user = scope.user.$clone();
        }
      }
    };
  });

  module.directive('clmMatch', function () {
    return {
      require : 'ngModel',
      link : function(scope, element, attrs, ctrl) {
        function emptyString(val) {
          if (val === '' || val === null) {
            return undefined;
          }
          return val;
        }

        ctrl.$validators.match = function (value) {
          return emptyString(value) === emptyString(scope.$eval(attrs.clmMatch));
        };

        scope.$watch(function () {
          return scope.$eval(attrs.clmMatch);
        }, function () {
          ctrl.$$parseAndValidate();
        });
      }
    };
  });

  module.directive('expandUserOnEvent', function() {
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$on(attrs.expandUserOnEvent, function(event, data) {
          $('#collapse' + data.userId).collapse('show');
        });
      }
    };
  });
  
  module.directive('zeroClipboard', function() {
    ZeroClipboard.config({
      moviePath: '../assets/lib/zeroclipboard/ZeroClipboard-1.3.2.swf'
    });
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        var clip = new ZeroClipboard(element);
        
        clip.on('dataRequested', function () {
          clip.setText( $('#' + attrs.zeroClipboard).val() );
        });
      }
    };
  });
  
  //simple directive that will select the text in an input field
  //when user clicks on it
  module.directive('selectText', [function () {
    return function (scope, element) {
      element.bind('focus', function () {
        this.select();
      });
    };
  }]);
}());