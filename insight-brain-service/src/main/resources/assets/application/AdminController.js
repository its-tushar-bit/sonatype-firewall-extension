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

    adminModule.service('ApplicationId', function() {
        return {
            encoded : 'orgf0367c36c57a42f2a494ecb1ba26b7e7'
        };
    });

    adminModule.controller('AdminController', [ '$http', '$scope', 'CLMLocations', function($http, $scope, clmLocations) {
        $scope.uploadUrl = clmLocations.getLicenseUploadUrl();

        $scope.viewInstallLicense = function() {
            $scope.showInstall = true;
        };
        
        $scope.viewUninstallLicense = function() {
            $('#licenseUninstallConfirmationModal').modal('show');
        }

        $scope.installLicense = function() {
            $scope.showInstall = false;
            $('#licenseInstalledModal').modal('show');
        };
        
        $scope.uninstallLicense = function() {
            $http.delete($scope.uploadUrl).success(function (data) {
                $('#licenseUninstallConfirmationModal').modal('hide');
                $('#licenseUninstalledModal').modal('show');
            });
        }
        
        $scope.licenseInstalled = function() {
            window.location.href='/application-assets/index.html';
        }
        
        $scope.licenseUninstalled = function() {
            window.location.reload();
        }
    } ]);
}());