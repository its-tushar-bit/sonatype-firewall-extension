/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var applicationModule = angular.module('Management', ['InsightAngularCommon', 'CLMLocation']);

    applicationModule.controller('ManagementController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, clmLocations) {
        $http.get(clmLocations.getApplicationsUrl()).success(function (data) {
            $scope.applications = data;
        }).error($scope.showServerError);

        $scope.buttonclick = function () {
            $scope.showError('test');
        };
    }]);
}());