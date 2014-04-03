/**
* @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
* Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
* "Sonatype" is a trademark of Sonatype, Inc.
*/
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  var dashboardModule = angular.module('DashboardModule', ['ui.router', 'Stores', 'AngularCommon'], ['$stateProvider', function($stateProvider) {
    $stateProvider.state('dashboard', {
      url: '/dashboard',
      templateUrl: '../dashboard-assets/dashboard.html?' + clmBuildTimestamp,
      controller: 'DashboardController'
    });
  }]);

  dashboardModule.controller('DashboardController', ['$scope', '$q', 'ApplicationStore', '$http', 'CLMLocations',
    function($scope, $q, ApplicationStore, $http, CLMLocations) {
    $scope.appliedApplicationPublicIds = [];

    function load() {
      var promises = [
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params: {
            applicationPublicIds: $scope.appliedApplicationPublicIds,
          }
        })
      ];
      $q.all(promises).then(function(data) {
        $scope.highestRisks = data[0].data;
      });
    }

    function loadFilters() {
      var promises = [
        ApplicationStore.get()
      ];
      $q.all(promises).then(function(data) {
        $scope.applications = data[0];

        $scope.$watchCollection('appliedApplicationPublicIds', load);
      });
    }

    loadFilters();
  }]);
}());