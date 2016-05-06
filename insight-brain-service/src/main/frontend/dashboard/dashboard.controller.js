/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DashboardController($scope, $modal) {
    $scope.maxResults = 100;
    $scope.showTrendDialog = showTrendDialog;
    $scope.filters = {
      applicationIds: [],
      policyThreatTypes: [],
      stageTypeIds: [],
      applicationTagIds: [],
      policyThreatLevel: [0, 10]
    };

    function showTrendDialog() {
      $modal.open({
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'policy-trends-dialog-template',
        windowClass: 'dashboard-policy-trend-dialog clm-modal',
        controller: 'PolicyTrendController',
        resolve: {
          filters: function() {
            return $scope.filters;
          }
        }
      });
    }
  }

  DashboardController.$inject = ['$scope', '$modal'];

  angular.module('dashboard.module').controller('dashboard.controller', DashboardController);

}(angular));
