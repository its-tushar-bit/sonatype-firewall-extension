/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";

  var module = angular.module('UserControls', ['AngularCommon', 'CLMLocation']);

  module.controller('LogoutController', ['$scope', '$http', function ($scope, $http) {
    $scope.logout = function () {
      // TODO This ought to perform a dirty check before it simply logs the user out
      $http['delete'](CLMLocations.getSessionUrl()).success(function(){
        $rootScope.authenticated = false;
        $state.transitionTo('home');
      });
    };
  }]);

  module.controller('ChangePassword', ['$scope', '$http', '$modal', 'CLMLocations', 'CurrentUser', function ($scope, $http, modal, clmLocations, currentUser) {

    // Errors should be handled @ application level
    currentUser.then(function (userId) {
      $scope.userId = userId;
    }, angular.noop);

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

              $http.put(clmLocations.getChangePasswordUrl($scope.userId), {
                oldPassword : scope.result.originalPassword,
                newPassword : scope.result.newPassword
              }).success(function () {
                scope.$close();
              }).error(function () {
                scope.submitActive = false;
                scope.error = Messages.getHttpErrorMessage(arguments);
              });
            }
          };
        }]
      });
    };
  }]);

  module.directive('userControls', function () {
    return {
      restrict: 'A',
      templateUrl : '../assets/components/user-controls/user_controls.html?' + clmBuildTimestamp
    };
  });

  module.directive('match', function () {
    return {
      restrict: 'A',
      require: 'ngModel',
      scope: false,
      priority: 99,
      link: function(scope, elm, attrs, ctrl) {
        function validate (newVal) {
          var match = !elm.val() || attrs.match === elm.val();

          ctrl.$setValidity('match', match);

          return match ? newVal : undefined;
        }

        ctrl.$parsers.unshift(validate);
        attrs.$observe('match', validate);
      }
    };
  });

  // TODO We need to unify the user role services to avoid multiple requests
  module.factory('CurrentUser', ['$http', '$q', 'CLMLocations', function ($http, $q, clmLocations) {
    var deferred = $q.defer();
    $http.get(clmLocations.getSessionUrl()).success(function (data) {
      deferred.resolve(data.username);
    }).error(function () {
      deferred.reject(arguments);
    });
    return deferred.promise;
  }]);
}());