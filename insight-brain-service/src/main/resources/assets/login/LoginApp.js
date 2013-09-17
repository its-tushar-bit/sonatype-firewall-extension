/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var loginApp = angular.module('LoginApp', ['CLMLocation', 'CommonServices', 'Hudson']);

  var loginController = loginApp.controller('LoginController', [
      '$scope',
      'hudson',
      '$location',
      '$window',
      'CLMLocations',
      'Messages',
      function($scope, hudson, $location, $window, CLMLocations, Messages) {
        $scope.data = {};

        // Focus field
        angular.element('#user').focus();

        // Remove error when user changes login information
        $scope.$watch('data.username', function() {
          $scope.loginError = null;
        });
        $scope.$watch('data.password', function() {
          $scope.loginError = null;
        });

        $scope.signIn = function() {
          var authz = Base64.encode($scope.data.username + ':' + $scope.data.password);

          $scope.loginError = null;
          $scope.processing = true;

          hudson.post(CLMLocations.getLoginUrl(), {}, {
            headers: {
              'Authorization': 'Basic ' + authz
            },
            params: {
              timestamp: new Date().getTime()
            }
          }).success(
                  function() {
                    $scope.redirecting = true;
                    $scope.data = {};
                    delete $scope.loginError;

                    var redirectPath = '../';
                    var match = new RegExp('[\\?&]redirectTo=([^&#]*)').exec($window.location.href);
                    if (match) {
                      var srcRoot = $window.location.href.substring(0, $window.location.href.indexOf('/',
                              ($window.location.protocol + '//').length));
                      match = decodeURIComponent(match[1]);
                      var targetRoot = match
                              .substring(0, match.indexOf('/', ($window.location.protocol + '//').length));
                      // just to be safe make sure we are on the same server
                      if (srcRoot === targetRoot) {
                        redirectPath = match;
                      }
                    }

                    $window.location.replace(redirectPath);
                  }).error(function(data, status, headers, config) {
            $scope.processing = false;
            if (status === 401) {
              $scope.loginError = 'Invalid credentials. Please try again.';
            } else {
              // Non-login related error occurred
              $scope.loginError = Messages.getHttpErrorMessage(arguments);
            }
          });
        };
      }]);

  loginApp.directive('autofill', ['$timeout', '$parse', function($timeout, $parse) {
    return {
      restrict: 'A',
      require: '?ngModel',
      link: function postLink($scope, element, attrs, controller) {
        function checkForChange() {
          var elementValue = element.val();

          var modelParser = $parse(attrs.ngModel);
          if (elementValue !== modelParser($scope)) {
            AngularUtils.safeApply($scope, function() {
              modelParser.assign($scope, elementValue);
            });
          }

          $timeout(checkForChange, 500);
        }

        $timeout(checkForChange, 500);
      }
    };
  }]);
}());