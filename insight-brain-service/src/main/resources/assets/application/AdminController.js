/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window, setTimeout */
(function () {
    'use strict';

    var adminModule = angular.module('Admin', [ 'AngularCommon', 'CLMLocation', 'ngUpload' ]);

    adminModule.controller('AdminController', [ '$http', '$scope', 'CLMLocations', function ($http, $scope, clmLocations) {
        $scope.reload = function () {
            if (window.location.href.indexOf('unlicensed-assets') > -1) {
                window.location.href = window.location.href.replace('unlicensed-assets', 'application-assets');
            } else {
                window.location.reload();
            }
        };

        $scope.uploadUrl = '../rest/product/license';

        $scope.viewInstallLicense = function () {
            $scope.showInstall = true;
        };
        
        $scope.viewUninstallLicense = function () {
            $('#licenseUninstallConfirmationModal').modal('show');
        };

        $scope.installLicense = function () {
            $scope.showInstall = false;
            $('#licenseInstalledModal').modal('show');
            setTimeout($scope.reload, 5000);
        };

        $scope.uninstallLicense = function () {
            $http['delete']($scope.uploadUrl).success(function (data) {
                $('#licenseUninstallConfirmationModal').modal('hide');
                $('#licenseUninstalledModal').modal('show');
                setTimeout($scope.reload, 5000);
            });
        };
    } ]);
}());