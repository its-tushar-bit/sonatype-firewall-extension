/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var reportApp = angular.module('reportApp', ['DashboardModule', 'ReportList', 'Report'], 
      [
        '$urlRouterProvider',
        function($urlRouterProvider) {
          $urlRouterProvider.when('', '/reports');
        }
      ]);
}());

(function() {
  'use strict';

  var reportModule = angular.module('ReportList', ['ui.router', 'AngularCommon', 'DashboardModule', 'CLMLocation'],
          ['$stateProvider', function($stateProvider) {
            $stateProvider.state('reports', {
              url: '/reports',
              templateUrl: '../assets/components/report-list.html?' + clmBuildTimestamp,
              controller: 'ReportListController'
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
