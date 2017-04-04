/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DashboardResultsController($scope, $modal, EventNameConstant, $state, filterToParams, CLMLocations) {
    $scope.maxResults = 100;
    $scope.maxDaysOld = 30;
    $scope.showTrendDialog = showTrendDialog;
    $scope.getViewTitle = getViewTitle;
    $scope.getExportUrl = getExportUrl;
    $scope.getFilterJson = getFilterJson;

    $scope.filters = undefined;

    $scope.$on(EventNameConstant.UPDATE_DASHBOARD_FILTERS, function(e, newFilters) {
      $scope.filters = newFilters;
      $scope.maxDaysOld = newFilters.maxDaysOld;
    });

    function showTrendDialog() {
      $modal.open({
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'policy-trends-dialog-template',
        windowClass: 'fit-content dashboard-policy-trend-dialog clm-modal',
        controller: 'PolicyTrendController',
        resolve: {
          filters: function() {
            return $scope.filters;
          }
        }
      });
    }

    function getViewTitle() {
      return $state.current.data.title;
    }

    function getExportUrl() {
      switch ($state.current.name) {
        case 'dashboard.overview.violations':
          return CLMLocations.getNewestRisksExportUrl();

        case 'dashboard.overview.components':
          return CLMLocations.getComponentRisksExportUrl();

        case 'dashboard.overview.applications':
          return CLMLocations.getApplicationRisksExportUrl();

        default:
          throw new Error('Export is not supported for state ' + $state.current.name);
      }
    }

    function getFilterJson() {
      var filterJson = filterToParams($scope.filters);
      return JSON.stringify(filterJson);
    }
  }

  DashboardResultsController.$inject = [
    '$scope', '$modal', 'event.name.constant', '$state', 'filterToParams', 'CLMLocations'
  ];

  angular.module('dashboard.module').controller('dashboard.results.controller', DashboardResultsController);

}(angular));
