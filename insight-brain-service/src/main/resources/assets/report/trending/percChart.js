/*
Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
"Sonatype" is a trademark of Sonatype, Inc.
*/


(function() {
  'use strict';
  var reportTrending;

  reportTrending = angular.module('ReportTrending');

  reportTrending.controller('PercChartCtrl', ['$scope', function($scope) {
      $scope.$watch('data', function(newData) {
        $scope.applicationComponents = [
          {
            value: newData.components.exact,
            color: '#838383'
          }, {
            value: newData.components.partial,
            color: '#AAA'
          }, {
            value: newData.components.unknown,
            color: '#CCC'
          }, {
            value: newData.components.proprietary,
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