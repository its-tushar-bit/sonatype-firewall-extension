/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/pro/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, HealthCheck, Brain, window */
window.CLM = {
  path: '../'
};

(function () {
  'use strict';

  var module = angular.module('Audit', ['AngularCommon', 'UnauthenticatedResponseHttpInterceptor', 'ui.bootstrap', 'CLMLocation', 'component.information.panel']);

  module.controller('AuditSummaryController', ['$scope', '$http', '$window', 'OwnerContext', 'CLMLocations', function ($scope, $http, $window, OwnerContext, CLMLocations) {

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
  }]);

  module.controller('ReevaluatorController', ['$scope', '$http', '$window', function ($scope, $http, $window) {
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
  }]);

  module.directive('coverageDonut', function () {
    return {
      scope : {
        percentKnownComponents : '=coverageDonut'
      },
      link: function(scope, element) {
        function updateGraph() {
          if (scope.percentKnownComponents !== undefined) {
            HealthCheck.artifactsChart(1 - scope.percentKnownComponents / 100, { element: element[0] });
          }
        }
        scope.$watch('percentKnownComponents', updateGraph);
      }
    };
  });
}());
