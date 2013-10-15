/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var module = angular.module('SecurityModule', ['ui.router', 'ManagementModule', 'AngularCommon'], ['$stateProvider',
      function($stateProvider) {
        $stateProvider.state('management.security', {
          parent: 'management',
          url: '/security',
          templateUrl: '../security-assets/security-navigation.html?' + clmBuildTimestamp,
          controller: 'SecurityMenuController'
        });
      }]);

  module.controller('SecurityMenuController', ['$state', '$scope', function($state, $scope) {
    $scope.$state = $state;
  }]);
}());

(function() {
  "use strict";

  var module = angular.module('UserModule', ['ui.router', 'SecurityModule', 'CLMLocation', 'ResourceModule', 'Hudson'],
          ['$stateProvider', function($stateProvider) {
            $stateProvider.state('management.security.users', {
              parent: 'management.security',
              url: '/users',
              controller: 'UserListController',
              templateUrl: '../security-assets/user-list.html?' + clmBuildTimestamp
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
      url: clmLocations.getUserUrl(),
      params: {
        timestamp: new Date().getTime()
      }
    }, store = clmResource.getStore(config);

    return store;
  }]);

  module.controller('UserListController', ['$http', 'hudson', 'CLMLocations', 'UserStore', 'Messages', '$scope',
      '$modal', '$rootScope', function($http, hudson, clmLocations, UserStore, messages, $scope, $modal, $rootScope) {
        $scope.context = {
          userEditMap: {},
          users: []
        };
        $scope.doLoad = function() {
          $scope.error = null;

          UserStore.refresh().then(function(data) {
            $scope.context.users = data;
          }, function(error) {
            $scope.error = error;
          });
        };
        $scope.editClick = function(user) {
          $scope.context.userEditMap[user.id] = user;
          $scope.$broadcast('userEditClick', {
            userId: user.id
          });
        };
        $scope.isCurrentUser = function(user) {
          return $rootScope.username === user.username;
        };
        $scope.changePasswordClick = function(user) {
          $modal.open({
            backdrop: 'static',
            templateUrl: 'change-password-modal',
            scope: $scope,
            controller: ['$scope', '$modalInstance', function($localScope, $modalInstance) {
              $localScope.save = function() {
                var scope = this;
                if (scope.changePasswordForm.$valid) {
                  if (!scope.saving) {
                    scope.saving = true;
                    scope.errorMsg = null;
                    $http.put(clmLocations.getUserUrl() + '/' + user.id + '/password', {
                      oldPassword: scope.currentPassword,
                      newPassword: scope.newPassword
                    }).success(function() {
                      $modalInstance.close();
                      scope.saving = false;
                    }).error(function(error) {
                      scope.errorMsg = error;
                      scope.saving = false;
                    });
                  }
                }
              };
              $localScope.cancel = function() {
                $modalInstance.close();
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
                  $localScope.errorMsg = error.data;
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
        $scope.errorMsg = null;
        $scope.saving = true;
        $scope.user.$save().then(function(data) {
          if ($scope.context.userEditMap[user.id]) {
            $scope.context.userEditMap[user.id] = null
          } else {
            $scope.user = null;
          }
          $scope.saving = false;
        }, function(error) {
          $scope.errorMsg = error.data;
          $scope.saving = false;
        });
      }
    };
    $scope.cancelClick = function(user) {
      if ($scope.context.userEditMap[user.id]) {
        $scope.context.userEditMap[user.id] = null
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
      link: function(scope, element, attrs) {
        // so data changes dont affect orig
        if (scope.user && scope.user.id) {
          scope.user = scope.user.$clone();
        }
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
}());