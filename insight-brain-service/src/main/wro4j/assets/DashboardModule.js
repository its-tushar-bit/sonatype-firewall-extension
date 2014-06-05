/**
* @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
* Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
* "Sonatype" is a trademark of Sonatype, Inc.
*/
/* global angular, clmBuildTimestamp, $, d3, AngularUtils */
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

      var threatLvls = filter.policyThreatLevel;
      if (threatLvls) {
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
            // Attach the popover to the body of the document, as certain browsers (IE9) will fail otherwise.
            container: 'body',
            // Add our styling, pathnames-popover and pathnames-popover-content, to the popover template.
            template: '<div class="popover pathnames-popover"><div class="pathnames-popover-arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content pathnames-popover-content"><p></p></div></div></div>'
          };
          
          // Configure the popover so that it functions modally.
          element.popover(options);
          // The position function will be modified to move the popover over the text
          // within the TD dynamically based on the current element positions.
          element.data('popover').getOriginalPosition = element.data('popover').getPosition;
          
          // Display the popover when hovering over the component element, but only hide
          // the popover when the mouse leaves the popover.
          element.on('mouseenter', function() {
            // Add a slight delay so popovers aren't appearing as the user moves
            // their mouse across the table.
            setTimeout(function() {
              if(!element.is(':hover')) {
                return;
              }
              
              // Calculate the position of the popover in reference to the left adjusted text.
              var emphasizedPathnameElement = element.find('em');
              if (emphasizedPathnameElement.length > 0) {
                var popoverLeftPosition = emphasizedPathnameElement.offset().left;
                element.data('popover').getPosition = function () {
                  var originalPosition = this.getOriginalPosition();
                  originalPosition.left = popoverLeftPosition;
                  // Set the width to the width of the popover so that it stays aligned
                  // with the left of the text.
                  originalPosition.width = this.tip()[0].offsetWidth;
                  return originalPosition;
                };
              }
              
              element.popover('show');
              
              // Because we've used the table element as the popover container,
              // start selecting from the parent of the tr (td -> tr -> table).
              // We also only want to select the popover of the current element, not all popovers.
              var popover = $('.popover:contains(\'' + pathnames[0] + '\')');
              popover.on('mouseleave', function() {
                element.popover('hide');
              });
            }, 50);
          });

          // Also, hide the popover if the mouse leaves the component element and is no
          // longer hovering over the popover.
          element.on('mouseleave', function() {
            var popover = $('.popover:contains(\'' + pathnames[0] + '\')');
            setTimeout(function() {
              if (popover.length > 0 && !popover.is(':hover')) {
                element.popover('hide');
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
            scope.stageTypes = angular.copy(data[1]); // Stores should not be modified directly
            scope.applicationTags = data[3].data;

            for (var i = 0; i < scope.stageTypes.length; i++) {
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

        scope.$on('reloadFilter', function(){
          loadFilters();
        });
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
    function createFilterWatch($scope, $rootScope, $http, Dialog) {
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
              if (arguments[1] && arguments[1] === 403) {
                Dialog.open({
                  title : 'Filter invalid',
                  body : 'Your filter settings have become invalid because of permission changes, click OK to reload.',
                  buttons : [{
                    name : 'OK',
                    click: function() {
                      $rootScope.$broadcast('reloadFilter');
                    }
                  }]
                });
              } else {
                $scope.error = arguments;
              }
            }
          });
        }
      };
    }

    return {
      transclude: true,
      templateUrl: 'dashboard-table',
      controller: ['$scope', '$rootScope', '$http', 'Dialog', function($scope, $rootScope, $http, Dialog) {
        var filterChangedFn = createFilterWatch($scope, $rootScope, $http, Dialog);
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
    return dashboardTable(CLMLocations.getApplicationRisksUrl());
  }]);

  dashboardModule.directive('componentRiskTable', ['CLMLocations', function(CLMLocations) {
    return dashboardTable(CLMLocations.getComponentRisksUrl());
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

  dashboardModule.filter('stageTypeSort', function () {
    function priority(stage){
      var ordinal = null;
      switch (stage.stageTypeId || stage.id) {
        case 'build':
          ordinal = 0;
          break;
        case 'stage-release':
          ordinal = 1;
          break;
        case 'release':
          ordinal = 2;
          break;
        case 'operate':
          ordinal = 3;
          break;
      }
      return ordinal;
    }

    return function (input) {
      if (input) {
        return input.sort(function (a, b) {
          return priority(a) - priority(b);
        });
      }
    };
  });

  dashboardModule.controller('NewestRiskTableController', [
    '$scope', 'StageTypeStore', '$filter', function($scope, StageTypeStore, $filter) {
      StageTypeStore.getDashboardStages().then(function(data) {
        $scope.stageTypes = [];
        // Copy values so we can modify the content for this use-case
        for (var i = 0; i < data.length; i++) {
          $scope.stageTypes.push({
            stageTypeId: data[i].id,
            name: data[i].name
          });
        }
      });
      // to aid sortability, copy the times from each stage to a property on the row
      for (var i = 0; i < $scope.data.length; i++) {
        var risk = $scope.data[i];
        if (risk.stageDetails) {
          for (var j = 0; j < risk.stageDetails.length; j++) {
            var stageDetail = risk.stageDetails[j];
            var propName = $filter('removeDashes')(stageDetail.stageTypeId) + 'Time';
            risk[propName] = stageDetail.time > 0 ? stageDetail.time : null;
          }
        }
      }
    }
  ]);

  dashboardModule.controller('componentRiskTable', ['$scope', function($scope) {
    $scope.totalRisk = 0;
    $scope.criticalRisk = 0;
    $scope.severeRisk = 0;
    $scope.moderateRisk = 0;
    $scope.lowRisk = 0;
    angular.forEach($scope.data, function(data) {
      $scope.totalRisk = Math.max($scope.totalRisk, data.score);
      $scope.criticalRisk = Math.max($scope.criticalRisk, data.scoreCritical);
      $scope.severeRisk = Math.max($scope.severeRisk, data.scoreSevere);
      $scope.moderateRisk = Math.max($scope.moderateRisk, data.scoreModerate);
      $scope.lowRisk = Math.max($scope.lowRisk, data.scoreLow);
    });
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

    $scope.totalRisk = 0;
    $scope.criticalRisk = 0;
    $scope.severeRisk = 0;
    $scope.moderateRisk = 0;
    $scope.lowRisk = 0;
    angular.forEach($scope.data, function(data) {
      $scope.totalRisk = Math.max($scope.totalRisk, data.totalApplicationRisk.totalRisk);
      $scope.criticalRisk = Math.max($scope.criticalRisk, data.totalApplicationRisk.criticalRisk);
      $scope.severeRisk = Math.max($scope.severeRisk, data.totalApplicationRisk.severeRisk);
      $scope.moderateRisk = Math.max($scope.moderateRisk, data.totalApplicationRisk.moderateRisk);
      $scope.lowRisk = Math.max($scope.lowRisk, data.totalApplicationRisk.lowRisk);
    });
  }]);

  dashboardModule.directive('alphaBackground', [function() {
    return {
      scope: {
        alphaBackground: '@'
      },
      link: function(scope, element) {
        var backgroundProperty = 'background-color';
        var background = element.css(backgroundProperty);
        var backgroundMatched = /(rgb|rgba)\((.*)\)/.exec(background);
        if (!backgroundMatched || backgroundMatched.length < 2) {
          return;
        }
        var rgb = backgroundMatched[2].split(',').map(function(color) {
          return parseInt(color);
        });

        // enforce a lower bound on all alpha values
        scope.alphaBackground = (9 * scope.alphaBackground + 1) / 10;

        if (rgb.length === 4) {
          rgb[3] = scope.alphaBackground;
        } else {
          rgb.push(scope.alphaBackground);
        }
        background = 'rgba(' + rgb.join(',') + ')';
        element.css(backgroundProperty, background);
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
          $scope.error = null;
          $http.get(CLMLocations.getDashboardViewingSummaryUrl(), {
            params: filterToParams($scope.filters)
          }).success(function (data) {
            $scope.data = data;
          }).error(function () {
            $scope.error = arguments;
          });
        };

        $scope.formatPercentage = AngularUtils.formatPercentage;

        $scope.$watch('filters', function(){
          $scope.doLoad();
        });
      }]
    };
  });

  dashboardModule.directive('dashboardComponentMatchResults', function() {
    return {
      restrict: 'A',
      templateUrl: 'dashboard-component-match-results',
      scope: {
        filters: '=filters'
      },
      controller: [
        '$scope', 'CLMLocations', '$http', function($scope, CLMLocations, $http) {
          $scope.doLoad = function() {
            $scope.data = null;
            $scope.error = null;
            $http.get(CLMLocations.getDashboardComponentMatchSummaryUrl(), {
              params: filterToParams($scope.filters)
            }).success(function(data) {
              $scope.data = data;

              $scope.data.items = [{
                count: $scope.data.exact,
                colorCss: 'match-exact',
                label: 'Exact Match'
              },{
                count: $scope.data.similar,
                colorCss: 'match-partial',
                label: 'Similar Match'
              },{
                count: $scope.data.unknown,
                colorCss: 'match-none',
                label: 'Unknown'
              }];
            }).error(function() {
              $scope.error = arguments;
            });
          };

          $scope.$watch('filters', function() {
            $scope.doLoad();
          });
        }
      ]
    };
  });

  /**
   * expects a model in the following format
   * {
       total: 7, //the total count of all items
   *   items: [{
   *     count: 1, //the count associated with the item]
   *     label: 'label', //the label put in the legend
   *     colorCss: 'css-class' //the css class to assign to the graph section for this item,
   *       and the legend for this item
   *   }]
   * }
   *
   * ex.
   * <div horizontal-percentage-graph model="myData"></div>
   *
   * This directive should be moved someplace more generic, but I don't feel like bloating
   * AngularCommon at the moment..
   */
  dashboardModule.directive('horizontalPercentageGraph', function() {
    return {
      restrict: 'A',
      scope: {
        model: '=model'
      },
      templateUrl: 'horizontal-percentage-graph',
      controller: ['$scope', function ($scope) {
        $scope.formatPercentage = AngularUtils.formatPercentage;
      }]
    };
  });

  /**
   * Provides all data required for the policy summary view.
   */
  dashboardModule.directive('dashboardPolicySummary', function() {
    return {
      restrict: 'A',
      scope: {
        filters: '=filters'
      },
      templateUrl: 'dashboard-policy-summary',
      controller: [
        '$scope', 'CLMLocations', '$http', function($scope, CLMLocations, $http) {

          function delta(counts) {
            return counts[counts.length-1] - counts[0];
          }

          function calculateRunningTotals(counts) {
            var runningTotals = [];
            for (var i = 0; i < counts.length; i++) {
              if (i > 0) {
                runningTotals[i] = counts[i] + runningTotals[i - 1];
              }
              else {
                runningTotals[i] = counts[i];
              }
            }
            return runningTotals;
          }

          function generateModel(policySummaryData) {
            var newCounts = policySummaryData.newCounts,
              fixedCounts = policySummaryData.fixedCounts,
              unresolvedCounts = policySummaryData.unresolvedCounts;
            return [
              {
                name: 'New',
                counts: newCounts[newCounts.length-1],
                delta: delta(newCounts),
                barChartData : newCounts,
                sparklineData: calculateRunningTotals(newCounts),
                inverseGreen: true
              },
              {
                name: 'Fixed',
                counts: fixedCounts[fixedCounts.length-1],
                delta: delta(fixedCounts),
                barChartData : fixedCounts,
                sparklineData: calculateRunningTotals(fixedCounts),
                inverseGreen: false
              },
              {
                name: 'Unresolved',
                counts: unresolvedCounts[unresolvedCounts.length-1],
                delta: delta(unresolvedCounts),
                barChartData : unresolvedCounts,
                sparklineData: calculateRunningTotals(unresolvedCounts),
                inverseGreen: true
              }
            ];
          }

          $http.get(CLMLocations.getPolicySummaryUrl(), {
            params: filterToParams($scope.filters)
          }).success(function(data) {
            $scope.policySummaryData = generateModel(data);
          }).error(function() {
            $scope.error = arguments;
          });
        }
      ]
    };
  });

  dashboardModule.directive('valueBars', function() {
    return {
      restrict: 'A',
      scope: {
        data: '=data'
      },
      replace: true,
      link: function(scope, element) {
        var data = scope.data;
        var width = element.width(), height = element.height();

        // scale the graph to keep the zero line centered only if there are negative numbers
        var min = d3.min(data);
        var max = d3.max([Math.abs(min), d3.max(data)]);
        var extents = [((min >= 0) ? 0 : -max), max];
        var y = d3.scale.linear()
          .domain(extents)
          .range([0, height]);
        var x = d3.scale.ordinal()
          .domain(d3.range(data.length))
          .rangeRoundBands([0, width], 0.2);

        // allow the svg element to take up all the parent's space
        var chart = d3.select(element[0])
          .append('svg')
          .attr('width', '100%')
          .attr('height', '100%');

        chart.selectAll('g')
          .data(data).enter()
          .append('rect')
          .attr('x', function(d, i) {
            return x(i);
          })
          .attr('y', function(d) {
            // render starting above or at the center line, depending on +/-
            return (d > 0) ? height - y(d) : y(0);
          })
          .attr('height', function(d) {
            return Math.abs(y(d) - y(0));
          })
          .attr('width', x.rangeBand())
          .attr('class', function(d) {
            return (d > 0) ? 'bar positive' : 'bar negative';
          })
          // tooltip to highlight actual figures involved
          .append('title').text(function(d){ return d;});

        // baseline will render at bottom for positive data, center for positive/negative data
        // Need to fudge a bit if we're drawing at the bottom of the container to account for the stroke-width
        var baseline = ((min < 0) ? y(0) : y(max) - 0.25);
        chart.append('line')
          .attr('x1', x(0))
          .attr('x2', width - x.rangeBand() + 1)
          .attr('y1', baseline)
          .attr('y2', baseline)
          .attr('stroke', '#777777')
          .attr('stroke-width', '0.5');
      }
    };
  });

  dashboardModule.directive('sparkline', ['windowEventsFactory', function(windowEventsFactory) {
    return {
      scope:{
        data: '=',
        inverseGreen: '='
      },
      link: function postLink(scope, element) {
        function sparkline() {
          var config = {
            width : element.width() || 100,
            height: element.height() || 25
          };
          var data = scope.data || [];

          d3.select(element[0]).select('svg').remove();

          var guideHeight = 12, guidePadding = 3, transitionDuration = 50;
          function getGuidePositions(snapX, snapY, yValue) {
            // Calculate rectangle width. Each digit takes ~7 pixels with 7 pixels for single digits and 6 pixel pad
            var digits = yValue === 0 ? 0 : Math.log(yValue)/Math.log(10);
            var width = Math.floor(digits) * 7 + 7 + 2 * guidePadding;
            var x = Math.max(Math.min(snapX - width / 2, config.width - width), 0);
            var y;
            if (snapY > config.height / 2) {
              y = Math.max(snapY - guideHeight - guidePadding, 0);
            } else {
              y = Math.min(snapY + guidePadding, config.height - guideHeight);
            }
            return {
              width: width,
              rect: {
                x: x,
                y: y
              },
              text: {
                x: x + guidePadding,
                y: y + guideHeight - guidePadding + 1
              }
            };
          }

          var svg = d3.select(element[0]).append('svg')
            .attr('width', config.width)
            .attr('height', config.height)
            .append('g');

          var yScale = d3.scale.linear().range([config.height, 0]),
            pastX = d3.scale.linear().range([0, config.width - config.width / data.length]),
            recentX = d3.scale.linear().range([config.width - config.width / data.length, config.width]);

          pastX.domain([0, data.length-2]);
          recentX.domain([0, 1]);
          yScale.domain([0, d3.max(data)]);

          var area = d3.svg.area().x(function(d, index) {
            return pastX(index);
          }).y0(config.height).y1(function(d) {
            return yScale(d);
          });

          var line = d3.svg.line().x(function(d, index) {
            return pastX(index);
          }).y(function(d) {
            return yScale(d);
          });

          svg.append('path')
            .datum(data.slice(0, data.length - 1))
            .attr('d', area)
            .attr('class', 'fill base');

          svg.append('path')
            .datum(data.slice(0, data.length - 1))
            .attr('d', line)
            .attr('class', 'line base');

          area = d3.svg.area().x(function(d, index) {
            return recentX(index);
          }).y0(config.height).y1(function(d) {
            return yScale(d);
          });

          line = d3.svg.line().x(function(d, index) {
            return recentX(index);
          }).y(function(d) {
            return yScale(d);
          });

          var trailingClass = (data[data.length - 2] - data[data.length - 1] > 0) ^ scope.inverseGreen ? 'red' : 'green';

          svg.append('path')
            .datum(data.slice(data.length - 2))
            .attr('d', area)
            .attr('class', 'fill ' + trailingClass);

          svg.append('path')
            .datum(data.slice(data.length - 2))
            .attr('d', line)
            .attr('class', 'line ' + trailingClass);

          var guideLine = svg.append('line').attr({
            opacity: 0,
            x1: 0,
            y1: 0,
            x2: 0,
            y2: config.height
          }).attr('class', 'guideline');

          var circlePoint = svg.append('circle').attr({
            opacity: 0,
            r: 3
          }).attr('class', 'guide-circle');

          var guideRectangle = svg.append('rect').attr({
            opacity: 0,
            rx: 4,
            ry: 4,
            height: guideHeight
          }).attr('class', 'guide-rect');

          var guideText = svg.append('text').attr({
            opacity: 0
          }).attr('class', 'guide-text');

          var hoverElements = [ guideLine, circlePoint, guideRectangle, guideText ];

          var guideSpace = svg.append('rect').attr({
            w: 0,
            h: 0,
            width: config.width,
            height: config.height,
            fill: 'transparent'
          });

          guideSpace.on('mouseover', function() {
            angular.forEach(hoverElements, function(element) {
              element.transition(transitionDuration).attr('opacity', 1);
            });
          }).on('mousemove', function() {
            var position = d3.mouse(this),
              x = position[0],
              dataX = Math.round(pastX.invert(x)),
              snapX = pastX(dataX),
              yValue = data[dataX],
              snapY = yScale(yValue);

            circlePoint.attr('cx', snapX).attr('cy', snapY);

            var guidePositions = getGuidePositions(snapX, snapY, yValue);
            guideRectangle.attr({
              width: guidePositions.width,
              x: guidePositions.rect.x,
              y: guidePositions.rect.y
            });
            guideText.attr({
              x: guidePositions.text.x,
              y: guidePositions.text.y
            }).text(yValue);

            d3.select(element[0]).select('.guideline').attr('transform', function() {
              return 'translate(' + snapX + ',0)';
            });
          });

          d3.select(element[0]).on('mouseout', function() {
            var position = d3.mouse(this),
              x = position[0],
              y = position[1];
            if (x < 0 || x > config.width || y < 0 || y > config.height) {
              angular.forEach(hoverElements, function(element) {
                element.transition(transitionDuration).attr('opacity', 0);
              });
            }
          });
        }

        windowEventsFactory.addResizeHandler(scope, element, sparkline);
        sparkline();
      }
    };
  }]);
  
  dashboardModule.filter('removeDashes', function() {
    return function(input) {
      return input.replace('-', '');
    };
  });

  dashboardModule.factory('windowEventsFactory', ['$window', function($window) {
    return {
      addResizeHandler: function(scope, element, callBack) {
        var width = element.width();
        var height = element.height();

        function callBackWrapper() {
          var newWidth = element.width();
          var newHeight = element.height();
          if (newWidth !== width || newHeight !== height) {
            width = newWidth;
            height = newHeight;
            callBack();
          }
        }

        angular.element($window).on('resize', callBackWrapper);
        scope.$on('$destroy', function() {
          angular.element($window).off('resize', callBackWrapper);
        });
      }
    };
  }]);
}());