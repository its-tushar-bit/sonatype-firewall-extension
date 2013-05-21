/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(){
	"use strict";
	
	var dashboardApp = angular.module('dashboardApp', ['ui.compat'], ['$stateProvider', '$routeProvider', function ($stateProvider, $routeProvider) {	
		$stateProvider.state('home', {
			url : '/',
			controller : angular.noop
		}).state('Management', {
			url : '/management',
			templateUrl : '../management.html',
			controller : function($scope) {
				$scope.managementPanes = [
              		{
              			name: 'Applications',
              			state: 'application',
              			isEnabled: true,
              			isSelected: true
              		},
              		{
              			name: 'Organizations',
              			isEnabled: true
              		},
              		{
              			name: 'Security',
              			isEnabled: true
              		},
              		{
              			name: 'Metadata',
              			isEnabled: false
              		}];
			},
			onEnter : function($state) {
				$state.selectedDashboard = {
					name: 'Management',
					state: 'management'
				};
			}
		}).state('application', {
			parent : 'Management',
			url : '/application',
			controller : angular.noop,
			templateUrl : '../../application-assets/application.html'
		});
		$routeProvider.when('', { redirectTo : '/management' });
	}]);
	
	dashboardApp.controller('dashboardController', function($scope, $state) {
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
	});
}());