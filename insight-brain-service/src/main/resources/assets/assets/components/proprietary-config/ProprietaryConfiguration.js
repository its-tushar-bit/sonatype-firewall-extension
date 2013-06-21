/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
    'use strict';

    function getMessage(data, status, headersFn, config) {
        if (status === 0) {
            return 'Error: Unable to contact server';
        } else {
            return 'Error: ' + status + ' ' + data;
        }
    }

    var module = angular.module('ProprietaryConfiguration', ['ListEditor']);

    module.controller('ProprietaryConfigurationController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, clmLocations) {
        var PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$'); 

        $scope.doLoad = function () {
            $http.get(clmLocations.getProprietaryConfig(), { params : { "ts" : new Date().getTime() } }).success(function (data) {
                $scope.proprietary = data;
                $scope.reset();
            }).error(function () {
                $scope.loadError = getMessage.apply(null, arguments);
            });
        };

        $scope.save = function () {
            var proprietary = angular.extend({}, $scope.proprietary, { packages : angular.copy($scope.packages) });

            $scope.saving = true;

            $http.put(clmLocations.getProprietaryConfig(), proprietary).success(function () {
                $scope.saving = false;
                $scope.proprietary = proprietary;
            }).error(function (data, status, headersFn, config) {
                $scope.saving = false;
                $scope.error = getMessage.apply(null, arguments);
            });
        };

        $scope.reset = function () {
            $scope.packages = angular.copy($scope.proprietary.packages);
        };

        $scope.setEditorError = function (error) {
            $scope.error = error;
        };

        $scope.validatePackage = function (value) {
            return PACKAGE_REGEXP.test(value);
        };

        $scope.doLoad();
    }]);
}());
