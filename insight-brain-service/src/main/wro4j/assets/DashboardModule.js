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

  dashboardModule.controller('DashboardController', ['$scope', '$q', 'ApplicationStore', '$http', 'CLMLocations', '$timeout',
    function($scope, $q, ApplicationStore, $http, CLMLocations, $timeout) {
    $scope.appliedApplicationPublicIds = [];
    $scope.queuedApplicationPublicIds = [];
    $scope.maxResults = 20;

    function load() {
      var promises = [
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params: {
            maxResults: $scope.maxResults,
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
      });
    }

    loadFilters();
    load();
    $scope.applyFilters = function() {
      $scope.appliedApplicationPublicIds = angular.copy($scope.queuedApplicationPublicIds);
      load();
      $scope.toggleCollapse();
    };

    $scope.cancelFilters = function() {
      $scope.queuedApplicationPublicIds = angular.copy($scope.appliedApplicationPublicIds);
      $scope.toggleCollapse();
    };

    $scope.getSelectedApplicationNames = function() {
      if (!$scope.applications || $scope.appliedApplicationPublicIds.length === 0) {
        return 'All Applications';
      }
      var applicationNames = [];
      for (var i = 0; i < $scope.applications.length; i++) {
        var application = $scope.applications[i];
        if ($scope.appliedApplicationPublicIds.indexOf(application.publicId) > -1) {
          applicationNames.push(application.name);
        }
      }
      return applicationNames.join(', ');
    };

    $scope.toggleCollapse = function() {
      // Dropdown leaves artifact on screen w/o $timeout
      $timeout(function() {
        $('.accordion-body').collapse('toggle');
      }, 10);
    };
  }]);
}());