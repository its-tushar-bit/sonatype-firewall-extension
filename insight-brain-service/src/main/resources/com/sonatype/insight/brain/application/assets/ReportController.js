/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var reportModule = angular.module('Report', []);

    reportModule.controller('ReportController', ['$scope', '$routeParams', function ($scope, $routeParams) {
        $scope.reportUrl = '/rest/report/' + $routeParams.applicationId + '/' + $routeParams.scanId + '/embedReport/index.html?readonly=true';
    }]);

    reportModule.directive('expandableIframe', function () {
        return {
            template: "<iframe ng-src='{{reportUrl}}' width='100%' height='1000px' border='0' frameborder='0' scrolling='yes' style='overflow:auto;'/>",
            compile: function () {
                function setDimensions() {
                    var iframe = $('iframe');
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

                setTimeout(setDimensions, 100);
                $('.container').css({ 'width': '955px' });
            }
        };
    });
}());
