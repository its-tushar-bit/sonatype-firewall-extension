/*
Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
"Sonatype" is a trademark of Sonatype, Inc.
*/
/* global angular */

(function() {
  'use strict';
  var reportTrending;

  reportTrending = angular.module('ReportTrending');

  reportTrending.controller('PolicyProgressionCtrl', [
    '$scope', 'colors', 'orderByFilter', function($scope, colors, orderByFilter) {
      $scope.policies = [];
      $scope.$watch('data', function(newData) {
        $scope.policies = orderByFilter(newData.violations, '-threat');
      });
      $scope.threatLevel = function(index) {
        var threatLevel;
        threatLevel = $scope.policies[index].threat;
        if (index > 0 && $scope.policies[index].threat === $scope.policies[index - 1].threat) {
          threatLevel = '';
        }
        return threatLevel;
      };
      $scope.abs = function(number) {
        if (number) {
          return Math.abs(number);
        } else {
          return '';
        }
      };
      $scope.threatLevelClass = function(threatLevel) {
        return colors.threatLevelClass(threatLevel);
      };

      $scope.lastViolationCount = function(policy) {
        if (angular.isUndefined(policy.violations) || policy.violations.length === 0) {
          return 0;
        }
        return policy.violations.slice(-1)[0];
      };

      $scope.firstViolationCount = function(policy) {
        if (angular.isUndefined(policy.violations) || policy.violations.length === 0) {
          return 0;
        }
        return policy.violations[0];
      };

      $scope.violationDifference = function(policy){
        return $scope.lastViolationCount(policy) - $scope.firstViolationCount(policy);
      };
    }
  ]);

}).call(this);
