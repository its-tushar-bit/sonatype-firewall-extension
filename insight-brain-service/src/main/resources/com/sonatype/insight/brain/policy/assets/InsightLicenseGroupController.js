/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

(function () {
	'use strict';

	var licenseGroupModule = angular.module('LicenseGroup', []);
	
	licenseGroupModule.controller('InsightLicenseGroupController', ['$scope', function ($scope) {
		$scope.features.licenseGroup = false;
		
		$scope.viewCreateLicenseGroup = function() {
			$('#labelEditModal').modal('show');
		};
	}]);
}());