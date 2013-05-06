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

    adminModule.controller('AdminController', [ '$http', '$scope', function ($http, $scope) {
        $scope.reload = function () {
            if (window.location.href.indexOf('unlicensed-assets') > -1) {
                window.location.href = window.location.href.replace('unlicensed-assets', 'application-assets');
            } else {
                window.location.reload();
            }
        };

        $scope.uploadUrl = '../rest/product/license';
        
        $scope.viewUninstallLicense = function () {
            $('#licenseUninstallConfirmationModal').modal('show');
        };
        
        $scope.viewEula = function () {
            $scope.showEula = true;
            $scope.showInstall = false;
        }
        
        $scope.acceptEula = function() {
            $scope.showEula = false;
            $scope.showInstall = true;
        }
        
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