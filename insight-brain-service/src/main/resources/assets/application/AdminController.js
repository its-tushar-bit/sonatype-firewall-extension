/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp, window */
(function() {
    'use strict';

    var adminModule = angular.module('Admin', [ 'AngularCommon', 'CLMLocation', 'ngUpload' ]);

    adminModule.controller('AdminController', [ '$scope', 'CLMLocations', function($scope, clmLocations) {
        $scope.uploadUrl = clmLocations.getLicenseUploadUrl();

        $scope.viewInstallLicense = function() {
            $scope.showInstall = true;
        };

        $scope.installLicense = function() {
            $scope.showInstall = false;
            $('#licenseInstalledModal').modal('show');
        };
    } ]);
}());