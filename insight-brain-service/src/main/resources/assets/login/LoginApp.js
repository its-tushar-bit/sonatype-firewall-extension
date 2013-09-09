/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var loginApp = angular.module('LoginApp', ['CLMLocation', 'CommonServices']);

  var loginController = loginApp.controller('LoginController', ['$scope', '$http', '$location', 'CLMLocations',
      function($scope, $http, $location, CLMLocations) {
        $scope.data = {
          forwardTo: $location.search().forwardTo
        };

        $scope.signIn = function() {
          var authz = Base64.encode($scope.data.username + ':' + $scope.data.password);
          $http.get(CLMLocations.getLoginUrl(), {
            headers: {
              'Authorization': 'Basic ' + authz
            }
          }).success(function() {
            //TODO: handle redirect properly
            if ($scope.data.forwardTo) {
              $location.url($scope.data.forwardTo);
            }
          }).error(function() {
            // TODO: handle error response by showing msg to user
          });
        };
      }]);
}());