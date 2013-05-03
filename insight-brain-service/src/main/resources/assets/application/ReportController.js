/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window, document */
(function () {
    'use strict';

    var reportModule = angular.module('Report', ['CLMLocation']);

    reportModule.controller('ReportController', ['$scope', '$routeParams', '$http', 'CLMLocations', function ($scope, $routeParams, $http, clmLocations) {
        $http.get(clmLocations.getApplicationUrl(decodeURIComponent($routeParams.encodedApplicationId)), {
            params: { timestamp: new Date().getTime() }
        }).success(function (data) {
            var stageId = decodeURIComponent($routeParams.encodedStageId),
                i;
            for (i = 0; i < data.policyEvaluations.length; i++) {
                if (data.policyEvaluations[i].stage.stageTypeId === stageId) {
                    $scope.policyEvaluation = data.policyEvaluations[i];
                    break;
                }
            }
            $scope.application = data;
            $scope.reportUrl = '../rest/report/' + $routeParams.encodedApplicationId + '/' + encodeURIComponent($scope.policyEvaluation.scanId) + '/embedReport/index.html?readonly=true';
            $http.get(clmLocations.getActionStageUrl(), {
                params: { timestamp: new Date().getTime() }
            }).success(function (stages) {
                var i;
                for (i = 0; i < stages.length; i++) {
                    if (stages[i].id == $scope.policyEvaluation.stage.stageTypeId) {
                        $scope.policyEvaluation.stage.stageName = stages[i].name;
                        break;
                    }
                }
            });
        });
    }]);

    reportModule.directive('expandableIframe', function () {
        return {
            template: "<iframe ng-src='{{reportUrl}}' width='100%' height='1000px' border='0' frameborder='0' scrolling='yes' style='overflow:auto;'/>",
            compile: function () {
                var resizeTimeoutId;

                function setDimensions() {
                    var iframe = angular.element('iframe');
                    if (!iframe) {
                        clearTimeout(resizeTimeoutId);
                        return;
                    }
                    var windowHeight = window.innerHeight || $(document.body).getHeight(),
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
            }
        };
    });
}());
