/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window */
(function () {
    'use strict';

    var managementModule = angular.module('Management', ['AngularCommon', 'Hudson', 'CLMLocation']);

    managementModule.controller('ManagementController', ['$scope', '$location', '$http', 'hudson', 'CLMLocations', function ($scope, $location, $http, hudson, clmLocations) {
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

        $http.get(clmLocations.getActionStageUrl(), {
            params: { timestamp: new Date().getTime() }
        }).success(function (data) {
            $scope.stages = data;
        }).error($scope.showServerError);

        $http.get(clmLocations.getApplicationsUrl(), {
            params: { timestamp: new Date().getTime() }
        }).success(function (data) {
            $scope.applications = data;
        }).error($scope.showServerError);

        $scope.getApplicationNames = function () {
            var names = [];
            if ($scope.applications) {
                for (var i = 0; i < $scope.applications.length; i++) {
                    names.push($scope.applications[i].publicId);
                }
            }
            return names;
        };

        $scope.registerNewApplication = function () {
            $('#addApplicationModal').modal('show');
        };

        $scope.addApplication = function () {
            if (!$scope.applicationPublicId) {
                $scope.addApplicationError = 'Please enter a value for the Application Id';
            }
            hudson.post(clmLocations.getApplicationsUrl(), $scope.applicationPublicId).success(function (application) {
                $scope.applications.push(application);
                $scope.clearAddApplicationError();
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
            } else if ($scope.orderColumn === application.policyEvaluation.stage.stageTypeId) {
                return application.policyEvaluation.time;
            }
            // return max value to prevent empty values showing up as low
            return Number.MAX_VALUE;
        };

        $scope.encodeURIComponent = window.encodeURIComponent;
        }
    }]);

    managementModule.filter('filterReportColumns', function () {
        return function (items) {
            var arrayToReturn = [];
            if (items) {
                var validReportColumns = ['Build', 'Stage Release', 'Release'];
                for (var i = 0; i < items.length; i++) {
                    if (validReportColumns.indexOf(items[i].name) > -1) {
                        arrayToReturn.push(items[i]);
                    }
                }
            }
            return arrayToReturn;
        };
    });
}());