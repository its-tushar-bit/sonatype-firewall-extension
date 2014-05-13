/*
Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
"Sonatype" is a trademark of Sonatype, Inc.
*/
/* global angular, $ */
(function() {
  'use strict';
  var reportTrending;

  reportTrending = angular.module('ReportTrending');

  reportTrending.controller('DiffChartCtrl', [
    '$scope', 'colors', function($scope, colors) {
      $scope.$watch('data', function(newData) {
        $scope.barGraphDatas = [];
        $scope.barGraphDatas.push(newData.diffData.SECURITY);
        $scope.barGraphDatas.push(newData.diffData.LICENSE);
        $scope.barGraphDatas.push(newData.diffData.QUALITY);
        $scope.barGraphDatas.push(newData.diffData.OTHER);
        $scope.barGraphMax = Math.max.apply(null, $.map($scope.barGraphDatas, function(data) {
          return $.map(data, function(threatValue) {
            if (threatValue.violations > threatValue.previousViolations) {
              return threatValue.violations;
            } else {
              return threatValue.previousViolations;
            }
          });
        }));
        $scope.barGraphSelector = function(d) {
          return d.violations;
        };
        $scope.barGraphDiffSelector = function(d) {
          return d.previousViolations;
        };
        $scope.barGraphRenderer = function(d) {
          return colors.barFromThreatName(d.threat);
        };
        $scope.barGraphTextRenderer = function(d) {
          return colors.textFromThreatName(d.threat);
        };
        $scope.getBackgroundColor = function(index) {
          if (index % 2 === 0) {
            return '#EEE';
          } else {
            return '#F4F4F4';
          }
        };
      });
    }
  ]);

}).call(this);
