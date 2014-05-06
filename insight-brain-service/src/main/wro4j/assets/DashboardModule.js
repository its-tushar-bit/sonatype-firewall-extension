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

  dashboardModule.controller('DashboardController', ['$scope', '$q', '$http', 'CLMLocations', function($scope, $q, $http, CLMLocations) {

    $scope.maxResults = 20;

    $scope.noDataHighestRiskMessage = 'No data available given the applied filters and available permissions.';
    $scope.noDataNewestRiskMessage = 'No data for the last 30 days available given the applied filters and available permissions.';

    $scope.doLoad = function() {
      $scope.error = null;
      var params = {
        maxResults: $scope.maxResults,
        applicationPublicIds: $scope.filters.applicationPublicIds,
        policyThreatCategories: $scope.filters.policyThreatTypes.length > 0 ?
                                $scope.filters.policyThreatTypes.join(',') : null,
        stageIds: $scope.filters.stageTypeIds,
        tagIds: $scope.filters.applicationTagIds
      };

      //don't add this unless outside of defaults
      var threatLvls = $scope.filters.policyThreatLevel;
      if (threatLvls[0] > 0 || threatLvls[1] < 10) {
        params.policyThreatLevelRange = threatLvls.join();
      }
      var promises = [
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params: angular.copy(params)
        }),
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params: angular.extend(params, {
            newest: true
          })
        })
      ];
      $q.all(promises).then(function(data) {
        $scope.highestRisks = data[0].data;
        $scope.newestRisks = data[1].data;
      }, function(error) {
        $scope.error = error;
      });
    };
    $scope.$watch('filters', function (newFilter) {
      if (newFilter) {
        $scope.doLoad();
      }
    });

    $scope.highestRisk = 'policy-violations';
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

  dashboardModule.directive('dashboardFilter',
          ['$timeout', '$http', '$q', 'ApplicationStore', 'OrganizationStore', 'StageTypeStore', 'CLMLocations',
          function ($timeout, $http, $q, ApplicationStore, OrganizationStore, StageTypeStore, CLMLocations) {
    return {
      scope : {
        filter : '=dashboardFilter',
        toggle : '='
      },
      templateUrl : 'dashboard-filter',
      link : function (scope) {
        function resetFilter() {
          scope.dirtyFilter = angular.copy(scope.filter);
        }
        function loadFilters() {
          var promises = [
            ApplicationStore.get(),
            StageTypeStore.get(),
            OrganizationStore.get(),
            $http.get(CLMLocations.getApplicationTags())
          ];
          $q.all(promises).then(function(data) {
            scope.applications = data[0];
            scope.stageTypes = data[1];
            scope.applicationTags = data[3].data;

            var organizations = data[2];
            angular.forEach(scope.applicationTags, function(tag) {
              for (var i = 0; i < organizations.length; i++) {
                if (tag.organizationId === organizations[i].id) {
                  tag.owner = organizations[i].name;
                  break;
                }
              }
            });

            scope.policyThreatTypes = [
              {id:'security', name:'Security'},
              {id:'license', name:'License'},
              {id:'quality', name:'Quality'},
              {id:'other', name:'Other'}
            ];
          });
        }

        scope.filter = {
          applicationPublicIds: [],
          policyThreatTypes: [],
          stageTypeIds: [],
          applicationTagIds: [],
          policyThreatLevel: [0,10]
        };
        resetFilter();

        // TODO we should use load error n' stuff
        loadFilters();

        scope.applicationNameFor = function(applicationId) {
          for (var i = 0; i < scope.applications.length; i++) {
            var application = scope.applications[i];
            if (application.publicId === applicationId) {
              return application.name;
            }
          }
        };

        scope.policyThreatTypeNameFor = function(policyThreatTypeId) {
          for (var i = 0; i < scope.policyThreatTypes.length; i++) {
            var policyThreatType = scope.policyThreatTypes[i];
            if (policyThreatType.id === policyThreatTypeId) {
              return policyThreatType.name;
            }
          }
        };

        scope.stageTypeNameFor = function(stageTypeId) {
          for (var i = 0; i < scope.stageTypes.length; i++) {
            if (scope.stageTypes[i].id === stageTypeId) {
              return scope.stageTypes[i].name;
            }
          }
        };

        scope.applicationTagNameFor = function(applicationTagId) {
          for (var i = 0; i < scope.applicationTags.length; i++) {
            var applicationTag = scope.applicationTags[i];
            if (applicationTag.id === applicationTagId) {
              return applicationTag.name;
            }
          }
        };

        scope.applyFilter = function () {
          // We copy it so the object is not shared
          scope.filter = angular.copy(scope.dirtyFilter);
          scope.toggle();
        };

        scope.cancelFilter = function () {
          resetFilter();
          scope.toggle();
        };

        scope.toggle = function() {
          // Dropdown leaves artifact on screen w/o $timeout
          $timeout(function() {
            $('.filter-edit').collapse('toggle');
          }, 10);
        };
      }
    };
  }]);

  //integrating the bootstrap-slider
  dashboardModule.directive('slider', function() {
    return {
      restrict: 'A',
      scope: {
        model: '=ngModel',
        min: '@',
        max: '@'
      },
      priority : 99,
      link: function(scope, element) {
        $(element).slider({
          min: parseInt(scope.min),
          max: parseInt(scope.max),
          value: scope.model,
          orientation: 'horizontal',
          selection: 'after',
          handle: 'square',
          tooltip: 'none',
          labels: true,
          showHandleValues: true
        }).on('slide', function(event){
          scope.$apply(function () {
            scope.model = event.value;
          });
        });
      }
    };
  });
}());