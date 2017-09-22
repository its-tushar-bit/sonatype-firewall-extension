/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';

var reportListModule = angular.module('ReportViolations', [angularCommonModule.name, CLMLocationModule.name, 'vs-repeat']);

reportListModule.controller('ReportViolationsController', ['$scope', '$http', '$q', 'CLMLocations', '$filter',
  function($scope, $http, $q, clmLocations, $filter) {
    let allApplications = undefined;

    const isVisible = appFilter => item => {
      return !appFilter ||
          item.name.toLowerCase().indexOf(appFilter.toLowerCase()) > -1 ||
          item.organizationName.toLowerCase().indexOf(appFilter.toLowerCase()) > -1;
    };

    $scope.encodeURIComponent = window.encodeURIComponent;

    $scope.doLoad = function() {
      $scope.error = null;

      var promises = [];

      promises.push($http.get(clmLocations.getActionStageUrl()));
      promises.push($http.get(clmLocations.getApplicationSummariesUrl()));

      $q.all(promises).then(function(results) {
        $scope.stages = results[0].data;
        allApplications = results[1].data;
        $scope.noReports = allApplications.length === 0;
        $scope.showReports = allApplications.length > 0;
        $scope.applications = sortAndIndex(allApplications);
      }, function() {
        $scope.error = arguments[0];
      });
    };
    $scope.doLoad();

    $scope.$watch('[appFilter, getSortField()[0]]', () => {
      if (allApplications) {
        $scope.applications = sortAndIndex(filter(allApplications));
      }
    });

    function sortAndIndex(apps) {
      return index(sort(apps));
    }

    function filter(apps) {
      return apps.filter(isVisible($scope.appFilter));
    }

    function sort(apps) {
      return $filter('orderBy')(apps, $scope.getSortField());
    }

    function index(apps) {
      return apps.map((app, index) => ({...app, index}));
    }
  }]);
