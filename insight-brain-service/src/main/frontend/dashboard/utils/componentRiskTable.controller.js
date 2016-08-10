/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.controller('componentRiskTable', [
    '$scope', function($scope) {
      $scope.totalRisk = 0;
      $scope.criticalRisk = 0;
      $scope.severeRisk = 0;
      $scope.moderateRisk = 0;
      $scope.lowRisk = 0;
      angular.forEach($scope.data, function(data) {
        $scope.totalRisk = Math.max($scope.totalRisk, data.score);
        $scope.criticalRisk = Math.max($scope.criticalRisk, data.scoreCritical);
        $scope.severeRisk = Math.max($scope.severeRisk, data.scoreSevere);
        $scope.moderateRisk = Math.max($scope.moderateRisk, data.scoreModerate);
        $scope.lowRisk = Math.max($scope.lowRisk, data.scoreLow);
      });
    }
  ]);

}());
