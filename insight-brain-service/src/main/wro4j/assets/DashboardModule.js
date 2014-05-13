/**
* @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
* Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
* "Sonatype" is a trademark of Sonatype, Inc.
*/
/* global angular, clmBuildTimestamp, $ */
(function() {
  'use strict';

  function filterToParams(filter, maxResults) {
    var params = {
      maxResults: maxResults + 1
    };
    if (filter) {
      params.applicationPublicIds = filter.applicationPublicIds;
      params.policyThreatCategories = (filter.policyThreatTypes &&
          filter.policyThreatTypes.length > 0) ?
          filter.policyThreatTypes.join(',') : undefined;
      params.stageIds = filter.stageTypeIds;
      params.tagIds = filter.applicationTagIds;

      //don't add this unless outside of defaults
      var threatLvls = filter.policyThreatLevel;
      if (threatLvls && (threatLvls[0] > 0 || threatLvls[1] < 10)) {
        params.policyThreatLevelRange = threatLvls.join();
      }
    }
    return params;
  }

  var dashboardModule = angular.module('DashboardModule', ['ui.router', 'Stores', 'AngularCommon', 'CommonServices', 'ComponentModule'],
    // To avoid hacking dependency order, states must be declared with their parent.
    // Fixed https://github.com/angular-ui/ui-router/pull/492
    ['$stateProvider', function($stateProvider) {
    $stateProvider.state('dashboard', {
      url: '/dashboard',
      templateUrl: '../dashboard-assets/dashboard.html?' + clmBuildTimestamp,
      abstract: true
    }).state('dashboard.overview', {
      parent: 'dashboard',
      url: '',
      controller: 'DashboardController',
      templateUrl: '../dashboard-assets/overview.html?' + clmBuildTimestamp
    }).state('dashboard.component', {
      parent: 'dashboard',
      url: '/component/{hash}',
      controller: 'componentController',
      templateUrl: '../dashboard-assets/component.html?' + clmBuildTimestamp
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

  /**
   * Returns the last element of a path with the assumption that it is a file name. Path elements are assumed to be
   * delimited by a '/'.
   */
  dashboardModule.filter('fileName', function() {
    return function(path) {
      var pathDelimiter = '/';
      var stringPath = String(path);
      // Avoid checking the last character as paths might end in a delimiter.
      var lastIndexOfDelimiter = stringPath.lastIndexOf(pathDelimiter, stringPath.length - 2);

      if (lastIndexOfDelimiter > -1) {
        // If the last character is a delimiter, do not return it.
        if (stringPath.charAt(stringPath.length - 1) === pathDelimiter) {
          return stringPath.substring(lastIndexOfDelimiter + 1, stringPath.length - 1);
        }

        return stringPath.substring(lastIndexOfDelimiter + 1);
      }

      return stringPath;
    };
  });
  
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

  dashboardModule.directive('pathnamesPopover', function() {
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watch(attrs.pathnamesPopover, function(pathnames) {
          if (!pathnames) {
            return;
          }

          var pathnamesTitle = 'Component Path';
          if (pathnames.length > 1) {
            pathnamesTitle = 'Component Path, ' + pathnames.length + ' Locations';
          }

          var options = {
            trigger: 'hover',
            placement: 'top',
            content: pathnames[0],
            title: pathnamesTitle,
            // Attach the popover to the parent, as placing the popover in the td could resize it.
            container: element.parent()
          };
          $(element).popover(options);
        });
      }
    };
  });

  dashboardModule.directive('dashboardFilter',
          ['$timeout', '$http', '$q', 'ApplicationStore', 'OrganizationStore', 'StageTypeStore', 'CLMLocations', 'Messages',
          function ($timeout, $http, $q, ApplicationStore, OrganizationStore, StageTypeStore, CLMLocations, Messages) {
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
          //we don't want to update the data to be saved until they hit apply button
          scope.dirtyFilter = {
            applicationPublicIds: [],
            policyThreatTypes: [],
            stageTypeIds: [],
            applicationTagIds: [],
            policyThreatLevel: [0,10]
          };

          var promises = [
            ApplicationStore.get(),
            StageTypeStore.get(),
            OrganizationStore.get(),
            $http.get(CLMLocations.getApplicationTagsUrl()),
            $http.get(CLMLocations.getDashboardFilters())
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
              {id:'SECURITY', name:'Security'},
              {id:'LICENSE', name:'License'},
              {id:'QUALITY', name:'Quality'},
              {id:'OTHER', name:'Other'}
            ];

            if (data[4].data) {
              scope.filter = {
                applicationPublicIds: data[4].data.applicationFilters,
                policyThreatTypes: data[4].data.policyThreatCategoryFilters,
                stageTypeIds: data[4].data.stageTypeFilters,
                applicationTagIds: data[4].data.tagFilters,
                policyThreatLevel: [data[4].data.minPolicyThreatLevel, data[4].data.maxPolicyThreatLevel]
              };
              resetFilter();
            }
            else {
              //need to init the filter to something, to trigger a data load
              scope.filter = {};
            }
            scope.filtersLoaded = true;
          }, function(){
            scope.filtersLoaded = true;
          });
        }

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
          scope.applyError = null;
          // We copy it so the object is not shared
          scope.filter = angular.copy(scope.dirtyFilter);
          $http.put(CLMLocations.getDashboardFilters(), {
            applicationFilters : scope.filter.applicationPublicIds,
            policyThreatCategoryFilters : scope.filter.policyThreatTypes,
            stageTypeFilters : scope.filter.stageTypeIds,
            tagFilters : scope.filter.applicationTagIds,
            minPolicyThreatLevel : scope.filter.policyThreatLevel[0],
            maxPolicyThreatLevel : scope.filter.policyThreatLevel[1]
          }).then(function(){
            scope.toggle();
          }, function() {
            scope.applyError = Messages.getHttpErrorMessage(arguments);
          });
        };

        scope.cancelFilter = function () {
          resetFilter();
          scope.toggle();
        };

        scope.toggle = function() {
          $('.filter-edit').collapse('toggle');
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

        scope.$watch('model', function(newValue){
          $(element).slider('setValue', newValue);
        });
      }
    };
  });

  function createFilterWatch($scope, $http, url) {
    return function (newFilter) {
      if (newFilter) {
        $scope.error = $scope.data = null;

        $http.get(url, {
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
    };
  }

  dashboardModule.controller('policyRiskTableController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, CLMLocations) {
    $scope.noDataHighestRiskMessage = 'No data available given the applied filters and available permissions.';
    $scope.$watch('filters', createFilterWatch($scope, $http, CLMLocations.getPolicyViolationsUrl()));
  }]);

  dashboardModule.controller('componentRiskTableController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, CLMLocations) {
    $scope.$watch('filters', createFilterWatch($scope, $http, CLMLocations.getComponentRisksUrl()));
  }]);

  dashboardModule.directive('breadcrumb', ['$state', function($state) {
    var stateLookup = {
      'dashboard': {
        name: 'Dashboard',
        icon: 'sonatype-icons dashboard'
      },
      'dashboard.component': {
        name: 'Component Details'
      }
    };
    return {
      template: '<p class="nav-crumb"><a ng-repeat="state in states" ui-sref="{{state.state}}">' +
                '<i ng-if="!$first" class="glyphicons-sonatype play"></i>' +
                '<i ng-if="state.icon" class="{{state.icon}}"></i>&nbsp;{{state.name}}' +
                '</a></p>',
      link: function(scope) {
        var states = [];
        var state = $state.$current;
        while (state && state.name) {
          states.unshift(angular.extend(stateLookup[state.name],
            {
              // dashboard is an abstract state an ui-sref will throw an exception rather than routing to default
              state: state.name === 'dashboard' ? 'dashboard.overview' : state.name
            }
          ));
          state = state.parent;
        }
        scope.states = states;
      }
    };
  }]);
}());