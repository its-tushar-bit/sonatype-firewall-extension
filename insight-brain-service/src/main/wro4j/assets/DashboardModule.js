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

  dashboardModule.controller('DashboardController', ['$scope', '$q', 'ApplicationStore', '$http', 'CLMLocations', '$timeout', 'StageTypeStore',
    function($scope, $q, ApplicationStore, $http, CLMLocations, $timeout, StageTypeStore) {
    $scope.appliedApplicationPublicIds = [];
    $scope.queuedApplicationPublicIds = [];
    $scope.appliedPolicyThreatCategories = [];
    $scope.queuedPolicyThreatCategories = [];
    $scope.appliedStageTypeIds = [];
    $scope.queuedStageTypeIds = [];
    $scope.maxResults = 20;

    $scope.noDataHighestRiskMessage = 'No data available given the applied filters and available permissions.';
    $scope.noDataNewestRiskMessage = 'No data for the last 30 days available given the applied filters and available permissions.';

    $scope.doLoad = function() {
      $scope.error = null;
      var appliedPolicyThreatCategoryParam =
          $scope.appliedPolicyThreatCategories.length > 0 ? $scope.appliedPolicyThreatCategories.join(',') : null;
      var promises = [
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params: {
            maxResults: $scope.maxResults,
            applicationPublicIds: $scope.appliedApplicationPublicIds,
            policyThreatCategories: appliedPolicyThreatCategoryParam,
            stageIds: $scope.appliedStageTypeIds
          }
        }),
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params: {
            maxResults: $scope.maxResults,
            applicationPublicIds: $scope.appliedApplicationPublicIds,
            policyThreatCategories: appliedPolicyThreatCategoryParam,
            stageIds: $scope.appliedStageTypeIds,
            newest: true
          }
        })
      ];
      $q.all(promises).then(function(data) {
        $scope.highestRisks = data[0].data;
        $scope.newestRisks = data[1].data;
      }, function(error) {
        $scope.error = error;
      });
    };

    function loadFilters() {
      var promises = [
        ApplicationStore.get(),
        StageTypeStore.get()
      ];
      $q.all(promises).then(function(data) {
        $scope.applications = data[0];
        $scope.stageTypes = data[1];

        $scope.policyThreatCategories = [
          {id:'security', name:'Security'},
          {id:'license', name:'License'},
          {id:'quality', name:'Quality'},
          {id:'other', name:'Other'}
        ];
      });
    }

    loadFilters();
    $scope.doLoad();
    $scope.applyFilters = function() {
      $scope.appliedApplicationPublicIds = $scope.queuedApplicationPublicIds;
      $scope.appliedPolicyThreatCategories = $scope.queuedPolicyThreatCategories;
      $scope.appliedStageTypeIds = $scope.queuedStageTypeIds;
      $scope.doLoad();
      $scope.toggleCollapse();
    };

    $scope.cancelFilters = function() {
      $scope.queuedApplicationPublicIds = angular.copy($scope.appliedApplicationPublicIds);
      $scope.queuedPolicyThreatCategories = angular.copy($scope.appliedPolicyThreatCategories);
      $scope.queuedStageTypeIds = angular.copy($scope.appliedStageTypeIds);
      $scope.toggleCollapse();
    };

    $scope.applicationNameFor = function(applicationId) {
      for (var i = 0; i < $scope.applications.length; i++) {
        var application = $scope.applications[i];
        if (application.publicId === applicationId) {
          return application.name;
        }
      }
    };

    $scope.policyThreatCategoryNameFor = function(policyThreatCategoryId) {
      for (var i = 0; i < $scope.policyThreatCategories.length; i++) {
        var policyThreatCategory = $scope.policyThreatCategories[i];
        if (policyThreatCategory.id === policyThreatCategoryId) {
          return policyThreatCategory.name;
        }
      }
    };
    
    $scope.stageTypeNameFor = function(stageTypeId) {
      for (var i = 0; i < $scope.stageTypes.length; i++) {
        if ($scope.stageTypes[i].id === stageTypeId) {
          return $scope.stageTypes[i].name;
        }
      }
    };

    $scope.toggleCollapse = function() {
      // Dropdown leaves artifact on screen w/o $timeout
      $timeout(function() {
        $('.filter-edit').collapse('toggle');
      }, 10);
    };
  }]);

  dashboardModule.directive('riskTable', [function() {
    return {
      scope: {
        risks: '=',
        title: '@',
        riskId: '@',
        emptyMessage: '='
      },
      templateUrl: 'risk-table',
      controller: ['$scope', function($scope) {
        $scope.orderColumn = 'threatLevel';
        $scope.orderDirection = true;
        $scope.setSort = function (field) {
          $scope.orderDirection = (field === $scope.orderColumn && !$scope.orderDirection) ||
            // Threat level is orderDirection true by default
            (field === 'threatLevel' && (!$scope.orderDirection || field !== $scope.orderColumn));
          $scope.orderColumn = field;
        };
      }]
    };
  }]);
}());