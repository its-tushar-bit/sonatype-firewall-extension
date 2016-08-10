/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.directive('newestRiskTable', getTableDirective('getNewestRisksUrl'));

  dashboardUtilsModule.directive('applicationRiskTable', getTableDirective('getApplicationRisksUrl'));

  dashboardUtilsModule.directive('componentRiskTable', getTableDirective('getComponentRisksUrl'));

  function getTableDirective(urlField) {
    return [
      '$timeout', '$window', 'maximizeHeightService', 'windowEventsFactory', 'CLMLocations', 'filterToParams',
      function($timeout, $window, maximizeHeightService, windowEventsFactory, CLMLocations, filterToParams) {
        function createFilterWatch($scope, $rootScope, $http, Dialog, ApplicationStore) {
          return function(newFilter) {
            function isOverlapping(min, max, policyThreatLevel) {
              return min <= policyThreatLevel[1] && policyThreatLevel[0] <= max;
            }

            if (newFilter) {
              $scope.error = $scope.data = null;
              var params = filterToParams($scope.filters, $scope.maxResults);

              $scope.policyThreatLevelCategories = {
                low: isOverlapping(0, 1, $scope.filters.policyThreatLevel),
                moderate: isOverlapping(2, 3, $scope.filters.policyThreatLevel),
                severe: isOverlapping(4, 7, $scope.filters.policyThreatLevel),
                critical: isOverlapping(8, 10, $scope.filters.policyThreatLevel)
              };

              $http.post(CLMLocations[urlField](), params
              ).success(function(data) {
                if (angular.equals(newFilter, $scope.filters)) {
                  $scope.data = data;
                }
              }).error(function() {
                if (angular.equals(newFilter, $scope.filters)) {
                  if (arguments[1] && arguments[1] === 403) {
                    Dialog.open({
                      title: 'Filter invalid',
                      body: 'Your filter settings have become invalid because of permission changes, click OK to reload.',
                      buttons: [
                        {
                          name: 'OK',
                          click: function() {
                            //make sure to get any stale apps out of the app list
                            ApplicationStore.refresh();
                            $rootScope.$broadcast('reloadFilter');
                          }
                        }
                      ]
                    });
                  }
                  else {
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
          link: function(scope, element) {

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

            scope.$watch('data', function(newValue, oldValue) {
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
            scope.$on('$destroy', function() {
              $($window).unbind('resize', dedupe);
            });
          },
          controller: [
            '$scope', '$rootScope', '$http', 'Dialog', 'ApplicationStore',
            function($scope, $rootScope, $http, Dialog, ApplicationStore) {
              var filterChangedFn = createFilterWatch($scope, $rootScope, $http, Dialog, ApplicationStore);
              $scope.doLoad = function() {
                filterChangedFn($scope.filters);
              };
              $scope.$watch('filters', filterChangedFn);
            }
          ]
        };
      }
    ];
  }

}());
