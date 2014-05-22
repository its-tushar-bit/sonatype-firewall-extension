/**
* @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
* Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
* "Sonatype" is a trademark of Sonatype, Inc.
*/
/* global angular, clmBuildTimestamp, $ */
(function() {
  'use strict';

  function filterToParams(filter, maxResults) {
    var params = {};
    if (maxResults) {
      params.maxResults = maxResults + 1;
    }
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

  dashboardModule.controller('DashboardController', ['$scope', function($scope) {
    $scope.maxResults = 100;
    $scope.riskTable = 'newest-risk';
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
            pathnamesTitle = 'Component Path, Found in ' + pathnames.length + ' Locations';
          }

          var options = {
            trigger: 'manual',
            placement: 'top',
            content: pathnames[0],
            title: pathnamesTitle,
            // Attach the popover to the parent, as placing the popover in the td could resize it.
            container: element.parent(),
            // Add our styling, pathnames-popover and pathnames-popover-content, to the popover template.
            template: '<div class="popover pathnames-popover"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content pathnames-popover-content"><p></p></div></div></div>'
          };
          
          // Configure the popover so that it functions modally.
          var componentElement = $(element);
          componentElement.popover(options);
          // Display the popover when hovering over the component element, but only hide
          // the popover when the mouse leaves the popover.
          componentElement.on('mouseenter', function() {
            componentElement.popover('show');
            // Because we've used the parent of the element as the popover container,
            // start selecting from the parent.
            var popover = componentElement.parent().children('.popover');
            popover.on('mouseleave', function() {
              componentElement.popover('hide');
            });
          });
          // Also, hide the popover if the mouse leaves the component element and is no
          // longer hovering over the popover.
          componentElement.on('mouseleave', function() {
            var popover = componentElement.parent().children('.popover');
            setTimeout(function() {
              if (!popover.is(':hover')) {
                componentElement.popover('hide');
              }
            }, 100);
          });
          
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

            for (var i=0; i<scope.stageTypes.length; i++) {
              if (scope.stageTypes[i].id === 'develop') {
                scope.stageTypes.splice(i, 1);
                break;
              }
            }

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

  function dashboardTable(url) {
    function createFilterWatch($scope, $http) {
      return function (newFilter) {
        if (newFilter) {
          $scope.error = $scope.data = null;
          var params = filterToParams($scope.filters, $scope.maxResults);

          $http.get(url, {
            params : params
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

    return {
      transclude: true,
      templateUrl: 'dashboard-table',
      controller: ['$scope', '$http', function($scope, $http) {
        var filterChangedFn = createFilterWatch($scope, $http);
        $scope.doLoad = function () {
          filterChangedFn($scope.filters);
        };
        $scope.$watch('filters', filterChangedFn);
      }]
    };
  }

  dashboardModule.directive('newestRiskTable', ['CLMLocations', function(CLMLocations) {
    return dashboardTable(CLMLocations.getNewestRisksUrl());
  }]);

  dashboardModule.directive('applicationRiskTable', ['CLMLocations', function(CLMLocations){
    return dashboardTable(CLMLocations.getApplicationRisksUrl(), null, 'No data available given the applied filters and available permissions');
  }]);

  dashboardModule.directive('componentRiskTable', ['CLMLocations', function(CLMLocations) {
    return dashboardTable(CLMLocations.getComponentRisksUrl(), null, 'No data available given the applied filters and available permissions');
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
        function loadCurrentState() {
          var state = $state.$current;
          var states = [];
          while (state && state.name) {
            if (state.name !== 'dashboard.overview') {
              states.unshift(angular.extend(stateLookup[state.name],
                {
                  // dashboard is an abstract state an ui-sref will throw an exception rather than routing to default
                  state: state.name === 'dashboard' ? 'dashboard.overview' : state.name
                }
              ));
            }
            state = state.parent;
          }
          scope.states = states;
        }

        scope.$watch('$state.$current', loadCurrentState);
      }
    };
  }]);

  dashboardModule.controller('applicationRiskTable', ['$scope', function ($scope) {
    $scope.encodeURIComponent = window.encodeURIComponent;

    $scope.expanded = {};

    $scope.canExpand = function (application) {
      return application.stageRisks.length > 0;
    };
    $scope.isExpanded = function (application) {
      return $scope.expanded[application.applicationId];
    };
    $scope.expand = function (application) {
      if ($scope.canExpand(application)) {
        $scope.expanded[application.applicationId] = !$scope.expanded[application.applicationId];
      }
    };
  }]);

  dashboardModule.directive('sortColumn', function () {
    return {
      require : '^sortable',
      scope : {
        field : '@sortColumn',
        inverse : '@?sortInverse'
      },
      transclude : true,
      template : '<a ng-click="setSort()"><span ng-transclude></span> <i class="sonatype-icons" ng-class="{ up : isUp(), down : isDown(), emptyIconGlyph : !isUp() && !isDown() }"></i></a>',
      link : function (scope, element, attrs, sortableCtrl) {
        scope.setSort = function () {
          sortableCtrl.setSort(scope.field, scope.inverse);
        };

        scope.isUp = function () {
          var reverse = sortableCtrl.sort.reverse,
              inverse = scope.inverse;
          return scope.field === sortableCtrl.sort.field && ((inverse && !reverse) || (!inverse && reverse));
        };

        scope.isDown = function () {
          var reverse = sortableCtrl.sort.reverse,
              inverse = scope.inverse;
          return scope.field === sortableCtrl.sort.field && !((inverse && !reverse) || (!inverse && reverse));
        };
      }
    };
  });

  dashboardModule.directive('sortable', function () {
    return {
      require : 'sortable',
      controller : ['$scope', function ($scope) {
        var me = this;
        me.sort = {};

        $scope.getSortReverse = function () {
          return me.sort.reverse;
        };
        $scope.getSortField = function () {
          return me.sort.field;
        };
        me.setSort = function (field, defaultReverse) {
          if (me.sort.field === field) {
            me.sort.reverse = ! me.sort.reverse;
          } else {
            me.sort = {
              field : field,
              reverse : defaultReverse
            };
          }
        };
      }],
      link : function (scope, element, attrs, sortable) {
        sortable.sort = {
          field : attrs.sortableField,
          reverse : attrs.sortableReverse
        };
      }
    };
  });

  dashboardModule.directive('dashboardViewSummary', function() {
    return {
      restrict: 'A',
      templateUrl: 'dashboard-view-summary',
      scope: {
        filters : '=filters'
      },
      controller: ['$scope', '$http', 'CLMLocations', function ($scope, $http, CLMLocations) {
        $scope.doLoad = function(){
          $scope.data = null;
          $http.get(CLMLocations.getDashboardViewingSummaryUrl(), {
            params: filterToParams($scope.filters)
          }).success(function (data) {
            $scope.data = data;
          }).error(function () {
            $scope.error = arguments;
          });
        };

        $scope.formatPercentage = function(matched, total){
          if (!total) {
            return "0";
          }

          return (matched / total * 100).toFixed(0);
        };

        $scope.$watch('filters', function(){
          $scope.doLoad();
        });
      }]
    };
  });
}());