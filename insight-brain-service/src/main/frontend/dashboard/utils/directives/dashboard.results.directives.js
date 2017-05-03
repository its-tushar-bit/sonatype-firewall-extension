/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.directive('violationsResults', getTableDirective('getNewestRisks'));

  dashboardUtilsModule.directive('applicationsResults', getTableDirective('getApplicationRisks'));

  dashboardUtilsModule.directive('componentsResults', getTableDirective('getComponentRisks'));

  function getTableDirective(serviceMethod) {
    return [
      'dashboard.data.service', 'filterToParams',
      function(dashboardDataService, filterToParams) {
        function createFilterWatch($scope, $rootScope, Dialog, ApplicationStore) {
          return function(newFilter) {
            if (newFilter && !$scope.needsAcknowledgement) {
              $scope.error = $scope.data = null;
              var params = filterToParams($scope.filters, $scope.maxResults);

              dashboardDataService[serviceMethod](params).then(function(results) {
                if (angular.equals(newFilter, $scope.filters)) {
                  $scope.data = results[0];
                  if ($scope.brew) {
                    $scope.brew.setSeriesInclusive(results[1]);
                    $scope.brew.setNumClasses(Math.min(7, results[1].length));
                    $scope.brew.classify('quantile');
                  }
                }
              }, function() {
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
          controller: [
            '$scope', '$rootScope', '$state', 'Dialog', 'ApplicationStore', 'ClassyBrew',
            function($scope, $rootScope, $state, Dialog, ApplicationStore, ClassyBrew) {
              var filterChangedFn = createFilterWatch($scope, $rootScope, Dialog, ApplicationStore);
              if ($state.is('dashboard.overview.components') || $state.is('dashboard.overview.applications')) {
                $scope.brew = ClassyBrew.create();
              }
              $scope.$watch('filters', filterChangedFn);
              $scope.goToComponentDetails = function(component) {
                $state.go('dashboard.component', {hash: component.hash});
              };
              $scope.getColor = function(score) {
                return $scope.brew.getColor(score);
              };
              $scope.getTextColorClass = function(score) {
                return score === 0 ? 'grey-text' : $scope.brew.isWhiteText(score) ? 'white-text' : undefined;
              };
              $scope.encodeURIComponent = window.encodeURIComponent;
            }
          ]
        };
      }
    ];
  }

}());
