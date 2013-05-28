/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window */
(function () {
	'use strict';

	var organizationModule = angular.module('Organization', ['ui.compat']);

	organizationModule.controller('OrganizationController', function($scope, $state) {
		$scope.$state = $state;
		
		$scope.organizations = [{name:'a', id: '1'},{name:'b', id: '2'},{name:'c', id: '3'},{name:'d', id: '4'}];
	});
}());