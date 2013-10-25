/*
Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
"Sonatype" is a trademark of Sonatype, Inc.
*/


(function() {
  'use strict';
  var reportTrending;

  reportTrending = angular.module('ReportTrending');

  reportTrending.controller('PercChartCtrl', [
    '$scope', 'trendingReportService', function($scope, trendingReportService) {
      return trendingReportService.get().then(function(data) {
        $scope.applicationComponents = [
          {
            value: data.components.exact,
            color: '#AAA'
          }, {
            value: data.components.partial,
            color: '#CCC'
          }, {
            value: data.components.unknown,
            color: '#e8e8e8'
          }
        ];
        $scope.componentPercentageSelector = function(d) {
          return d.value;
        };
        $scope.componentColorRenderer = function(d) {
          return d.color;
        };
        return $scope.loaded = true;
      });
    }
  ]);

}).call(this);