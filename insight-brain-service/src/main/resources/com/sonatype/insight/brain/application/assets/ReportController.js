/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var reportModule = angular.module('Report', ['CLMLocation']);

    reportModule.controller('ReportController', ['$scope', '$routeParams', '$http', 'CLMLocations', function ($scope, $routeParams, $http, clmLocations) {
        $http.get(clmLocations.getApplicationUrl($routeParams.applicationId), {
            params: { timestamp: new Date().getTime() }
        }).success(function (data) {
                $http.get(clmLocations.getActionStageUrl(), {
                    params: { timestamp: new Date().getTime() }
                }).success(function (stages) {
                        for (var i = 0; i < stages.length; i++) {
                            if (stages[i].id == data.policyEvaluation.stage.stageTypeId) {
                                data.policyEvaluation.stage.stageName = stages[i].name;
                                break;
                            }
                        }
                        $scope.application = data;
                    });
            });

        $scope.reportUrl = '../rest/report/' + $routeParams.applicationId + '/' + $routeParams.scanId + '/embedReport/index.html?readonly=true';
    }]);

    reportModule.directive('expandableIframe', function () {
        return {
            template: "<iframe ng-src='{{reportUrl}}' width='100%' height='1000px' border='0' frameborder='0' scrolling='yes' style='overflow:auto;'/>",
            compile: function () {
                function setDimensions() {
                    var iframe = angular.element('iframe');
                    if (!iframe) {
                        clearTimeout(setDimensions);
                        return;
                    }
                    var windowHeight = (window.innerHeight) ? window.innerHeight : $(document.body).getHeight(),
                        containerTop = iframe.offset().top,
                        bottomPadding = 20,
                        height = Math.max(400, windowHeight - containerTop - bottomPadding);

                    iframe.css({ 'height': height + 'px' });
                }

                function dedupe() {
                    clearTimeout(resizeTimeoutId);
                    resizeTimeoutId = setTimeout(setDimensions, 100);
                }

                var resizeTimeoutId;
                setTimeout(setDimensions, 100);
                window.onresize = dedupe;
            }
        };
    });
}());
