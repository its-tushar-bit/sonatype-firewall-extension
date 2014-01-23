/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";

  var module = angular.module('DashboardHeader', ['ui.router', 'AngularCommon', 'CLMLocation']);

  module.controller('LogoutController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, CLMLocations) {
      $scope.logout = function () {
        // TODO This ought to perform a dirty check before it simply logs the user out
        // https://issues.sonatype.org/browse/CLM-1251
        $http['delete'](CLMLocations.getSessionUrl()).success(function(){
          $scope.$emit('logout');
        });
      };
    }]);

  module.controller('ChangePassword', ['$scope', '$http', '$modal', 'CLMLocations', 'CurrentUser', 'Messages', function ($scope, $http, modal, clmLocations, currentUser, messages) {
    var clmUser = null;

    // Errors should be handled @ application level
    currentUser.then(function (authenticationStatus) {
      clmUser = authenticationStatus.clmUser;
    }, angular.noop);

    $scope.canChangePassword = function () {
      return clmUser;
    };

    $scope.change = function () {
      modal.open({
        templateUrl : 'change-password',
        backdrop : 'static',
        controller : ['$scope', function (scope) {
          scope.result = {};
          scope.save = function () {
            if (this.passwordForm.$valid) {
              scope.error = null;
              scope.submitActive = true;

              $http.put(clmLocations.getChangeMyPasswordUrl(), {
                oldPassword : scope.result.originalPassword,
                newPassword : scope.result.newPassword
              }).success(function () {
                scope.$close();
              }).error(function () {
                scope.submitActive = false;
                scope.error = messages.getHttpErrorMessage(arguments);
              });
            }
          };
        }]
      });
    };
  }]);

  module.controller('dashboardHeaderController', ['$scope', '$state', 'CurrentUser', function($scope, $state, currentUser) {
    $scope.$state = $state;

    currentUser.then(function(status) {
      $scope.displayName = status.displayName;
    });
    
    $scope.getServerVersion = function() {
      return serverVersion.version;
    }
  }]);

  module.directive('dashboardHeader', function () {
    return {
      restrict: 'A',
      controller: 'dashboardHeaderController',
      templateUrl : '../assets/components/dashboard-header/dashboard-header.html?' + clmBuildTimestamp
    };
  });

  module.factory('CurrentUser', ['$http', '$q', 'CLMLocations', function ($http, $q, clmLocations) {
    var deferred = $q.defer();
    $http.get(clmLocations.getSessionUrl(), {
      params: { timestamp: new Date().getTime() }
    }).success(function (authenticationStatus) {
      deferred.resolve(authenticationStatus);
    }).error(function () {
      deferred.reject(arguments);
    });
    return deferred.promise;
  }]);
}());