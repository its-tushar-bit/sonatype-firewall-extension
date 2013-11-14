/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var reportListModule = angular.module('ReportViolations', ['AngularCommon', 'CLMLocation']);

  reportListModule.controller('ReportViolationsController', ['$scope', '$http', '$q', 'CLMLocations',
    function($scope, $http, $q, clmLocations) {

      $scope.orderColumn = 'name';
      $scope.orderDirection = false;
      $scope.encodeURIComponent = window.encodeURIComponent;

      $scope.doLoad = function() {
        $scope.error = null;

        var promises = [];

        promises.push($http.get(clmLocations.getActionStageUrl()));
        promises.push($http.get(clmLocations.getApplicationSummariesUrl(), {
          params: {
            timestamp: new Date().getTime()
          }
        }));

        $q.all(promises).then(function(results) {
          $scope.stages = results[0].data;
          $scope.applications = results[1].data;
        }, function() {
          $scope.error = arguments[0];
        });
      };
      $scope.doLoad();
    }]);
}());