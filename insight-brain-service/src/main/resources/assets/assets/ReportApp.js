/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var reportApp = angular.module('reportApp', ['DashboardModule', 'ReportList', 'Report'], ['$stateProvider',
      '$routeProvider', '$urlRouterProvider', function($stateProvider, $routeProvider, $urlRouterProvider) {
        $routeProvider.when('', {
          redirectTo: '/reports'
        });
      }]);
}());

(function() {
  'use strict';

  var reportModule = angular.module('ReportList', ['ui.compat', 'AngularCommon', 'DashboardModule', 'CLMLocation'],
          ['$stateProvider', function($stateProvider) {
            $stateProvider.state('reports', {
              url: '/reports',
              templateUrl: '../assets/components/report-list.html?' + clmBuildTimestamp,
              controller: 'ReportListController'
            });
          }]).run(['serverStatus', function(serverStatus) {
    serverStatus.check().then(null, function(status) {
      if (status) {
        // TODO: some big error thing
      } else {
        // nothing to do, some redirect must've occurred
      }
    });
  }]);

  reportModule.controller('ReportListController', [
      '$scope',
      '$http',
      '$q',
      'CLMLocations',
      function($scope, $http, $q, clmLocations) {
        $scope.doLoad = function() {
          var promises = [$http.get(clmLocations.getActionStageUrl()),
              $http.get(clmLocations.getApplicationSummariesUrl(), {
                params: {
                  timestamp: new Date().getTime()
                }
              })];
          $scope.error = null;

          $q.all(promises).then(function(results) {
            $scope.stages = results[0].data;
            $scope.applications = results[1].data;
          }, function() {
            $scope.error = arguments[0];
          });
        };
        $scope.orderColumn = 'name';
        $scope.orderDirection = false;
        $scope.encodeURIComponent = window.encodeURIComponent;
        $scope.doLoad();
      }]);
}());
