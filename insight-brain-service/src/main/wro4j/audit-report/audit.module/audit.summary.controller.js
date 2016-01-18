/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function AuditSummaryController($scope, $http, $window, OwnerContext, CLMLocations) {

    $scope.doLoad = function () {
      $scope.error = null;
      $scope.loadActive = true;

      $http.get(CLMLocations.getAuditReportSummary(OwnerContext.ownerId)).success(function(data) {
        $scope.loadActive = false;

        $scope.knownComponentCount = data.knownComponentCount;
        $scope.percentKnownComponents = data.totalComponentCount ? Math.round(100 * data.knownComponentCount / data.totalComponentCount) : 0;

        $scope.criticalComponentCount = data.criticalComponentCount;
        $scope.severeComponentCount = data.severeComponentCount;
        $scope.moderateComponentCount = data.moderateComponentCount;
        $scope.affectedComponentCount = data.affectedComponentCount;
        $scope.quarantinedComponentCount = data.quarantinedComponentCount;

        $scope.policyViolationCount = data.criticalComponentCount + data.severeComponentCount + data.moderateComponentCount;
      }).error(function () {
        $scope.loadActive = false;
        $scope.error = arguments;
      });
    };

    ($window.Insight = $window.Insight || {}).updateSummary = $scope.doLoad;

    $scope.doLoad();
  }
  AuditSummaryController.$inject = ['$scope', '$http', '$window', 'OwnerContext', 'CLMLocations'];

  angular.module('audit').controller('audit.summary.controller', AuditSummaryController);
}());
