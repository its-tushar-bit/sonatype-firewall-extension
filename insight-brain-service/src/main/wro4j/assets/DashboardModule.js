/**
* @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
* Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
* "Sonatype" is a trademark of Sonatype, Inc.
*/
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  function filterToParams(filter, maxResults) {
    var params = {
      maxResults: maxResults + 1,
      applicationPublicIds: filter.applicationPublicIds,
      policyThreatCategories: filter.policyThreatTypes.length > 0 ?
                              filter.policyThreatTypes.join(',') : null,
      stageIds: filter.stageTypeIds,
      tagIds: filter.applicationTagIds
    };
    //don't add this unless outside of defaults
    var threatLvls = filter.policyThreatLevel;
    if (threatLvls[0] > 0 || threatLvls[1] < 10) {
      params.policyThreatLevelRange = threatLvls.join();
    }
    return params;
  }

  var dashboardModule = angular.module('DashboardModule', ['ui.router', 'Stores', 'AngularCommon'], ['$stateProvider', function($stateProvider) {
    $stateProvider.state('dashboard', {
      url: '/dashboard',
      templateUrl: '../dashboard-assets/dashboard.html?' + clmBuildTimestamp,
      controller: 'DashboardController'
    });
  }]);

  dashboardModule.controller('DashboardController', ['$scope', '$q', '$http', 'CLMLocations', function($scope, $q, $http, CLMLocations) {
    $scope.maxResults = 100;
    $scope.noDataNewestRiskMessage = 'No data for the last 30 days available given the applied filters and available permissions.';

    $scope.doLoad = function() {
      $scope.error = null;

      $http.get(CLMLocations.getPolicyViolationsUrl(), {
        params: angular.extend(filterToParams($scope.filters, $scope.maxResults), {
          newest: true
        })
      }).success(function (data) {
        $scope.newestRisks = data;
      }).error(function () {
        $scope.error = arguments;
      });
    };
    $scope.$watch('filters', function (newFilters) {
      if (newFilters) {
        $scope.doLoad();
      }
    });

    $scope.riskTable = 'policy-violations';
  }]);

  dashboardModule.directive('riskTable', [function() {
    return {
      scope: {
        risks: '=',
        title: '@',
        riskId: '@',
        emptyMessage: '=',
        maxResults: '='
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
            $http.get(CLMLocations.getApplicationTagsUrl())
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

  dashboardModule.controller('policyRiskTableController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, CLMLocations) {
    $scope.noDataHighestRiskMessage = 'No data available given the applied filters and available permissions.';
    $scope.$watch('filters', function (newFilter) {
      if (newFilter) {
        $http.get(CLMLocations.getPolicyViolationsUrl(), {
          params : filterToParams($scope.filters, $scope.maxResults)
        }).success(function (data) {
          if (angular.equals(newFilter, $scope.filters)) {
            $scope.data = data;
          }
        }).error(function () {
          if (angular.equals(newFilter, $scope.filters)) {
            $scope.error = arguments;
          }
        });
      }
    });
  }]);

  dashboardModule.controller('componentRiskTableController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, CLMLocations) {
    $scope.$watch('filters', function (newFilter) {
      if (newFilter) {
        $http.get(CLMLocations.getComponentRisksUrl(), {
          params : filterToParams($scope.filters, $scope.maxResults)
        }).success(function (data) {
          if (angular.equals(newFilter, $scope.filters)) {
            $scope.data = data;
          }
        }).error(function () {
          if (angular.equals(newFilter, $scope.filters)) {
            $scope.error = arguments;
          }
        });
      }
    });
  }]);
}());