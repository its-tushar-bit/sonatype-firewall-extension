/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var loginApp = angular.module('loginApp', []);
  
  var loginController = loginApp.controller('loginController', ['$scope', function($scope){
    $scope.data = {};
    $scope.signIn = function() {
      //TODO: send POST to server with auth data
      //TODO: handle successful response by redirecting to the clm app
      //TODO: handle error response by showing msg to user
    };
  }]);
}());