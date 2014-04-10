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
    $scope.appliedPolicyThreatCategories = [];
    $scope.queuedPolicyThreatCategories = [];
    $scope.filtersExpanded = false;
    $scope.maxResults = 20;

    $scope.doLoad = function() {
      $scope.error = null;
      var appliedPolicyThreatCategoryParam =
          $scope.appliedPolicyThreatCategories.length > 0 ? $scope.appliedPolicyThreatCategories.join(',') : null;
      var promises = [
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params: {
            maxResults: $scope.maxResults,
            applicationPublicIds: $scope.appliedApplicationPublicIds,
            policyThreatCategories: appliedPolicyThreatCategoryParam
          }
        })
      ];
      $q.all(promises).then(function(data) {
        $scope.highestRisks = data[0].data;
      }, function(error) {
        $scope.error = error;
      });
    };

    function loadFilters() {
      var promises = [
        ApplicationStore.get()
      ];
      $q.all(promises).then(function(data) {
        $scope.applications = data[0];

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
      $scope.doLoad();
      $scope.toggleCollapse();
    };

    $scope.cancelFilters = function() {
      $scope.queuedApplicationPublicIds = angular.copy($scope.appliedApplicationPublicIds);
      $scope.queuedPolicyThreatCategories = angular.copy($scope.appliedPolicyThreatCategories);
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

    $scope.toggleCollapse = function() {
      // Dropdown leaves artifact on screen w/o $timeout
      $timeout(function() {
        $('.accordion-body').collapse('toggle');
      }, 10).then(function(){
        $scope.filtersExpanded = $('.accordion-body').hasClass('in');
      });
    };
  }]);
}());