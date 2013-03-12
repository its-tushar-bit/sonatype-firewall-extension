/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var applicationModule = angular.module('Management', ['InsightAngularCommon', 'Hudson', 'CLMLocation']);

    applicationModule.controller('ManagementController', ['$scope', '$http', 'hudson', 'CLMLocations', function ($scope, $http, hudson, clmLocations) {
        $scope.orderColumn = 'publicId';
        $scope.orderDirection = true;

        function onAddApplicationError(data, status, headersFn, config) {
            var header = headersFn();
            if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
                $scope.addApplicationError = 'Server Error';
            } else {
                $scope.addApplicationError = data;
            }
        }

        $http.get(clmLocations.getActionStageUrl()).success(function (data) {
            $scope.stages = data;
        }).error($scope.showServerError);

        $http.get(clmLocations.getApplicationsUrl()).success(function (data) {
            $scope.applications = data;
        }).error($scope.showServerError);

        $scope.registerNewApplication = function () {
            $('#addApplicationModal').modal('show');
        };

        $scope.addApplication = function () {
            hudson.post(clmLocations.getApplicationsUrl(), $scope.applicationPublicId).success(function (application) {
                $scope.applications.push(application);
                $('#addApplicationModal').modal('hide');
            }).error(onAddApplicationError);
        };

        $scope.clearAddApplicationError = function () {
            $scope.addApplicationError = null;
        };

        $scope.order = function (column) {
            if ($scope.orderColumn === column) {
                $scope.orderDirection = !$scope.orderDirection;
            } else {
                $scope.orderColumn = column;
                $scope.orderDirection = true;
            }
        };

        $scope.orderBy = function (application) {
            if ($scope.orderColumn === 'publicId') {
                return application.publicId;
            } else {
                if (application.policyEvaluation.data.stage.stageTypeId === $scope.orderColumn) {
                    return application.policyEvaluation.time;
                }
            }
            // return max value to prevent empty values showing up as low
            return Number.MAX_VALUE;
        };
    }]);
}());