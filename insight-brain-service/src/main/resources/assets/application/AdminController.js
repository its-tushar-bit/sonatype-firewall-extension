/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window, setTimeout */
(function () {
    'use strict';

    var adminModule = angular.module('Admin', [ 'AngularCommon', 'ngUpload' ]);

    adminModule.controller('AdminController', [ '$http', '$scope', function ($http, $scope) {
		function showLicense() {
			$scope.showInstall = false;
			$('#licenseInstalledModal').modal('show');
			setTimeout($scope.reload, 5000);
		}

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
        };
        
        $scope.isLicenseInstalled = function () {
            return window.location.href.indexOf("unlicensed-assets") === -1;
        };

        $scope.acceptEula = function() {
            $scope.showEula = false;
            $scope.showInstall = true;
        };

        $scope.installLicense = function (content, completed) {
            if (completed) {
                if (content.length === 0) {
					showLicense();
                } else {
					setTimeout(function () {
						$scope.$apply(function () {
							$scope.$broadcast('showError', content);
						});
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

		$scope.doUpload = function () {
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
						$scope.$apply(function () {
							$scope.$broadcast('showError', req.responseText);
						});
					}
				});
			} else {
				$('input[type=submit]').trigger('click');
			}
		};
    } ]);

    adminModule.directive('fileRequired', ['$parse', '$timeout', function ($parse, $timeout) {
        return {
			restrict: 'A',
			scope : {
                valid : '=fileRequired'
            },
			link: function(scope, elem, attr, ctrl) {
                angular.element(elem).bind('change', function (event) {
                    scope.valid.valid = angular.element(this).val();
                    $timeout(function () {
                        // Some sort of bizarre digest bug prevents updates without this async call.
                    });
                });
                scope.valid = { valid : false };
			}
        };
    }]);
}());