/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(){
	"use strict";

	var dashboardApp = angular.module('dashboardApp', ['ui.compat', 'ui.bootstrap', 'OrganizationModule', 'ApplicationModule'], ['$stateProvider', '$routeProvider', function ($stateProvider, $routeProvider) {
		$stateProvider.state('home', {
			url : '/',
			controller : angular.noop
		});
		$routeProvider.when('', { redirectTo : '/management' });
	}]);

	dashboardApp.controller('dashboardController', function($scope, $state) {
		function switchDashboard() {
			for (var i = 0; i < $scope.availableDashboards.length; i++) {
				if ($state.current.name.indexOf($scope.availableDashboards[i].state) !== -1) {
					$scope.selectedDashboard = $scope.availableDashboards[i];
					break;
				}
			}
		}
		
		$scope.$state = $state;
		$scope.availableDashboards = [
			{
				name: 'Dashboard',
				state: 'dashboard'
			}, {
				name: 'Management',
				state: 'management'
			}, {
				name: 'Reports',
				state: 'reports'
			}];
		
		$scope.$watch('$state.current.name', switchDashboard);
		switchDashboard();
	});
}());

(function () {
	'use strict';

	var managementModule = angular.module('ManagementModule', ['ui.compat'], ['$stateProvider', function ($stateProvider) {
		$stateProvider.state('management', {
			url : '/management',
			templateUrl : '../assets/management.html',
			controller : 'ManagementController'
		});
	}]);

	managementModule.controller('ManagementController', function($scope, $state) {
		$scope.$state = $state;

		$scope.managementPanes = [
			{
				name: 'Applications',
				state: 'management/application',
				isEnabled: true
			},
			{
				name: 'Organizations',
				state: 'management/organization',
				isEnabled: true
			},
			{
				name: 'Security',
				state: 'management/security',
				isEnabled: true
			},
			{
				name: 'Metadata',
				state: 'management/metadata',
				isEnabled: false
			}
		];
		
		for (var i = 0; i < $scope.managementPanes.length; i++) {
			var normalizedState = $scope.managementPanes[i].state.replace('/', '.');
			if ($scope.$state.current.name.indexOf(normalizedState) !== -1) {
				$scope.$state.selectedPane = $scope.managementPanes[i];
				break;
			}
		}
		
		$scope.$watch('$state.current.name', function() {
			if ($state.current.name === 'management') {
				$state.transitionTo('management.application');
			}
		});
	});
}());

(function () {
	'use strict';

	var organizationModule = angular.module('OrganizationModule', ['ui.compat', 'ManagementModule', 'Organization'], ['$stateProvider', function ($stateProvider) {
		$stateProvider.state('management.organization', {
			parent : 'management',
			url : '/organization',
			controller : 'OrganizationController',
			templateUrl : '../organization-assets/components/organization-navigator.html'
		}).state('management.organization.view', {
			parent : 'management.organization',
			url : '/{organizationId}',
			controller : 'OrganizationEditorController',
			templateUrl : '../organization-assets/components/organization-editor.html'
		});
	}]);
}());