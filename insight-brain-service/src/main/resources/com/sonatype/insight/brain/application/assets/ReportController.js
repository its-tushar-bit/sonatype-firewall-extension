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
}());
