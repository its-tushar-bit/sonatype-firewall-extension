/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global AngularUtils */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.directive('dashboardViewSummary', function() {
    function watchFilter($scope) {
      $scope.$watch('filters', function(newFilter) {
        if (newFilter) {
          $scope.doLoad();
        }
      });
    }

    return {
      restrict: 'A',
      templateUrl: 'dashboard-view-summary',
      scope: {
        filters: '=filters'
      },
      controller: [
        '$scope', '$http', 'CLMLocations', 'filterToParams',
        function($scope, $http, CLMLocations, filterToParams) {
          $scope.doLoad = function() {
            $scope.data = null;
            $scope.error = null;
            $http.post(CLMLocations.getDashboardViewingSummaryUrl(), filterToParams($scope.filters)
            ).success(function(data) {
              $scope.data = data;
            }).error(function() {
              $scope.error = arguments;
            });
          };

          $scope.formatPercentage = AngularUtils.formatPercentage;

          watchFilter($scope);
        }
      ]
    };
  });

}());
