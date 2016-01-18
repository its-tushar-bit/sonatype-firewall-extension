/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Brain */
(function () {
  'use strict';

  function ReevaluatorController($scope, $http, $window) {
    $scope.error = null;

    $scope.reevaluate = function () {
      $scope.error = null;
      $scope.submitActive = true;

      $http.post(Brain.getCurrentReportReevaluateUrl()).success(function () {
        $window.location.reload();
      }).error(function () {
        $scope.submitActive = false;
        $scope.error = arguments;
      });
    };
  }
  ReevaluatorController.$inject = ['$scope', '$http', '$window'];

  angular.module('audit').controller('ReevaluatorController', ReevaluatorController);
}());
