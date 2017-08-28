/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';

var reportListModule = angular.module('ReportViolations', [angularCommonModule.name, CLMLocationModule.name, 'vs-repeat']);

reportListModule.controller('ReportViolationsController', ['$scope', '$http', '$q', 'CLMLocations',
  function($scope, $http, $q, clmLocations) {

    $scope.isVisible = function (item) {
      return !$scope.appFilter ||
            item.name.toLowerCase().indexOf($scope.appFilter.toLowerCase()) > -1 ||
            item.organizationName.toLowerCase().indexOf($scope.appFilter.toLowerCase()) > -1;
    };

    $scope.encodeURIComponent = window.encodeURIComponent;

    $scope.doLoad = function() {
      $scope.error = null;

      var promises = [];

      promises.push($http.get(clmLocations.getActionStageUrl()));
      promises.push($http.get(clmLocations.getApplicationSummariesUrl()));

      $q.all(promises).then(function(results) {
        $scope.stages = results[0].data;
        $scope.applications = results[1].data;
      }, function() {
        $scope.error = arguments[0];
      });
    };
    $scope.doLoad();
  }]);
