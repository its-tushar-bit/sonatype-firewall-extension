/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.controller('applicationRiskTable', [
    '$scope', '$filter', function($scope, $filter) {
      function updateApplications() {
        $scope.applications = $filter('orderBy')($filter('limitTo')($scope.data, $scope.maxResults),
            $scope.getSortField());
        updateStripes();
      }

      function updateStripes() {
        $scope.striped = [];
        var striped = false;
        angular.forEach($scope.applications, function(application, index) {
          $scope.striped[index] = (index === 0) || (!striped || $scope.isExpanded($scope.applications[index - 1]));
          striped = $scope.striped[index];
        });
      }

      $scope.$watch('data', updateApplications);
      $scope.$watch(function() {
        return $scope.getSortField()[0];
      }, updateApplications);

      $scope.$watch('expanded', updateStripes, true);
      $scope.encodeURIComponent = window.encodeURIComponent;

      $scope.expanded = {};

      $scope.canExpand = function(application) {
        return application.stageRisks.length > 0;
      };
      $scope.isExpanded = function(application) {
        return $scope.expanded[application.applicationId];
      };
      $scope.expand = function(application) {
        if ($scope.canExpand(application)) {
          $scope.expanded[application.applicationId] = !$scope.expanded[application.applicationId];
        }
      };

      $scope.totalRisk = 0;
      $scope.criticalRisk = 0;
      $scope.severeRisk = 0;
      $scope.moderateRisk = 0;
      $scope.lowRisk = 0;
      angular.forEach($scope.data, function(data) {
        $scope.totalRisk = Math.max($scope.totalRisk, data.totalApplicationRisk.totalRisk);
        $scope.criticalRisk = Math.max($scope.criticalRisk, data.totalApplicationRisk.criticalRisk);
        $scope.severeRisk = Math.max($scope.severeRisk, data.totalApplicationRisk.severeRisk);
        $scope.moderateRisk = Math.max($scope.moderateRisk, data.totalApplicationRisk.moderateRisk);
        $scope.lowRisk = Math.max($scope.lowRisk, data.totalApplicationRisk.lowRisk);
      });
    }
  ]);

}());
