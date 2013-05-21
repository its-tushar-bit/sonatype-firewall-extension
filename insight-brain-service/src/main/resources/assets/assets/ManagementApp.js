/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(){
	"use strict";
	
	var dashboardStores = angular.module('dashboardStores', ['CLMLocation', 'ResourceModule', 'Hudson']);
	
	dashboardStores.service('applicationStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
		var applicationStore = clmResource.getStore({
			id : 'id',
			url : clmLocations.getApplicationsUrl(),
			template : { id: null, publicId: null, name: null },
			params : {
				timestamp : new Date().getTime()
			}
		});
		return applicationStore;
	}]);
	
	var dashboardApp = angular.module('dashboardApp', ['ui.compat', 'dashboardStores'], ['$stateProvider', '$routeProvider', function ($stateProvider, $routeProvider) {	
		$stateProvider.state('home', {
			url : '/',
			controller : angular.noop
		}).state('management', {
			url : '/management',
			templateUrl : '../management.html',
			controller : function($scope) {
				$scope.managementPanes = [
              		{
              			name: 'Applications',
              			state: 'management/application',
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
		}).state('management.application', {
			parent : 'management',
			url : '/application',
			controller : 'applicationController',
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

	
	dashboardApp.controller('applicationController', function($scope, $state, $q, applicationStore) {
		$scope.$state = $state;
		
		applicationStore.get().then(function(applications) {
			$scope.applications = applications;
		}, function (error) {
			alert(error.data);
		});
	});
}());