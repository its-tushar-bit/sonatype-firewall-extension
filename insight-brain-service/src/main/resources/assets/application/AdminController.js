/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window, setTimeout */
(function () {
    'use strict';

    var adminModule = angular.module('Admin', [ 'AngularCommon', 'ngUpload', 'CLMLocation' ]);

    adminModule.controller('AdminController', [ '$http', '$scope', 'CLMLocations', function ($http, $scope, clmLocations) {
		function showLicense() {
			$scope.showInstall = false;
			$('#eulaModal').modal('hide');
			$('#licenseInstalledModal').modal('show');
			setTimeout($scope.reload, 5000);
		}
		
        function showError(content) {
            $scope.$apply(function() {
                $('#eulaModal').modal('hide');
                $scope.$broadcast('showError', content);
            });
        }

        $scope.reload = function () {
            if (window.location.href.indexOf('unlicensed-assets') > -1) {
                window.location.href = window.location.href.replace('unlicensed-assets', 'application-assets');
            } else {
                window.location.reload();
            }
        };

        $scope.summaryUrl = clmLocations.getLicenseSummaryUrl();
        $scope.uploadUrl = clmLocations.getLicenseUploadUrl();

        $scope.viewUninstallLicense = function () {
            $('#licenseUninstallConfirmationModal').modal('show');
        };
        
        $scope.onFileChanged = function() {
            $('#eulaModal').modal('show');
        }
        
        $scope.viewInstall = function() {
            $scope.showInstall = true;
        };
        
        $scope.fileSelected = function() {
            $('#eulaModal').modal('show');
        };
        
        $scope.eulaAccepted = function() {
            if (window.FormData) {
                var form = new FormData(angular.element('form')[0]);
                form.append('file', angular.element('input[type=file]')[0]);
                $.ajax({
                    url : $scope.uploadUrl,
                    data : form,
                    processData : false,
                    contentType : false,
                    type : 'POST',
                    success : function () {
                        $scope.$apply(function () {
                            showLicense();
                        });
                    },
                    error : function (req, status, error) {
                        showError(req.responseText);
                    }
                });
            } else {
                $('input[type=submit]').trigger('click');
            }
        };
        
        $scope.installLicense = function (content, completed) {
            if (completed) {
                if (content.length === 0) {
					showLicense();
                } else {
					setTimeout(function () {
					    showError(content);
					}, 0);
                }
            }
        };
        
        $scope.uninstallLicense = function () {
            $http['delete']($scope.uploadUrl).success(function (data) {
                $('#licenseUninstallConfirmationModal').modal('hide');
                $('#licenseUninstalledModal').modal('show');
                setTimeout($scope.reload, 5000);
            }).error(function () { $scope.$broadcast('showServerError', arguments); });
        };

        $scope.isLoaded = function () {
			return typeof $scope.license !== 'undefined';
		};

		$http.get($scope.summaryUrl).success(function (data) {
			$scope.license = data;
		}).error(function () {
			$scope.license = false;
		});
    } ]);

    adminModule.directive('onFileChange', [function () {
        return {
			restrict: 'A',
			scope : false,
			link: function(scope, elem, attr, ctrl) {
                angular.element(elem).bind('change', function (event) {
                    if (attr.onFileChange) {
                        scope.$apply(attr.onFileChange);
                    }
                });
			}
        };
    }]);
}());