/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var loginApp = angular.module('LoginApp', ['CLMLocation', 'CommonServices']);

  var loginController = loginApp.controller('LoginController', ['$scope', '$http', '$location', '$window',
      'CLMLocations', function($scope, $http, $location, $window, CLMLocations) {
        $scope.data = {};

        $scope.signIn = function() {
          delete $scope.loginError;
          var authz = Base64.encode($scope.data.username + ':' + $scope.data.password);
          $http.post(CLMLocations.getLoginUrl(), {}, {
            headers: {
              'Authorization': 'Basic ' + authz
            },
            params: {
              timestamp: new Date().getTime()
            }
          }).success(function() {
            $scope.data = {};
            delete $scope.loginError;
            // TODO: handle redirect properly, with url user initially browsed
            $window.location = '../';
          }).error(function(data, status, headers, config) {
            $scope.loginError = 'Invalid credentials entered, please try again.';
          });
        };
      }]);
}());