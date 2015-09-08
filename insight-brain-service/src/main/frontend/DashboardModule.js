/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
      params.applicationIds = filter.applicationIds;
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

  var dashboardModule = angular.module('DashboardModule', ['ui.router', 'Stores', 'AngularCommon', 'ComponentModule',
    'FilterModule', 'ComponentDisplay'],
    // To avoid hacking dependency order, states must be declared with their parent.
    // Fixed https://github.com/angular-ui/ui-router/pull/492
    ['$stateProvider', function($stateProvider) {
    $stateProvider.state('dashboard', {
      url: '/dashboard',
      templateUrl: 'dashboard/dashboard.html?' + clmBuildTimestamp,
      abstract: true,
      data : {
        title : 'Dashboard',
        crumb: 'Dashboard'
      }
    }).state('dashboard.overview', {
      parent: 'dashboard',
      url: '',
      abstract: true,
      controller: 'DashboardController',
      templateUrl: 'dashboard/overview.html?' + clmBuildTimestamp
    }).state('dashboard.overview.newest-risk', {
      parent: 'dashboard.overview',
      url: '/newest-risk',
      templateUrl: 'dashboard/newest-risk.html?' + clmBuildTimestamp,
      data: {
        crumb: 'Newest Risk'
      }
    }).state('dashboard.overview.components', {
      parent: 'dashboard.overview',
      url: '/components',
      templateUrl: 'dashboard/components.html?' + clmBuildTimestamp,
      data: {
        crumb: 'By Component'
      }
    }).state('dashboard.overview.applications', {
      parent: 'dashboard.overview',
      url: '/applications',
      templateUrl: 'dashboard/applications.html?' + clmBuildTimestamp,
      data: {
        crumb: 'By Application'
      }
    }).state('dashboard.component', {
      parent: 'dashboard',
      url: '/component/{hash}',
      controller: 'componentController',
      templateUrl: 'dashboard/component.html?' + clmBuildTimestamp,
      data: {
        crumb: 'Component Details'
      }
    });
  }]);

  dashboardModule.controller('DashboardController', ['$scope', '$modal', function($scope, $modal) {
    $scope.maxResults = 100;
    $scope.showTrendDialog = function() {

      $modal.open({
        backdrop: 'static',
        keyboard : false,
        templateUrl : 'policy-trends-dialog',
        windowClass : 'dashboard-policy-trend-dialog',
        controller: 'PolicyTrendController',
        resolve: {
          filters: function() {
            return $scope.filters;
          }
        }
      });
    };
  }]);

  dashboardModule.directive('pathnamesPopover', function() {
    var uniqueCounter = 0;
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watch(attrs.pathnamesPopover, function(pathnames) {
          var uniqueId = 'pathname-popover-' + uniqueCounter;
          uniqueCounter++;

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
            template: '<div data-popup-id="' + uniqueId + '" class="popover pathnames-popover"><div class="pathnames-popover-arrow"></div>' +
              '<div class="popover-inner"><h3 class="popover-title"></h3>' +
              '<div class="popover-content pathnames-popover-content"><p></p></div></div></div>'
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

              var popover = $('.pathnames-popover[data-popup-id=' + uniqueId + ']');
              popover.on('mouseleave', function() {
                element.popover('hide');
              });
            }, 50);
          });

          // Also, hide the popover if the mouse leaves the component element and is no
          // longer hovering over the popover.
          element.on('mouseleave', function() {
            var popover = $('.pathnames-popover[data-popup-id=' + uniqueId + ']');
            setTimeout(function() {
              if (popover.length > 0 && !popover.is(':hover')) {
                element.popover('hide');
              }
            }, 100);
          });
          
          // When the element is removed we need to remove the popover as well.
          scope.$on('$destroy', function() {
            var popover = $('.pathnames-popover[data-popup-id=' + uniqueId + ']');
            if (popover.length > 0) {
              popover.remove();
            }
          });
          
        });
      }
    };
  });

  function watchFilter($scope) {
    $scope.$watch('filters', function(newFilter){
      if (newFilter) {
        $scope.doLoad();
      }
    });
  }

  function getTableDirective(urlField) {
    return ['$timeout', '$window', 'maximizeHeightService', 'windowEventsFactory', 'CLMLocations', function ($timeout, $window, maximizeHeightService, windowEventsFactory, CLMLocations) {
      function createFilterWatch($scope, $rootScope, $http, Dialog, ApplicationStore) {
        return function (newFilter) {
          function isOverlapping(min, max, policyThreatLevel) {
            return min <= policyThreatLevel[1] && policyThreatLevel[0] <= max;
          }

          if (newFilter) {
            $scope.error = $scope.data = null;
            var params = filterToParams($scope.filters, $scope.maxResults);

            $scope.policyThreatLevelCategories = {
              low : isOverlapping(0, 1, $scope.filters.policyThreatLevel),
              moderate : isOverlapping(2, 3, $scope.filters.policyThreatLevel),
              severe : isOverlapping(4, 7, $scope.filters.policyThreatLevel),
              critical : isOverlapping(8, 10, $scope.filters.policyThreatLevel)
            };

            $http.get(CLMLocations[urlField](), {
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
                        //make sure to get any stale apps out of the app list
                        ApplicationStore.refresh();
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
        link : function (scope, element) {

          function updateDimensions() {
            var container = $('.scrollable-container', element);
            if (container.length > 0) {
              timerId = maximizeHeightService.updateDimensions(container) || timerId;
            }
          }

          var timerId;
          function dedupe() {
            if (timerId) {
              $timeout.cancel(timerId);
            }
            timerId = $timeout(updateDimensions, 20);
          }

          scope.$watch('data', function (newValue, oldValue) {
            if (newValue && !oldValue) {
              if (!$.browser.msie || $.browser.version > 8) {
                $timeout(updateDimensions, 100);
                $($window).resize(dedupe);
              }
            }
            else if (!newValue) {
              $($window).unbind('resize', dedupe);
            }
          });
          scope.$on('$destroy', function () {
            $($window).unbind('resize', dedupe);
          });
        },
        controller: ['$scope', '$rootScope', '$http', 'Dialog', 'ApplicationStore', function($scope, $rootScope, $http, Dialog, ApplicationStore) {
          var filterChangedFn = createFilterWatch($scope, $rootScope, $http, Dialog, ApplicationStore);
          $scope.doLoad = function () {
            filterChangedFn($scope.filters);
          };
          $scope.$watch('filters', filterChangedFn);
        }]
      };
    }];
  }

  dashboardModule.directive('newestRiskTable', getTableDirective('getNewestRisksUrl'));

  dashboardModule.directive('applicationRiskTable', getTableDirective('getApplicationRisksUrl'));

  dashboardModule.directive('componentRiskTable', getTableDirective('getComponentRisksUrl'));

  /**
   * Remove stages which are not part of the filter
   */
  dashboardModule.filter('stageFilter', function () {
    return function (input, filter) {
      if (angular.isArray(input) && filter && filter.stageTypeIds.length > 0) {
        for (var i=0; i<input.length; i++) {
          if ($.inArray(input[i].id || input[i].stageTypeId, filter.stageTypeIds) === -1) {
            input.splice(i,1);
            --i;
          }
        }
      }
      return input;
    };
  });

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
    '$scope', 'StageTypeStore', 'ComponentDisplayNameUtil', '$filter',
    function($scope, StageTypeStore, ComponentDisplayNameUtil, $filter) {
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
      // to aid sortability:
      // - copy the times from each stage to a property on the row
      // - provide a single sortable string for the component name
      for (var i = 0; i < $scope.data.length; i++) {
        var risk = $scope.data[i];
        if (risk.stageDetails) {
          for (var j = 0; j < risk.stageDetails.length; j++) {
            var stageDetail = risk.stageDetails[j];
            var propName = $filter('removeDashes')(stageDetail.stageTypeId) + 'Time';
            risk[propName] = stageDetail.time > 0 ? stageDetail.time : null;
          }
        }
        if (risk.displayName) {
          risk.gavName = ComponentDisplayNameUtil.renderToString(risk.displayName);
        }
        else {
          risk.gavName = risk.pathnames ? $filter('fileName')(risk.pathnames[0]) : 'Unknown';
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

  dashboardModule.controller('applicationRiskTable', ['$scope', '$filter', function ($scope, $filter) {
    function updateApplications() {
      $scope.applications = $filter('orderBy')($filter('limitTo')($scope.data, $scope.maxResults), $scope.getSortField());
      updateStripes();
    }
    function updateStripes() {
      $scope.striped = [];
      var striped = false;
      angular.forEach($scope.applications, function (application, index) {
        $scope.striped[index] = (index === 0) || (!striped || $scope.isExpanded($scope.applications[index - 1]));
        striped = $scope.striped[index];
      });
    }

    $scope.$watch('data', updateApplications);
    $scope.$watch(function () {
      return $scope.getSortField()[0];
    }, updateApplications);

    $scope.$watch('expanded', updateStripes, true);
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

  function extractColumn(orderedColumn) {
    if (orderedColumn.indexOf('-') === 0) {
      return orderedColumn.substring(1);
    } else {
      return orderedColumn;
    }
  }

  dashboardModule.directive('sortColumns', function () {
    return {
      require : '^sortable',
      scope: {
        field: '@sortColumns',     // comma separated list
        inverted: '@?sortInverted', // is the data logically inverted, i.e. AGE vs TIME
        centered : '@?'
      },
      transclude : true,
      template : '<a class="sort-column" ng-click="setSort()">' +
                   '<i ng-if="centered" class="sonatype-icons emptyIconGlyph"></i> ' + // should help center
                   '<span ng-transclude></span> <i class="sonatype-icons" ng-class="{ up : isUp(), down : isDown(), emptyIconGlyph : !isUp() && !isDown() }"></i></a>',
      link : function (scope, element, attrs, sortableCtrl) {
        var mainSort = scope.field.split(',')[0];
        var isInverted = scope.inverted === 'true';

        scope.setSort = function () {
          sortableCtrl.setSort(scope.field.split(','));
        };

        scope.isUp = function() {
          var sortColumn = extractColumn(sortableCtrl.sortFields[0]);
          var reversed = sortColumn !== sortableCtrl.sortFields[0];
          var currentColumn = extractColumn(mainSort);
          return sortColumn === currentColumn && (isInverted ? reversed : !reversed);
        };

        scope.isDown = function() {
          var sortColumn = extractColumn(sortableCtrl.sortFields[0]);
          var reversed = sortColumn !== sortableCtrl.sortFields[0];
          var currentColumn = extractColumn(mainSort);
          return sortColumn === currentColumn && (!isInverted ? reversed : !reversed);
        };
      }
    };
  });

  dashboardModule.directive('sortable', function () {
    return {
      require : 'sortable',
      controller : ['$scope', function ($scope) {
        var me = this;
        me.sortFields = [];

        $scope.getSortField = function () {
          return me.sortFields;
        };
        me.setSort = function (newFields) {
          if (angular.equals(me.sortFields, newFields)) {
            var column = extractColumn(newFields[0]);
            if (me.sortFields[0] !== column) {
              me.sortFields[0] = column;
            } else {
              me.sortFields[0] = '-' + column;
            }
          } else {
            me.sortFields = newFields;
          }
        };
      }],
      link : function (scope, element, attrs, sortable) {
        sortable.sortFields = attrs.sortable.split(',');
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

        watchFilter($scope);
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

          watchFilter($scope);
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

  dashboardModule.controller('PolicyTrendController', [
    '$scope', 'CLMLocations', '$http', 'filters', function($scope, CLMLocations, $http, filters) {
      function delta(counts) {
        return counts.reduce(function(a, b) {
          return a + b;
        });
      }

      function calculateRunningTotals(counts, startValue) {
        var runningTotals = [startValue];
        for (var i = 0; i < counts.length; i++) {
          runningTotals[i + 1] = counts[i] + runningTotals[i];
        }
        return runningTotals;
      }

      function generateModel(policySummaryData) {
        var weeklyDeltaNew = policySummaryData.weeklyDeltaNew,
            weeklyDeltaFixed = policySummaryData.weeklyDeltaFixed,
            weeklyDeltaUnresolved = policySummaryData.weeklyDeltaUnresolved,
            weeklyDeltaWaived = policySummaryData.weeklyDeltaWaived,
            totalNew = policySummaryData.totalNew,
            totalFixed = policySummaryData.totalFixed,
            currentUnresolved = policySummaryData.currentUnresolved,
            totalWaived = policySummaryData.totalWaived,
            newDelta = delta(weeklyDeltaNew),
            fixedDelta = delta(weeklyDeltaFixed),
            unresolvedDelta = delta(weeklyDeltaUnresolved),
            waivedDelta = delta(weeklyDeltaWaived);
        return [
          {
            name: 'Pending',
            counts: currentUnresolved,
            avg: policySummaryData.ageAverageUnresolved,
            p90: policySummaryData.agePercentile90Unresolved,
            delta: unresolvedDelta,
            barChartData: weeklyDeltaUnresolved,
            sparklineData: calculateRunningTotals(weeklyDeltaUnresolved, currentUnresolved - unresolvedDelta),
            naturalOrder: false
          },
          {
            name: 'Waived',
            counts: totalWaived,
            avg: policySummaryData.ageAverageWaived,
            p90: policySummaryData.agePercentile90Waived,
            delta: waivedDelta,
            barChartData: weeklyDeltaWaived,
            sparklineData: calculateRunningTotals(weeklyDeltaWaived, totalWaived - waivedDelta),
            naturalOrder: false
          },
          {
            name: 'Fixed',
            counts: totalFixed,
            avg: policySummaryData.ageAverageFixed,
            p90: policySummaryData.agePercentile90Fixed,
            delta: fixedDelta,
            barChartData: weeklyDeltaFixed,
            sparklineData: calculateRunningTotals(weeklyDeltaFixed, totalFixed - fixedDelta),
            naturalOrder: true
          },
          {
            name: 'Discovered',
            counts: totalNew,
            delta: newDelta,
            barChartData: weeklyDeltaNew,
            sparklineData: calculateRunningTotals(weeklyDeltaNew, totalNew - newDelta)
          }
        ];
      }

      $scope.doLoad = function() {
        $scope.data = null;
        $scope.error = null;
        $http.get(CLMLocations.getPolicySummaryUrl(), {
          params: filterToParams($scope.filters)
        }).success(function(data) {
          $scope.policySummaryData = generateModel(data);
        }).error(function() {
          $scope.error = arguments;
        });
      };

      $scope.filters = filters;
      $scope.doLoad();
    }
  ]);

  dashboardModule.directive('valueBars',  ['windowEventsFactory', function(windowEventsFactory) {
    return {
      restrict: 'A',
      scope: {
        data: '=data'
      },
      replace: true,
      link: function(scope, element) {
        function barChart() {
          var data = scope.data;

          d3.select(element[0]).select('svg').remove();

          // allow the svg element to take up all the parent's space
          var chart = d3.select(element[0])
            .append('svg')
            .attr('class', 'chart');

          var width = $(chart[0]).width() || 100,
              height = $(chart[0]).height() || 25;

          var y = d3.scale.linear()
              .domain(d3.extent(data))
            .range([0, height]);
          var x = d3.scale.ordinal()
            .domain(d3.range(data.length))
            .rangeRoundBands([0, width], 0.2);

          // baseline will render at bottom for positive data, somewhere in between for positive/negative data
          // Need to fudge a bit if we're drawing at the bottom of the container to account for the stroke-width
          var baseline = Math.min(height - 0.50, height - y(0));

          chart.selectAll('g')
            .data(data).enter()
            .append('rect')
            .attr('x', function(d, i) {
              return x(i);
            })
            .attr('y', function(d) {
              // render starting above or at the center line, depending on +/-
              return (d > 0) ? height - y(d) : baseline;
            })
            .attr('height', function(d) {
              return Math.abs(y(d) - y(0));
            })
            .attr('width', x.rangeBand())
            .attr('class', function(d) {
              return (d > 0) ? 'bar up' : 'bar down';
            })
            // tooltip to highlight actual figures involved
            .append('title').text(function(d) {
              return d;
            });

          chart.append('line')
            .attr('x1', x(0))
            .attr('x2', x(data.length - 1) + x.rangeBand())
            .attr('y1', baseline)
            .attr('y2', baseline)
            .attr('stroke', '#777777')
            .attr('stroke-width', '0.5');
        }

        windowEventsFactory.addResizeHandler(scope, element, barChart);
        barChart();
      }
    };
  }]);

  dashboardModule.directive('sparkline', [function() {
    return {
      scope:{
        data: '='
      },
      link: function postLink(scope, element) {
        function sparkline() {
          var data = scope.data || [],
              config = {};

          d3.select(element[0]).select('svg').remove();

          var guideHeight = 16, guidePadding = 3, transitionDuration = 50, digitLength = 10, graphPadding = 2;
          function getGuidePositions(snapX, snapY, yValue) {
            // Calculate rectangle width. Each digit takes ~7 pixels with 7 pixels for single digits and 6 pixel pad
            var digits = yValue === 0 ? 0 : Math.log(yValue)/Math.log(10);
            var width = Math.floor(digits) * digitLength + digitLength + 2 * guidePadding;
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
            .attr('class', 'chart')
            .append('g');

          config.width = $(svg[0][0]).parent().width() || 100;
          config.height = $(svg[0][0]).parent().height() || 25;

          var yScale = d3.scale.linear().range([config.height - graphPadding, graphPadding]),
            pastX = d3.scale.linear().range([graphPadding, config.width - config.width / data.length - graphPadding]),
            recentX = d3.scale.linear().range([config.width - config.width / data.length - graphPadding, config.width - graphPadding]);

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

          var trailingClass = (data[data.length - 1] - data[data.length - 2] > 0) ? 'up' : 'down';

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

        sparkline();
      }
    };
  }]);

  dashboardModule.filter('removeDashes', function() {
    return function(input) {
      return input.replace('-', '');
    };
  });

  /**
   * Filter an array to ensure that null entries are always at the end.
   */
  dashboardModule.filter('emptyToEnd', function() {
    return function(array, key) {
      if (!angular.isArray(array)) {
        return;
      }
      // in the event of a compound sort, use the first field
      var sortField = angular.isArray(key) ? key[0] : key;
      var sortColumn = extractColumn(sortField);
      return array.filter(function(item) {
        return item[sortColumn];
      }).concat(array.filter(function(item) {
        return !item[sortColumn];
      }));
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

  dashboardModule.directive('modalHelp', ['$modal', function($modal) {
    return {
      restrict: 'A',
      scope: {
        modalHelp : '@',
        modalHelpClass : '@',
        modalHelpTrigger : '@'
      },
      link: function (scope, element) {
        var helpClass = 'modal-help';
        if(scope.modalHelpClass) {
          helpClass = scope.modalHelpClass;
        }
        
        var trigger = 'click';
        if(scope.modalHelpTrigger) {
          trigger = scope.modalHelpTrigger;
        }
        
        var options = {
          templateUrl: scope.modalHelp,
          windowClass: helpClass
        };

        element.on(trigger, function() {
          $modal.open(options);
        });
      }
    };
  }]);

}());
