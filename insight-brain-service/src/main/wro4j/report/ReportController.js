/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window */
(function() {
  'use strict';

  var reportModule = angular.module('Report', ['CLMLocation', 'ui.router', 'AngularCommon', 'CommonServices'], [
    '$stateProvider', function($stateProvider) {
      $stateProvider.state('report', {
        url: '/reports/{publicId}/{scanId}',
        controller: 'ReportController',
        templateUrl: '../report-assets/report/report.html?' + clmBuildTimestamp
      });
    }
  ]);

  reportModule.controller('ReportController', [
    '$scope', '$state', '$http', '$q', 'CLMLocations', function($scope, $state, $http, $q, clmLocations) {
      $scope.doLoad = function() {
        $scope.error = null;

        var actionStagePromise = $http.get(clmLocations.getActionStageUrl()),
        appScanSummary = $http.get(clmLocations.getApplicationScanSummary($state.params.publicId, $state.params.scanId));

        $scope.reportUrl = '../rest/report/' + encodeURIComponent($state.params.publicId) + '/' +
          encodeURIComponent($state.params.scanId) + '/browseReport/index.html';

        $q.all([actionStagePromise, appScanSummary]).then(function(results) {
          $scope.application = results[1].data;

          angular.forEach($scope.application.policyEvaluations, function (evaluation) {
            if (evaluation.scanId === $state.params.scanId) {
              $scope.policyEvaluation = evaluation;
            }
          });

          for (var i = 0; i < results[0].data.length; i++) {
            if (results[0].data[i].id === $scope.policyEvaluation.stage.stageTypeId) {
              $scope.policyEvaluation.stage.stageName = results[0].data[i].name;
              break;
            }
          }
        }, function() {
          $scope.error = arguments[0];
        });
      };
      $scope.doLoad();
    }
  ]);

  reportModule.directive('expandableIframe', function() {
    return {
      template: '<iframe ng-src="{{url}}" width="100%" height="1000px" border="0" frameborder="0" scrolling="yes" style="overflow:auto;"/>',
      scope: {
        url: '=expandableIframe'
      },
      link: function(scope) {
        var resizeTimeoutId;

        function setDimensions() {
          var iframe = angular.element('iframe');
          if (!iframe || iframe.length === 0) {
            clearTimeout(resizeTimeoutId);
            return;
          }
          var windowHeight = $(window).height(),
              containerTop = iframe.offset().top,
              bottomPadding = 20,
              height = Math.max(400, windowHeight - containerTop - bottomPadding);

          iframe.css({ 'height': height + 'px' });
        }

        function dedupe() {
          clearTimeout(resizeTimeoutId);
          resizeTimeoutId = setTimeout(setDimensions, 100);
        }

        setTimeout(setDimensions, 100);
        window.onresize = dedupe;
        scope.$on('$destroy', function() {
          clearTimeout(resizeTimeoutId);
        });
      }
    };
  });
}());
