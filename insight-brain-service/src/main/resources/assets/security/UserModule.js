/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var module = angular.module('SecurityModule', ['ui.router', 'ManagementModule', 'AngularCommon', 'ApplicationSecurityModule'], ['$stateProvider',
      function($stateProvider) {
        $stateProvider.state('management.security', {
          parent: 'management',
          url: '/security',
          templateUrl: '../security-assets/security-navigation.html?' + clmBuildTimestamp,
          controller: 'SecurityMenuController'
        }).state('management.security.global', {
          parent: 'management.security',
          url: '/global',
          controller: 'AppSecurityController',
          templateUrl: '../policy-assets/components/app-security/app-security.html?' + clmBuildTimestamp
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

  module.controller('UserListController', ['$http', 'hudson', 'CLMLocations', 'UserStore', 'Messages', 'CurrentUser', '$scope',
      '$modal', '$q', 'Dialog', function($http, hudson, clmLocations, UserStore, messages, CurrentUser, $scope, $modal, $q, Dialog) {
        var username = null;

        $scope.context = {
          userEditMap: {},
          users: []
        };
        $scope.doLoad = function() {
          $scope.error = null;

          $q.all([UserStore.refresh(), CurrentUser]).then(function(results) {
            $scope.context.users = results[0];
            username = results[1].username;
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
          return username === user.username;
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
        
        $scope.resetPasswordClick = function(user) {
          $modal.open({
            templateUrl: 'reset-password-modal',
            scope: $scope,
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
              }
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
  
  module.directive('zeroClipboard', function() {
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        var clip = new ZeroClipboard( element, {
          moviePath: "../assets/lib/zeroclipboard/ZeroClipboard-1.2.3.swf"
        });
        
        clip.on( 'dataRequested', function ( client, args ) {
          clip.setText( $('#' + attrs.zeroClipboard).val() );
        });
      }
    }
  });
  
  //simple directive that will select the text in an input field
  //when user clicks on it
  module.directive('selectText', ['$timeout', function ($timeout) {
    return function (scope, element, attrs) {
      element.bind('click', function () {
        this.select();
      });
    };
  }]);
}());